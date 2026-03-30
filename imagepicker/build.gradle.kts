import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("maven-publish")
    id("signing")
}

val localProperties = Properties()
val localPropertiesFile = project.rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

fun resolvePublishProperty(primaryKey: String, fallbackKey: String? = null): String? {
    return providers.gradleProperty(primaryKey).orNull
        ?: fallbackKey?.let { providers.gradleProperty(it).orNull }
        ?: localProperties.getProperty(primaryKey)
        ?: fallbackKey?.let(localProperties::getProperty)
}

val keyId = resolvePublishProperty("signing.keyId")
val signingPassword = resolvePublishProperty("signing.password")
val signingKey = resolvePublishProperty("signingKey", "signing.key")
val secretKeyFile = resolvePublishProperty("signing.secretKeyRingFile")
val centralUsername = resolvePublishProperty("centralUsername", "ossrhUsername")
val centralPassword = resolvePublishProperty("centralPassword", "ossrhPassword")
val libraryGroupId = providers.gradleProperty("GROUP").orNull ?: "io.github.seunghee17"
val libraryVersionName = providers.gradleProperty("VERSION_NAME").orNull ?: "1.0.0"

android {
    namespace = "io.github.seunghee17.imagepicker"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = libraryGroupId
            artifactId = "imagepicker"
            version = libraryVersionName

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("Dynamic Image Picker")
                description.set("Android Compose 기반 편집가능한 이미지 픽커 라이브러리")
                url.set("https://github.com/seunghee17/DynamicImagePicker")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("seunghee17")
                        name.set("Seunghee")
                        email.set("seunghuisong41@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:github.com/seunghee17/DynamicImagePicker.git")
                    developerConnection.set("scm:git:ssh://github.com/seunghee17/DynamicImagePicker.git")
                    url.set("https://github.com/seunghee17/DynamicImagePicker")
                }
            }
        }
    }

    repositories {
        maven {
            name = "central"
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            credentials {
                username = centralUsername
                password = centralPassword
            }
        }
    }
}

if (centralUsername != null && centralPassword != null) {
    extra.set("centralUsername", centralUsername)
    extra.set("centralPassword", centralPassword)
}

signing {
    when {
        signingKey != null && signingPassword != null -> {
            useInMemoryPgpKeys(keyId, signingKey, signingPassword)
            sign(publishing.publications["release"])
        }

        keyId != null && signingPassword != null && secretKeyFile != null -> {
            extra.set("signing.keyId", keyId)
            extra.set("signing.password", signingPassword)
            extra.set("signing.secretKeyRingFile", secretKeyFile)
            sign(publishing.publications["release"])
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    // Lifecycle + ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Activity
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.exifinterface)

    // Compose
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Image loading
    implementation(libs.coil.compose)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
