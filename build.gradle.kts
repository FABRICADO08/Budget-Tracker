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
      // Align every transitive Netty 4.1 module, not only the modules listed below.
      eachDependency {
        if (requested.group == "io.netty" && requested.version?.startsWith("4.1.") == true) {
          useVersion("4.1.139.Final")
          because("Align Netty modules to the patched 4.1.139.Final security release")
        }
      }
      force(
        "org.bouncycastle:bcprov-jdk18on:1.86",
        "org.bouncycastle:bcpkix-jdk18on:1.86",
        "io.netty:netty-buffer:4.2.19.Final",
        "io.netty:netty-transport:4.2.19.Final",
        "io.netty:netty-resolver:4.2.19.Final",
        "io.netty:netty-transport-native-unix-common:4.2.19.Final",
        "io.netty:netty-codec-socks:4.2.19.Final",
        "io.netty:netty-codec-haproxy:4.2.19.Final",
        "io.netty:netty-codec-mqtt:4.2.19.Final",
        "io.netty:netty-codec-stomp:4.2.19.Final",
        "io.netty:netty-codec:4.2.19.Final",
        "io.netty:netty-codec-http:4.2.19.Final",
        "io.netty:netty-codec-http2:4.2.19.Final",
        "io.netty:netty-handler:4.2.19.Final",
        "io.netty:netty-common:4.2.19.Final",
        "io.netty:netty-handler-proxy:4.2.19.Final",
        "org.jdom:jdom2:2.0.6.1",
        "org.bitbucket.b_c:jose4j:0.9.7",
        "org.apache.commons:commons-lang3:3.21.0",
        "org.apache.httpcomponents:httpclient:4.5.14"
      )
    }
  }
}
