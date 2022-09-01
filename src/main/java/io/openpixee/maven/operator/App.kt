package io.openpixee.maven.operator

import org.docopt.Docopt
import java.io.File
import java.lang.IllegalStateException


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

        @Suppress("UNCHECKED_CAST") val files = opts["POMFILES"]!! as List<String>

        for (path in files) {
            val ctx = Context.load(File(path), dep)

            println("Upgrading dependency ($dep) in path $path")

            val upgradeResult = POMOperator.upgradePom(ctx)

            if (!upgradeResult)
                throw IllegalStateException("Unexpected failure on upgradeResult")

            val xmlContent = ctx.resultPom.asXML()

            File(path).writeText(xmlContent)
        }
    }
}