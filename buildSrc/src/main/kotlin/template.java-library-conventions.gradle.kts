import java.net.URI

plugins {
    `java-library`
    `maven-publish`
    id("template.java-conventions")
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = project.name
            url = URI.create(System.getenv().getOrDefault("REPOSITORY_URL", ""))

            credentials {
                username = System.getenv("REPOSITORY_USERNAME")
                password = System.getenv("REPOSITORY_PASSWORD")
            }
        }
    }
}