package de.optadata.odil.onshape

import org.springframework.boot.testcontainers.service.connection.ServiceConnection
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
 */
abstract class AbstractIntegrationTest {

    companion object {
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16").apply { start() }
    }
}
