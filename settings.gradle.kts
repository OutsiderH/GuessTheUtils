pluginManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal()
		maven("https://maven.fabricmc.net/")
		maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
	}
}

plugins {
	id("dev.kikugie.stonecutter") version "0.7-beta.7"
}

stonecutter {
	create(rootProject) {
		versions("26.1")
		vcsVersion = "26.1"
	}
}

rootProject.name = "GuessTheUtils"
