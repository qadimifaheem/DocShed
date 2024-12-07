plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.test"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.test"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
    packagingOptions {
        resources {
            merges += setOf("META-INF/NOTICE.md", "META-INF/LICENSE.md")
        }
    }
}

dependencies {

    // Frameworks
    // Core Android dependencies
    // Provides essential Android extensions like core utility functions (KTX for easier syntax)
    implementation(libs.androidx.core.ktx)  // Example: accessing SharedPreferences with easier syntax.
    implementation(libs.androidx.appcompat)  // Example: provides backward-compatible AppCompatActivity for all Android versions.
    implementation(libs.material)  // Example: includes Material Design components like buttons, text fields, and dialogs.
    implementation(libs.androidx.activity)  // Example: supports Activity lifecycle and provides features like Activity Result API.
    implementation(libs.androidx.constraintlayout)  // Example: allows creating flexible and complex layouts in XML with ConstraintLayout.

    // Firebase BoM (manages versions of Firebase dependencies)
    // Ensures that all Firebase libraries use compatible versions
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))

    // Google Play services
    // Used for integrating Google services like Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")  // Example: allows users to sign in with their Google account.

    // Image loading with Glide
    // Glide: popular library for loading and caching images in Android apps
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.firebase.database)
    implementation(libs.androidx.ui.text.android)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.androidx.material3.android)
    implementation(libs.cronet.embedded)  // Example: display doctor or patient profile images in an ImageView.
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")  // Compiler for generating Glide’s API bindings.

    // Additional UI components
    // DotsIndicator: library for creating dot indicators in ViewPagers (usually for swipeable UI elements)
    implementation("com.tbuonomo:dotsindicator:5.1.0")  // Example: show a dot indicator to represent multiple pages in a ViewPager.

    // WorkManager: handles background tasks like scheduled job execution
    implementation("androidx.work:work-runtime-ktx:2.9.1")  // Example: sending periodic reminders to users about upcoming appointments in the background.

    // Testing dependencies
    // JUnit: unit testing framework used for writing and running unit tests
    testImplementation(libs.junit)  // Example: writing test cases for app functions.

    // AndroidX JUnit: JUnit extensions specifically for Android tests
    androidTestImplementation(libs.androidx.junit)  // Example: running Android-specific JUnit tests.

    // Espresso: UI testing framework for Android to interact with UI elements
    androidTestImplementation(libs.androidx.espresso.core)  // Example: writing tests that simulate user interactions with UI components.

    implementation ("com.google.firebase:firebase-auth:23.1.0")


    implementation ("com.sun.mail:android-mail:1.6.7")
    implementation ("com.sun.mail:android-activation:1.6.7")


    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation ("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")// For Java projects
    implementation ("com.google.code.gson:gson:2.8.8")

    implementation("com.google.android.material:material:1.9.0")

    // APIs
    // Firebase dependencies
    // Firebase Authentication: used to implement user login, registration, etc.
    implementation("com.google.firebase:firebase-auth-ktx")  // Example: sign in users with email and password.

    // Firebase Firestore: cloud-based NoSQL database for storing and syncing app data
    implementation("com.google.firebase:firebase-firestore-ktx")  // Example: store user profiles or appointments in collections and documents.

    // Firebase Storage: allows storing files (e.g., images, videos) in the cloud
    implementation("com.google.firebase:firebase-storage-ktx")  // Example: uploading and retrieving doctor or patient profile pictures.

    // Firebase Cloud Messaging: enables push notifications to the app
    implementation("com.google.firebase:firebase-messaging")  // Example: sending appointment reminders to patients via push notifications.

    // Firebase Firestore (for data storage)
    implementation("com.google.firebase:firebase-firestore-ktx")  // Example: storing doctor appointment details in Firestore collections.

    // Notifications
    // Core-ktx: AndroidX core extensions with support for notifications
    implementation("androidx.core:core-ktx:1.13.1")  // Example: working with notifications and background services using simplified code.


    // Firebase Cloud Messaging (FCM): supports sending push notifications
    implementation("com.google.firebase:firebase-messaging:24.0.2")  // Example: sending push notifications to notify patients of upcoming appointments.
}

