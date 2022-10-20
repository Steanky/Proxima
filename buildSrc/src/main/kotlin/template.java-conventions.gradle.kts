plugins {
    java
}

group = "com.github.steanky"
version = "0.1.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()
}

val catalogs = extensions.getByType<VersionCatalogsExtension>()
pluginManager.withPlugin("java") {
    val libs = catalogs.named("libs")

    dependencies.addProvider("compileOnly", libs.findLibrary("jetbrains.annotations").get())
    dependencies.addProvider("testCompileOnly", libs.findLibrary("jetbrains.annotations").get())

    dependencies.addProvider("testImplementation", libs.findLibrary("junit.jupiter.api").get())
    dependencies.addProvider("testImplementation", libs.findLibrary("mockito.junit.jupiter").get())

    dependencies.addProvider("testRuntimeOnly", libs.findLibrary("junit.jupiter.engine").get())
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    (options as StandardJavadocDocletOptions).tags(
        "apiNote:a:API Note:",
        "implSpec:a:Implementation Requirements:",
        "implNote:a:Implementation Note:"
    )
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<Jar> {
    val currentDir = project.projectDir
    val currentLicense = currentDir.resolve("./LICENSE")

    if (currentLicense.exists()) {
        // Local licenses override the one defined in the project directory
        from(currentDir) {
            include("LICENSE")
        }
    } else {
        from(rootProject.projectDir) {
            include("LICENSE")
        }
    }
}