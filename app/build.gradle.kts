import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.foodmaker.pizzamaker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.foodmaker.pizzamaker"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
    }

    sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/pizzaAssets"))

    buildTypes {
        release { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

val generatedPizzaAssetsDir = layout.buildDirectory.dir("generated/pizzaAssets")
val preparePizzaAssets = tasks.register("preparePizzaAssets") {
    val chunks = fileTree("src/main/assetpack") { include("part*.b64") }
    inputs.files(chunks)
    outputs.file(generatedPizzaAssetsDir.map { it.file("pizzamaker_assets.zip") })
    doLast {
        val encoded = chunks.files.sortedBy { it.name }.joinToString("") { it.readText() }
        val output = generatedPizzaAssetsDir.get().file("pizzamaker_assets.zip").asFile
        output.parentFile.mkdirs()
        output.writeBytes(Base64.getDecoder().decode(encoded))
    }
}

tasks.named("preBuild").configure { dependsOn(preparePizzaAssets) }
