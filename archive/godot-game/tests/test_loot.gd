extends GutTest

func test_loot_is_seeded() -> void:
	var first := GearRules.roll(17, "kaku", 2, 1, true)
	var again := GearRules.roll(17, "kaku", 2, 1, true)
	assert_eq(first, again)
	var other := GearRules.roll(99, "kaku", 2, 1, true)
	assert_ne(first["id"], other["id"])

func test_bosses_drop_and_luck_improves_rarity() -> void:
	for n in 12:
		assert_false(GearRules.roll(n + 4, "iku", 3, 0, true).is_empty())
	var plain := 0
	var lucky := 0
	for n in 240:
		var low: Dictionary = GearRules.roll(5000 + n, "kaku", 1, 0, false)
		var high: Dictionary = GearRules.roll(5000 + n, "kaku", 1, 8, false)
		if not low.is_empty() and low["rarity"] in ["rare", "epic"]:
			plain += 1
		if not high.is_empty() and high["rarity"] in ["rare", "epic"]:
			lucky += 1
	assert_gt(lucky, plain)

func test_music_credits_are_complete() -> void:
	assert_eq(MusicCredits.TRACKS.size(), 3)
	for track in MusicCredits.TRACKS:
		assert_eq(track["license"], "CC0")
		assert_true(str(track["source"]).begins_with("https://"))
		assert_true(str(track["author"]) != "")
		assert_true(FileAccess.file_exists(track["file"]))
