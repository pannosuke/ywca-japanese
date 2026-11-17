extends GutTest

func test_every_verb_matches_the_checked_table() -> void:
	var pack := ContentPack.load_default()
	assert_eq(pack.verbs.size(), ExpectedForms.FORMS.size())
	for verb in pack.verbs:
		var row: Dictionary = ExpectedForms.FORMS[verb["dict_kana"]]
		for form in row.keys():
			assert_eq(Conjugation.kana(verb, form), row[form], "%s %s" % [verb["dict_kana"], form])

func test_iku_is_not_the_ordinary_ku_rule() -> void:
	var pack := ContentPack.load_default()
	var iku: Dictionary = pack.verb_by_id("iku")
	var hiku: Dictionary = pack.verb_by_id("hiku")
	var kiru: Dictionary = pack.verb_by_id("kiru")
	var miru: Dictionary = pack.verb_by_id("miru")
	assert_eq(Conjugation.kana(iku, "te"), "いって")
	assert_eq(Conjugation.kana(hiku, "te"), "ひいて")
	assert_eq(Conjugation.kana(kiru, "te"), "きって")
	assert_eq(Conjugation.kana(miru, "te"), "みて")
	assert_true(Conjugation.explain(iku, "te")["ruby"].contains("って"))
	assert_true(Conjugation.ruby(iku, "te").begins_with("行[い]"))

func test_ruby_keeps_a_reading_on_the_kanji() -> void:
	var parts: Array = RubyText.parse("書[か]く")
	assert_eq(parts[0]["base"], "書")
	assert_eq(parts[0]["reading"], "か")
	assert_eq(parts[1]["base"], "く")
	var pack := ContentPack.load_default()
	assert_true(Conjugation.ruby(pack.verb_by_id("kaku"), "te").begins_with("書[か]"))

func test_covered_forms_follow_the_pack() -> void:
	var pack := ContentPack.load_default()
	var forms := Conjugation.covered_forms(pack.lessons)
	assert_true("masu" in forms)
	assert_true("mashita" in forms)
	assert_true("masen" in forms)
	assert_true("te" in forms)
	assert_false("ta" in forms)

func test_commute_lives_in_the_pack_not_the_rules() -> void:
	var pack := ContentPack.load_default()
	var commute := str(pack.profile["commute"]["summary_ruby"])
	assert_true(commute.contains("茨木駅"))
	assert_true(commute.contains("大阪駅"))
	assert_true(commute.contains("快速"))
	assert_true(commute.contains("M16"))
	assert_true(commute.contains("E"))
	for path in ["res://scripts/rules/stat_rules.gd", "res://scripts/rules/gear_rules.gd", "res://scripts/rules/conjugation.gd", "res://scripts/rules/consequences.gd"]:
		var source := FileAccess.get_file_as_string(path)
		assert_eq(source.find("Eric"), -1, path)
		assert_eq(source.find("茨木"), -1, path)
