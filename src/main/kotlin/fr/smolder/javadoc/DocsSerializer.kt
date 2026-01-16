package fr.smolder.javadoc

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.net.URL

object DocsSerializer {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun export(entries: List<DocsEntry>, file: File, sourceName: String) {
        val metadata = DocsMetadata(generatedFrom = sourceName)
        val docFile = DocsFile(metadata, entries)
        
        file.parentFile.mkdirs()
        file.writeText(gson.toJson(docFile))
    }

    fun import(file: File): DocsFile {
        if (!file.exists()) throw IllegalArgumentException("Docs file not found: ${file.absolutePath}")
        return gson.fromJson(file.readText(), DocsFile::class.java)
    }

    fun importFromUrl(url: String): DocsFile {
        try {
            val content = URL(url).readText()
            return gson.fromJson(content, DocsFile::class.java)
        } catch (e: Exception) {
            throw RuntimeException("Failed to download docs from $url", e)
        }
    }
    
    fun writeReport(report: MigrationReport, file: File) {
        file.parentFile.mkdirs()
        file.writeText(gson.toJson(report))
    }
}
