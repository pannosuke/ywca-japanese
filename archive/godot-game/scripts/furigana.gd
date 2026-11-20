class_name Furigana
extends RefCounted

## Readings for kanji that never appear with brackets in the pack.
const EXTRAS := [
	"課[か]", "熱[あつ]い", "小[ちい]さ", "勝[か]つ", "守[まも]り", "少[すこ]し",
	"五段[ごだん]", "一段[いちだん]", "別科[べっか]", "日本語[にほんご]", "学科[がっか]",
	"て形[けい]", "ない形[けい]", "た形[けい]", "出[だ]した", "出[で]る", "出[だ]す",
	"灯[あか]り", "見[み]なお", "金[きん]の", "玉[だま]", "森[もり]", "火[ひ]",
	"戦[たたか]う", "強[つよ]い", "分[わ]ける", "速[はや]い", "店[みせ]", "読[よ]む", "主[ぬし]",
	"休[やす]む",
]

static var known: Array = []
static var _prepared := false
static var _tally: Dictionary = {}
static var _entries: Array = []

static func prepare(pack: ContentPack) -> void:
	if _prepared:
		return
	_prepared = true
	_tally = {}
	_entries = []
	_walk(pack.profile)
	_walk(pack.lessons)
	_walk(pack.words)
	_walk(pack.verbs)
	_walk(pack.cards)
	_walk(pack.yokai)
	_walk(pack.homework)
	_harvest_tree("res://scripts")
	_harvest_tree("res://scenes")
	for extra in EXTRAS:
		_harvest(str(extra), 10000)
	for surface in _tally.keys():
		var options: Dictionary = _tally[surface]
		var best := ""
		var best_n := -1
		for ruby in options.keys():
			var n := int(options[ruby])
			if n > best_n:
				best_n = n
				best = str(ruby)
		_entries.append({"surface": str(surface), "ruby": best})
	_entries.sort_custom(func(a, b): return str(a["surface"]).length() > str(b["surface"]).length())

static func apply(text: String, known_kanji: Array) -> String:
	var out := ""
	var i := 0
	while i < text.length():
		var ch := text.substr(i, 1)
		if _kanji(ch):
			var j := i
			while j < text.length() and _kanji(text.substr(j, 1)):
				j += 1
			if j < text.length() and text.substr(j, 1) == "[":
				var close := text.find("]", j)
				var base := text.substr(i, j - i)
				var reading := text.substr(j + 1, close - j - 1)
				out += _filter(base, reading, known_kanji)
				i = close + 1
				continue
			var matched := false
			for entry in _entries:
				var surface := str(entry["surface"])
				if text.substr(i, surface.length()) == surface:
					out += apply(str(entry["ruby"]), known_kanji)
					i += surface.length()
					matched = true
					break
			if not matched:
				out += ch
				i += 1
		else:
			out += ch
			i += 1
	return out

static func bare(text: String) -> String:
	var i := 0
	while i < text.length():
		if _kanji(text.substr(i, 1)):
			var j := i
			while j < text.length() and _kanji(text.substr(j, 1)):
				j += 1
			if j >= text.length() or text.substr(j, 1) != "[":
				var from := maxi(0, i - 6)
				return text.substr(from, j + 6 - from)
			i = j
		else:
			i += 1
	return ""

static func _filter(base: String, reading: String, known_kanji: Array) -> String:
	if reading == "":
		return base
	var i := 0
	while i < base.length():
		var ch := base.substr(i, 1)
		if _kanji(ch) and not known_kanji.has(ch):
			return base + "[" + reading + "]"
		i += 1
	return base

static func _walk(value) -> void:
	var kind := typeof(value)
	if kind == TYPE_STRING:
		_harvest(str(value), 1)
	elif kind == TYPE_ARRAY:
		for child in value:
			_walk(child)
	elif kind == TYPE_DICTIONARY:
		for key in value.keys():
			_walk(value[key])

static func _harvest_tree(root: String) -> void:
	var dir := DirAccess.open(root)
	if dir == null:
		return
	dir.list_dir_begin()
	var name := dir.get_next()
	while name != "":
		if name != "." and name != "..":
			var path := root + "/" + name
			if dir.current_is_dir():
				_harvest_tree(path)
			elif name.ends_with(".gd") or name.ends_with(".tscn"):
				_harvest(FileAccess.get_file_as_string(path), 1)
		name = dir.get_next()

static func _harvest(text: String, weight: int) -> void:
	var i := 0
	while i < text.length():
		if not _kanji(text.substr(i, 1)):
			i += 1
			continue
		var start := i
		while i < text.length() and _kanji(text.substr(i, 1)):
			i += 1
		var kanji := text.substr(start, i - start)
		if i >= text.length() or text.substr(i, 1) != "[":
			continue
		var close := text.find("]", i)
		if close == -1:
			return
		var reading := text.substr(i + 1, close - i - 1)
		i = close + 1
		var kana_start := i
		while i < text.length() and _kana(text.substr(i, 1)):
			i += 1
		var kana := text.substr(kana_start, i - kana_start)
		_add(kanji + kana, kanji + "[" + reading + "]" + kana, weight)
		if kana != "":
			_add(kanji, kanji + "[" + reading + "]", weight)

static func _add(surface: String, ruby: String, weight: int) -> void:
	if not _tally.has(surface):
		_tally[surface] = {}
	var options: Dictionary = _tally[surface]
	options[ruby] = int(options.get(ruby, 0)) + weight

static func _kanji(ch: String) -> bool:
	var code := ch.unicode_at(0)
	return (code >= 0x4e00 and code <= 0x9fff) or code == 0x3005

static func _kana(ch: String) -> bool:
	var code := ch.unicode_at(0)
	return code >= 0x3040 and code <= 0x30ff
