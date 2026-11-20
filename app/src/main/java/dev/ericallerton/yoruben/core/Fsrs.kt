package dev.ericallerton.yoruben.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Local FSRS-5, used only when AnkiDroid is not answering the card.
 * Ratings are 1 again, 2 hard, 3 good, 4 easy. Again is always due sooner than Good.
 */
object Fsrs {
    const val DECAY = -0.5
    const val FACTOR = 19.0 / 81.0
    val weights: DoubleArray = doubleArrayOf(
        0.40255, 1.18385, 3.173, 15.69105, 7.1949, 0.5345, 1.4604, 0.0046, 1.54575,
        0.1192, 1.01925, 1.9395, 0.11, 0.29605, 2.2698, 0.2315, 2.9898, 0.51655, 0.6621,
    )

    fun review(card: FsrsCard, rating: Int, nowMs: Long): FsrsCard {
        val g = rating.coerceIn(1, 4)
        val elapsedDays = if (card.lastMs == 0L) 0.0 else (nowMs - card.lastMs) / 86_400_000.0
        val first = card.reps == 0 || card.stability <= 0.0
        val (stability, difficulty) = if (first) {
            initStability(g) to initDifficulty(g)
        } else {
            val r = retrievability(card.stability, elapsedDays.coerceAtLeast(0.0))
            val d = nextDifficulty(card.difficulty, g)
            val s = if (elapsedDays < 1.0) {
                card.stability * exp(weights[17] * (g - 3 + weights[18]))
            } else if (g == 1) {
                forgetStability(d, card.stability, r)
            } else {
                recallStability(d, card.stability, r, g)
            }
            s.coerceAtLeast(0.01) to d
        }
        val due = nowMs + intervalMs(stability, g, first)
        return FsrsCard(
            stability = stability,
            difficulty = difficulty,
            dueMs = due,
            reps = card.reps + 1,
            lapses = card.lapses + if (g == 1) 1 else 0,
            lastMs = nowMs,
        )
    }

    fun retrievability(stability: Double, elapsedDays: Double): Double {
        if (stability <= 0.0) return 0.0
        return (1.0 + FACTOR * elapsedDays / stability).pow(DECAY)
    }

    private fun initStability(g: Int) = weights[g - 1]

    private fun initDifficulty(g: Int): Double {
        val d = weights[4] - exp(weights[5] * (g - 1)) + 1.0
        return d.coerceIn(1.0, 10.0)
    }

    private fun nextDifficulty(d: Double, g: Int): Double {
        val next = d * exp(-weights[6] * (g - 3))
        val easy = initDifficulty(4)
        val reverted = weights[7] * easy + (1.0 - weights[7]) * next
        return reverted.coerceIn(1.0, 10.0)
    }

    private fun recallStability(d: Double, s: Double, r: Double, g: Int): Double {
        val hard = if (g == 2) weights[15] else 1.0
        val easy = if (g == 4) weights[16] else 1.0
        val growth = exp(weights[8]) *
            (11.0 - d) *
            s.pow(-weights[9]) *
            (exp(weights[10] * (1.0 - r)) - 1.0) *
            hard * easy
        return s * (growth + 1.0)
    }

    private fun forgetStability(d: Double, s: Double, r: Double): Double =
        weights[11] *
            d.pow(-weights[12]) *
            ((s + 1.0).pow(weights[13]) - 1.0) *
            exp(weights[14] * (1.0 - r))

    private fun intervalMs(stability: Double, g: Int, first: Boolean): Long {
        if (first && g == 1) return 60_000L
        if (first && g == 2) return 10 * 60_000L
        if (g == 1) return 10 * 60_000L
        val days = stability.coerceAtLeast(1.0)
        return (days * 86_400_000.0).roundToLong()
    }
}

@Serializable
data class FsrsCard(
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    @SerialName("due_ms") val dueMs: Long = 0,
    val reps: Int = 0,
    val lapses: Int = 0,
    @SerialName("last_ms") val lastMs: Long = 0,
)
