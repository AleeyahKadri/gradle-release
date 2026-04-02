import net.researchgate.release.ReleaseExtension
import org.gradle.api.Action
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType

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
    id("org.gradle.java-gradle-plugin")
    id("org.gradle.groovy")
    id("maven-publish")
    id("net.researchgate.release") version "2.8.0"
    id("com.gradle.plugin-publish") version "0.18.0"
    idea
}

group = "net.researchgate"

repositories {
    mavenCentral()
}

dependencies {
    testCompile("org.spockframework:spock-core:2.1-groovy-2.5") {
        exclude(group = "org.codehaus.groovy")
    }
    testCompile("org.eclipse.jgit:org.eclipse.jgit:5.0.3.201809091024-r")
    testCompile("cglib:cglib-nodep:3.2.8")
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")

    // work-around for https://github.com/gradle/gradle/issues/16774
    testRuntimeOnly(
        files(
            (project as ProjectInternal).services
                .get(ModuleRegistry::class.java)
                .getModule("gradle-tooling-api-builders")
                .classpath
                .asFiles
                .first()
        )
    )
    testRuntimeOnly("com.google.guava:guava:27.1-android")
}

gradlePlugin {
    plugins {
        create("releasePlugin") {
            displayName = "Maven style release plugin for gradle"
            description =
                "gradle-release is a plugin for providing a Maven-like release process to project using Gradle that supports git, subversion bazaar and mercurial"
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

the<ReleaseExtension>().git(
    Action {
        it.requireBranch.set("(main|\\d+\\.\\d+)")
    }
)

tasks.wrapper {
    gradleVersion = "6.9.2"
}

tasks.named("updateVersion") {
    doFirst {
        val readmeFile = file("README.md")
        var content = readmeFile.readText()
        val versionPattern = "\\d+(?:\\.\\d+)+"
        val currentVersion = version.toString()
        content =
            content.replace(
                Regex("id 'net\\.researchgate\\.release' version '$versionPattern'"),
                "id 'net.researchgate.release' version '$currentVersion'"
            )
        content =
            content.replace(
                Regex("net\\.researchgate:gradle-release:$versionPattern"),
                "net.researchgate:gradle-release:$currentVersion"
            )
        readmeFile.writeText(content)
    }
}
