import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
plugins {
   java
   application
   id("com.github.johnrengelman.shadow") version "6.1.0"
}

java {                                      
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

dependencies {
	implementation(group = "org.bouncycastle", name = "bcmail-jdk15on", version = "1.68")
	implementation(group = "org.bouncycastle", name = "bcprov-jdk15on", version = "1.68")
}

application {
    mainClass.set("p7mx") 
    mainClassName = "p7mx"
}

tasks.withType<ShadowJar>() {
    manifest {
        attributes["Main-Class"] = "p7mx"
        attributes["Implementation-Name"] = "p7mx"
   }
}
