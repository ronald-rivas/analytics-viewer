import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    testImplementation(libs.junit)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        local("C:/Program Files/Android/Android Studio")
        testFramework(TestFrameworkType.Platform)

        // Add plugin dependencies for compilation here, for example:
        // bundledPlugin("com.intellij.java")
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
