#!/bin/bash

# Android Project Setup Script for PBLOCK
echo "Setting up Android project structure..."

# Create directory structure
mkdir -p app/src/main/cpp
mkdir -p app/src/main/java/com/pblock/app
mkdir -p app/src/main/res/layout
mkdir -p app/src/main/res/values
mkdir -p gradle/wrapper

# Move existing files
echo "Moving C++ files..."
mv mainPBLOCK.cpp app/src/main/cpp/ 2>/dev/null || cp mainPBLOCK.cpp app/src/main/cpp/
mv CMakeLists.txt app/src/main/cpp/ 2>/dev/null || cp CMakeLists.txt app/src/main/cpp/
mv build.gradle app/ 2>/dev/null || echo "build.gradle will be created"

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
// Top-level build file
plugins {
    id 'com.android.application' version '8.1.0' apply false
}
EOF

# Create app/build.gradle
cat >app/build.gradle <<'EOF'
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.pblock.app'
    compileSdk 34

    defaultConfig {
        applicationId "com.pblock.app"
        minSdk 21
        targetSdk 34
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

# Create AndroidManifest.xml
cat >app/src/main/AndroidManifest.xml <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="PBLOCK"
        android:theme="@style/Theme.AppCompat.Light.DarkActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
EOF

# Create MainActivity.java
cat >app/src/main/java/com/pblock/app/MainActivity.java <<'EOF'
package com.pblock.app;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    // Load native library
    static {
        System.loadLibrary("pblock");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example: Call native method
        TextView tv = findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());
    }

    // Native method declaration
    public native String stringFromJNI();
}
EOF

# Create activity_main.xml
cat >app/src/main/res/layout/activity_main.xml <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res/auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextView
        android:id="@+id/sample_text"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Loading..."
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
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
echo "✅ Android project structure created!"
echo ""
echo "Next steps:"
echo "1. Edit local.properties and set your Android SDK path"
echo "2. Update app/src/main/cpp/CMakeLists.txt if needed"
echo "3. Run: chmod +x gradlew"
echo "4. Run: ./gradlew assembleDebug"
echo ""
echo "Or open the project in Android Studio!"
