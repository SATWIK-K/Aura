package com.aura.weight

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aura.weight.ui.Quotes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class QuotesTest {

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    @Test
    fun quotesAreBundledAndPairedWithAuthors() {
        val quotes = Quotes.all(context())
        assertTrue("expected a decent quote library, got ${quotes.size}", quotes.size >= 24)
        quotes.forEach {
            assertTrue("blank quote text", it.text.isNotBlank())
            assertTrue("blank author for: ${it.text}", it.author.isNotBlank())
            assertTrue("quote too long to render: ${it.text.length} chars", it.text.length <= 130)
        }
        assertEquals(
            "duplicate quote text",
            quotes.size,
            quotes.map { it.text }.distinct().size,
        )
    }

    @Test
    fun sameDayAlwaysReturnsTheSameQuote() {
        val quotes = Quotes.all(context())
        val first = Quotes.forDay(context(), 20_000L)
        repeat(5) { assertEquals(first, Quotes.forDay(context(), 20_000L)) }
    }

    @Test
    fun quoteCyclesAcrossDaysAndSurvivesNonPositiveEpochs() {
        val quotes = Quotes.all(context())
        val seen = (0..quotes.size).map { Quotes.forDay(context(), 20_000L + it) }.toSet()
        assertEquals("expected a full cycle over ${quotes.size} days", quotes.size, seen.size)
        assertEquals(Quotes.forDay(context(), -5L), Quotes.forDay(context(), -5L + quotes.size))
    }
}
