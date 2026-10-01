package com.aura.weight.ui

import android.content.Context
import com.aura.weight.R

/**
 * A short daily encouragement. Quotes are bundled with the app so the card
 * works offline and never blocks on the network.
 */
data class Quote(val text: String, val author: String)

object Quotes {

    fun all(context: Context): List<Quote> {
        val texts = context.resources.getStringArray(R.array.quote_text)
        val authors = context.resources.getStringArray(R.array.quote_author)
        return texts.indices.map { i ->
            Quote(
                text = texts[i],
                author = authors.getOrElse(i) { "Aura" },
            )
        }
    }

    /**
     * One quote per calendar day, chosen deterministically so the card does not
     * flicker when the UI recomposes, rotates, or reloads.
     */
    fun forDay(context: Context, epochDay: Long): Quote {
        val quotes = all(context)
        if (quotes.isEmpty()) return Quote("Every day is a fresh start.", "Aura")
        val index = Math.floorMod(epochDay, quotes.size.toLong()).toInt()
        return quotes[index]
    }
}
