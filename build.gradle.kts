plugins {
    `kotlin-dsl`
}

group = "fr.smolder.javadoc"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.javaparser:javaparser-core:3.25.10")
    implementation("com.google.code.gson:gson:2.10.1")
}

gradlePlugin {
    plugins {
        create("javadocMigration") {
            id = "fr.smolder.javadoc.migration"
            implementationClass = "fr.smolder.javadoc.JavadocMigrationPlugin"
        }
    }
}
