package dev.ericallerton.yoruben.data

import dev.ericallerton.yoruben.core.AnswerRecord
import dev.ericallerton.yoruben.core.BuyResult
import dev.ericallerton.yoruben.core.CosmeticSlot
import dev.ericallerton.yoruben.core.Cosmetics
import dev.ericallerton.yoruben.core.Ledger
import dev.ericallerton.yoruben.core.Streak
import dev.ericallerton.yoruben.data.db.AppDatabase
import dev.ericallerton.yoruben.data.db.DrillLogEntity
import dev.ericallerton.yoruben.data.db.MetaUnlockEntity
import dev.ericallerton.yoruben.data.db.OwnedItemEntity
import dev.ericallerton.yoruben.data.db.PlayerEntity
import dev.ericallerton.yoruben.data.db.RegionClearEntity
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GameStore(private val db: AppDatabase) {
    private val dao = db.gameDao()

    fun observe(): Flow<Ledger> = combine(dao.observePlayer(), dao.observeOwned()) { player, owned ->
        toLedger(player, owned.map { it.itemId }.toSet())
    }

    suspend fun ensure() {
        if (dao.player() != null) return
        val now = System.currentTimeMillis()
        dao.upsertPlayer(PlayerEntity(gems = 0, xp = 0, streak = 0, lastStreakDay = "", equipped = encode(Cosmetics.defaults)))
        Cosmetics.freeIds.forEach { dao.insertOwned(OwnedItemEntity(it, now)) }
    }

    suspend fun ledger(): Ledger {
        ensure()
        return toLedger(dao.player(), dao.owned().map { it.itemId }.toSet())
    }

    suspend fun history(): List<AnswerRecord> {
        return dao.recentDrills().map { AnswerRecord(it.verbId, it.ruleId, it.correct, it.atEpochMs) }
    }

    suspend fun clears(): Set<String> = dao.clears().toSet()

    fun observeClears(): Flow<List<String>> = dao.observeClears()

    suspend fun unlocks(): Set<String> = dao.unlocks().toSet()

    suspend fun grant(gems: Int, xp: Int) {
        ensure()
        val row = dao.player() ?: return
        val today = tokyoDay()
        val (streak, day) = if (gems == 0 && xp == 0) {
            row.streak to row.lastStreakDay
        } else {
            Streak.next(row.streak, row.lastStreakDay, today)
        }
        dao.upsertPlayer(row.copy(gems = row.gems + gems, xp = row.xp + xp, streak = streak, lastStreakDay = day))
        if (streak >= 3) dao.insertUnlock(MetaUnlockEntity("ach-streak", System.currentTimeMillis()))
    }

    suspend fun noteDrill(verbId: String, ruleId: String, correct: Boolean, gems: Int, xp: Int) {
        dao.insertDrill(DrillLogEntity(verbId = verbId, ruleId = ruleId, correct = correct, atEpochMs = System.currentTimeMillis()))
        grant(gems, xp)
    }

    suspend fun spend(amount: Int): Boolean {
        ensure()
        val row = dao.player() ?: return false
        if (row.gems < amount) return false
        dao.upsertPlayer(row.copy(gems = row.gems - amount))
        return true
    }

    suspend fun buy(itemId: String): BuyResult {
        val item = Cosmetics.byId(itemId) ?: return BuyResult.TooPoor
        val current = ledger()
        val price = Cosmetics.priceOf(item, tokyoEpochDay())
        val result = Cosmetics.buy(current, item, price)
        if (result is BuyResult.Ok) {
            val row = dao.player() ?: return result
            dao.upsertPlayer(row.copy(gems = result.ledger.gems))
            dao.insertOwned(OwnedItemEntity(item.id, System.currentTimeMillis()))
            dao.insertUnlock(MetaUnlockEntity("ach-buy", System.currentTimeMillis()))
        }
        return result
    }

    suspend fun equip(itemId: String) {
        val item = Cosmetics.byId(itemId) ?: return
        val current = ledger()
        val next = Cosmetics.equip(current, item)
        if (next.equipped == current.equipped) return
        val row = dao.player() ?: return
        dao.upsertPlayer(row.copy(equipped = encode(next.equipped)))
    }

    suspend fun clearRegion(date: String) {
        dao.insertClear(RegionClearEntity(date, System.currentTimeMillis()))
        dao.insertUnlock(MetaUnlockEntity("ach-region", System.currentTimeMillis()))
    }

    suspend fun unlock(id: String) {
        dao.insertUnlock(MetaUnlockEntity(id, System.currentTimeMillis()))
    }

    private fun toLedger(player: PlayerEntity?, owned: Set<String>): Ledger {
        if (player == null) return Cosmetics.starter()
        val equipped = decode(player.equipped)
        return Ledger(
            gems = player.gems,
            xp = player.xp,
            streak = player.streak,
            lastStreakDay = player.lastStreakDay,
            owned = owned.ifEmpty { Cosmetics.freeIds },
            equipped = Cosmetics.defaults + equipped,
        )
    }

    private fun encode(equipped: Map<CosmeticSlot, String>): String {
        return equipped.entries.joinToString(",") { "${it.key.name}=${it.value}" }
    }

    private fun decode(raw: String): Map<CosmeticSlot, String> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(",").mapNotNull { part ->
            val bits = part.split("=", limit = 2)
            if (bits.size != 2) return@mapNotNull null
            val slot = runCatching { CosmeticSlot.valueOf(bits[0]) }.getOrNull() ?: return@mapNotNull null
            slot to bits[1]
        }.toMap()
    }

    companion object {
        private val zone = ZoneId.of("Asia/Tokyo")

        fun tokyoDay(now: Long = System.currentTimeMillis()): String {
            return Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toString()
        }

        fun tokyoEpochDay(now: Long = System.currentTimeMillis()): Long {
            return Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toEpochDay()
        }
    }
}
