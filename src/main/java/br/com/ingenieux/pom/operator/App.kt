package br.com.ingenieux.pom.operator

import br.com.ingenieux.pom.operator.POMOperator.readPomFile
import org.docopt.Docopt
import java.io.File


/**
 * Hello world!
 *
 */
object App {
    @JvmStatic
    fun main(args: Array<String>) {
        val opts = Docopt("""pom-operator 0.0.1
            
            Usage:
              pom-operator DEPENDENCY POMFILES...
            
        """.trimIndent()).withVersion("pom-operator 0.0.1").parse(*args)

        //println(opts)

        val dep = Dependency.fromString(opts["DEPENDENCY"]!! as String)

        val files = opts["POMFILES"]!! as List<String>

        for (path in files) {
            val pomFile = readPomFile(File(path))

            println("Upgrading dependency ($dep) in path $path")

            val newPomFileContents = POMOperator.upgradePom(pomFile, dep)

            val xmlContent = newPomFileContents.asXML()

            File(path).writeText(xmlContent)
        }
    }
}