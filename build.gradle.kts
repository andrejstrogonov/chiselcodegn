plugins {
    id("java")
    id("scala")
}

group = "org.andrejstrogonov"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.scala-lang:scala3-library_3:3.0.1")
    implementation("commons-collections:commons-collections:3.2.2")
    implementation("io.circe:circe-yaml_3:0.16.0")
    implementation("com.github.scopt:scopt_3:4.1.0")
    implementation("org.json4s:json4s-native_3:4.1.0-M8")
    implementation("org.apache.commons:commons-text:1.12.0")
    implementation("com.lihaoyi:os-lib_3:0.11.3")
    implementation("com.typesafe.scala-logging:scala-logging_3:3.9.5")
    testImplementation("org.scala-lang:scala-library:2.13.12")
    testImplementation("ch.qos.logback:logback-classic:1.5.12")
    testImplementation("org.scalatest:scalatest_3:3.2.9")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}