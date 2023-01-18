plugins {
    id("template.java-conventions")
    id("me.champeau.jmh") version "0.6.8"
}

dependencies {
    implementation(project(":proxima-core"))
    implementation(libs.vector.core)
    implementation(libs.toolkit.collection)
    implementation(libs.toolkit.function)
    implementation(libs.jmh.core)
    implementation(libs.jmh.generator.annprocess)

    jmhAnnotationProcessor(libs.jmh.generator.annprocess)
}

jmh {
    jvmArgsPrepend.addAll("-Xms5G", "-Xmx5G")
}