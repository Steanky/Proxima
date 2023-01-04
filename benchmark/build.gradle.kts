plugins {
    id("template.java-library-conventions")
    id("me.champeau.jmh") version "0.6.8"
}

dependencies {
    implementation(project(":proxima-core"))
    api(libs.vector.core)
    api(libs.toolkit.collection)
    api(libs.toolkit.function)
    api(libs.jmh.core)
    api(libs.jmh.generator.annprocess)

    jmhAnnotationProcessor(libs.jmh.generator.annprocess)
}

jmh {
    jvmArgsPrepend.addAll("-Xms5G", "-Xmx5G")
}