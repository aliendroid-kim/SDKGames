plugins {
    alias(libs.plugins.androidLibrary)
    id("maven-publish")
}

android {
    namespace = "com.jambox.monetisation"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)

    implementation("com.applovin:applovin-sdk:12.5.0")
    implementation("com.applovin.mediation:chartboost-adapter:9.7.0.0")
    implementation("com.google.android.gms:play-services-base:18.5.0")
    implementation("com.applovin.mediation:fyber-adapter:8.2.7.1")
    implementation("com.applovin.mediation:google-ad-manager-adapter:23.1.0.0")
    implementation("com.applovin.mediation:google-adapter:23.1.0.0")
    implementation("com.applovin.mediation:inmobi-adapter:10.6.7.0")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.applovin.mediation:ironsource-adapter:8.0.0.0.0")
    implementation("com.applovin.mediation:vungle-adapter:7.3.2.0")
    implementation("com.applovin.mediation:facebook-adapter:6.17.0.0")
    implementation("com.applovin.mediation:mintegral-adapter:16.7.31.0")
    implementation("com.applovin.mediation:bytedance-adapter:5.9.0.5.0")
    implementation("com.applovin.mediation:smaato-adapter:22.6.1.0")
    implementation("com.applovin.mediation:unityads-adapter:4.11.3.0")
    implementation("com.applovin.mediation:verve-adapter:3.0.0.0")
    implementation("com.applovin.mediation:yandex-adapter:7.0.1.0")

    //Adjust
    implementation("com.adjust.sdk:adjust-android:4.38.3")
    implementation("com.android.installreferrer:installreferrer:2.2")

    // Add the following if you are using the Adjust SDK inside web views on your app
    //implementation("com.adjust.sdk:adjust-android-webbridge:4.38.3")

    //mplementation("com.google.android.gms:play-services-ads-identifier:18.0.1")
    //implementation("com.google.android.gms:play-services-appset:16.0.2")
}

android {
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                groupId = "com.github.aliendroid-kim"
                artifactId = "SDKGames"
                version = "1.0.6"

                from(components["release"])

                //com.github.jamboxgames:monetise-sdk:1.2.1
                //https://github.com/aliendroid-kim/SDKGames
            }
        }
    }
}