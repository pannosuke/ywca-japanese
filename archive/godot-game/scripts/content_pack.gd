class_name ContentPack
extends RefCounted

const ROOT := "res://content/packs/ywca-evening"

var id: String
var title_ruby: String
var profile: Dictionary
var lessons: Array
var words: Array
var verbs: Array
var cards: Array
var packs: Array
var yokai: Array
var homework: Array
var known_kanji: Array = []

static func load_default() -> ContentPack:
	return load_from(ROOT)

static func load_from(root: String) -> ContentPack:
	var pack := ContentPack.new()
	var meta: Dictionary = _read(root + "/pack.json")
	pack.id = str(meta.get("id", "pack"))
	pack.title_ruby = str(meta.get("title_ruby", ""))
	pack.profile = _read(root + "/profile.json")
	var listed = pack.profile.get("known_kanji", [])
	pack.known_kanji = listed if listed is Array else []
	pack.lessons = _read(root + "/lessons.json")["lessons"]
	pack.words = _read(root + "/words.json")["words"]
	pack.verbs = _read(root + "/verbs.json")
	pack.cards = []
	pack.packs = []
	var catalog = _read(root + "/catalog.json")
	if typeof(catalog) == TYPE_DICTIONARY:
		for entry in catalog["packs"]:
			var file: Dictionary = _read(root + "/" + str(entry["file"]))
			pack.packs.append(file)
			for card in file["cards"]:
				card["pack_id"] = file["id"]
				pack.cards.append(card)
	var yokai_file = _read(root + "/yokai.json")
	pack.yokai = yokai_file["yokai"] if typeof(yokai_file) == TYPE_DICTIONARY else []
	pack.homework = []
	var sheet = _read(root + "/homework/no89.json")
	if typeof(sheet) == TYPE_DICTIONARY:
		pack.homework.append(sheet)
	return pack

static func _read(path: String) -> Variant:
	var resource = load(path)
	if resource is JSON:
		return resource.data
	return JSON.parse_string(FileAccess.get_file_as_string(path))

func verb_by_id(verb_id: String) -> Dictionary:
	for verb in verbs:
		if verb["id"] == verb_id:
			return verb
	return {}

func card_by_id(card_id: String) -> Dictionary:
	for card in cards:
		if str(card["id"]) == card_id:
			return card
	return {}

func yokai_by_id(yokai_id: String) -> Dictionary:
	for entry in yokai:
		if str(entry["id"]) == yokai_id:
			return entry
	return {}

func unseen_packs(known: Array) -> Array:
	var fresh: Array = []
	for pack in packs:
		if not known.has(pack["id"]):
			fresh.append(pack)
	return fresh

func lessons_sorted() -> Array:
	var copy: Array = lessons.duplicate()
	copy.sort_custom(func(a, b): return str(a["date"]) < str(b["date"]))
	return copy
