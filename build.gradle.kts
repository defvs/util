import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	java
	maven
	kotlin("jvm") version "1.2.70"
}

allprojects {
	repositories {
		jcenter()
	}
}

subprojects {
	apply(plugin = "java")
	apply(plugin = "kotlin")
	apply(plugin = "maven")
	
	group = "xerus.util"
	
	tasks {
		val sourcesJar by creating(Jar::class) {
			classifier = "sources"
			from(sourceSets.getByName("main").allSource)
		}
		val javadocJar by creating(Jar::class) {
			classifier = "javadoc"
			from(tasks.getByName<Javadoc>("javadoc").destinationDir)
		}
		getByName("install").dependsOn("javadocJar", "sourcesJar")
		artifacts {
			add("archives", sourcesJar.outputs.files.first()) { classifier = "sources" }
			add("archives", javadocJar.outputs.files.first()) { classifier = "javadoc" }
		}
		
		withType<KotlinCompile> {
			kotlinOptions.jvmTarget = "1.8"
		}
	}
	
	kotlin.experimental.coroutines = Coroutines.ENABLE
	
}