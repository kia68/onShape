package de.optadata.odil.onshape

import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.TestPropertySource
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Gemeinsamer Postgres-Testcontainer fuer alle @SpringBootTest-Klassen.
 *
 * Bewusst OHNE @Testcontainers/@Container: dieses JUnit5-Lifecycle stoppt den Container
 * am Ende JEDER Testklasse. Spring's Testkontext-Cache haelt den (bereits migrierten)
 * ApplicationContext aber klassenuebergreifend am Leben ("Singleton Container"-Pattern) —
 * die zweite Testklasse haette sonst einen Kontext mit DataSource auf einen laengst
 * gestoppten Container. Stattdessen wird der Container einmalig beim Laden dieser Klasse
 * gestartet und lebt bis zum Prozessende (Aufraeumen uebernimmt Testcontainers' Ryuk).
 *
 * NFR-08: das produktive Rate-Limit fuer die Auth-Endpunkte (application.properties) ist bewusst
 * niedrig genug, um echten Brute-Force zu bremsen -- fuer die Suite hier faellt aber der GESAMTE
 * Registrierungs-Traffic vieler Testklassen in EINEN gecachten Kontext (Spring's Test-Context-
 * Cache haelt identisch konfigurierte @SpringBootTest-Klassen in derselben Instanz), daher hier
 * grosszuegig ueberschrieben. [de.optadata.odil.onshape.security.AuthRateLimitFilterIntegrationTest]
 * ueberschreibt das wiederum eigens auf ein sehr niedriges Limit (eigener, dadurch isolierter
 * Kontext) um das Verhalten selbst zu testen.
 */
@TestPropertySource(properties = ["app.security.auth-rate-limit.max-requests=100000"])
abstract class AbstractIntegrationTest {

    companion object {
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16").apply { start() }
    }
}
