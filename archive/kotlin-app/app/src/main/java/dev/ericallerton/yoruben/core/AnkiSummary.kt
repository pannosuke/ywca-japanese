package dev.ericallerton.yoruben.core

data class AnkiDeckRow(val id: Long, val name: String)

data class AnkiNoteRow(val id: Long, val tags: String)

data class AnkiCardRow(
    val id: Long,
    val noteId: Long,
    val deckId: Long,
    val queue: Int,
)

enum class CardBucket { New, Learning, Review, Suspended, Other }

data class LessonAnkiStats(
    val lessonDate: String,
    val tag: String,
    val total: Int,
    val newCount: Int,
    val learning: Int,
    val review: Int,
    val suspended: Int,
    val other: Int,
)

data class AnkiSummary(
    val deckFound: Boolean,
    val deckNames: List<String>,
    val lessons: List<LessonAnkiStats>,
    val matchedCards: Int,
)

object AnkiSummarizer {
    fun bucket(queue: Int): CardBucket = when (queue) {
        -1 -> CardBucket.Suspended
        0 -> CardBucket.New
        1, 3 -> CardBucket.Learning
        2 -> CardBucket.Review
        else -> CardBucket.Other
    }

    fun parseTags(tags: String): List<String> =
        tags.split(' ', '\u3000').map { it.trim() }.filter { it.isNotEmpty() }

    fun isTargetDeck(name: String, deckName: String): Boolean {
        return name == deckName || name.startsWith("$deckName::") || name.startsWith("$deckName:")
    }

    fun summarize(
        decks: List<AnkiDeckRow>,
        notes: List<AnkiNoteRow>,
        cards: List<AnkiCardRow>,
        lessonTags: Map<String, String>,
        deckName: String,
    ): AnkiSummary {
        val matchedDecks = decks.filter { isTargetDeck(it.name, deckName) }
        val deckIds = matchedDecks.map { it.id }.toSet()
        val cardsByNote = cards.groupBy { it.noteId }
        val stats = lessonTags.map { (date, tag) ->
            val noteIds = notes.filter { tag in parseTags(it.tags) }.map { it.id }.toSet()
            val relevant = noteIds.flatMap { id -> cardsByNote[id].orEmpty() }
                .filter { card -> deckIds.isEmpty() || card.deckId in deckIds }
            val buckets = relevant.groupingBy { bucket(it.queue) }.eachCount()
            LessonAnkiStats(
                lessonDate = date,
                tag = tag,
                total = relevant.size,
                newCount = buckets[CardBucket.New] ?: 0,
                learning = buckets[CardBucket.Learning] ?: 0,
                review = buckets[CardBucket.Review] ?: 0,
                suspended = buckets[CardBucket.Suspended] ?: 0,
                other = buckets[CardBucket.Other] ?: 0,
            )
        }
        return AnkiSummary(
            deckFound = matchedDecks.isNotEmpty(),
            deckNames = matchedDecks.map { it.name },
            lessons = stats,
            matchedCards = stats.sumOf { it.total },
        )
    }
}
