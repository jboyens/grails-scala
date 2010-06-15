import org.codehaus.gant.GantBinding

/*
 * Hooks an event handler to the CompileStart event to run the Scala compiler on Java and Scala sources.
 * It also updates source and class paths for the groovyc compiler with scala sources and libraries and optionally
 * copies Scala runtime libraries to the project's lib folder.
 *
 * Scala sources may be placed and combined in any way with Java sources in and across both /src/java and /src/scala.
 * All the Groovy sources including Grails-specific ones, like domain classes, controllers and such,
 * are directly visible inside the Scala sources, as well as all Groovy sources can use all Scala
 * or Java classes.
 */
Ant.property(environment: "env")
scalaHome = Ant.antProject.properties."env.SCALA_HOME"

/**
 * Hooks to the compile grails event
 */
eventCompileStart = {GantBinding compileBinding ->
    if (compilingScalaPlugin()) return

    if ((!scalaHome) || (buildConfig.scala?.useBundledLibs)) {
        println '[scalaPlugin] Ignoring SCALA_HOME. Using bundled Scala distribution'
        scalaHome = "${getPluginDirForName("scala").file}/lib/bundledScala"
    } else {
        println '[scalaPlugin] Using SCALA_HOME Scala distribution'
    }

//ant.path(id: "grails.compile.classpath", compileClasspath)

    ant.path(id: "scalaJars") {
        fileset(dir: "${scalaHome}/lib", includes: "*.jar")
    }

    ant.path(id: "scala.compile.classpath") {
        path(refid: "grails.compile.classpath")
        path(refid: "scalaJars")
    }

    ant.taskdef(name: 'generateGroovyStubsForScala', classname: 'org.codehaus.groovy.grails.cli.GenerateStubsTask')
    ant.taskdef(name: 'scalac', classname: 'scala.tools.ant.Scalac', classpathref: "scala.compile.classpath")

    try {
        //todo use ${grails.target}/docs/classes
        //todo exclude "**/*.properties" from src for stubs
        def stubdir = "${classesDir.absolutePath}/stubs"
        println "[scalaPlugin] Generating Groovy stubs to $stubdir"
        ant.mkdir(dir: stubdir)
//        ant.chmod(file:stubdir, perm:"777")

        ant.generateGroovyStubsForScala(destdir: stubdir, classpathref: "grails.compile.classpath") {
            def excludedPaths = ["views", "i18n", "conf"] // conf gets special handling

            for (dir in new File("${basedir}/grails-app").listFiles()) {
                if (!excludedPaths.contains(dir.name) && dir.isDirectory())
                    src(path: "${dir}")
            }
            // Handle conf/ separately to exclude subdirs/package misunderstandings
//                src(path: "${basedir}/grails-app/conf")
            // This stops resources.groovy becoming "spring.resources"
//                src(path: "${basedir}/grails-app/conf/spring")
            src(path: "${basedir}/src/groovy")
//                src(path: "${basedir}/src/java")
//                src(path: "${basedir}/src/scala")
        }


        println "[scalaPlugin] Compiling Scala sources with SCALA_HOME=${scalaHome} to $classesDirPath"
        def scalaSrcEncoding = buildConfig.scala?.src?.encoding ?: 'UTF-8'
        addScalaToCompileSrcPaths(compileBinding)

        ant.mkdir(dir: classesDirPath)
        ant.scalac(destdir: classesDirPath,
                classpathref: "scala.compile.classpath",
                encoding: scalaSrcEncoding) {
            src(path: "${basedir}/src/java")
            src(path: "${basedir}/src/scala")
            src(path: "${classesDir.absolutePath}/stubs")
        }

        //todo enable
//            ant.delete(dir:"${classesDir.absolutePath}/stubs")

        if (!buildConfig.scala?.no?.jar?.copy) copyScalaLibs(ant)
    } catch (Exception e) {
        Ant.fail(message: "Could not compile Scala sources: " + e.class.simpleName + ": " + e.message)
    }
}

//todo update to scala-2.7.4
//todo test on a fresh project
//todo test with updated IntelliJ IDEA Scala plugin - jars needed in dependencies
/**
 * Copies the scala libraries from either SCALA_HOME or bundled Scala location to the lib folder.
 * Doesn't overwrite alredy exitent libraries.
 */
private def copyScalaLibs(ant) {
    println "[scalaPlugin] Copying Scala jar files from ${scalaHome}/lib"
    ant.copy(todir: "${basedir}/lib", overwrite: false) {
        fileset(dir: "${scalaHome}/lib", includes: "scala-library.jar scala-compiler.jar")
    }
}

/**
 * Enhances the "compilerPaths" variable with "/src/scala" so that the groovyc compiler can find Java sources under
 * the "/src/scala" folder
 */
private def addScalaToCompileSrcPaths(GantBinding compileBinding) {
    def compilerPaths = compileBinding.getVariable("compilerPaths")
    def newCompilerPaths = { String classpathId ->
        src(path: "${basedir}/src/scala")
        compilerPaths.delegate = delegate
        compilerPaths(classpathId)
    }
    compileBinding.setVariable("compilerPaths", newCompilerPaths)
}

/**
 * Detects whether we're compiling the scala plugin itself
 */
private boolean compilingScalaPlugin() { getPluginDirForName("scala") == null }
