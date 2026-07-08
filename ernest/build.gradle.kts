dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}

// Use an integer for version numbers
version = 1

cloudstream {
    // All of these properties are optional, you can safely remove any of them.

    description = "The top streaming plugin made soecifically for nothing but can be used for cloudstream we use apibay.ord api key we just add several topngs to it like streaming and downloading"
    authors = listOf("Pease Ernest", "ErnestTech House")

    /**
    * Status int as one of the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta-only
    **/
    status = 1 // Will be 3 if unspecified

    tvTypes = listOf("Movie")

    requiresResources = true
    language = "en"

    // Random CC logo I found
    iconUrl = "https://upload.wikimedia.org/wikipedia/commons/2/2f/Korduene_Logo.png"
}

android {
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    
    // HTTP Client for API calls
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    
    // JSON parsing
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    
    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}

// Use an integer for version numbers
version = 1

cloudstream {
    description = "Quality-filtered torrent provider for Nothing & CloudStream using apibay.org. Features smart size filtering, quality detection, and magnet link generation for movies, series, and anime."
    authors = listOf("Pease Ernest", "ErnestTech House")

    /**
    * Status int as one of the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta-only
    **/
    status = 1
    
    // Supported content types
    tvTypes = listOf(
        "Movie",
        "TvSeries",
        "Anime"
    )

    requiresResources = true
    language = "en"

    // Logo - you should replace this with your own
    iconUrl = "https://upload.wikimedia.org/wikipedia/commons/2/2f/Korduene_Logo.png"
}

android {
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}