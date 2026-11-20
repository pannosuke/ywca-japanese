class_name DeckRules
extends RefCounted

const HAND := 5
const ENERGY := 3

static func shuffle(cards: Array, rng: Rng) -> Array:
	var copy: Array = cards.duplicate()
	for i in range(copy.size() - 1, 0, -1):
		var j := rng.next_index(i + 1)
		var tmp = copy[i]
		copy[i] = copy[j]
		copy[j] = tmp
	return copy

static func opening(deck: Array, rng: Rng) -> Dictionary:
	return {
		"draw": shuffle(deck, rng),
		"discard": [],
		"hand": [],
		"energy": ENERGY,
		"energy_max": ENERGY,
	}

static func start_turn(state: Dictionary, rng: Rng) -> void:
	state["energy"] = int(state["energy_max"])
	while state["hand"].size() < HAND:
		if state["draw"].is_empty():
			if state["discard"].is_empty():
				return
			state["draw"] = shuffle(state["discard"], rng)
			state["discard"] = []
		state["hand"].append(state["draw"].pop_back())

static func can_play(state: Dictionary, card: Dictionary) -> bool:
	return int(state["energy"]) >= int(card["cost"]) and _in_hand(state, str(card["id"]))

static func spend(state: Dictionary, card: Dictionary) -> void:
	state["energy"] = int(state["energy"]) - int(card["cost"])
	var kept: Array = []
	var moved := false
	for item in state["hand"]:
		if not moved and str(item["id"]) == str(card["id"]):
			state["discard"].append(item)
			moved = true
		else:
			kept.append(item)
	state["hand"] = kept

static func end_turn(state: Dictionary) -> void:
	for item in state["hand"]:
		state["discard"].append(item)
	state["hand"] = []

static func _in_hand(state: Dictionary, card_id: String) -> bool:
	for item in state["hand"]:
		if str(item["id"]) == card_id:
			return true
	return false
