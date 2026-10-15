package pl.cheapskountant.mobile

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class StringCatalogTest {
    @Test
    fun polishAndEnglishCatalogsHaveTheSameKeys() {
        val polish = keys(File("src/main/res/values/strings.xml"))
        val english = keys(File("src/main/res/values-en/strings.xml"))
        assertEquals(polish, english)
    }

    private fun keys(file: File): Set<String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it).attributes.getNamedItem("name").nodeValue }.toSet()
    }
}
