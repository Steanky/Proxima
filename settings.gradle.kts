rootProject.name = "template"

val localSettings = file("local.settings.gradle.kts")
if (localSettings.exists()) {
    // Local settings can be used for environment variables, secrets, or other things that shouldn't be on VCS
    apply(localSettings)
}

// New modules are added by appending them to the sequenceOf function
sequenceOf("java").forEach {
    val projectDirectory = file(it)
    include(":${rootProject.name}-$it")
    project(":${rootProject.name}-$it").projectDir = projectDirectory

    // Automatically create .gitignore if it doesn't exist, make sure /build/ is ignored, and add it to git
    val gitignore = projectDirectory.resolve(".gitignore")
    if (gitignore.createNewFile()) {
        gitignore.writeText("/build/")
        Runtime.getRuntime().exec("git add ${gitignore.absoluteFile}")
    }
}