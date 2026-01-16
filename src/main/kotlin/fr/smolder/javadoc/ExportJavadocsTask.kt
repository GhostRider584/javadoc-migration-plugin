package fr.smolder.javadoc

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

abstract class ExportJavadocsTask : DefaultTask() {

    @get:InputDirectory
    abstract val inputSources: DirectoryProperty

    @get:OutputFile
    abstract val exportFile: RegularFileProperty

    @TaskAction
    fun export() {
        val inputDir = inputSources.get().asFile
        val outputFile = exportFile.get().asFile

        if (!inputDir.exists()) throw IllegalStateException("Input sources not found: $inputDir")

        println("Extracting Javadocs from $inputDir...")
        val extractor = JavadocExtractor()
        val result = extractor.extract(inputDir)

        println("Found ${result.docsEntries.size} documented elements.")
        println("Exporting to ${outputFile.name}...")

        DocsSerializer.export(result.docsEntries, outputFile, "source-export")
        
        println("Export complete: ${outputFile.absolutePath}")
    }
}
