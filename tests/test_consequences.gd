extends GutTest

func test_wrong_answers_wear_durability_down_to_zero() -> void:
	var state := SaveGame.fresh("pack")
	state["equipped"]["weapon"]["durability"] = 2
	Consequences.wear_equipped(state)
	assert_eq(int(state["equipped"]["weapon"]["durability"]), 1)
	Consequences.wear_equipped(state)
	Consequences.wear_equipped(state)
	assert_eq(int(state["equipped"]["weapon"]["durability"]), 0)
	assert_false(Consequences.repair(state, "starter-sword"))
	state["gems"] = 10
	assert_true(Consequences.repair(state, "starter-sword"))
	assert_eq(int(state["equipped"]["weapon"]["durability"]), 12)
	assert_eq(int(state["gems"]), 8)

func test_death_drops_unbanked_loot_and_keeps_study() -> void:
	var state := SaveGame.fresh("pack")
	state["pouch"] = [GearRules.roll(3, "kaku", 1, 2, true)]
	state["unbanked_gems"] = 9
	state["gems"] = 4
	state["inventory"] = [GearRules.starter_weapon()]
	Consequences.note_review(state, {"id": "kaku", "lesson_date": "2026-11-13"}, false, 100)
	var log: Array = state["study_log"]
	var before := log.size()
	Consequences.die(state)
	var pouch: Array = state["pouch"]
	assert_eq(pouch.size(), 0)
	assert_eq(int(state["unbanked_gems"]), 0)
	assert_eq(int(state["gems"]), 4)
	var kept: Array = state["inventory"]
	var study: Array = state["study_log"]
	assert_eq(kept.size(), 1)
	assert_eq(study.size(), before)
	assert_true("kaku" in state["reviews"])
	assert_false(state["equipped"]["weapon"].is_empty())

func test_a_neglected_verb_corrupts_its_region_until_reviewed() -> void:
	var state := SaveGame.fresh("pack")
	var verb := {"id": "kaku", "lesson_date": "2026-11-13"}
	Consequences.note_review(state, verb, false, 1_000)
	assert_true("2026-11-13" in Consequences.corrupt_dates(state, 2_000))
	Consequences.note_review(state, verb, true, 3_000)
	assert_false("2026-11-13" in Consequences.corrupt_dates(state, 4_000))
	var later := 3_000 + Consequences.NEGLECT_SECONDS + 10
	assert_true("2026-11-13" in Consequences.corrupt_dates(state, later))

func test_banking_moves_the_pouch() -> void:
	var state := SaveGame.fresh("pack")
	state["pouch"] = [{"id": "gem-ring", "slot": "accessory", "rarity": "common", "name_ruby": "木[き]の ゆびわ", "affixes": {"luk": 1}, "durability": 6, "max_durability": 6}]
	state["unbanked_gems"] = 5
	Consequences.bank(state)
	var pouch: Array = state["pouch"]
	var stored: Array = state["inventory"]
	assert_eq(pouch.size(), 0)
	assert_eq(stored.size(), 1)
	assert_eq(int(state["gems"]), 5)
	assert_eq(int(state["unbanked_gems"]), 0)

func test_hooks_stay_closed() -> void:
	assert_false(SocialHooks.sync_available())
	assert_false(SocialHooks.leaderboard_available())
	assert_false(SocialHooks.party_available())
	assert_true(SocialHooks.friend_code("local").begins_with("YB-"))
