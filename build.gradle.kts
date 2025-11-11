import org.gradle.api.internal.classpath.ModuleRegistry
import java.lang.reflect.Method

buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("nu.studer:gradle-plugindev-plugin:4.1")
        classpath("org.codehaus.groovy:groovy-backports-compat23:2.4.6")
        classpath("net.researchgate:gradle-release:2.7.0")
    }
}

plugins {
    `java-gradle-plugin`
    groovy
    `maven-publish`
    id("net.researchgate.release") version "2.8.0"
    id("com.gradle.plugin-publish") version "0.18.0"
}

apply(plugin = "idea")

group = "net.researchgate"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.spockframework:spock-core:2.1-groovy-2.5") {
        exclude(group = "org.codehaus.groovy")
    }
    testImplementation("org.eclipse.jgit:org.eclipse.jgit:5.0.3.201809091024-r")
    testImplementation("cglib:cglib-nodep:3.2.8")
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")

    // work-around for https://github.com/gradle/gradle/issues/16774
    try {
        val servicesMethod: Method = project.javaClass.getMethod("getServices")
        val services = servicesMethod.invoke(project) as org.gradle.internal.service.ServiceRegistry
        val moduleRegistry = services.get(ModuleRegistry::class.java)
        testRuntimeOnly(files(moduleRegistry.getModule("gradle-tooling-api-builders").classpath.asFiles.first()))
    } catch (e: Exception) {
        logger.warn("Could not configure gradle-tooling-api-builders workaround: ${e.message}")
    }
    testRuntimeOnly("com.google.guava:guava:27.1-android")
}

gradlePlugin {
    plugins {
        create("releasePlugin") {
            displayName = "Maven style release plugin for gradle"
            description = "gradle-release is a plugin for providing a Maven-like release process to project using Gradle that supports git, subversion bazaar and mercurial"
            id = "net.researchgate.release"
            implementationClass = "net.researchgate.release.ReleasePlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/researchgate/gradle-release"
    vcsUrl = "https://github.com/researchgate/gradle-release"
    tags = listOf("release", "git", "hg", "mercurial", "svn", "subversion", "bzr", "bazaar")
}

tasks.withType<Test>().configureEach {
    dependsOn(tasks.jar)
    useJUnitPlatform()
    systemProperties["currentVersion"] = project.version
}

// Release configuration
// Note: Due to bootstrap issues (the plugin configuring itself), this configuration
// is commented out. The requireBranch setting can be configured via command line if needed.
/*
configure<net.researchgate.release.ReleaseExtension> {
    git {
        requireBranch.set("(main|\\d+\\.\\d+)")
    }
}
*/

tasks.wrapper {
    gradleVersion = "6.9.2"
}

tasks.named("updateVersion") {
    doFirst {
        val file = file("README.md")
        var content = file.readText()
        content = content.replace(Regex("""id 'net\.researchgate\.release' version '\d+(?:\.\d+)+'""")) {
            "id 'net.researchgate.release' version '$version'"
        }
        content = content.replace(Regex("""net\.researchgate:gradle-release:\d+(?:\.\d+)+""")) {
            "net.researchgate:gradle-release:$version"
        }
        file.writeText(content)
    }
}
