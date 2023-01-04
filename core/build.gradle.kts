plugins {
    id("template.java-library-conventions")
}

dependencies {
    api(libs.vector.core)
    api(libs.toolkit.collection)
    api(libs.toolkit.function)
    api(libs.flare)
    api(libs.flare.fastutil)
}