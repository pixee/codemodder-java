package br.com.ingenieux.pom.operator


/**
 * Hello world!
 *
 */
object App {
    @JvmStatic
    fun main(args: Array<String>) {
//        val xmlReader = SAXReader()
//
//        val file = File("pom.xml")
//
//        val originalContent = file.readText()
//
//        val doc = xmlReader.read(originalContent.byteInputStream())
//
//        val testNodes  = doc.selectNodes("/*[local-name()='project']/*[local-name()='dependencies']/*[local-name()='dependency']")
//
//        val nodes = doc.selectNodes("/*[local-name()='project']/*[local-name()='dependencies']/*[local-name()='dependency'][./*[local-name()='groupId'][text()='jaxen'] and ./*[local-name()='artifactId'][text()='jaxen']]/*[local-name()='version']")
//
//        nodes[0].text = "1.1.6"
//
//        val result = doc.asXML()
//
//        val diff = DiffUtils.diff(originalContent, result, null)
//
//        println(diff)
//
        /*
        // TODO: At some point write main flow and CLI App (see docopt)
        val registry = DOMImplementationRegistry.newInstance()
        val domImplementationLS = registry.getDOMImplementation("LS") as DOMImplementationLS
        val builder = domImplementationLS.createLSParser(
                DOMImplementationLS.MODE_SYNCHRONOUS, null)
        val document = builder.parseURI("pom.xml")



        val writer: LSSerializer = domImplementationLS.createLSSerializer()

        val str = writer.writeToString(document)

        println(str)
         */


    }
}