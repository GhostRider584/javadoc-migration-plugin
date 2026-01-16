package fr.smolder.javadoc

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.ListProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.DirectoryProperty
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

interface JavadocMigrationExtension {
    val oldSources: DirectoryProperty
    val newJar: RegularFileProperty
    val outputDir: DirectoryProperty

    val exportFile: RegularFileProperty
    val docsFile: RegularFileProperty
    val docsUrl: Property<String>

    val reportFile: RegularFileProperty

    val decompileFilter: ListProperty<String>
    val changedMarker: Property<String>
    val newMarker: Property<String>
}

class JavadocMigrationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create<JavadocMigrationExtension>("javadocMigration")

        extension.changedMarker.convention("// @NeedsReview")
        extension.newMarker.convention("// @Undocumented")
        extension.decompileFilter.convention(listOf("**"))
        extension.exportFile.convention(project.layout.buildDirectory.file("javadocs-export.json"))
        extension.reportFile.convention(project.layout.buildDirectory.file("migration-report.json"))

        val decompilerConfig = project.configurations.create("javadocDecompiler")
        decompilerConfig.isCanBeConsumed = false
        decompilerConfig.isCanBeResolved = true
        
        project.afterEvaluate {
            if (decompilerConfig.dependencies.isEmpty()) {
                project.dependencies.add("javadocDecompiler", "org.vineflower:vineflower:1.11.2")
            }
        }

        project.tasks.register<ExportJavadocsTask>("exportJavadocs") {
            group = "javadoc"
            description = "Exports Javadocs from sources to JSON file"
            inputSources.set(extension.oldSources)
            exportFile.set(extension.exportFile)
        }

        project.tasks.register<MigrateJavadocsTask>("migrateJavadocs") {
            group = "javadoc"
            description = "Migrates Javadocs from old sources or JSON file to new decompiled sources"
            
            oldSources.set(extension.oldSources)
            docsFile.set(extension.docsFile)
            docsUrl.set(extension.docsUrl)
            newJar.set(extension.newJar)
            outputDir.set(extension.outputDir)
            reportFile.set(extension.reportFile)
            
            decompileFilter.set(extension.decompileFilter)
            changedMarker.set(extension.changedMarker)
            newMarker.set(extension.newMarker)
            decompilerClasspath.from(decompilerConfig)
        }
    }
}
