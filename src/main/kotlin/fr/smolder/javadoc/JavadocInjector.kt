package fr.smolder.javadoc

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.comments.LineComment
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.Collections

data class MigrationStats(
    var preserved: Int = 0,
    var newItems: Int = 0,
    var orphaned: Int = 0
)

class JavadocInjector(
    private val oldJavadocs: Map<String, String>,
    private val oldSignatures: Set<String>,
    private val changedMarker: String,
    private val newMarker: String
) {

    val stats = MigrationStats()
    val usedSignatures = Collections.synchronizedSet(HashSet<String>())

    fun inject(sourceDir: File, outputDir: File) {
        if (!sourceDir.exists()) return

        sourceDir.walkTopDown().filter { it.extension == "java" }.forEach { file ->
            val relativePath = file.relativeTo(sourceDir).path
            val outputFile = File(outputDir, relativePath)
            
            try {
                processFile(file, outputFile)
            } catch (e: Exception) {
                println("Error processing ${file.name}: ${e.message}")
                file.copyTo(outputFile, overwrite = true)
            }
        }
    }

    private fun processFile(inputFile: File, outputFile: File) {
        val cu = SignatureExtractor.parse(inputFile)
        val packageName = cu.packageDeclaration.map { it.nameAsString }.orElse("")
        
        cu.findAll(ClassOrInterfaceDeclaration::class.java).forEach { classDecl ->
            val className = if (packageName.isNotEmpty()) "$packageName.${classDecl.nameAsString}" else classDecl.nameAsString
            
            val classSig = SignatureExtractor.getClassSignature(className, classDecl)
            injectOrMark(classDecl, classSig.hash)

            classDecl.findAll(ConstructorDeclaration::class.java).forEach { ctor ->
                val sig = SignatureExtractor.getConstructorSignature(className, ctor)
                injectOrMark(ctor, sig.hash)
            }

            classDecl.findAll(MethodDeclaration::class.java).forEach { method ->
                val sig = SignatureExtractor.getMethodSignature(className, method)
                injectOrMark(method, sig.hash)
            }

            classDecl.findAll(FieldDeclaration::class.java).forEach { field ->
                val sig = SignatureExtractor.getFieldSignature(className, field)
                injectOrMark(field, sig.hash)
            }
        }

        outputFile.parentFile.mkdirs()
        outputFile.writeText(cu.toString())
    }

    private fun injectOrMark(node: com.github.javaparser.ast.nodeTypes.NodeWithJavadoc<*>, hash: String) {
        when {
            oldJavadocs.containsKey(hash) -> {
                // exact match - inject preserved Javadoc
                node.setJavadocComment(oldJavadocs[hash]!!)
                usedSignatures.add(hash)
                stats.preserved++
            }
            oldSignatures.contains(hash) -> {
                // signature existed but had no javadoc - leave as is
                usedSignatures.add(hash)
            }
            else -> {
                // new item (signature not in old sources at all)
                if (newMarker.isNotEmpty() && !node.javadoc.isPresent) {
                    val markerText = newMarker.removePrefix("//").trim()
                    node.setComment(LineComment(markerText))
                    stats.newItems++
                }
            }
        }
    }
}
