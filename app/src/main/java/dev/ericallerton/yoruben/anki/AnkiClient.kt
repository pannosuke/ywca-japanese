package dev.ericallerton.yoruben.anki

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import dev.ericallerton.yoruben.core.StudyCard

/**
 * AnkiDroid's public ContentProvider. Scheduling stays inside Anki so AnkiWeb stays in sync.
 * https://github.com/ankidroid/Anki-Android/blob/main/api/src/main/java/com/ichi2/anki/FlashCardsContract.kt
 */
class AnkiClient(private val context: Context) {
    enum class Availability { NOT_INSTALLED, NEEDS_PERMISSION, READY }

    fun availability(): Availability {
        val installed = try {
            context.packageManager.getPackageInfo(PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
        if (!installed) return Availability.NOT_INSTALLED
        val granted = context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED
        return if (granted) Availability.READY else Availability.NEEDS_PERMISSION
    }

    fun dueCards(deckName: String, noteType: String, limit: Int): List<StudyCard>? {
        if (availability() != Availability.READY) return null
        return try {
            val deckId = findDeck(deckName) ?: return null
            val resolver = context.contentResolver
            val cursor = resolver.query(
                SCHEDULE,
                null,
                "limit=?, deckID=?",
                arrayOf(limit.toString(), deckId.toString()),
                null,
            ) ?: return null
            cursor.use {
                val noteCol = it.getColumnIndex("note_id")
                val ordCol = it.getColumnIndex("ord")
                if (noteCol < 0 || ordCol < 0) return emptyList()
                val cards = mutableListOf<StudyCard>()
                while (it.moveToNext()) {
                    val noteId = it.getLong(noteCol)
                    val ord = it.getInt(ordCol)
                    val card = loadNote(noteId, ord, noteType) ?: continue
                    cards += card
                }
                cards
            }
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun answer(noteId: Long, ord: Int, ease: Int, timeTakenMs: Long): Boolean {
        if (availability() != Availability.READY) return false
        return try {
            val values = ContentValues().apply {
                put("note_id", noteId)
                put("ord", ord)
                put("answer_ease", ease.coerceIn(1, 4))
                put("time_taken", timeTakenMs)
            }
            context.contentResolver.update(SCHEDULE, values, null, null) > 0
        } catch (_: SecurityException) {
            false
        }
    }

    private fun findDeck(name: String): Long? {
        val cursor = context.contentResolver.query(DECKS, arrayOf("deck_id", "deck_name"), null, null, null)
            ?: return null
        cursor.use {
            val idCol = it.getColumnIndex("deck_id")
            val nameCol = it.getColumnIndex("deck_name")
            if (idCol < 0 || nameCol < 0) return null
            while (it.moveToNext()) {
                if (it.getString(nameCol) == name) return it.getLong(idCol)
            }
        }
        return null
    }

    private fun loadNote(noteId: Long, ord: Int, noteType: String): StudyCard? {
        val note = context.contentResolver.query(Uri.withAppendedPath(NOTES, noteId.toString()), null, null, null, null)
            ?: return null
        val (mid, fields) = note.use {
            if (!it.moveToFirst()) return null
            val midCol = it.getColumnIndex("mid")
            val fldCol = it.getColumnIndex("flds")
            if (midCol < 0 || fldCol < 0) return null
            it.getLong(midCol) to it.getString(fldCol).orEmpty().split('\u001f')
        }
        val model = context.contentResolver.query(Uri.withAppendedPath(MODELS, mid.toString()), null, null, null, null)
            ?: return null
        val (modelName, names) = model.use {
            if (!it.moveToFirst()) return null
            val nameCol = it.getColumnIndex("name")
            val fieldsCol = it.getColumnIndex("field_names")
            val raw = if (fieldsCol >= 0) it.getString(fieldsCol).orEmpty() else ""
            val parsed = if (raw.contains('\u001f')) raw.split('\u001f') else raw.split(',')
            (if (nameCol >= 0) it.getString(nameCol).orEmpty() else "") to parsed.map { n -> n.trim().trim('"') }
        }
        if (noteType.isNotBlank() && modelName.isNotBlank() && modelName != noteType) return null
        fun field(name: String, fallback: Int): String {
            val index = names.indexOfFirst { it.equals(name, ignoreCase = true) }
            val i = if (index >= 0) index else fallback
            return fields.getOrElse(i) { "" }
        }
        return StudyCard(
            id = "anki:$noteId:$ord",
            lessonId = "",
            word = field("Word", 0),
            definition = field("Definition", 1),
            meaning = field("Meaning", 2),
            sentence = field("Sentence", 3),
            sentenceEn = field("SentenceEN", 4),
            noteId = noteId,
            ord = ord,
            fromAnki = true,
        )
    }

    companion object {
        const val PACKAGE = "com.ichi2.anki"
        const val PERMISSION = "com.ichi2.anki.permission.READ_WRITE_DATABASE"
        private val SCHEDULE: Uri = Uri.parse("content://com.ichi2.anki.flashcards/schedule")
        private val DECKS: Uri = Uri.parse("content://com.ichi2.anki.flashcards/decks")
        private val NOTES: Uri = Uri.parse("content://com.ichi2.anki.flashcards/notes")
        private val MODELS: Uri = Uri.parse("content://com.ichi2.anki.flashcards/models")
    }
}
