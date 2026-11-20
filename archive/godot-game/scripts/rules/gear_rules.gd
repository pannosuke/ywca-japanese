class_name GearRules
extends RefCounted

const SLOTS := ["weapon", "shield", "armor", "accessory", "charm"]
const RARITIES := ["common", "uncommon", "rare", "epic"]
const AFFIXES := ["atk", "spd", "def", "luk", "wis"]

static func starter_weapon() -> Dictionary:
	return {
		"id": "starter-sword",
		"slot": "weapon",
		"rarity": "common",
		"name_ruby": "木[き]の つるぎ",
		"affixes": {"atk": 1},
		"durability": 12,
		"max_durability": 12,
	}

static func roll(seed_value: int, verb_id: String, floor_n: int, luk: int, boss: bool) -> Dictionary:
	var rng := Rng.new((seed_value ^ Rng.hash_text(verb_id) ^ (floor_n * 97) ^ (luk * 13)) & 0x7fffffff)
	var chance := 55 + luk * 4 + (20 if boss else 0)
	if rng.next_index(100) >= chance and not boss:
		return {}
	var rarity := _rarity(rng, luk, boss)
	var slot: String = SLOTS[rng.next_index(SLOTS.size())]
	var affix: String = AFFIXES[rng.next_index(AFFIXES.size())]
	var magnitude := 1 + RARITIES.find(rarity)
	if boss:
		magnitude += 1
	var max_dur := 6 + RARITIES.find(rarity) * 3
	return {
		"id": "loot-%s-%s-%d" % [verb_id, slot, seed_value],
		"slot": slot,
		"rarity": rarity,
		"name_ruby": _name(slot, rarity),
		"affixes": {affix: magnitude},
		"durability": max_dur,
		"max_durability": max_dur,
	}

static func wear(item: Dictionary) -> Dictionary:
	var copy := item.duplicate(true)
	copy["durability"] = maxi(0, int(copy["durability"]) - 1)
	return copy

static func repair_cost(item: Dictionary) -> int:
	return (RARITIES.find(item["rarity"]) + 1) * 2

static func repaired(item: Dictionary) -> Dictionary:
	var copy := item.duplicate(true)
	copy["durability"] = int(copy["max_durability"])
	return copy

static func _rarity(rng: Rng, luk: int, boss: bool) -> String:
	var weights := [70, 22, 6, 2]
	var shift := mini(luk + (2 if boss else 0), 8)
	weights[0] = maxi(10, weights[0] - shift * 6)
	weights[1] += shift * 2
	weights[2] += shift * 3
	weights[3] += shift
	var total := 0
	for weight in weights:
		total += weight
	var roll := rng.next_index(total)
	for i in weights.size():
		roll -= weights[i]
		if roll < 0:
			return RARITIES[i]
	return "common"

static func _name(slot: String, rarity: String) -> String:
	var metals := {"common": "木[き]", "uncommon": "はがね", "rare": "金[きん]", "epic": "森[もり]"}
	var nouns := {
		"weapon": "の つるぎ",
		"shield": "の 盾[たて]",
		"armor": "の ころも",
		"accessory": "の ゆびわ",
		"charm": "の おまもり",
	}
	var metal: String = metals[rarity]
	var noun: String = nouns[slot]
	return metal + noun
