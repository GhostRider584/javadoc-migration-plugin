package fr.smolder.javadoc

data class DocsFile(
    val metadata: DocsMetadata,
    val entries: List<DocsEntry>
)

data class DocsMetadata(
    val formatVersion: String = "1.0",
    val generatedFrom: String = "unknown",
    val timestamp: Long = System.currentTimeMillis()
)

data class DocsEntry(
    val hash: String,
    val type: String,
    val signature: String,
    val javadoc: String
)

data class MigrationReport(
    val timestamp: String,
    val stats: MigrationStats,
    val orphaned: List<DocsEntry>
)
