includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("_GrailsCreateArtifacts")

target(main: "Gant script that creates a Grails RabbitMQ consumer") {
    depends(checkVersion, parseArguments)

    def type = "Consumer"
    promptForName(type: type)

    def name = argsMap["params"][0]
    createArtifact(name: name, suffix: type, type: type, path: "grails-app/consumers")
}

setDefaultTarget(main)
