class_name RubyText
extends RefCounted

static func parse(markup: String) -> Array:
	var parts: Array = []
	var plain := ""
	var i := 0
	while i < markup.length():
		var open := markup.find("[", i)
		if open == -1:
			plain += markup.substr(i)
			break
		var close := markup.find("]", open)
		if close == -1:
			plain += markup.substr(i)
			break
		var kanji := ""
		var start := open - 1
		while start >= i:
			var ch := markup.substr(start, 1)
			if _is_kana_or_space(ch):
				break
			kanji = ch + kanji
			start -= 1
		plain += markup.substr(i, start + 1 - i)
		if plain != "":
			parts.append({"base": plain, "reading": ""})
			plain = ""
		parts.append({"base": kanji, "reading": markup.substr(open + 1, close - open - 1)})
		i = close + 1
	if plain != "":
		parts.append({"base": plain, "reading": ""})
	if parts.is_empty():
		parts.append({"base": markup, "reading": ""})
	return parts

static func _is_kana_or_space(ch: String) -> bool:
	if ch == " " or ch == "　" or ch == "、" or ch == "。" or ch == "？" or ch == "！" or ch == "・" or ch == "「" or ch == "」" or ch == "（" or ch == "）":
		return true
	var code := ch.unicode_at(0)
	return (code >= 0x3040 and code <= 0x30ff) or (code >= 0x30 and code <= 0x39) or (code >= 0x41 and code <= 0x7a)
