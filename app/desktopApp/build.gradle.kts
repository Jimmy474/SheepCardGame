import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.app.shared)

    implementation(compose.desktop.currentOs)

    implementation(libs.compose.uiToolingPreview)
}

kotlin{
    compilerOptions {
        freeCompilerArgs.add("-Xexplicit-backing-fields")
    }
}

compose.desktop {
    application {
        mainClass = "com.jimmy.sheepcardgame.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.jimmy.sheepcardgame"
            packageVersion = "1.0.0"
        }
    }
}
