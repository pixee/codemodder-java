package br.com.ingenieux.pom.operator


object POMOperator {
    fun upgradePom(context: Context) = Chain.create().execute(context)
}
