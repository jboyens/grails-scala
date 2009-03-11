import org.codehaus.gant.GantBinding

Ant.property(environment: "env")
scalaHome = Ant.antProject.properties."env.SCALA_HOME"

/**
 * Returns a classpath containing all Scala jar files found in ${SCALA_HOME}/lib
 */
scalaClasspath = {
    def scalaDir = resolveResources("file:${scalaHome}/lib/*")
    if (!scalaDir) println "[scalaPlugin] No Scala jar files found at SCALA_HOME=${scalaHome}"
    for (d in scalaDir) {
        pathelement(location: "${d.file.absolutePath}")
    }
}

/**
 * Hooks to the compile grails event
 */
eventCompileStart = {GantBinding compileBinding ->

//ant.path(id: "grails.compile.classpath", compileClasspath)

    ant.path(id: "grails.compile.scala", scalaClasspath)

    ant.taskdef(name: 'scalac', classname: 'scala.tools.ant.Scalac', classpathref: "grails.compile.scala")

    if (!scalaHome) println '[scalaPlugin] Cannot find SCALA_HOME. Skipping Scala compilation'
    else {
        println "[scalaPlugin] Compiling Scala sources with SCALA_HOME=${scalaHome} to $classesDirPath"

        Ant.sequential {
            addScalaToCompileSrcPaths(compileBinding)
            addScalaToCompileClassPath(compileBinding)

            ant.mkdir(dir: classesDirPath)
            def scalaSrcEncoding = buildConfig.scala?.src?.encoding ?: 'UTF-8'

            //todo document limitations
            //todo document src folders
            //todo document configuration
            //todo document need for scala-lib in the lib folder
            //todo document need for SCALA_HOME
            //todo enable Scala in tests
            //todo generate Groovy skeletons
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

