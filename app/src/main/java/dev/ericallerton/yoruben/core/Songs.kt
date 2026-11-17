package dev.ericallerton.yoruben.core

data class MnemonicSong(
    val id: String,
    val titleRuby: String,
    val verseRuby: String,
    val verseEn: String,
    val tones: List<Int>,
)

object Songs {
    val all: List<MnemonicSong> = listOf(
        MnemonicSong(
            "te-tte",
            "っての うた",
            "う と つ と る は、って。帰[かえ]って、入[はい]って、待[ま]って。",
            "う, つ, and る become って.",
            listOf(70, 70, 90),
        ),
        MnemonicSong(
            "te-nde",
            "んでの うた",
            "む と ぶ と ぬ は、んで。飲[の]んで、遊[あそ]んで、死[し]んで。",
            "む, ぶ, and ぬ become んで.",
            listOf(60, 64, 55),
        ),
        MnemonicSong(
            "te-ite",
            "いての うた",
            "く は いて。書[か]いて、聞[き]いて。行[い]く だけ 行[い]って。",
            "く becomes いて. 行く alone becomes いって.",
            listOf(72, 76, 72, 64),
        ),
        MnemonicSong(
            "te-ide",
            "いでの うた",
            "ぐ は いで。泳[およ]いで。",
            "ぐ becomes いで.",
            listOf(55, 60, 52),
        ),
        MnemonicSong(
            "te-shite",
            "しての うた",
            "す は して。話[はな]して、消[け]して。",
            "す becomes して.",
            listOf(67, 67, 74),
        ),
    )

    fun forRule(ruleId: String): MnemonicSong? = when (ruleId) {
        "te-iku" -> all.first { it.id == "te-ite" }
        else -> all.firstOrNull { it.id == ruleId }
    }
}
