package br.com.ingenieux.pom.operator

import org.docopt.Docopt
import java.io.File


/**
 * Hello world!
 *
 */
object App {
    @JvmStatic
    fun main(args: Array<String>) {
        val opts = Docopt(
            """pom-operator 0.0.1
            
            Usage:
              pom-operator DEPENDENCY POMFILES...
            
        """.trimIndent()
        ).withVersion("pom-operator 0.0.1").parse(*args)

        val dep = Dependency.fromString(opts["DEPENDENCY"]!! as String)

        val files = opts["POMFILES"]!! as List<String>

        for (path in files) {
            val ctx = Context.load(File(path), dep)

            println("Upgrading dependency ($dep) in path $path")

            val newPomFileContents = POMOperator.upgradePom(ctx)

            val xmlContent = ctx.resultPom.asXML()

            File(path).writeText(xmlContent)
        }
    }
}