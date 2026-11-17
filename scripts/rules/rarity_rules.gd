class_name RarityRules
extends RefCounted

const ORDER := ["common", "uncommon", "rare", "ultra"]
const GEM_ON_USE := {"common": 1, "uncommon": 2, "rare": 4, "ultra": 7}
const DAMAGE_BONUS := {"common": 0, "uncommon": 2, "rare": 4, "ultra": 7}

static func fresh(card_id: String) -> Dictionary:
	return {
		"id": card_id,
		"rarity": "common",
		"uses": 0,
		"correct": 0,
		"miss_streak": 0,
		"speed_total": 0,
	}

static func record(state: Dictionary, correct: bool, speed_ms: int) -> Dictionary:
	var next := state.duplicate(true)
	next["uses"] = int(next["uses"]) + 1
	next["speed_total"] = int(next["speed_total"]) + speed_ms
	if correct:
		next["correct"] = int(next["correct"]) + 1
		next["miss_streak"] = 0
		next["rarity"] = _promote(next)
	else:
		next["miss_streak"] = int(next["miss_streak"]) + 1
		if int(next["miss_streak"]) >= 3:
			next["rarity"] = _demote(str(next["rarity"]))
			next["miss_streak"] = 0
	return next

static func accuracy(state: Dictionary) -> float:
	if int(state["uses"]) == 0:
		return 0.0
	return float(state["correct"]) / float(state["uses"])

static func average_ms(state: Dictionary) -> int:
	if int(state["uses"]) == 0:
		return 99999
	return int(float(state["speed_total"]) / float(state["uses"]))

static func gem_value(rarity: String) -> int:
	return int(GEM_ON_USE[rarity])

static func damage_bonus(rarity: String) -> int:
	return int(DAMAGE_BONUS[rarity])

static func next_need(state: Dictionary) -> String:
	var rarity := str(state["rarity"])
	if rarity == "ultra":
		return "さいこう"
	if rarity == "common":
		return "4かい ただしく"
	if rarity == "uncommon":
		return "8かい、せいかい 85%"
	return "12かい、せいかい 90%、はやく"

static func _promote(state: Dictionary) -> String:
	var rarity := str(state["rarity"])
	var uses := int(state["uses"])
	var rate := accuracy(state)
	var speed := average_ms(state)
	if rarity == "common" and uses >= 4 and rate >= 0.75:
		return "uncommon"
	if rarity == "uncommon" and uses >= 8 and rate >= 0.85:
		return "rare"
	if rarity == "rare" and uses >= 12 and rate >= 0.9 and speed <= 6000:
		return "ultra"
	return rarity

static func _demote(rarity: String) -> String:
	var index := ORDER.find(rarity)
	if index <= 0:
		return "common"
	return ORDER[index - 1]
