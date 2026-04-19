// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
<<<<<<< HEAD
    id("com.android.application") version "8.2.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
=======
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
>>>>>>> b70b825a8c72a68be7180cb96879a7473c3462d0
}