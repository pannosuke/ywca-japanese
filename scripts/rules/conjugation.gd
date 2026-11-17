class_name Conjugation
extends RefCounted

const FORMS := ["masu", "masen", "mashita", "masen_deshita", "te", "ta", "nai"]
const A_ROW := {
	"う": "わ", "く": "か", "ぐ": "が", "す": "さ", "つ": "た",
	"ぬ": "な", "ぶ": "ば", "む": "ま", "る": "ら",
}
const I_ROW := {
	"う": "い", "く": "き", "ぐ": "ぎ", "す": "し", "つ": "ち",
	"ぬ": "に", "ぶ": "び", "む": "み", "る": "り",
}

static func kana(verb: Dictionary, form: String) -> String:
	var dict: String = verb["dict_kana"]
	if dict == "くる":
		return _kuru(form)
	if dict.ends_with("する"):
		return dict.trim_suffix("する") + _suru(form)
	if verb["group"] == "ichidan":
		return dict.trim_suffix("る") + _ichidan(form)
	return _godan(dict, form)

static func ruby(verb: Dictionary, form: String) -> String:
	return ruby_for(verb, kana(verb, form))

static func ruby_for(verb: Dictionary, done: String) -> String:
	if verb["dict_kana"] == "くる":
		return "来[き]" + done.trim_prefix("き")
	var source: String = verb["dict_ruby"]
	var open := source.find("[")
	var close := source.find("]")
	if open <= 0 or close < 0:
		return done
	var kanji := source.substr(0, open)
	var reading := source.substr(open + 1, close - open - 1)
	if not done.begins_with(reading):
		return done
	return kanji + "[" + reading + "]" + done.trim_prefix(reading)

static func rule_id(verb: Dictionary, form: String) -> String:
	if form == "nai":
		return "nai"
	if form == "te" or form == "ta":
		return _te_rule(verb)
	if form == "masu":
		return _masu_rule(verb)
	if form == "masen":
		return "neg-polite"
	if form == "mashita":
		return "past-polite"
	return "past-neg-polite"

static func explain(verb: Dictionary, form: String) -> Dictionary:
	var done := ruby(verb, form)
	var rule := rule_id(verb, form)
	var ruby_text := ""
	var en := ""
	match rule:
		"te-iku":
			ruby_text = "「行[い]く」だけ て形[けい]は「って」。ほかの「く」は「いて」。"
			en = "行く is the one く-verb whose て-form is って. Other く-verbs use いて."
		"te-ite":
			ruby_text = "グループ1。「く」は「いて」。"
			en = "Group 1. A verb ending in く changes to いて."
		"te-ide":
			ruby_text = "グループ1。「ぐ」は「いで」。"
			en = "Group 1. A verb ending in ぐ changes to いで."
		"te-shite":
			ruby_text = "グループ1。「す」は「して」。"
			en = "Group 1. A verb ending in す changes to して."
		"te-tte":
			ruby_text = "グループ1。「う・つ・る」は「って」。"
			en = "Group 1. Endings う, つ, and る change to って."
		"te-nde":
			ruby_text = "グループ1。「む・ぶ・ぬ」は「んで」。"
			en = "Group 1. Endings む, ぶ, and ぬ change to んで."
		"te-ichidan":
			ruby_text = "グループ2。る を とって「て」。"
			en = "Group 2. Drop る and add て."
		"te-suru":
			ruby_text = "する は「して」。"
			en = "する becomes して."
		"te-kuru":
			ruby_text = "来[く]る は「来[き]て」。"
			en = "来る becomes きて."
		_:
			if verb["group"] == "godan":
				ruby_text = "グループ1。いだん＋%s。" % _ending(form)
				en = "Group 1. Use the i-row, then the polite ending."
			elif verb["group"] == "ichidan":
				ruby_text = "グループ2。る を とって%s。" % _ending(form)
				en = "Group 2. Drop る, then add the ending."
			else:
				ruby_text = "グループ3。する は し〜、来[く]る は き〜。"
				en = "Group 3. する uses し and 来る uses き."
	if form == "ta" and rule.begins_with("te-"):
		ruby_text += " た形[けい]も おなじ おとの へんか。"
		en += " The plain past uses the same sound change."
	return {"ruby": ruby_text + " → " + done, "en": en, "rule_id": rule}

static func choices(verb: Dictionary, form: String, rng: Rng) -> Array:
	var correct := kana(verb, form)
	var wrongs: Array = _wrongs(verb, form, correct)
	var pool: Array = []
	for item in wrongs:
		if item != correct and item not in pool:
			pool.append(item)
	var picked: Array = []
	while picked.size() < 3 and pool.size() > 0:
		picked.append(pool.pop_at(rng.next_index(pool.size())))
	picked.append(correct)
	var out: Array = []
	while picked.size() > 0:
		out.append(picked.pop_at(rng.next_index(picked.size())))
	return out

static func prompt(verb: Dictionary, form: String) -> String:
	var source: String = str(verb["dict_ruby"]) if form in ["masu", "te", "ta"] else ruby(verb, "masu")
	var names := {
		"masu": "ます",
		"masen": "ません",
		"mashita": "ました",
		"masen_deshita": "ませんでした",
		"te": "て形[けい]",
		"ta": "た形[けい]",
		"nai": "ない形[けい]",
	}
	var ask: String = names[form]
	return source + " の " + ask + " は？"

static func covered_forms(lessons: Array) -> Array:
	var forms := ["masu"]
	for lesson in lessons:
		var blob := JSON.stringify(lesson)
		if "ました" in blob and "mashita" not in forms:
			forms.append("mashita")
		if "ませんでした" in blob and "masen_deshita" not in forms:
			forms.append("masen_deshita")
		if "ません" in blob and "masen" not in forms:
			forms.append("masen")
		if ("て形" in blob or "てください" in blob) and "te" not in forms:
			forms.append("te")
	return forms

static func _godan(dict: String, form: String) -> String:
	var stem := dict.substr(0, dict.length() - 1)
	var tail := dict.substr(dict.length() - 1, 1)
	if form == "nai":
		return stem + A_ROW[tail] + "ない"
	var polite: String = stem + I_ROW[tail]
	match form:
		"masu":
			return polite + "ます"
		"masen":
			return polite + "ません"
		"mashita":
			return polite + "ました"
		"masen_deshita":
			return polite + "ませんでした"
		"te":
			return _te_godan(dict)
		_:
			return _ta_from_te(_te_godan(dict))

static func _te_godan(dict: String) -> String:
	if dict == "いく":
		return "いって"
	var stem := dict.substr(0, dict.length() - 1)
	var tail := dict.substr(dict.length() - 1, 1)
	if tail in ["う", "つ", "る"]:
		return stem + "って"
	if tail in ["む", "ぶ", "ぬ"]:
		return stem + "んで"
	if tail == "く":
		return stem + "いて"
	if tail == "ぐ":
		return stem + "いで"
	if tail == "す":
		return stem + "して"
	return stem + "て"

static func _ta_from_te(te: String) -> String:
	if te.ends_with("って"):
		return te.trim_suffix("って") + "った"
	if te.ends_with("んで"):
		return te.trim_suffix("んで") + "んだ"
	if te.ends_with("いて"):
		return te.trim_suffix("いて") + "いた"
	if te.ends_with("いで"):
		return te.trim_suffix("いで") + "いだ"
	if te.ends_with("して"):
		return te.trim_suffix("して") + "した"
	if te.ends_with("て"):
		return te.trim_suffix("て") + "た"
	return te

static func _kuru(form: String) -> String:
	match form:
		"masu": return "きます"
		"masen": return "きません"
		"mashita": return "きました"
		"masen_deshita": return "きませんでした"
		"te": return "きて"
		"nai": return "こない"
		_: return "きた"

static func _suru(form: String) -> String:
	match form:
		"masu": return "します"
		"masen": return "しません"
		"mashita": return "しました"
		"masen_deshita": return "しませんでした"
		"te": return "して"
		"nai": return "しない"
		_: return "した"

static func _ichidan(form: String) -> String:
	match form:
		"masu": return "ます"
		"masen": return "ません"
		"mashita": return "ました"
		"masen_deshita": return "ませんでした"
		"te": return "て"
		"nai": return "ない"
		_: return "た"

static func _ending(form: String) -> String:
	var names := {"masu": "ます", "masen": "ません", "mashita": "ました", "masen_deshita": "ませんでした", "te": "て", "ta": "た", "nai": "ない"}
	var ending: String = names[form]
	return ending

static func _te_rule(verb: Dictionary) -> String:
	var dict: String = verb["dict_kana"]
	if dict == "くる":
		return "te-kuru"
	if dict.ends_with("する"):
		return "te-suru"
	if verb["group"] == "ichidan":
		return "te-ichidan"
	if dict == "いく":
		return "te-iku"
	var tail := dict.substr(dict.length() - 1, 1)
	if tail == "く":
		return "te-ite"
	if tail == "ぐ":
		return "te-ide"
	if tail == "す":
		return "te-shite"
	if tail in ["う", "つ", "る"]:
		return "te-tte"
	return "te-nde"

static func _masu_rule(verb: Dictionary) -> String:
	var dict: String = verb["dict_kana"]
	if dict == "くる":
		return "masu-kuru"
	if dict.ends_with("する"):
		return "masu-suru"
	if verb["group"] == "ichidan":
		return "masu-ichidan"
	return "masu-godan"

static func _wrongs(verb: Dictionary, form: String, correct: String) -> Array:
	var dict: String = verb["dict_kana"]
	var stem := dict.substr(0, dict.length() - 1)
	var out: Array = []
	if form == "nai":
		for piece in [stem + "あない", stem + "わない", stem + "いない", stem + "えない", "しない", "こない"]:
			if piece != correct:
				out.append(piece)
		return out
	if form == "te" or form == "ta":
		for piece in [stem + "って", stem + "んで", stem + "いて", stem + "いで", stem + "して", stem + "て", "いって", "きて", "して"]:
			var value: String = piece if form == "te" else _ta_from_te(piece)
			if value != correct:
				out.append(value)
		return out
	for guess_value in [stem + "ます", dict + "ます", stem + "ります", stem + "します", "します", "きます"]:
		var guess: String = str(guess_value)
		var swapped: String = guess.trim_suffix("ます") + _ending(form)
		if swapped != correct:
			out.append(swapped)
	return out
