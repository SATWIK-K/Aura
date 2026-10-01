package com.aura.weight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class WidgetManifestTest {

    private val androidNs = "http://schemas.android.com/apk/res/android"
    private val requiredAction = "android.appwidget.action.APPWIDGET_UPDATE"
    private val forbiddenAction = "android.appwidget.provider.APPWIDGET_UPDATE"
    private val providerMetaData = "android.appwidget.provider"

    private val expectedWidgets = mapOf(
        "TodayWidgetProvider" to "widget_today",
        "TrendWidgetProvider" to "widget_trend",
        "RingWidgetProvider" to "widget_ring",
        "StreakWidgetProvider" to "widget_streak",
        "BmiWidgetProvider" to "widget_bmi",
        "CompactWidgetProvider" to "widget_compact",
        "ProgressWidgetProvider" to "widget_progress",
    )

    @Test
    fun widgetReceiversDeclareTheSystemUpdateAction() {
        val manifest = findSourceManifest()
        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(manifest)
        document.documentElement.normalize()

        val receivers = document.getElementsByTagName("receiver")
        val checked = mutableSetOf<String>()

        for (index in 0 until receivers.length) {
            val receiver = receivers.item(index) as Element
            val providerMeta = childElements(receiver, "meta-data").firstOrNull {
                it.androidAttr("name") == providerMetaData
            } ?: continue

            val className = receiver.androidAttr("name")
            val simpleName = className.substringAfterLast('.')
            val expectedResource = expectedWidgets[simpleName]
                ?: fail("Unexpected widget provider in manifest: $className")
            checked.add(simpleName)

            assertEquals(
                "$simpleName must be exported so the system can deliver widget broadcasts",
                "true",
                receiver.androidAttr("exported"),
            )
            assertEquals(
                "$simpleName must reference its provider metadata",
                "@xml/$expectedResource",
                providerMeta.androidAttr("resource"),
            )

            val actions = childElements(receiver, "intent-filter")
                .flatMap { childElements(it, "action") }
                .map { it.androidAttr("name") }
                .toSet()
            assertTrue(
                "$simpleName must use $requiredAction, not $forbiddenAction. Found: $actions",
                requiredAction in actions,
            )
            assertTrue(
                "$simpleName must not use $forbiddenAction. Found: $actions",
                forbiddenAction !in actions,
            )
        }

        assertEquals(expectedWidgets.keys, checked)
    }

    private fun Element.androidAttr(localName: String): String =
        getAttributeNS(androidNs, localName).ifEmpty { getAttribute("android:$localName") }

    private fun childElements(element: Element, tag: String): List<Element> {
        val matches = mutableListOf<Element>()
        val children = element.childNodes
        for (index in 0 until children.length) {
            val child = children.item(index)
            if (child is Element && child.tagName == tag) {
                matches.add(child)
            }
        }
        return matches
    }

    private fun findSourceManifest(): File {
        var directory: File? = File(System.getProperty("user.dir", ".")).absoluteFile
        while (directory != null) {
            val moduleManifest = File(directory, "src/main/AndroidManifest.xml")
            if (moduleManifest.isFile) return moduleManifest
            val rootManifest = File(directory, "app/src/main/AndroidManifest.xml")
            if (rootManifest.isFile) return rootManifest
            directory = directory.parentFile
        }
        throw AssertionError("Could not locate src/main/AndroidManifest.xml from the test working directory")
    }
}
