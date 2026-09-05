plugins {
	java
	id("org.springframework.boot") version "3.5.6"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.asciidoctor.jvm.convert") version "3.3.2"
	id("jacoco")
}

group = "be.kdg"
version = "0.0.1-SNAPSHOT"
description = "order-service"

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

ext {
	set("snippetsDir", file("build/generated-snippets"))
}

dependencies {
	// Spring Boot starters
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-amqp")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

	// Stripe payments (US20 test-mode provider; SDK handles session create + webhook signature)
	implementation("com.stripe:stripe-java:29.2.0")

	// DDD with jMolecules (incl. JPA integration so AR annotations map to JPA)
	implementation("org.jmolecules:jmolecules-ddd:1.9.0")
	implementation("org.jmolecules.integrations:jmolecules-starter-ddd:0.29.0")
	implementation("org.jmolecules.integrations:jmolecules-jpa:0.29.0")
	implementation("org.jspecify:jspecify:1.0.0")

	// Database
	runtimeOnly("org.postgresql:postgresql")

	// Lombok
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	// Test
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.amqp:spring-rabbit-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.jmolecules.integrations:jmolecules-starter-test:0.29.0")
	testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
	testImplementation("org.testcontainers:postgresql:1.19.3")
	testImplementation("org.testcontainers:rabbitmq:1.19.3")
	testImplementation("org.testcontainers:junit-jupiter:1.19.3")
	testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testRuntimeOnly("com.h2database:h2")
}

tasks.withType<Test> {
	useJUnitPlatform()
	finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required.set(true)
		html.required.set(true)
	}
}

tasks.named("asciidoctor") {
	dependsOn(tasks.test)
}

// Frontend install task (kept from old build). Not used in Step 1.
tasks.register<Copy>("installWebApp") {
	group = "build"
	dependsOn(":frontend:build")
	from("../frontend/dist")
	into("./src/main/resources/static")
}

tasks.processResources {
	duplicatesStrategy = DuplicatesStrategy.INCLUDE
}