class_name ContentPack
extends RefCounted

const ROOT := "res://content/packs/ywca-evening"

var id: String
var title_ruby: String
var profile: Dictionary
var lessons: Array
var words: Array
var verbs: Array

static func load_default() -> ContentPack:
	return load_from(ROOT)

static func load_from(root: String) -> ContentPack:
	var pack := ContentPack.new()
	var meta: Dictionary = _read(root + "/pack.json")
	pack.id = str(meta.get("id", "pack"))
	pack.title_ruby = str(meta.get("title_ruby", ""))
	pack.profile = _read(root + "/profile.json")
	pack.lessons = _read(root + "/lessons.json")["lessons"]
	pack.words = _read(root + "/words.json")["words"]
	pack.verbs = _read(root + "/verbs.json")
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

func lessons_sorted() -> Array:
	var copy: Array = lessons.duplicate()
	copy.sort_custom(func(a, b): return str(a["date"]) < str(b["date"]))
	return copy
