// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}

allprojects {
  configurations.all {
    resolutionStrategy {
      force(
        "org.bouncycastle:bcprov-jdk18on:1.85",
        "org.bouncycastle:bcpkix-jdk18on:1.85",
        "io.netty:netty-codec:4.1.137.Final",
        "io.netty:netty-codec-http:4.1.137.Final",
        "io.netty:netty-codec-http2:4.1.137.Final",
        "io.netty:netty-handler:4.1.137.Final",
        "io.netty:netty-common:4.1.137.Final",
        "io.netty:netty-handler-proxy:4.1.137.Final",
        "org.jdom:jdom2:2.0.6.1",
        "org.bitbucket.b_c:jose4j:0.9.7",
        "org.apache.commons:commons-lang3:3.21.0",
        "org.apache.httpcomponents:httpclient:4.5.14"
      )
    }
  }
}
