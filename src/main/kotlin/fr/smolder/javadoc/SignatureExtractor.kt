package fr.smolder.javadoc

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import java.security.MessageDigest
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets

data class Signature(
    val hash: String,
    val raw: String,
    val type: String
)

object SignatureExtractor {

    private val parser = JavaParser()

    fun parse(file: File): CompilationUnit {
        return parser.parse(file).result.orElseThrow { 
            RuntimeException("Failed to parse file: ${file.absolutePath}") 
        }
    }
    
    fun parse(inputStream: InputStream): CompilationUnit {
        return parser.parse(inputStream).result.orElseThrow {
            RuntimeException("Failed to parse input stream")
        }
    }

    fun getClassSignature(className: String, classDecl: ClassOrInterfaceDeclaration): Signature {
        val extended = classDecl.extendedTypes.joinToString(",") { it.nameAsString }
        val implemented = classDecl.implementedTypes.map { it.nameAsString }.sorted().joinToString(",")
        val raw = "CLASS:$className:EXTENDS:$extended:IMPLEMENTS:$implemented"
        return Signature(hash(raw), raw, "CLASS")
    }

    fun getMethodSignature(className: String, method: MethodDeclaration): Signature {
        val returnType = method.type.asString()
        val name = method.nameAsString
        val params = method.parameters.joinToString(",") { it.type.asString() }
        val raw = "METHOD:$className:$returnType $name($params)"
        return Signature(hash(raw), raw, "METHOD")
    }

    fun getConstructorSignature(className: String, constructor: ConstructorDeclaration): Signature {
        val name = constructor.nameAsString
        val params = constructor.parameters.joinToString(",") { it.type.asString() }
        val raw = "CONSTRUCTOR:$className:$name($params)"
        return Signature(hash(raw), raw, "CONSTRUCTOR")
    }
    
    fun getFieldSignature(className: String, field: FieldDeclaration): Signature {
        val type = field.elementType.asString()
        val modifiers = field.modifiers.joinToString(",") { it.keyword.asString() }
        val count = field.variables.size
        val raw = "FIELD:$className:$modifiers $type x$count"
        return Signature(hash(raw), raw, "FIELD")
    }

    private fun hash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
