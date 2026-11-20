extends GutTest

func test_damage_seconds_and_level() -> void:
	var state := SaveGame.fresh("pack")
	state["equipped"] = {}
	state["base"]["atk"] = 2
	state["base"]["spd"] = 1
	state["base"]["def"] = 1
	state["xp"] = 40
	assert_eq(StatRules.level(40), 2)
	assert_eq(StatRules.unspent(40, 0), 1)
	var totals := StatRules.totals(state)
	assert_eq(StatRules.attack_damage(totals, "te", []), 8 + 2 * 3)
	assert_eq(StatRules.attack_damage(totals, "te", ["te_form"]), 8 + 2 * 3 + 2)
	assert_eq(StatRules.seconds(totals, []), 13)
	assert_eq(StatRules.hurt(totals, []), 5)
	state["base"]["def"] = 20
	assert_eq(StatRules.hurt(StatRules.totals(state), []), 1)
	assert_eq(StatRules.xp_gain(totals, true, []), 5 + int(totals["wis"]))
	assert_eq(StatRules.enemy_hp("irregular", true), 56)

func test_streak_buffs_defense() -> void:
	var state := SaveGame.fresh("pack")
	state["streak"] = 3
	var totals := StatRules.totals(state)
	assert_eq(int(totals["def"]), int(state["base"]["def"]) + 1)

func test_broken_gear_does_not_add_stats() -> void:
	var state := SaveGame.fresh("pack")
	state["equipped"]["weapon"]["durability"] = 0
	state["equipped"]["weapon"]["affixes"] = {"atk": 5}
	var totals := StatRules.totals(state)
	assert_eq(int(totals["atk"]), int(state["base"]["atk"]))
