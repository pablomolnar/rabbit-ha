import grails.plugin.rabbitha.ConsumerArtefactHandler
import grails.plugin.rabbitha.GrailsConsumerClass
import grails.plugin.rabbitha.RabbitHAConsumer
import org.apache.commons.lang.WordUtils

class RabbitHaGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3.7 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp",
            "grails-app/services/**",
            "grails-app/controllers/**",
            "grails-app/consumers/**",
            "grails-app/views/message/*",
            "grails-app/conf/Config.groovy",
            "**/.gitignore"
    ]

    // TODO Fill in these fields
    def title = "RabbitMQ-HA Plugin" // Headline display name of the plugin
    def author = "Pablo Molnar"
    def authorEmail = "pablomolnar@gmail.com"
    def description = '''\
Alternative RabbitMQ plugin for Grails.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/rabbit-ha"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
    def issueManagement = [system: "GITHUB", url: "https://github.com/pablomolnar/rabbit-ha/issues"]

    // Online location of the plugin's browseable source code.
    def scm = [url: "https://github.com/pablomolnar/rabbit-ha"]

    def watchedResources = [
            "file:./grails-app/consumers/**/*Consumer.groovy",
            "file:./plugins/*/grails-app/consumers/**/*Consumer.groovy"
    ]

    def artefacts = [new ConsumerArtefactHandler()]

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before
    }

    def doWithSpring = {
        def cfg = application.config.rabbitmq?.connectionfactory
        if (!cfg || !cfg.username || !cfg.password || !cfg.virtualHost || !cfg.addresses) {
            log.error(

                    """
RabbitMQ connection factory settings (rabbitmq.connectionfactory.username, rabbitmq.connectionfactory.password and rabbitmq.connectionfactory.virtualHost and rabbitmq.connectionfactory.addresses) must be defined in Config.groovy

e.g.:
    rabbitmq {
        connectionfactory {
            username = "guest"
            password = "guest"
            virtualHost = "/"
            addresses = ['hostname_1','hostname_2', ...]

        }
    }

RabbitMQ consumers configuration will be ignored...
"""
            )

            return
        }

        application.consumerClasses.each {GrailsConsumerClass consumerClass ->
            def beanName = WordUtils.uncapitalize(consumerClass.getShortName())

            "$beanName"(consumerClass.getClazz()) { bean -> bean.autowire = "byName" }
        }
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->

        // Start consumers
        def containerBeans = applicationContext.getBeansOfType(RabbitHAConsumer)
        containerBeans.each { beanName, bean ->
            bean.start()
        }

    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    def onShutdown = { event ->
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
