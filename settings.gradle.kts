pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "Budget Tracker"

include(":app")

// Dependabot reports the vulnerable transitive dependencies of the build plugins
// (AGP, KSP, Google services, secrets) against this file, because the version catalog
// constraints in app/build.gradle.kts and the force() list in the root build script only
// cover application configurations, not buildscript (plugin) classpaths. Pin the same
// patched releases on every plugin classpath. Keep in sync with gradle/libs.versions.toml.
//
// Fixes: CVE-2026-8763 + CVE-2026-13506 + CVE-2026-0636 (bcprov), CVE-2026-5588 (bcpkix),
// CVE-2021-33813 (jdom2), CVE-2024-29371 (jose4j), CVE-2025-48924 (commons-lang3),
// CVE-2026-102554 (guava).
gradle.allprojects {
  buildscript {
    configurations["classpath"].resolutionStrategy {
      force(
        "org.bouncycastle:bcprov-jdk18on:1.86",
        "org.bouncycastle:bcpkix-jdk18on:1.86",
        "org.jdom:jdom2:2.0.6.1",
        "org.bitbucket.b_c:jose4j:0.9.7",
        "org.apache.commons:commons-lang3:3.21.0"
      )
      eachDependency {
        if (requested.group == "com.google.guava" && requested.name == "guava") {
          // Guava ships -jre and -android flavors; keep the requested flavor and only
          // upgrade versions older than the patched 33.7.2 release.
          val requestedVersion = requested.version ?: ""
          val parts = requestedVersion.removeSuffix("-jre").removeSuffix("-android")
            .split(".").map { it.toIntOrNull() ?: 0 }
          val target = listOf(33, 7, 2)
          val comparison = (0 until maxOf(parts.size, target.size))
            .map { parts.getOrElse(it) { 0 }.compareTo(target.getOrElse(it) { 0 }) }
            .firstOrNull { it != 0 } ?: 0
          if (requestedVersion.isNotEmpty() && comparison < 0) {
            val flavor = if (requestedVersion.endsWith("-android")) "-android" else "-jre"
            useVersion("33.7.2$flavor")
            because("Fix CVE-2026-102554: uncontrolled memory allocation on deserialization")
          }
        }
      }
    }
  }
}
