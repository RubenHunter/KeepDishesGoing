plugins {
	java
	id("org.springframework.boot") version "3.5.5"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "be.kdg.sa"
version = "0.0.1-SNAPSHOT"
description = "backend"

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(21))
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")

	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-amqp")

	// DDD with jMolecules
	implementation("org.jmolecules:jmolecules-ddd:1.9.0")
	implementation("org.jmolecules.integrations:jmolecules-starter-ddd:0.29.0")
	implementation("org.jmolecules.integrations:jmolecules-jpa:0.29.0")

	// ArchUnit for testing architecture
	testImplementation("org.jmolecules.integrations:jmolecules-starter-test:0.29.0")
	testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")

	// Database
	runtimeOnly("org.postgresql:postgresql")

	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.amqp:spring-rabbit-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.register<Copy>("installWebApp"){
	group="build"
	dependsOn(":frontend:build")
	from("../frontend/dist")
	into("./src/main/resources/static")
}

tasks.processResources{
	duplicatesStrategy = DuplicatesStrategy.INCLUDE
}