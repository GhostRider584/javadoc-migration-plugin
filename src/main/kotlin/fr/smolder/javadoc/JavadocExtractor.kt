package fr.smolder.javadoc

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.Collections

data class ExtractionResult(
    val docsEntries: List<DocsEntry>,
    val allHashes: Set<String>
)

class JavadocExtractor {

    private val docsEntries = Collections.synchronizedList(ArrayList<DocsEntry>())
    private val allHashes = ConcurrentHashMap.newKeySet<String>()

    fun extract(sourceDir: File): ExtractionResult {
        if (!sourceDir.exists()) return ExtractionResult(emptyList(), emptySet())

        sourceDir.walkTopDown().filter { it.extension == "java" }.forEach { file ->
            try {
                processFile(file)
            } catch (e: Exception) {
                println("Warning: Failed to process ${file.name}: ${e.message}")
            }
        }
        return ExtractionResult(docsEntries.toList(), allHashes.toSet())
    }

    private fun processFile(file: File) {
        val cu = SignatureExtractor.parse(file)
        val packageName = cu.packageDeclaration.map { it.nameAsString }.orElse("")

        cu.findAll(ClassOrInterfaceDeclaration::class.java).forEach { classDecl ->
            val className = if (packageName.isNotEmpty()) "$packageName.${classDecl.nameAsString}" else classDecl.nameAsString
            
            val sig = SignatureExtractor.getClassSignature(className, classDecl)
            register(sig, classDecl.javadoc.map { it.toText() }.orElse(null))

            classDecl.findAll(ConstructorDeclaration::class.java).forEach { ctor ->
                val ctorSig = SignatureExtractor.getConstructorSignature(className, ctor)
                register(ctorSig, ctor.javadoc.map { it.toText() }.orElse(null))
            }

            classDecl.findAll(MethodDeclaration::class.java).forEach { method ->
                val methodSig = SignatureExtractor.getMethodSignature(className, method)
                register(methodSig, method.javadoc.map { it.toText() }.orElse(null))
            }

            classDecl.findAll(FieldDeclaration::class.java).forEach { field ->
                val fieldSig = SignatureExtractor.getFieldSignature(className, field)
                register(fieldSig, field.javadoc.map { it.toText() }.orElse(null))
            }
        }
    }

    private fun register(sig: Signature, javadoc: String?) {
        allHashes.add(sig.hash)
        if (javadoc != null) {
            docsEntries.add(DocsEntry(
                hash = sig.hash,
                type = sig.type,
                signature = sig.raw,
                javadoc = javadoc
            ))
        }
    }
}
