extends GutTest

const ASKED := ["masu", "mashita", "masen", "masen_deshita", "te", "ta", "nai"]

func test_exceptions_match_the_class_rules() -> void:
	var pack := ContentPack.load_default()
	var iku: Dictionary = pack.verb_by_id("iku")
	var hiku: Dictionary = pack.verb_by_id("hiku")
	var suru: Dictionary = pack.verb_by_id("suru")
	var kuru: Dictionary = pack.verb_by_id("kuru")
	assert_eq(str(iku["group"]), "godan")
	assert_eq(str(hiku["group"]), "godan")
	assert_eq(str(suru["group"]), "irregular")
	assert_eq(str(kuru["group"]), "irregular")
	assert_eq(Conjugation.kana(iku, "te"), "いって")
	assert_eq(Conjugation.kana(iku, "ta"), "いった")
	assert_eq(Conjugation.kana(iku, "nai"), "いかない")
	assert_eq(Conjugation.kana(hiku, "te"), "ひいて")
	assert_eq(Conjugation.kana(suru, "te"), "して")
	assert_eq(Conjugation.kana(suru, "nai"), "しない")
	assert_eq(Conjugation.kana(kuru, "te"), "きて")
	assert_eq(Conjugation.kana(kuru, "nai"), "こない")
	assert_true(ChallengeRules.accepts("いって", iku, "te"))
	assert_true(ChallengeRules.accepts("行って", iku, "te"))
	assert_true(ChallengeRules.accepts(" いって ", iku, "te"))
	assert_false(ChallengeRules.accepts("いいて", iku, "te"))

func test_oyogu_mashita_is_spellable() -> void:
	var pack := ContentPack.load_default()
	var verb: Dictionary = pack.verb_by_id("oyogu")
	var answer := Conjugation.kana(verb, "mashita")
	assert_eq(answer, "およぎました")
	var bank: Array = ChallengeRules.tiles(answer, Rng.new(3))
	assert_true(ChallengeRules.counts_cover(bank, answer))
	var prompt := ChallengeRules.instruction(verb, "mashita")
	assert_true(prompt.find("タップ") >= 0)
	assert_true(prompt.find("ました") >= 0)
	assert_eq(ChallengeRules.instruction_en("mashita"), "Tap the tiles to spell the ました form.")
	var spent: Array = ChallengeRules.take(bank, "お")
	assert_eq(spent.size(), bank.size() - 1)
	assert_eq(spent.find("お"), -1)

func test_every_card_challenge_is_sound() -> void:
	Furigana.prepare(ContentPack.load_default())
	var pack := ContentPack.load_default()
	assert_gt(pack.cards.size(), 100)
	var verbs := 0
	var others := 0
	for item in pack.cards:
		var card: Dictionary = item
		var id := str(card["id"])
		var ruby := str(card.get("ruby", ""))
		assert_gt(ruby.length(), 0, id)
		assert_gt(str(card.get("gloss_ja", "")).length(), 0, id)
		assert_gt(str(card.get("gloss_en", "")).length(), 0, id)
		assert_gte(int(card.get("cost", 0)), 1, id)
		var effect := str(card.get("effect", ""))
		assert_true(effect in ["strike", "ward", "chant", "spark"], id)
		var line := ChallengeRules.effect_line(card, "common")
		assert_gt(line.length(), 0, id)
		assert_lte(line.length(), 16, id)
		assert_eq(Furigana.bare(Furigana.apply(line, [])), "", id)
		for rarity in ["common", "uncommon", "rare", "ultra"]:
			var preview: Dictionary = ChallengeRules.preview(card, rarity)
			var hit: Dictionary = CombatRules.resolve(true, card, rarity, [], false)
			assert_eq(int(preview["damage"]), int(hit["damage"]), "%s %s" % [id, rarity])
			assert_eq(int(preview["block"]), int(hit["block"]), id)
			assert_eq(int(preview["draw"]), int(hit["draw"]), id)
		if effect == "strike":
			assert_gt(int(ChallengeRules.preview(card, "common")["damage"]), 0, id)
			assert_eq(int(ChallengeRules.preview(card, "common")["block"]), 0, id)
		elif effect == "ward" or effect == "spark":
			assert_eq(int(ChallengeRules.preview(card, "common")["damage"]), 0, id)
			assert_gt(int(ChallengeRules.preview(card, "common")["block"]), 0, id)
		elif effect == "chant":
			assert_gt(int(ChallengeRules.preview(card, "common")["block"]), 0, id)
			assert_eq(int(ChallengeRules.preview(card, "common")["draw"]), 1, id)
		var state := {
			"draw": [],
			"discard": [],
			"hand": [card],
			"energy": int(card["cost"]),
			"energy_max": 3,
		}
		assert_true(DeckRules.can_play(state, card), id)
		DeckRules.spend(state, card)
		assert_eq(int(state["energy"]), 0, id)
		assert_eq(state["hand"].size(), 0, id)
		state["energy"] = 0
		state["hand"] = [card]
		assert_false(DeckRules.can_play(state, card), id)
		var kind := ChallengeRules.kind(card)
		if str(card["type"]) == "verb":
			verbs += 1
			assert_eq(kind, "spell", id)
			var verb: Dictionary = pack.verb_by_id(str(card["verb_id"]))
			assert_false(verb.is_empty(), id)
			assert_eq(str(card["group"]), str(verb["group"]), id)
			var row: Dictionary = ExpectedForms.FORMS[str(verb["dict_kana"])]
			for form in ASKED:
				var expected: String = str(row[form])
				assert_eq(Conjugation.kana(verb, form), expected, "%s %s" % [id, form])
				var prompt := ChallengeRules.instruction(verb, form)
				assert_gt(prompt.length(), 0, id)
				assert_true(prompt.find("タップ") >= 0, id)
				assert_eq(Furigana.bare(Furigana.apply(prompt, [])), "", "%s %s" % [id, form])
				var english := ChallengeRules.instruction_en(form)
				assert_true(english.begins_with("Tap the tiles"), id)
				var bank: Array = ChallengeRules.tiles(expected, Rng.new(expected.length()))
				assert_true(ChallengeRules.counts_cover(bank, expected), "%s %s" % [id, form])
				assert_true(ChallengeRules.accepts(expected, verb, form), id)
				assert_true(ChallengeRules.accepts(" " + expected, verb, form), id)
				var shown := ChallengeRules.surface(Conjugation.ruby(verb, form))
				assert_true(ChallengeRules.accepts(shown, verb, form), "%s %s" % [id, form])
				assert_false(ChallengeRules.accepts(expected + "ん", verb, form), id)
		elif str(card["type"]) == "grammar":
			others += 1
			assert_eq(kind, "cloze", id)
			assert_false(ChallengeRules.asks_verb_form(card), id)
			assert_gt(str(card.get("cloze_ruby", "")).length(), 0, id)
			assert_gt(str(card.get("cloze_answer", "")).length(), 0, id)
		else:
			others += 1
			assert_eq(kind, "meaning", id)
			assert_false(ChallengeRules.asks_verb_form(card), id)
	assert_eq(verbs, 36)
	assert_gt(others, verbs)
