package dev.ericallerton.yoruben.core

enum class CosmeticSlot {
    TUNIC, SHIELD, SWORD, COMPANION, THEME, FRAME, TITLE, DECOR, JINGLE,
}

data class Cosmetic(
    val id: String,
    val slot: CosmeticSlot,
    val nameRuby: String,
    val nameEn: String,
    val price: Int,
    val swatch: Long,
)

data class Ledger(
    val gems: Int,
    val xp: Int,
    val streak: Int,
    val lastStreakDay: String,
    val owned: Set<String>,
    val equipped: Map<CosmeticSlot, String>,
) {
    fun equipped(slot: CosmeticSlot): String = equipped[slot] ?: Cosmetics.defaults.getValue(slot)
}

sealed class BuyResult {
    data class Ok(val ledger: Ledger) : BuyResult()
    data object AlreadyOwned : BuyResult()
    data object TooPoor : BuyResult()
}

object Cosmetics {
    const val GATES_STUDY = false

    val defaults: Map<CosmeticSlot, String> = mapOf(
        CosmeticSlot.TUNIC to "tunic-leaf",
        CosmeticSlot.SHIELD to "shield-wood",
        CosmeticSlot.SWORD to "sword-plain",
        CosmeticSlot.COMPANION to "light-gold",
        CosmeticSlot.THEME to "theme-forest",
        CosmeticSlot.FRAME to "frame-parchment",
        CosmeticSlot.TITLE to "title-walker",
        CosmeticSlot.DECOR to "deco-none",
        CosmeticSlot.JINGLE to "jingle-soft",
    )

    val catalog: List<Cosmetic> = listOf(
        item("tunic-leaf", CosmeticSlot.TUNIC, "葉[は]の ころも", "Leaf tunic", 0, 0xFF1F6B3A),
        item("tunic-gold", CosmeticSlot.TUNIC, "金[きん]の ころも", "Gold tunic", 8, 0xFFE2B33A),
        item("tunic-night", CosmeticSlot.TUNIC, "夜[よる]の ころも", "Night tunic", 12, 0xFF143D28),
        item("tunic-rose", CosmeticSlot.TUNIC, "夕[ゆう]の ころも", "Dusk tunic", 14, 0xFFB85C38),
        item("shield-wood", CosmeticSlot.SHIELD, "木[き]の 盾[たて]", "Wood shield", 0, 0xFF8A5A32),
        item("shield-gold", CosmeticSlot.SHIELD, "金[きん]の 盾[たて]", "Gold shield", 10, 0xFFE2B33A),
        item("shield-leaf", CosmeticSlot.SHIELD, "葉[は]の 盾[たて]", "Leaf shield", 16, 0xFF2E8B57),
        item("sword-plain", CosmeticSlot.SWORD, "はがねの つるぎ", "Plain sword", 0, 0xFFD9D3C5),
        item("sword-gold", CosmeticSlot.SWORD, "金[きん]の つるぎ", "Gold sword", 10, 0xFFE2B33A),
        item("sword-green", CosmeticSlot.SWORD, "森[もり]の つるぎ", "Forest sword", 18, 0xFF1F6B3A),
        item("light-gold", CosmeticSlot.COMPANION, "金[きん]の ともしび", "Gold light", 0, 0xFFE2B33A),
        item("light-aqua", CosmeticSlot.COMPANION, "みずいろの ともしび", "Aqua light", 8, 0xFF7ED9C3),
        item("light-rose", CosmeticSlot.COMPANION, "ばらいろの ともしび", "Rose light", 12, 0xFFE7A0B0),
        item("theme-forest", CosmeticSlot.THEME, "森[もり]の いろ", "Forest colors", 0, 0xFF1F6B3A),
        item("theme-night", CosmeticSlot.THEME, "夜[よる]の いろ", "Night colors", 15, 0xFF102018),
        item("theme-gold", CosmeticSlot.THEME, "金[きん]の いろ", "Gold colors", 20, 0xFFE2B33A),
        item("theme-parchment", CosmeticSlot.THEME, "かみの いろ", "Parchment colors", 12, 0xFFF3E6C5),
        item("frame-parchment", CosmeticSlot.FRAME, "かみの わく", "Parchment frame", 0, 0xFFF3E6C5),
        item("frame-gold", CosmeticSlot.FRAME, "金[きん]の わく", "Gold frame", 10, 0xFFE2B33A),
        item("frame-stone", CosmeticSlot.FRAME, "いしの わく", "Stone frame", 14, 0xFF3E4A42),
        item("title-walker", CosmeticSlot.TITLE, "森[もり]の 歩[ある]き手[て]", "Walker of the wood", 0, 0xFF1F6B3A),
        item("title-scribe", CosmeticSlot.TITLE, "洞[ほら]の 書[か]き手[て]", "Scribe of the cave", 12, 0xFF8A5A32),
        item("title-lamp", CosmeticSlot.TITLE, "灯[あかり]の 番[ばん]", "Keeper of the lamp", 18, 0xFFE2B33A),
        item("title-song", CosmeticSlot.TITLE, "笛[ふえ]の 友[とも]", "Friend of the flute", 24, 0xFF7ED9C3),
        item("deco-none", CosmeticSlot.DECOR, "そのままの 里[さと]", "Quiet village", 0, 0xFF1F6B3A),
        item("deco-lanterns", CosmeticSlot.DECOR, "つり灯籠[とうろう]", "Hanging lamps", 10, 0xFFE2B33A),
        item("deco-banner", CosmeticSlot.DECOR, "みどりの はた", "Green banner", 16, 0xFF1F6B3A),
        item("deco-flowers", CosmeticSlot.DECOR, "みちの はな", "Path flowers", 14, 0xFFE7A0B0),
        item("jingle-soft", CosmeticSlot.JINGLE, "やわらかい ね", "Soft chime", 0, 0xFF7ED9C3),
        item("jingle-bright", CosmeticSlot.JINGLE, "あかるい ね", "Bright chime", 12, 0xFFE2B33A),
        item("jingle-deep", CosmeticSlot.JINGLE, "ひくい ね", "Deep chime", 18, 0xFF143D28),
    )

    val freeIds: Set<String> = catalog.filter { it.price == 0 }.map { it.id }.toSet()

    fun byId(id: String): Cosmetic? = catalog.firstOrNull { it.id == id }

    fun dailyDeal(epochDay: Long): Cosmetic {
        val paid = catalog.filter { it.price > 0 }
        val index = Math.floorMod(epochDay, paid.size.toLong()).toInt()
        return paid[index]
    }

    fun priceOf(item: Cosmetic, epochDay: Long): Int {
        if (item.price == 0) return 0
        if (dailyDeal(epochDay).id != item.id) return item.price
        return (item.price * 3 / 5).coerceAtLeast(4)
    }

    fun buy(ledger: Ledger, item: Cosmetic, price: Int): BuyResult {
        if (item.id in ledger.owned) return BuyResult.AlreadyOwned
        if (ledger.gems < price) return BuyResult.TooPoor
        return BuyResult.Ok(ledger.copy(gems = ledger.gems - price, owned = ledger.owned + item.id))
    }

    fun equip(ledger: Ledger, item: Cosmetic): Ledger {
        if (item.id !in ledger.owned) return ledger
        return ledger.copy(equipped = ledger.equipped + (item.slot to item.id))
    }

    fun starter(): Ledger = Ledger(
        gems = 0,
        xp = 0,
        streak = 0,
        lastStreakDay = "",
        owned = freeIds,
        equipped = defaults,
    )

    private fun item(id: String, slot: CosmeticSlot, ruby: String, en: String, price: Int, swatch: Long) =
        Cosmetic(id, slot, ruby, en, price, swatch)
}

object Rewards {
    const val DRILL_CORRECT = 3
    const val XP_CORRECT = 5
    const val XP_WRONG = 1
    const val BOSS_CLEAR = 12
}

object Streak {
    fun next(previous: Int, lastDay: String, today: String): Pair<Int, String> {
        if (lastDay == today) return previous.coerceAtLeast(1) to today
        if (lastDay.isNotEmpty() && yesterday(today) == lastDay) return (previous + 1) to today
        return 1 to today
    }

    fun yesterday(today: String): String {
        val parts = today.split("-").map { it.toInt() }
        val date = java.time.LocalDate.of(parts[0], parts[1], parts[2]).minusDays(1)
        return date.toString()
    }
}

object Levels {
    fun level(xp: Int): Int = 1 + xp / 40
}

data class Biome(
    val id: String,
    val nameRuby: String,
    val nameEn: String,
    val price: Int,
    val lessonDate: String?,
    val preview: Boolean,
)

object Biomes {
    val all: List<Biome> = listOf(
        Biome("lamp-gate", "灯[あかり]の 入口[いりぐち]", "The lamp gate", 0, null, false),
        Biome("preview-past", "た形[けい]の プレビュー", "Plain-past preview", 0, null, true),
        Biome("biome-1023", "さそいの 林[はやし]", "Invitation wood", 20, "2026-10-23", false),
        Biome("biome-1113", "て形[けい]の 洞[ほら]", "Te-form cave", 20, "2026-11-13", false),
    )

    fun open(unlocks: Set<String>, cleared: Set<String>): List<Biome> {
        return all.filter { biome ->
            biome.price == 0 || biome.id in unlocks || (biome.lessonDate != null && biome.lessonDate in cleared)
        }
    }
}
