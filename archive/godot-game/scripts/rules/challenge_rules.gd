class_name ChallengeRules
extends RefCounted

const DECOYS := ["ん", "っ"]

static func kind(card: Dictionary) -> String:
	var type_name := str(card.get("type", ""))
	if type_name == "verb" and str(card.get("verb_id", "")) != "":
		return "spell"
	if type_name == "grammar":
		return "cloze"
	return "meaning"

static func asks_verb_form(card: Dictionary) -> bool:
	return kind(card) == "spell"

static func instruction(verb: Dictionary, form: String) -> String:
	var source := str(verb["dict_ruby"])
	if form != "masu" and form != "te" and form != "ta":
		source = Conjugation.ruby(verb, "masu")
	return "%s を %s に して、もじを タップして つくろう" % [source, ask_markup(form)]

static func instruction_en(form: String) -> String:
	return "Tap the tiles to spell the %s form." % ask_plain(form)

static func ask_markup(form: String) -> String:
	match form:
		"mashita":
			return "ました"
		"masen":
			return "ません"
		"masen_deshita":
			return "ませんでした"
		"te":
			return "て形[けい]"
		"ta":
			return "た形[けい]"
		"nai":
			return "ない形[けい]"
		_:
			return "ます"

static func ask_plain(form: String) -> String:
	match form:
		"mashita":
			return "ました"
		"masen":
			return "ません"
		"masen_deshita":
			return "ませんでした"
		"te":
			return "て"
		"ta":
			return "た"
		"nai":
			return "ない"
		_:
			return "ます"

static func tiles(answer: String, rng: Rng) -> Array:
	var bank: Array = []
	for i in answer.length():
		bank.append(answer.substr(i, 1))
	for decoy in DECOYS:
		bank.append(decoy)
	return DeckRules.shuffle(bank, rng)

static func take(bank: Array, piece: String) -> Array:
	var copy: Array = bank.duplicate()
	var index := copy.find(piece)
	if index >= 0:
		copy.remove_at(index)
	return copy

static func untake(bank: Array, piece: String) -> Array:
	var copy: Array = bank.duplicate()
	copy.append(piece)
	return copy

static func normalize(value: String) -> String:
	return value.replace(" ", "").replace("　", "")

static func surface(marked: String) -> String:
	var out := ""
	var i := 0
	while i < marked.length():
		var ch := marked.substr(i, 1)
		if ch == "[":
			var close := marked.find("]", i)
			if close < 0:
				break
			i = close + 1
		else:
			out += ch
			i += 1
	return out

static func accepts(given: String, verb: Dictionary, form: String) -> bool:
	var cleaned := normalize(given)
	if cleaned == "":
		return false
	if cleaned == Conjugation.kana(verb, form):
		return true
	return cleaned == normalize(surface(Conjugation.ruby(verb, form)))

static func counts_cover(bank: Array, answer: String) -> bool:
	var need := {}
	for i in answer.length():
		var ch := answer.substr(i, 1)
		need[ch] = int(need.get(ch, 0)) + 1
	var have := {}
	for item in bank:
		var piece := str(item)
		have[piece] = int(have.get(piece, 0)) + 1
	for key in need.keys():
		if int(have.get(str(key), 0)) < int(need[key]):
			return false
	return bank.size() == answer.length() + DECOYS.size()

static func preview(card: Dictionary, rarity: String) -> Dictionary:
	var hit: Dictionary = CombatRules.resolve(true, card, rarity, [], false)
	return {
		"damage": int(hit["damage"]),
		"block": int(hit["block"]),
		"draw": int(hit["draw"]),
		"cost": int(card.get("cost", 1)),
	}

static func effect_line(card: Dictionary, rarity: String) -> String:
	var hit := preview(card, rarity)
	var parts: Array = []
	if int(hit["damage"]) > 0:
		parts.append("こうげき %d" % int(hit["damage"]))
	if int(hit["block"]) > 0:
		parts.append("ぼうぎょ %d" % int(hit["block"]))
	if int(hit["draw"]) > 0:
		parts.append("ひく %d" % int(hit["draw"]))
	if parts.is_empty():
		return "えね %d" % int(hit["cost"])
	return " ".join(parts)
