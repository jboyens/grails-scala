import org.codehaus.gant.GantBinding

/*
 * Hooks an event handler to the CompileStart event to run the Scala compiler on Java and Scala sources.
 * It also updates source and class paths for the groovyc compiler with scala sources and libraries and optionally
 * copies Scala runtime libraries to the project's lib folder.
 *
 * Scala sources may be placed and combined in any way with Java sources in either /src/java or /src/scala.
 * Currently the groovy sources are not directly visible inside the Scala sources.
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

    if (!buildConfig.scala?.no?.jar?.copy) copyScalaLibs(ant)

//ant.path(id: "grails.compile.classpath", compileClasspath)

    ant.path(id: "grails.compile.scala", scalaClasspath)
    ant.taskdef(name: 'scalac', classname: 'scala.tools.ant.Scalac', classpathref: "grails.compile.scala")

    println "[scalaPlugin] Compiling Scala sources with SCALA_HOME=${scalaHome} to $classesDirPath"

    Ant.sequential {
        addScalaToCompileSrcPaths(compileBinding)
        addScalaToCompileClassPath(compileBinding)

        ant.mkdir(dir: classesDirPath)
        def scalaSrcEncoding = buildConfig.scala?.src?.encoding ?: 'UTF-8'

        //todo document src folders
        //todo enable Scala in tests
        try {
            ant.scalac(destdir: classesDirPath,
                    classpathref: "grails.compile.classpath",
                    encoding: scalaSrcEncoding) {
                src(path: "${basedir}/src/java")
                src(path: "${basedir}/src/scala")
            }
        }
        catch (Exception e) {
            Ant.fail(message: "Could not compile Scala sources: " + e.class.simpleName + ": " + e.message)
        }
    }
}

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
 * Returns a classpath containing all Scala jar files found in ${SCALA_HOME}/lib
 */
scalaClasspath = {
    def scalaDir = resolveResources("file:${scalaHome}/lib/*")
    if (!scalaDir) println "[scalaPlugin] No Scala jar files found at ${scalaHome}"
    for (d in scalaDir) {
        pathelement(location: "${d.file.absolutePath}")
    }
}

/**
 * Enhances the "compilerPaths" variable with "/src/scala"
 */
private def addScalaToCompileSrcPaths(GantBinding compileBinding) {
    def compilerPaths = compileBinding.getVariable("compilerPaths")
    def newCompilerPaths = {String classpathId, boolean compilingTests ->
        src(path: "${basedir}/src/scala")
        compilerPaths.delegate = delegate
        compilerPaths(classpathId, compilingTests)
    }
    compileBinding.setVariable("compilerPaths", newCompilerPaths)
}

/**
 * Enhances "grails.compile.classpath" scala libraries from ${SCALA_HOME}/lib
 */
private def addScalaToCompileClassPath(GantBinding compileBinding) {
    def originalCompilerClassPath = compileBinding.getVariable("classpath")
    def newCompilerClassPath = {->
        scalaClasspath()
        originalCompilerClassPath.delegate = delegate
        originalCompilerClassPath()
    }
    ant.path(id: "grails.compile.classpath", newCompilerClassPath)
}

/**
 * Detects whether we're compiling the scala plugin itself
 */
private boolean compilingScalaPlugin() { getPluginDirForName("scala") == null }