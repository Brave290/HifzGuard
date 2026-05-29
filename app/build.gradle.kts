import java.util.Base64
import java.net.URL
import java.net.HttpURLConnection

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.aistudio.hifzguard.hzgdf"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Ensure debug.keystore is decoded from its base64 file for signing the debug APK properly
  val base64File = file("${rootDir}/debug.keystore.base64")
  val keystoreFile = file("${rootDir}/debug.keystore")
  if (base64File.exists() && !keystoreFile.exists()) {
      try {
          val sanitizedBase64 = base64File.readText().replace("\\s".toRegex(), "")
          val decodedBytes = Base64.getDecoder().decode(sanitizedBase64)
          keystoreFile.writeBytes(decodedBytes)
          println("Successfully decoded debug.keystore from debug.keystore.base64!")
      } catch (e: Exception) {
          throw GradleException("FAILED TO DECODE DEBUGA KEYSTORE: ${e.message}", e)
      }
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
      enableV1Signing = true
      enableV2Signing = true
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      isShrinkResources = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation("androidx.security:security-crypto:1.0.0")
  // implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  // implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}

val buildDirObj = layout.buildDirectory.asFile.get()
val rootDirObj = rootDir

tasks.register("copyApkToRoot") {
    val srcFile = File(buildDirObj, "outputs/apk/debug/app-debug.apk")
    val destFile = File(rootDirObj, "app-debug.apk")
    val distFolder = File(rootDirObj, "distribution")
    val distFile = File(distFolder, "app-debug.apk")
    
    inputs.file(srcFile).optional()
    outputs.file(destFile)
    outputs.file(distFile)
    
    doLast {
        if (srcFile.exists()) {
            srcFile.copyTo(destFile, overwrite = true)
            distFolder.mkdirs()
            srcFile.copyTo(distFile, overwrite = true)
            println("=== SUCCESS: Compiled APK copied programmatically to root at ${destFile.absolutePath} and distribution/ at ${distFile.absolutePath} ===")
        } else {
            println("=== WARNING: Compiled APK was not found at ${srcFile.absolutePath} ===")
        }
    }
}

tasks.matching { it.name == "assembleDebug" }.all {
    finalizedBy("copyApkToRoot")
}

abstract class DownloadQuranTask : DefaultTask() {
    @get:OutputFile
    abstract val outputFile: org.gradle.api.file.RegularFileProperty

    @get:OutputFile
    abstract val outputTranslationFile: org.gradle.api.file.RegularFileProperty

    @org.gradle.api.tasks.TaskAction
    fun download() {
        val targetFile = outputFile.get().asFile
        val targetTransFile = outputTranslationFile.get().asFile
        
        targetFile.parentFile.mkdirs()
        targetTransFile.parentFile.mkdirs()

        // 1. Download Arabic Text
        if (targetFile.exists() && targetFile.length() > 100000) {
            println("=== QURAN ARABIC DATABASE ALREADY EXISTS ON DISK: ${targetFile.length()} bytes ===")
        } else {
            println("=== DOWNLOADING AUTHENTIC QURAN ARABIC DATABASE ===")
            val urls = listOf(
                "https://cdn.jsdelivr.net/gh/risan/quran-json@master/data/quran.json",
                "https://raw.githubusercontent.com/risan/quran-json/master/data/quran.json"
            )
            downloadFileWithFallback(urls, targetFile)
        }

        // 2. Download English Translation
        if (targetTransFile.exists() && targetTransFile.length() > 100000) {
            println("=== QURAN ENGLISH TRANSLATION ALREADY EXISTS ON DISK: ${targetTransFile.length()} bytes ===")
        } else {
            println("=== DOWNLOADING AUTHENTIC QURAN ENGLISH TRANSLATION ===")
            val urls = listOf(
                "https://cdn.jsdelivr.net/gh/fawazahmed0/quran-api@1/editions/eng-yusufali.json",
                "https://raw.githubusercontent.com/fawazahmed0/quran-api/main/editions/eng-yusufali.json",
                "https://unpkg.com/quran-json@1.0.1/json/quran/en.json",
                "https://cdn.jsdelivr.net/npm/quran-json@1.0.1/json/quran/en.json"
            )
            downloadFileWithFallback(urls, targetTransFile)
        }
    }

    private fun downloadFileWithFallback(urls: List<String>, file: File) {
        var success = false
        for (urlStr in urls) {
            println("Trying CDN source for ${file.name}: $urlStr")
            try {
                var currentUrl = URL(urlStr)
                var conn = currentUrl.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 25000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                
                var status = conn.responseCode
                var redirectCount = 0
                while ((status == HttpURLConnection.HTTP_MOVED_TEMP || 
                        status == HttpURLConnection.HTTP_MOVED_PERM || 
                        status == 307 || status == 308) && redirectCount < 5) {
                    val newUrl = conn.getHeaderField("Location")
                    conn.disconnect()
                    println("Following redirect ($status) to $newUrl")
                    currentUrl = URL(newUrl)
                    conn = currentUrl.openConnection() as HttpURLConnection
                    conn.connectTimeout = 15000
                    conn.readTimeout = 25000
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                    status = conn.responseCode
                    redirectCount++
                }

                if (status == 200) {
                    file.outputStream().use { output ->
                        conn.inputStream.use { input ->
                            input.copyTo(output)
                        }
                    }
                    println("=== SUCCESS downloaded: ${file.name}! Size: ${file.length()} bytes ===")
                    success = true
                    break
                } else {
                    println("HTTP Code $status for $urlStr")
                }
            } catch (e: Exception) {
                println("Failed for $urlStr: ${e.message}")
            }
        }

        if (!success && !file.exists()) {
            file.writeText("[]")
            println("=== WARNING: Temporary placeholder written due to download failure for ${file.name} ===")
        }
    }
}

tasks.register<DownloadQuranTask>("downloadQuranJson") {
    outputFile.set(layout.projectDirectory.file("src/main/assets/quran.json"))
    outputTranslationFile.set(layout.projectDirectory.file("src/main/assets/translation_en.json"))
}

tasks.matching { it.name == "preBuild" }.all {
    dependsOn("downloadQuranJson")
}








