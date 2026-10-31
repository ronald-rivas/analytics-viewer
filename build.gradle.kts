import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    testImplementation(libs.junit)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        androidStudio("2024.3.2.14")
        testFramework(TestFrameworkType.Platform)

        // Add plugin dependencies for compilation here, for example:
        // bundledPlugin("com.intellij.java")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        // Don't generate bridge methods for interface default methods (avoids deprecated/experimental API usages)
        freeCompilerArgs.add("-jvm-default=no-compatibility")
    }
}

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("version")
        ideaVersion {
            sinceBuild = "243"
            untilBuild = provider { null }
        }
        changeNotes = """
            <b>0.4.2</b>
            <ul>
                <li>Fixed: Java and Kotlin bytecode now target Java 17 so the plugin loads in Android Studio Meerkat with JDK 17.</li>
            </ul>
            <b>0.4.1</b>
            <ul>
                <li>Fixed: Event-name search works independently of parameter filters, including while paused.</li>
            </ul>
            <b>0.4.0</b>
            <ul>
                <li>Parameter filters with visibility and value matching; applying filters resyncs without resetting them.</li>
                <li>Pause and resume incoming analytics events without losing events received during the pause.</li>
                <li>Parameter filters button is disabled when no events are available.</li>
            </ul>
            <b>0.2.0</b>
            <ul>
                <li>Internal restructuring into small components; no visual or behavioral changes.</li>
            </ul>
            <b>0.1.1</b>
            <ul>
                <li>ADB status indicator (green: device connected, red: no device).</li>
                <li>Automatic resync when a device reconnects.</li>
                <li>Fixed: Resync duplicated events; it now clears and reloads events and user properties.</li>
            </ul>
            <b>0.1.0</b>
            <ul>
                <li>Filter field is always active, below the toolbar.</li>
                <li>Icon-only Sync and Clear actions with hover feedback.</li>
                <li>New button-style event list and folder-style Parameters/Properties tabs.</li>
                <li>Thinner, easier-to-grab resize divider.</li>
                <li>Fixed: IDE freeze when clearing the filter with many events.</li>
            </ul>
            <b>0.0.3</b>
            <ul>
                <li>Search button to filter events by name.</li>
                <li>Fixed: Clear no longer removes user properties.</li>
            </ul>
            <b>0.0.2</b>
            <ul>
                <li>Long event titles are now truncated with an ellipsis; the full title is shown in a tooltip.</li>
            </ul>
            <b>0.0.1</b>
            <ul>
                <li>Real-time list of Firebase Analytics events read from logcat through adb.</li>
                <li>Parameters and user properties views.</li>
                <li>Clear and Sync buttons.</li>
            </ul>
        """.trimIndent()
    }
}
