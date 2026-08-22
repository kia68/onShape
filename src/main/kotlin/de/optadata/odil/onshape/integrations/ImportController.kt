package de.optadata.odil.onshape.integrations

import de.optadata.odil.onshape.security.currentUserId
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/** FR-153. */
@RestController
@RequestMapping("/api/import")
class ImportController(private val importService: ImportService) {

    @PostMapping("/hevy")
    fun hevy(authentication: Authentication, @RequestParam("file") file: MultipartFile): ResponseEntity<Any> =
        importOrBadRequest(file) { importService.importHevy(authentication.currentUserId(), it) }

    @PostMapping("/strong")
    fun strong(authentication: Authentication, @RequestParam("file") file: MultipartFile): ResponseEntity<Any> =
        importOrBadRequest(file) { importService.importStrong(authentication.currentUserId(), it) }

    private fun importOrBadRequest(file: MultipartFile, run: (String) -> ImportSummary): ResponseEntity<Any> {
        if (file.isEmpty) return ResponseEntity.badRequest().body(mapOf("error" to "file is empty"))
        val text = String(file.bytes, Charsets.UTF_8)
        return ResponseEntity.ok(run(text))
    }
}
