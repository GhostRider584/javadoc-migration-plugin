# Javadoc Migration Plugin
![Java](https://img.shields.io/badge/Java-orange?style=for-the-badge&logo=openjdk&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![Version](https://img.shields.io/badge/version-0.0.1-248cd6?labelColor=&style=for-the-badge)
![License: MIT](https://img.shields.io/badge/License-MIT-7267db.svg?style=for-the-badge)

A Gradle plugin to preserve and distribute Javadocs across decompiled source updates.

## Features
- **Signature-based matching** — Reliably transfers docs even when code is refactored
- **Portable export format** — Share documentation as JSON without distributing code
- **Orphan detection** — Identifies docs for removed methods/classes
- **New item markers** — Automatically flags undocumented code

## Usage

### 1. Configure Plugin Repository
Since this plugin is hosted on the Smolder repository, you must tell Gradle where to find it.
Add this to your `settings.gradle.kts` file:

```kotlin
pluginManagement {
    repositories {
        maven("https://repo.smolder.fr/public/")
        gradlePluginPortal()
    }
}
```

### 2. Apply the Plugin
In your `build.gradle.kts`:

```kotlin
plugins {
    id("fr.smolder.javadoc.migration") version "0.0.1"
}
```

### 3. Configure the Migration
```kotlin
javadocMigration {
    // Your documented source directory
    oldSources.set(file("documented-sources"))
    
    // The JAR to decompile and inject docs into
    newJar.set(file("libs/server.jar"))
    
    // Output directory for migrated sources
    outputDir.set(layout.buildDirectory.dir("migrated-sources"))
    
    // Filter which packages to process (glob patterns)
    decompileFilter.set(listOf("com/example/**"))
}
```

### Alternative: Import from JSON
For consuming documentation exported by others:

```kotlin
javadocMigration {
    // From a local file
    docsFile.set(file("libs/docs.json"))
    
    // OR from a URL
    // docsUrl.set("https://github.com/org/repo/releases/download/v1.0/docs.json")
    
    newJar.set(file("libs/server.jar"))
    outputDir.set(layout.buildDirectory.dir("documented-sources"))
    decompileFilter.set(listOf("com/example/**"))
}
```

### Tasks
- `./gradlew migrateJavadocs` — Decompiles JAR and injects preserved documentation
- `./gradlew exportJavadocs` — Exports documentation to portable JSON format

## Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `oldSources` | — | Directory containing documented Java sources |
| `newJar` | — | JAR file to decompile and inject docs into |
| `outputDir` | — | Output directory for migrated sources |
| `docsFile` | — | JSON file to import documentation from |
| `docsUrl` | — | URL to download documentation JSON |
| `exportFile` | `build/javadocs-export.json` | Output file for exported docs |
| `reportFile` | `build/migration-report.json` | Migration statistics report |
| `decompileFilter` | `["**"]` | Glob patterns for classes to include |
| `changedMarker` | `// @NeedsReview` | Comment for changed signatures |
| `newMarker` | `// @Undocumented` | Comment for new items |

## Migration Report

After migration, check `build/migration-report.json` for statistics:

```json
{
  "stats": {
    "preserved": 142,
    "newItems": 23,
    "orphaned": 5
  }
}
```

- **preserved** — Docs successfully applied
- **newItems** — New code marked with `@Undocumented`  
- **orphaned** — Docs that couldn't be applied (removed methods)

## Contributing
Contributions are welcome! Please feel free to submit issues, feature requests, or pull requests.

## License
This project is licensed under the MIT License.
