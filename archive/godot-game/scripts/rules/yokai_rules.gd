class_name YokaiRules
extends RefCounted

static func for_node(node_type: String, index: int, pool: Array) -> Dictionary:
	var wanted := "normal"
	if node_type == "elite":
		wanted = "elite"
	elif node_type == "boss":
		wanted = "boss"
	var matches: Array = []
	for yokai in pool:
		if str(yokai["tier"]) == wanted:
			matches.append(yokai)
	if matches.is_empty():
		matches = pool
	return matches[index % matches.size()]

static func drop_chance(node_type: String, accuracy: float) -> int:
	var base := 18
	if node_type == "elite":
		base = 40
	elif node_type == "boss":
		base = 100
	elif node_type != "battle":
		base = 8
	return mini(100, base + int(accuracy * 20.0))

static func dropped(rng: Rng, chance: int) -> bool:
	return rng.next_index(100) < chance

static func story_for(yokai: Dictionary, reader_level: int) -> Dictionary:
	var stories: Array = yokai["stories"]
	var best: Dictionary = stories[0]
	for story in stories:
		if int(story["level"]) <= reader_level:
			best = story
	return best

static func bare_kanji(text: String) -> bool:
	var i := 0
	while i < text.length():
		var code := text.unicode_at(i)
		if code >= 0x4e00 and code <= 0x9fff:
			var j := i
			while j < text.length():
				var next := text.unicode_at(j)
				if next < 0x4e00 or next > 0x9fff:
					break
				j += 1
			if j >= text.length() or text.substr(j, 1) != "[":
				return true
			i = j
		else:
			i += 1
	return false

static func questions_ok(story: Dictionary) -> bool:
	var questions: Array = story["questions"]
	if questions.size() < 3:
		return false
	for question in questions:
		var correct := 0
		for choice in question["choices"]:
			if bool(choice["correct"]):
				correct += 1
		if correct != 1:
			return false
	return true
