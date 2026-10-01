package com.aura.weight

import android.content.Context
import android.content.res.XmlResourceParser
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class WidgetProviderXmlTest {

    private val androidNs = "http://schemas.android.com/apk/res/android"
    private val appNs = "http://schemas.android.com/apk/res-auto"

    private val providers = listOf(
        R.xml.widget_today,
        R.xml.widget_trend,
        R.xml.widget_ring,
        R.xml.widget_streak,
        R.xml.widget_bmi,
        R.xml.widget_compact,
        R.xml.widget_progress,
    )

    private fun XmlResourceParser.rootToProviderTag() {
        while (eventType != android.content.res.XmlResourceParser.START_TAG ||
            name != "appwidget-provider"
        ) {
            next()
        }
    }

    private fun XmlResourceParser.res(namespace: String, attribute: String): Int =
        getAttributeResourceValue(namespace, attribute, 0)

    private fun assertDimension(message: String, value: String?) {
        assertTrue("$message missing", value != null)
        val number = value?.removeSuffix("dip")?.removeSuffix("dp")
        assertTrue("$message should be a positive dp dimension but was $value",
            number != null && number.toDoubleOrNull()?.let { it > 0 } == true)
    }

    @Test
    fun everyProviderDeclaresTheAttributesOneUiAndAospRequire() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        providers.forEach { id ->
            val tag = context.resources.getResourceEntryName(id)
            context.resources.getXml(id).use { p ->
                p.rootToProviderTag()

                assertNotEquals("$tag missing initialLayout", 0, p.res(androidNs, "initialLayout"))
                assertNotEquals("$tag missing previewImage", 0, p.res(androidNs, "previewImage"))
                assertNotEquals(
                    "$tag missing previewLayout (One UI picker reliability)",
                    0,
                    p.res(androidNs, "previewLayout"),
                )
                assertNotEquals("$tag missing description", 0, p.res(androidNs, "description"))
                assertDimension("$tag minWidth", p.getAttributeValue(androidNs, "minWidth"))
                assertDimension("$tag minHeight", p.getAttributeValue(androidNs, "minHeight"))
                assertTrue("$tag resizeMode", p.getAttributeIntValue(androidNs, "resizeMode", 0) != 0)
                assertTrue(
                    "$tag widgetCategory must include home_screen",
                    p.getAttributeIntValue(androidNs, "widgetCategory", 0) and 1 != 0,
                )
                assertTrue(
                    "$tag app:widgetStyle must be set for One UI blur",
                    p.getAttributeIntValue(appNs, "widgetStyle", 0) != 0,
                )
                assertTrue(
                    "$tag app:widgetSize must be set for One UI blur",
                    p.getAttributeIntValue(appNs, "widgetSize", 0) != 0,
                )
            }
        }
    }

    @Test
    fun everyPreviewLayoutInflatesToAnImageView() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        listOf(
            R.layout.widget_preview_today,
            R.layout.widget_preview_trend,
            R.layout.widget_preview_ring,
            R.layout.widget_preview_streak,
            R.layout.widget_preview_bmi,
            R.layout.widget_preview_compact,
            R.layout.widget_preview_progress,
            R.layout.widget_content_progress,
        ).forEach { id ->
            val tag = context.resources.getResourceEntryName(id)
            context.resources.getLayout(id).use { p ->
                p.rootToProviderTag2()
                assertTrue(
                    "$tag root should be a RemoteViews-safe container",
                    p.name in setOf("ImageView", "FrameLayout", "LinearLayout"),
                )
            }
        }
    }

    private fun XmlResourceParser.rootToProviderTag2() {
        while (eventType != android.content.res.XmlResourceParser.START_TAG) next()
    }
}
