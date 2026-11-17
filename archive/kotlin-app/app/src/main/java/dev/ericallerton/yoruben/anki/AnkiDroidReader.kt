package dev.ericallerton.yoruben.anki

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import dev.ericallerton.yoruben.core.AnkiCardRow
import dev.ericallerton.yoruben.core.AnkiDeckRow
import dev.ericallerton.yoruben.core.AnkiNoteRow
import dev.ericallerton.yoruben.core.AnkiSummary
import dev.ericallerton.yoruben.core.AnkiSummarizer

/**
 * Reads the YWCA deck through AnkiDroid's content provider
 * (com.ichi2.anki.FlashCardsContract). Queries only.
 */
class AnkiDroidReader(private val context: Context) {
    fun availability(): AnkiAvailability {
        if (!isInstalled()) return AnkiAvailability.NotInstalled
        val granted = ContextCompat.checkSelfPermission(context, PERMISSION) ==
            PackageManager.PERMISSION_GRANTED
        return if (granted) AnkiAvailability.Ready else AnkiAvailability.NeedsPermission
    }

    fun read(lessonTags: Map<String, String>, deckName: String): AnkiLoad {
        if (!isInstalled()) return AnkiLoad.NotInstalled
        if (ContextCompat.checkSelfPermission(context, PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            return AnkiLoad.NeedsPermission
        }
        return try {
            val decks = queryDecks()
            val notes = queryNotes()
            val cards = queryCards()
            val summary = AnkiSummarizer.summarize(decks, notes, cards, lessonTags, deckName)
            AnkiLoad.Loaded(summary)
        } catch (error: SecurityException) {
            AnkiLoad.NeedsPermission
        } catch (error: IllegalStateException) {
            AnkiLoad.Failed("AnkiDroid is installed, but its collection is not ready. Open AnkiDroid once and let it finish setup.")
        } catch (error: IllegalArgumentException) {
            AnkiLoad.Failed("AnkiDroid did not accept the read. Open AnkiDroid once, then try again.")
        } catch (error: Exception) {
            AnkiLoad.Failed("Could not read AnkiDroid (${error.javaClass.simpleName}).")
        }
    }

    private fun isInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun queryDecks(): List<AnkiDeckRow> {
        val cursor = context.contentResolver.query(
            DECKS,
            arrayOf("deck_id", "deck_name"),
            null,
            null,
            null,
        ) ?: return emptyList()
        return cursor.use {
            val rows = mutableListOf<AnkiDeckRow>()
            while (it.moveToNext()) {
                rows += AnkiDeckRow(id = it.long("deck_id"), name = it.str("deck_name"))
            }
            rows
        }
    }

    private fun queryNotes(): List<AnkiNoteRow> {
        val cursor = context.contentResolver.query(
            NOTES,
            arrayOf("_id", "tags"),
            null,
            null,
            null,
        ) ?: return emptyList()
        return cursor.use {
            val rows = mutableListOf<AnkiNoteRow>()
            while (it.moveToNext()) {
                rows += AnkiNoteRow(id = it.long("_id"), tags = it.str("tags"))
            }
            rows
        }
    }

    private fun queryCards(): List<AnkiCardRow> {
        val cursor = context.contentResolver.query(
            CARDS,
            arrayOf("_id", "note_id", "deck_id", "queue"),
            null,
            null,
            null,
        ) ?: return emptyList()
        return cursor.use {
            val rows = mutableListOf<AnkiCardRow>()
            while (it.moveToNext()) {
                rows += AnkiCardRow(
                    id = it.long("_id"),
                    noteId = it.long("note_id"),
                    deckId = it.long("deck_id"),
                    queue = it.int("queue"),
                )
            }
            rows
        }
    }

    private fun android.database.Cursor.long(name: String): Long {
        val index = getColumnIndex(name)
        return if (index >= 0 && !isNull(index)) getLong(index) else 0L
    }

    private fun android.database.Cursor.str(name: String): String {
        val index = getColumnIndex(name)
        return if (index >= 0 && !isNull(index)) getString(index).orEmpty() else ""
    }

    private fun android.database.Cursor.int(name: String): Int {
        val index = getColumnIndex(name)
        return if (index >= 0 && !isNull(index)) getInt(index) else 0
    }

    companion object {
        const val PACKAGE = "com.ichi2.anki"
        const val PERMISSION = "com.ichi2.anki.permission.READ_WRITE_DATABASE"
        private val DECKS: Uri = Uri.parse("content://com.ichi2.anki.flashcards/decks")
        private val NOTES: Uri = Uri.parse("content://com.ichi2.anki.flashcards/notes")
        private val CARDS: Uri = Uri.parse("content://com.ichi2.anki.flashcards/cards")
    }
}

enum class AnkiAvailability { NotInstalled, NeedsPermission, Ready }

sealed interface AnkiLoad {
    data object NotInstalled : AnkiLoad
    data object NeedsPermission : AnkiLoad
    data class Failed(val message: String) : AnkiLoad
    data class Loaded(val summary: AnkiSummary) : AnkiLoad
}
