class_name Consequences
extends RefCounted

const NEGLECT_SECONDS := 7 * 86400

static func note_review(state: Dictionary, verb: Dictionary, correct: bool, now: int) -> void:
	var key := str(verb["id"])
	var row: Dictionary = state["reviews"].get(key, {"wrong": 0, "correct": 0, "last_wrong": 0, "last_correct": 0, "lesson_date": verb.get("lesson_date", "")})
	row["lesson_date"] = verb.get("lesson_date", "")
	if correct:
		row["correct"] = int(row["correct"]) + 1
		row["last_correct"] = now
	else:
		row["wrong"] = int(row["wrong"]) + 1
		row["last_wrong"] = now
	state["reviews"][key] = row
	state["study_log"].append({"verb_id": key, "correct": correct, "at": now})

static func corrupt_dates(state: Dictionary, now: int) -> Array:
	var dirty := {}
	for row in state["reviews"].values():
		var date = row.get("lesson_date", "")
		if date == null or str(date) == "":
			continue
		var last_wrong := int(row.get("last_wrong", 0))
		var last_correct := int(row.get("last_correct", 0))
		if last_wrong == 0:
			continue
		var neglected: bool = last_wrong > last_correct
		if last_correct > 0 and now - last_correct > NEGLECT_SECONDS and int(row.get("wrong", 0)) > 0:
			neglected = true
		if neglected:
			dirty[str(date)] = true
	return dirty.keys()

static func clear_if_reviewed(state: Dictionary, verb: Dictionary, correct: bool, now: int) -> void:
	note_review(state, verb, correct, now)

static func wear_equipped(state: Dictionary) -> void:
	var next := {}
	for slot in state["equipped"].keys():
		next[slot] = GearRules.wear(state["equipped"][slot])
	state["equipped"] = next

static func die(state: Dictionary) -> Dictionary:
	var kept_log: Array = state["study_log"].duplicate(true)
	var kept_reviews: Dictionary = state["reviews"].duplicate(true)
	var kept_inventory: Array = state["inventory"].duplicate(true)
	var kept_equipped: Dictionary = state["equipped"].duplicate(true)
	state["pouch"] = []
	state["unbanked_gems"] = 0
	state["run"] = null
	state["study_log"] = kept_log
	state["reviews"] = kept_reviews
	state["inventory"] = kept_inventory
	state["equipped"] = kept_equipped
	return state

static func bank(state: Dictionary) -> void:
	for item in state["pouch"]:
		state["inventory"].append(item)
	state["pouch"] = []
	state["gems"] = int(state["gems"]) + int(state["unbanked_gems"])
	state["unbanked_gems"] = 0

static func repair(state: Dictionary, item_id: String) -> bool:
	var item := _find(state, item_id)
	if item.is_empty():
		return false
	var cost := GearRules.repair_cost(item)
	if int(state["gems"]) < cost:
		return false
	state["gems"] = int(state["gems"]) - cost
	var fixed := GearRules.repaired(item)
	_replace(state, fixed)
	return true

static func _find(state: Dictionary, item_id: String) -> Dictionary:
	for item in state["inventory"]:
		if item["id"] == item_id:
			return item
	for item in state["equipped"].values():
		if item["id"] == item_id:
			return item
	return {}

static func _replace(state: Dictionary, item: Dictionary) -> void:
	for i in state["inventory"].size():
		if state["inventory"][i]["id"] == item["id"]:
			state["inventory"][i] = item
			return
	for slot in state["equipped"].keys():
		if state["equipped"][slot]["id"] == item["id"]:
			state["equipped"][slot] = item
			return
