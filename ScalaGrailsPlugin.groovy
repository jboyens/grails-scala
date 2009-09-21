class ScalaGrailsPlugin {
    // the plugin version
    def version = 0.5
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.1 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def author = "Vaclav Pech"
    def authorEmail = ""
    def title = "Scala Plugin"
    def description = '''\\
Compiles Scala sources located under src/scala and src-java before grails kicks in with the grails compilation
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/Scala+Plugin"
}
