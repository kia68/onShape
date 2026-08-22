package de.optadata.odil.onshape.progress

import de.optadata.odil.onshape.security.currentUserId
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** FR-137. */
@RestController
@RequestMapping("/api/export")
class ExportController(private val exportService: ExportService) {

    @GetMapping("/json")
    fun json(authentication: Authentication): ResponseEntity<ExportData> {
        val data = exportService.exportData(authentication.currentUserId())
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("onshape-export.json").build().toString())
            .body(data)
    }

    @GetMapping("/csv")
    fun csv(authentication: Authentication): ResponseEntity<ByteArray> {
        val zip = exportService.exportCsvZip(authentication.currentUserId())
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/zip"))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("onshape-export-csv.zip").build().toString())
            .body(zip)
    }
}
