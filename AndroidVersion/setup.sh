#!/bin/bash

# Android Project Setup Script for PBLOCK
echo "Setting up Android project structure..."

# Create directory structure
mkdir -p app/src/main/cpp
mkdir -p app/src/main/java/com/pblock/app
mkdir -p app/src/main/res/layout
mkdir -p app/src/main/res/values
mkdir -p gradle/wrapper

# Create settings.gradle
cat >settings.gradle <<'EOF'
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "PBLOCK"
include ':app'
EOF

# Create root build.gradle
cat >build.gradle <<'EOF'
plugins {
    id 'com.android.application' version '8.7.3' apply false
}

tasks.register('clean', Delete) {
    delete rootProject.buildDir
}
EOF

# Create app/build.gradle
cat >app/build.gradle <<'EOF'
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.pblock.app'
    compileSdk 35

    defaultConfig {
        applicationId "com.pblock.app"
        minSdk 21
        targetSdk 35
        versionCode 1
        versionName "1.0"

        externalNativeBuild {
            cmake {
                cppFlags "-std=c++17"
            }
        }
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    externalNativeBuild {
        cmake {
            path "src/main/cpp/CMakeLists.txt"
            version "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.10.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
}
EOF

# Create gradle.properties
cat >gradle.properties <<'EOF'
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.enableJetifier=true
EOF

# Create gradle-wrapper.properties
cat >gradle/wrapper/gradle-wrapper.properties <<'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

# Create local.properties (user needs to set SDK path)
cat >local.properties <<'EOF'
# Set your Android SDK path here
# sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk
# or on Linux: sdk.dir=/home/YOUR_USERNAME/Android/Sdk
EOF

echo ""
echo "Android project structure created!"
echo ""
echo "Next steps:"
echo "1. Edit local.properties and set your Android SDK path"
echo "2. Run: chmod +x gradlew"
echo "3. Run: ./gradlew assembleDebug"
echo ""
echo "Or open the project in Android Studio!"
