plugins {
    `kotlin-dsl`
    `maven-publish`
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
        create("javadocMigration").apply {
            id = "fr.smolder.javadoc.migration"
            implementationClass = "fr.smolder.javadoc.JavadocMigrationPlugin"
        }
    }
}

publishing {
    repositories {
        maven {
            name = "Smolder"
            url = uri("https://repo.smolder.fr/public/")
            credentials {
                username = project.findProperty("smolderUsername") as String?
                password = project.findProperty("smolderPassword") as String?
            }
        }
    }
}
