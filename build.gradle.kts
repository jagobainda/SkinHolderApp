// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless)
}

// Spotless' ktlint step does not read .editorconfig, so the gate's copy of the
// ktlint_* values lives here. Keep it in sync with .editorconfig.
val ktlintSettings = mapOf(
    "ktlint_code_style" to "intellij_idea",
    "max_line_length" to "120",
    // `_binding` (ViewBinding) and `protected _uiState` (BaseViewModel) are the
    // idiomatic Android backing-property patterns this rule rejects.
    "ktlint_standard_backing-property-naming" to "disabled",
)

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintSettings)
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintSettings)
    }
}
