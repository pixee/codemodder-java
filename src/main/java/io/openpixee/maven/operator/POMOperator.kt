package io.openpixee.maven.operator


object POMOperator {
    fun upgradePom(context: Context) = Chain.create().execute(context)
}
