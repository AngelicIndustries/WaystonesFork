import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

val clientOnlyModIds = listOf(
    "ModularUI2",
    "Baubles-Expanded",
    "angelica",
    "shouldersurfing",
    "Sound-Physics",
)

tasks.withType<JavaExec>().configureEach {
    if (name.startsWith("runServer")) {
        doFirst("stripClientOnlyMods") {
            classpath = classpath.filter { file ->
                clientOnlyModIds.none { modId -> file.name.contains(modId, ignoreCase = true) }
            }
        }
    }
}

extensions.configure<PublishingExtension> {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("Waystones-X")
            description.set("Teleport back to activated waystones. For Survival, Adventure or Servers.")
            url.set("https://github.com/AngelicIndustries/WaystonesFork")
            organization {
                name.set("Angelic Industries")
                url.set("https://angelic.industries/")
            }
            scm {
                url.set("https://github.com/AngelicIndustries/WaystonesFork")
                connection.set("scm:git:https://github.com/AngelicIndustries/WaystonesFork.git")
                developerConnection.set("scm:git:ssh://git@github.com/AngelicIndustries/WaystonesFork.git")
            }
            licenses {
                license {
                    name.set("GNU Lesser General Public License, version 3")
                    url.set("https://github.com/AngelicIndustries/WaystonesFork/blob/master/LICENSE")
                    distribution.set("repo")
                }
                license {
                    name.set("MIT License (original Waystones portions)")
                    url.set("https://github.com/AngelicIndustries/WaystonesFork/blob/master/LICENSE-MIT")
                    distribution.set("repo")
                }
            }
        }
    }
}
