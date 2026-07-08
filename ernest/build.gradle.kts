dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}

version = 4

cloudstream {
    description = "Quality-filtered torrent provider for Nothing & CloudStream using apibay.org. Features smart size filtering, quality detection, and magnet link generation for movies, series, and anime."
    authors = listOf("Pease Ernest", "ErnestTech House")
    status = 1
    tvTypes = listOf("Movie", "TvSeries", "Anime")
    requiresResources = true
    language = "en"
    iconUrl = "https://upload.wikimedia.org/wikipedia/commons/2/2f/Korduene_Logo.png"
}

android {
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}