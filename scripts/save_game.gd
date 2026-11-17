class_name SaveGame
extends RefCounted

const PATH := "user://yoruben.json"

static func fresh(pack_id: String) -> Dictionary:
	var sword := GearRules.starter_weapon()
	return {
		"pack_id": pack_id,
		"anki_enabled": false,
		"music": 0.7,
		"sfx": 0.8,
		"xp": 0,
		"points_spent": 0,
		"base": {"atk": 1, "spd": 1, "def": 1, "luk": 1, "wis": 1},
		"skills": [],
		"gems": 0,
		"unbanked_gems": 0,
		"inventory": [],
		"pouch": [],
		"equipped": {"weapon": sword},
		"streak": 0,
		"last_day": "",
		"reviews": {},
		"study_log": [],
		"run": null,
		"cosmetic": "leaf",
		"profile_id": "local",
	}

static func load_or_new(pack_id: String) -> Dictionary:
	if not FileAccess.file_exists(PATH):
		var made := fresh(pack_id)
		write(made)
		return made
	var parsed = JSON.parse_string(FileAccess.get_file_as_string(PATH))
	if typeof(parsed) != TYPE_DICTIONARY:
		return fresh(pack_id)
	return parsed

static func write(state: Dictionary) -> void:
	var file := FileAccess.open(PATH, FileAccess.WRITE)
	if file == null:
		return
	file.store_string(JSON.stringify(state))

static func touch_day(state: Dictionary, today: String) -> void:
	var last := str(state.get("last_day", ""))
	if last == today:
		return
	if last != "" and _yesterday(today) == last:
		state["streak"] = int(state["streak"]) + 1
	else:
		state["streak"] = 1
	state["last_day"] = today

static func _yesterday(today: String) -> String:
	var bits := today.split("-")
	var unix := Time.get_unix_time_from_datetime_dict({
		"year": int(bits[0]), "month": int(bits[1]), "day": int(bits[2]),
		"hour": 12, "minute": 0, "second": 0,
	})
	var prior: Dictionary = Time.get_datetime_dict_from_unix_time(unix - 86400)
	return "%04d-%02d-%02d" % [int(prior.year), int(prior.month), int(prior.day)]

static func tokyo_day(unix: int) -> String:
	var shifted: Dictionary = Time.get_datetime_dict_from_unix_time(unix + 9 * 3600)
	return "%04d-%02d-%02d" % [int(shifted.year), int(shifted.month), int(shifted.day)]
