plugins {
    alias(libs.plugins.spring.boot) apply false
}

subprojects {
    // Container projects (e.g. :food-delivery) hold no code.
    if (project.path == ":food-delivery") return@subprojects

    apply(plugin = "java")
    group = "com.example.reactive"
    version = "1.0.0"

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(rootProject.libs.versions.java.get()))
    }

    dependencies {
        "implementation"(platform(rootProject.libs.spring.boot.bom))
        "testImplementation"(platform(rootProject.libs.spring.boot.bom))
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
    }
}
