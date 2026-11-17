package dev.ericallerton.yoruben.ui.fx

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import kotlin.concurrent.thread

class Chimes(context: Context) {
    private val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 72) }.getOrNull()

    fun answer(correct: Boolean) {
        val id = if (correct) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_NACK
        tone?.startTone(id, if (correct) 90 else 150)
    }

    fun jingle(id: String) {
        val notes = when (id) {
            "jingle-bright" -> intArrayOf(ToneGenerator.TONE_DTMF_1, ToneGenerator.TONE_DTMF_3, ToneGenerator.TONE_DTMF_6, ToneGenerator.TONE_DTMF_9)
            "jingle-deep" -> intArrayOf(ToneGenerator.TONE_DTMF_9, ToneGenerator.TONE_DTMF_6, ToneGenerator.TONE_DTMF_3)
            else -> intArrayOf(ToneGenerator.TONE_DTMF_5, ToneGenerator.TONE_DTMF_6, ToneGenerator.TONE_DTMF_4)
        }
        thread(name = "chime") {
            notes.forEach { note ->
                tone?.startTone(note, 110)
                Thread.sleep(130)
            }
        }
    }

    fun release() {
        tone?.release()
    }
}
