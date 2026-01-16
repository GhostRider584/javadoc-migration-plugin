package fr.smolder.javadoc

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.process.ExecOperations
import java.io.File
import java.time.Instant
import javax.inject.Inject

abstract class MigrateJavadocsTask @Inject constructor(
    private val execOperations: ExecOperations,
    private val fsOperations: FileSystemOperations,
    private val archiveOperations: ArchiveOperations
) : DefaultTask() {

    @get:InputDirectory
    @get:Optional
    abstract val oldSources: DirectoryProperty

    @get:InputFile
    @get:Optional
    abstract val docsFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val docsUrl: Property<String>

    @get:InputFile
    abstract val newJar: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @get:OutputFile
    @get:Optional
    abstract val reportFile: RegularFileProperty

    @get:InputFiles
    abstract val decompilerClasspath: ConfigurableFileCollection

    @get:Input
    abstract val decompileFilter: ListProperty<String>

    @get:Input
    abstract val changedMarker: Property<String>

    @get:Input
    abstract val newMarker: Property<String>

    @TaskAction
    fun migrate() {
        // prepare
        val (javadocs, knownSignatures, docsEntries) = loadJavadocs()
        
        // decompile
        val newJarFile = newJar.get().asFile
        val outputDirectory = outputDir.get().asFile
        val tempDir = File(project.layout.buildDirectory.get().asFile, "tmp/javadoc-migration")
        
        if (!newJarFile.exists()) throw IllegalStateException("New jar not found: $newJarFile")
        
        println("Step 2: Decompiling new JAR...")
        fsOperations.delete { delete(tempDir) }
        tempDir.mkdirs()

        val classesDir = File(tempDir, "classes")
        val sourcesDir = File(tempDir, "sources")
        classesDir.mkdirs()
        sourcesDir.mkdirs()

        fsOperations.copy {
            from(archiveOperations.zipTree(newJarFile))
            into(classesDir)
            decompileFilter.get().forEach { include(it) }
        }

        execOperations.javaexec {
            classpath(decompilerClasspath)
            mainClass.set("org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler")
            args(
                "-dgs=1", "-rsy=1", "-rbr=1", "-lit=1", "-jvn=1", "-log=ERROR",
                classesDir.absolutePath,
                sourcesDir.absolutePath
            )
            maxHeapSize = "4G"
        }

        val createdJar = sourcesDir.listFiles()?.firstOrNull { it.name.endsWith(".jar") }
        val rawSources = if (createdJar != null) {
            val unzippedDir = File(tempDir, "unzipped_sources")
            fsOperations.copy {
                from(archiveOperations.zipTree(createdJar))
                into(unzippedDir)
            }
            unzippedDir
        } else {
            sourcesDir
        }

        // inject
        println("Step 3: Injecting Javadocs...")
        val injector = JavadocInjector(
            javadocs,
            knownSignatures,
            changedMarker.get(),
            newMarker.get()
        )
        injector.inject(rawSources, outputDirectory)
        
        // find orphaned docs (docs we have but couldn't apply)
        val orphanedEntries = docsEntries.filter { !injector.usedSignatures.contains(it.hash) }
        injector.stats.orphaned = orphanedEntries.size
        
        if (reportFile.isPresent) {
            val report = MigrationReport(
                timestamp = Instant.now().toString(),
                stats = injector.stats,
                orphaned = orphanedEntries
            )
            DocsSerializer.writeReport(report, reportFile.get().asFile)
        }

        println("--------------------------------------------------")
        println("Migration complete!")
        println("  Preserved: ${injector.stats.preserved}")
        println("  New items: ${injector.stats.newItems}")
        println("  Orphaned: ${injector.stats.orphaned}")
        println("  Output: ${outputDirectory.absolutePath}")
        if (reportFile.isPresent) {
            println("  Report: ${reportFile.get().asFile.absolutePath}")
        }
        println("--------------------------------------------------")
    }

    private fun loadJavadocs(): Triple<Map<String, String>, Set<String>, List<DocsEntry>> {
        if (docsFile.isPresent || docsUrl.isPresent) {
             val docsFileObject = if (docsFile.isPresent) {
                 println("Loading Javadocs from file: ${docsFile.get().asFile}")
                 DocsSerializer.import(docsFile.get().asFile)
             } else {
                 println("Loading Javadocs from URL: ${docsUrl.get()}")
                 DocsSerializer.importFromUrl(docsUrl.get())
             }
             
             val map = docsFileObject.entries.associate { it.hash to it.javadoc }
             val signatures = docsFileObject.entries.map { it.hash }.toSet()
             return Triple(map, signatures, docsFileObject.entries)
        } 
        else if (oldSources.isPresent) {
            val dir = oldSources.get().asFile
            if (!dir.exists()) throw IllegalStateException("Old sources dir not found: $dir")
            println("Extracting Javadocs from ${dir.name}...")
            val result = JavadocExtractor().extract(dir)
            println("Found ${result.docsEntries.size} Javadoc comments.")
            val map = result.docsEntries.associate { it.hash to it.javadoc }
            return Triple(map, result.allHashes, result.docsEntries)
        }
        else {
             throw IllegalStateException("No Javadoc source provided! properties 'docsFile', 'docsUrl' or 'oldSources' must be set.")
        }
    }
}
