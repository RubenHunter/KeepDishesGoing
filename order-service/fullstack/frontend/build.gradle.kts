plugins {
    id("com.github.node-gradle.node") version "7.1.0"
    id("base")
}

group = "be.kdg.sa"
version = "0.0.1-SNAPSHOT"
description="frontend"


tasks.register<Delete>("cleanFrontend") {
    group = "build"
    description = "Cleans the Vite distribution folder"
    delete("dist")
}



tasks.build{
    dependsOn("npm_run_build")
}

tasks.clean{
    dependsOn("cleanFrontend")
}