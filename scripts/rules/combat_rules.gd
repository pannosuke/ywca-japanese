class_name CombatRules
extends RefCounted

const FORMS := ["masu", "mashita", "masen", "masen_deshita", "nai", "te"]

static func max_hp() -> int:
	return 32

static func enemy_hp(node_type: String) -> int:
	match node_type:
		"elite":
			return 26
		"boss":
			return 42
		_:
			return 16

static func enemy_attack(node_type: String) -> int:
	match node_type:
		"elite":
			return 7
		"boss":
			return 8
		_:
			return 5

static func strike_damage(correct: bool, rarity: String, relics: Array, group: String) -> int:
	if not correct:
		return 2
	var amount := 7 + RarityRules.damage_bonus(rarity)
	if group == "ichidan" and "nidan" in relics:
		amount *= 2
	return amount

static func open(deck: Array, rng: Rng) -> Dictionary:
	var state := DeckRules.opening(deck, rng)
	DeckRules.start_turn(state, rng)
	state["block"] = 0
	state["forgiven"] = false
	return state

static func resolve(correct: bool, card: Dictionary, rarity: String, relics: Array, forgiven_left: bool) -> Dictionary:
	var landed := correct or (not correct and forgiven_left and "lantern" in relics)
	var group := str(card.get("group", ""))
	var damage := 0
	var block := 0
	var draw := 0
	if str(card["effect"]) == "strike":
		damage = strike_damage(landed, rarity, relics, group)
	elif str(card["effect"]) == "ward":
		block = 6 if landed else 1
	elif str(card["effect"]) == "chant":
		draw = 1 if landed else 0
		block = 3 if landed else 0
	else:
		block = 2 if landed else 0
	return {
		"landed": landed,
		"damage": damage,
		"block": block,
		"draw": draw,
		"forgave": landed and not correct,
	}

static func player_hit(hp: int, attack: int, block: int) -> int:
	return maxi(0, hp - maxi(0, attack - block))
