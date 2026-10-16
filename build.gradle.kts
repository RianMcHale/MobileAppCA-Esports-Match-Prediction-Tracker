// makes the android plugin available to modules without applying it here.
plugins {
    alias(libs.plugins.android.application) apply false
}