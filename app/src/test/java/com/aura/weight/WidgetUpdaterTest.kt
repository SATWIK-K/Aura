package com.aura.weight

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import org.robolectric.Shadows.shadowOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aura.weight.widget.AuraWidgetProvider
import com.aura.weight.widget.BmiWidgetProvider
import com.aura.weight.widget.CompactWidgetProvider
import com.aura.weight.widget.ProgressWidgetProvider
import com.aura.weight.widget.RingWidgetProvider
import com.aura.weight.widget.StreakWidgetProvider
import com.aura.weight.widget.TodayWidgetProvider
import com.aura.weight.widget.TrendWidgetProvider
import com.aura.weight.widget.WidgetUpdater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class WidgetUpdaterTest {

    private fun app() = ApplicationProvider.getApplicationContext<Application>()

    private fun bind(id: Int, clazz: Class<out AuraWidgetProvider>) {
        shadowOf(AppWidgetManager.getInstance(app())).bindAppWidgetId(id, ComponentName(app(), clazz))
    }

    @Test
    fun updateAllBroadcastsToEveryProviderThatHasAWidget() {
        val app = app()
        val clazzes = listOf(
            TodayWidgetProvider::class.java,
            TrendWidgetProvider::class.java,
            RingWidgetProvider::class.java,
            StreakWidgetProvider::class.java,
            BmiWidgetProvider::class.java,
            CompactWidgetProvider::class.java,
            ProgressWidgetProvider::class.java,
        )
        clazzes.forEachIndexed { i, clazz -> bind(40 + i, clazz) }

        WidgetUpdater.updateAll(app)

        val intents = shadowOf(app).broadcastIntents
        assertEquals(7, intents.size)
        intents.forEach { intent ->
            assertEquals(AppWidgetManager.ACTION_APPWIDGET_UPDATE, intent.action)
            assertNotNull(intent.component)
        }
        clazzes.forEach { clazz ->
            assertTrue(
                "missing broadcast for ${clazz.simpleName}",
                intents.any { it.component?.className == clazz.name },
            )
        }
        intents.forEach { intent ->
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            assertNotNull("no ids extra for ${intent.component?.className}", ids)
            assertTrue(ids!!.isNotEmpty())
        }
    }

    @Test
    fun updateAllDoesNothingWithoutBoundWidgets() {
        val app = app()
        WidgetUpdater.updateAll(app)
        assertTrue(shadowOf(app).broadcastIntents.isEmpty())
    }

    private fun pollForViews(mgr: AppWidgetManager, id: Int): android.view.View {
        val deadline = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < deadline) {
            shadowOf(mgr).getViewFor(id)?.let { return it }
            Thread.sleep(50)
        }
        throw AssertionError("provider never delivered RemoteViews for $id")
    }

    @Test
    fun updateAllIntentsRenderWhenDispatchedToProviders() {
        val app = app()
        val mgr = AppWidgetManager.getInstance(app)
        val ids = IntArray(7) { 60 + it }
        listOf(
            TodayWidgetProvider::class.java,
            TrendWidgetProvider::class.java,
            RingWidgetProvider::class.java,
            StreakWidgetProvider::class.java,
            BmiWidgetProvider::class.java,
            CompactWidgetProvider::class.java,
            ProgressWidgetProvider::class.java,
        ).forEachIndexed { i, clazz -> bind(ids[i], clazz) }

        WidgetUpdater.updateAll(app)
        val intents = shadowOf(app).broadcastIntents
        assertEquals(7, intents.size)

        intents.forEach { intent ->
            val receiver = Class
                .forName(intent.component!!.className)
                .getDeclaredConstructor()
                .newInstance() as android.content.BroadcastReceiver
            receiver.onReceive(app, intent)
            shadowOf(android.os.Looper.getMainLooper()).idle()

            val deliveredIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)!!
            deliveredIds.forEach { id ->
                assertNotNull(
                    "no RemoteViews from ${intent.component!!.className} for $id",
                    pollForViews(mgr, id),
                )
            }
        }
    }

    @Test
    fun providerDeliversAdaptiveNativeViews() {
        val app = app()
        bind(77, TodayWidgetProvider::class.java)

        val mgr = AppWidgetManager.getInstance(app)
        TodayWidgetProvider().onUpdate(app, mgr, intArrayOf(77))

        val view = pollForViews(mgr, 77)
        val root = view.findViewById<android.view.View>(android.R.id.background)
        assertNotNull("missing @android:id/background shell", root)
        val alpha = (root.background as android.graphics.drawable.ColorDrawable).alpha
        assertTrue("shell alpha $alpha outside One UI blur range 1..254", alpha in 1..254)

        val value = view.findViewById<android.widget.TextView>(R.id.widget_value)
        assertNotNull("missing native widget value text", value)
        assertTrue("widget value text is empty", value.text.isNotEmpty())
    }
}
