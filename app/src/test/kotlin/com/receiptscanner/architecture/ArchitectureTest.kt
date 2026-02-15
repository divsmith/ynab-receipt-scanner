package com.receiptscanner.architecture

import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * Architecture validation tests to enforce Clean Architecture principles.
 * 
 * Phase 1: Creates package structure with placeholder files
 * Phase 2+: Adds actual implementation code
 * 
 * Rules:
 * - Domain layer (domain/) must not depend on Data or Presentation layers
 * - Data layer (data/) must not depend on Presentation layer
 * - Presentation layer can depend on Domain and Data
 */
class ArchitectureTest {

    private val projectRoot = File(System.getProperty("user.dir") ?: ".")
    private val sourceRoot = File(projectRoot, "src/main/kotlin/com/receiptscanner")

    @Test
    fun `domain layer should not depend on data layer`() {
        val domainDir = File(sourceRoot, "domain")
        assertTrue("Domain directory must exist", domainDir.exists())
        
        val domainFiles = domainDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
        
        // Phase 1: Structure exists, may have placeholder files only
        // Phase 2+: Will have actual implementation files
        if (domainFiles.isEmpty()) {
            return // No files to check yet
        }
        
        domainFiles.forEach { file ->
            val content = file.readText()
            assertFalse(
                "Domain layer file ${file.name} should not import from data layer",
                content.contains("import com.receiptscanner.data")
            )
        }
    }

    @Test
    fun `domain layer should not depend on presentation layer`() {
        val domainDir = File(sourceRoot, "domain")
        assertTrue("Domain directory must exist", domainDir.exists())
        
        val domainFiles = domainDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
        
        domainFiles.forEach { file ->
            val content = file.readText()
            assertFalse(
                "Domain layer file ${file.name} should not import from presentation layer",
                content.contains("import com.receiptscanner.presentation")
            )
        }
    }

    @Test
    fun `data layer should not depend on presentation layer`() {
        val dataDir = File(sourceRoot, "data")
        assertTrue("Data directory must exist", dataDir.exists())
        
        val dataFiles = dataDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
        
        // Phase 1: Structure exists, may have placeholder files only
        // Phase 2+: Will have actual implementation files
        if (dataFiles.isEmpty()) {
            return // No files to check yet
        }
        
        dataFiles.forEach { file ->
            val content = file.readText()
            assertFalse(
                "Data layer file ${file.name} should not import from presentation layer",
                content.contains("import com.receiptscanner.presentation")
            )
        }
    }

    @Test
    fun `presentation layer directory should exist`() {
        val presentationDir = File(sourceRoot, "presentation")
        assertTrue("Presentation directory must exist", presentationDir.exists())
    }

    @Test
    fun `all three layers should be present`() {
        assertTrue("Domain layer must exist", File(sourceRoot, "domain").exists())
        assertTrue("Data layer must exist", File(sourceRoot, "data").exists())
        assertTrue("Presentation layer must exist", File(sourceRoot, "presentation").exists())
    }
}
