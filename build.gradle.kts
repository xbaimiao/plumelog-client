plugins {
    id("java")
    `maven-publish`
}

group = "com.xbaimiao.plumelog.client"
version = "1.0.2"

repositories {
    mavenCentral()
}

dependencies {
    // gson
    implementation("com.google.code.gson:gson:2.8.9")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri("https://maven.xbaimiao.com/repository/releases/")
            credentials {
                username = project.findProperty("BaiUser").toString()
                password = project.findProperty("BaiPassword").toString()
            }
        }
    }
}
