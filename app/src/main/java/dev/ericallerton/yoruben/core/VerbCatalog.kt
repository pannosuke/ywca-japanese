package dev.ericallerton.yoruben.core

object VerbCatalog {
    val all: List<Verb> = listOf(
        v("miru", "見[み]る", "みる", VerbGroup.ICHIDAN, "watch; see", "2026-10-20", true),
        v("taberu", "食[た]べる", "たべる", VerbGroup.ICHIDAN, "eat", "2026-10-23", true),
        v("hairu", "入[はい]る", "はいる", VerbGroup.GODAN, "enter", "2026-10-23", true),
        v("ryokou", "旅行[りょこう]する", "りょこうする", VerbGroup.IRREGULAR, "travel", "2026-10-23", true),
        v("asobu", "遊[あそ]ぶ", "あそぶ", VerbGroup.GODAN, "play; hang out", "2026-10-23", true),
        v("neru", "寝[ね]る", "ねる", VerbGroup.ICHIDAN, "sleep", "2026-10-23", true),
        v("okiru", "起[お]きる", "おきる", VerbGroup.ICHIDAN, "get up", "2026-10-23", true),
        v("hataraku", "働[はたら]く", "はたらく", VerbGroup.GODAN, "work", "2026-10-23", true),
        v("benkyou", "勉強[べんきょう]する", "べんきょうする", VerbGroup.IRREGULAR, "study", "2026-10-23", true),
        v("kekkon", "結婚[けっこん]する", "けっこんする", VerbGroup.IRREGULAR, "get married", "2026-10-23", true),
        v("nomu", "飲[の]む", "のむ", VerbGroup.GODAN, "drink", "2026-10-23", true),
        v("au", "会[あ]う", "あう", VerbGroup.GODAN, "meet", "2026-10-30", true),
        v("noboru", "登[のぼ]る", "のぼる", VerbGroup.GODAN, "climb", "2026-10-30", true),
        v("tomaru", "止[と]まる", "とまる", VerbGroup.GODAN, "stop", "2026-11-13", true),
        v("noru", "乗[の]る", "のる", VerbGroup.GODAN, "ride", "2026-11-13", true),
        v("oriru", "降[お]りる", "おりる", VerbGroup.ICHIDAN, "get off", "2026-11-13", true),
        v("kasu", "貸[か]す", "かす", VerbGroup.GODAN, "lend", "2026-11-13", true),
        v("kaku", "書[か]く", "かく", VerbGroup.GODAN, "write", "2026-11-13", true),
        v("oyogu", "泳[およ]ぐ", "およぐ", VerbGroup.GODAN, "swim", "2026-11-13", true),
        v("motsu", "持[も]つ", "もつ", VerbGroup.GODAN, "hold", "2026-11-13", true),
        v("kaeru", "帰[かえ]る", "かえる", VerbGroup.GODAN, "go home", "2026-11-13", true),
        v("shinu", "死[し]ぬ", "しぬ", VerbGroup.GODAN, "die", "2026-11-13", true),
        v("hanasu", "話[はな]す", "はなす", VerbGroup.GODAN, "speak", "2026-11-13", true),
        v("akeru", "開[あ]ける", "あける", VerbGroup.ICHIDAN, "open", "2026-11-13", true),
        v("shimeru", "閉[し]める", "しめる", VerbGroup.ICHIDAN, "close", "2026-11-13", true),
        v("kiku", "聞[き]く", "きく", VerbGroup.GODAN, "listen", "2026-11-13", true),
        v("dasu", "出[だ]す", "だす", VerbGroup.GODAN, "take out", "2026-11-13", true),
        v("yomu", "読[よ]む", "よむ", VerbGroup.GODAN, "read", "2026-11-13", true),
        v("miseru", "見[み]せる", "みせる", VerbGroup.ICHIDAN, "show", "2026-11-13", true),
        v("kiru", "切[き]る", "きる", VerbGroup.GODAN, "cut", "2026-11-13", true),
        v("tsukeru", "付[つ]ける", "つける", VerbGroup.ICHIDAN, "turn on", "2026-11-13", true),
        v("kesu", "消[け]す", "けす", VerbGroup.GODAN, "turn off", "2026-11-13", true),
        v("toru", "撮[と]る", "とる", VerbGroup.GODAN, "take a photo", "2026-11-13", true),
        v("oshieru", "教[おし]える", "おしえる", VerbGroup.ICHIDAN, "teach", "2026-11-13", true),
        v("okuru", "送[おく]る", "おくる", VerbGroup.GODAN, "send", "2026-11-13", true),
        v("hiku", "弾[ひ]く", "ひく", VerbGroup.GODAN, "play an instrument", "2026-11-13", true),
        v("iku", "行[い]く", "いく", VerbGroup.GODAN, "go", null, false),
        v("kuru", "来[く]る", "くる", VerbGroup.IRREGULAR, "come", null, false),
        v("suru", "する", "する", VerbGroup.IRREGULAR, "do", null, false),
        v("kau", "買[か]う", "かう", VerbGroup.GODAN, "buy", null, false),
        v("matsu", "待[ま]つ", "まつ", VerbGroup.GODAN, "wait", null, false),
    )

    fun byId(id: String): Verb = all.first { it.id == id }

    fun forPractice(group: VerbGroup? = null, lessonDate: String? = null): List<Verb> {
        return all.filter { verb ->
            (group == null || verb.group == group) &&
                (lessonDate == null || verb.lessonDate == lessonDate)
        }.ifEmpty { all }
    }

    private fun v(
        id: String,
        ruby: String,
        kana: String,
        group: VerbGroup,
        gloss: String,
        date: String?,
        curriculum: Boolean,
    ) = Verb(id, ruby, kana, group, gloss, date, curriculum)
}
