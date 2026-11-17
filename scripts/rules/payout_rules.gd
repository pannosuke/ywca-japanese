class_name PayoutRules
extends RefCounted

static func battle(base: int, plays: Array, combo: int) -> Dictionary:
	var rarity_gems := 0
	for play in plays:
		if bool(play["correct"]):
			rarity_gems += RarityRules.gem_value(str(play["rarity"]))
	var combo_gems := int(combo / 3.0)
	return {
		"base": base,
		"rarity": rarity_gems,
		"combo": combo_gems,
		"total": base + rarity_gems + combo_gems,
	}

static func base_for(node_type: String) -> int:
	match node_type:
		"elite":
			return 6
		"boss":
			return 10
		"treasure":
			return 4
		_:
			return 3
