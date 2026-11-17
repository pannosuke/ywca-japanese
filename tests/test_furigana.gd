extends GutTest

func test_profile_known_kanji_starts_empty() -> void:
	var pack := ContentPack.load_default()
	assert_eq(pack.known_kanji.size(), 0)

func test_known_kanji_drops_only_a_learned_character() -> void:
	Furigana.prepare(ContentPack.load_default())
	assert_eq(Furigana.apply("出[で]る", ["出"]), "出る")
	assert_eq(Furigana.apply("出発[しゅっぱつ]", ["出"]), "出発[しゅっぱつ]")
	assert_eq(Furigana.bare(Furigana.apply("出発", [])), "")
	assert_eq(Furigana.bare(Furigana.apply("灯りの森", [])), "")

func test_every_shown_string_has_a_reading() -> void:
	Furigana.prepare(ContentPack.load_default())
	var files: Array = []
	_gather("res://scripts", files)
	_gather("res://scenes", files)
	_gather("res://content/packs", files)
	assert_gt(files.size(), 8)
	var checked := 0
	for path in files:
		var text := FileAccess.get_file_as_string(str(path))
		var strings: Array = _strings(str(path), text)
		for item in strings:
			var source := str(item)
			if source == "":
				continue
			checked += 1
			var shown := Furigana.apply(source, [])
			var bad := Furigana.bare(shown)
			assert_eq(bad, "", "%s :: %s" % [str(path), source.substr(0, 48)])
	assert_gt(checked, 100)

func _gather(root: String, into: Array) -> void:
	var dir := DirAccess.open(root)
	if dir == null:
		return
	dir.list_dir_begin()
	var name := dir.get_next()
	while name != "":
		if name != "." and name != "..":
			var path := root + "/" + name
			if dir.current_is_dir():
				_gather(path, into)
			elif name.ends_with(".gd") or name.ends_with(".tscn") or name.ends_with(".json"):
				into.append(path)
		name = dir.get_next()

func _strings(path: String, text: String) -> Array:
	if path.ends_with(".json"):
		var parsed = JSON.parse_string(text)
		var out: Array = []
		_walk(parsed, out)
		return out
	return _quotes(text)

func _walk(value, into: Array) -> void:
	var kind := typeof(value)
	if kind == TYPE_STRING:
		into.append(value)
	elif kind == TYPE_ARRAY:
		for child in value:
			_walk(child, into)
	elif kind == TYPE_DICTIONARY:
		for key in value.keys():
			_walk(value[key], into)

func _quotes(text: String) -> Array:
	var out: Array = []
	var i := 0
	while i < text.length():
		if text.substr(i, 1) != "\"":
			i += 1
			continue
		var j := i + 1
		var buf := ""
		while j < text.length():
			var ch := text.substr(j, 1)
			if ch == "\\":
				j += 2
				continue
			if ch == "\"":
				break
			buf += ch
			j += 1
		out.append(buf)
		i = j + 1
	return out
