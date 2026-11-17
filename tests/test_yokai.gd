extends GutTest

func test_stories_have_readings_and_one_answer() -> void:
	var pack := ContentPack.load_default()
	assert_gte(pack.yokai.size(), 12)
	assert_false(pack.yokai_by_id("ibaraki").is_empty())
	for yokai in pack.yokai:
		assert_false(YokaiRules.bare_kanji(str(yokai["ruby"])), str(yokai["id"]))
		assert_false(YokaiRules.bare_kanji(str(yokai["fact_ruby"])), str(yokai["id"]))
		for story in yokai["stories"]:
			assert_false(YokaiRules.bare_kanji(str(story["text_ruby"])), str(yokai["id"]))
			assert_true(YokaiRules.questions_ok(story), str(yokai["id"]))
			for question in story["questions"]:
				assert_false(YokaiRules.bare_kanji(str(question["prompt_ruby"])))

func test_bosses_drop_and_normals_are_rarer() -> void:
	assert_eq(YokaiRules.drop_chance("boss", 0.0), 100)
	assert_gt(YokaiRules.drop_chance("elite", 0.5), YokaiRules.drop_chance("battle", 0.5))
	var rng := Rng.new(8)
	var drops := 0
	for i in 50:
		if YokaiRules.dropped(rng, 100):
			drops += 1
	assert_eq(drops, 50)
	var scarce := 0
	var quiet := Rng.new(2)
	for i in 80:
		if YokaiRules.dropped(quiet, 0):
			scarce += 1
	assert_eq(scarce, 0)

func test_homework_rewards_the_named_misses() -> void:
	var pack := ContentPack.load_default()
	var sheet: Dictionary = pack.homework[0]
	assert_eq(str(sheet["worksheet_id"]), "No.89")
	assert_eq(str(sheet["date"]), "2026-11-13")
	var counted := HomeworkRules.score(sheet)
	assert_eq(int(counted["correct"]), 11)
	assert_eq(int(counted["total"]), 14)
	var misses := HomeworkRules.named_misses(sheet)
	assert_true("dakuten" in misses)
	assert_true("sokuon" in misses)
	assert_true("word-choice" in misses)
	var reward := HomeworkRules.reward_for(sheet)
	assert_eq(int(reward["gems"]), 4 + 11 * 2)
	assert_eq(reward["weak_verbs"].size(), 3)
	assert_true("dasu" in reward["weak_verbs"])
	assert_true("iku" in reward["weak_verbs"])
	assert_true("hanasu" in reward["weak_verbs"])
	assert_eq(str(reward["relic"]), "")
	var perfect := {"items": [{"correct": true, "card_id": "verb:kaku", "verb_id": "kaku"}, {"correct": true, "card_id": "verb:miru", "verb_id": "miru"}]}
	var full := HomeworkRules.reward_for(perfect)
	assert_eq(str(full["cosmetic_back"]), "gold-margin")
	assert_false(str(full["relic"]) == "")
