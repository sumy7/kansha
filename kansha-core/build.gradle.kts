plugins {
//    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
}

group = "com.sumygg.kansha"
version = libs.versions.libraryVersion.get()

val artifactId = "kansha-core"
mavenPublishing {
    coordinates("com.sumygg.kansha", artifactId, libs.versions.libraryVersion.get())
    pom {
        name.set(artifactId)
        description.set("Kansha")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://opensource.org/licenses/Apache-2.0")
            }
        }
        url.set("https://github.com/sumy7/kansha")
        issueManagement {
            system.set("Github")
            url.set("https://github.com/sumy7/kansha/issues")
        }
        scm {
            connection.set("https://github.com/sumy7/kansha.git")
            url.set("https://github.com/sumy7/kansha")
        }
        developers {
            developer {
                name.set("sumy7")
                email.set("sumyggsun@gmail.com")
                organization.set("SumyGG Sun")
            }
        }
    }
}