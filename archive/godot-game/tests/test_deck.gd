extends GutTest

func test_energy_and_hand() -> void:
	var deck: Array = []
	for i in 8:
		deck.append({"id": "c%d" % i, "cost": 1, "effect": "strike", "ruby": "みる", "type": "verb", "group": "ichidan"})
	var state := DeckRules.opening(deck, Rng.new(3))
	DeckRules.start_turn(state, Rng.new(3))
	assert_eq(state["hand"].size(), 5)
	assert_eq(int(state["energy"]), 3)
	var card: Dictionary = state["hand"][0]
	assert_true(DeckRules.can_play(state, card))
	DeckRules.spend(state, card)
	assert_eq(int(state["energy"]), 2)
	assert_eq(state["hand"].size(), 4)
	assert_false(DeckRules.can_play(state, {"id": "missing", "cost": 1}))
	state["energy"] = 0
	assert_false(DeckRules.can_play(state, state["hand"][0]))

func test_draw_reshuffles_the_discard() -> void:
	var deck: Array = [{"id": "only", "cost": 1, "effect": "ward", "ruby": "ねこ", "type": "noun"}]
	var state := {"draw": [], "discard": deck.duplicate(), "hand": [], "energy": 0, "energy_max": 3}
	DeckRules.start_turn(state, Rng.new(1))
	assert_eq(state["hand"].size(), 1)
	assert_eq(str(state["hand"][0]["id"]), "only")

func test_a_careful_fight_is_winnable() -> void:
	var deck: Array = []
	for i in 8:
		deck.append({"id": "v%d" % i, "cost": 1, "effect": "strike", "ruby": "かく", "type": "verb", "group": "godan"})
	var battle := CombatRules.open(deck, Rng.new(5))
	var hp := CombatRules.max_hp()
	var enemy := CombatRules.enemy_hp("battle")
	var turn := 0
	while enemy > 0 and hp > 0 and turn < 8:
		turn += 1
		var played := 0
		while played < 2 and battle["hand"].size() > 0 and int(battle["energy"]) > 0:
			var card: Dictionary = battle["hand"][0]
			var hit: Dictionary = CombatRules.resolve(true, card, "common", [], false)
			enemy -= int(hit["damage"])
			DeckRules.spend(battle, card)
			played += 1
		if enemy <= 0:
			break
		hp = CombatRules.player_hit(hp, CombatRules.enemy_attack("battle"), 0)
		DeckRules.end_turn(battle)
		DeckRules.start_turn(battle, Rng.new(9 + turn))
	assert_lte(enemy, 0)
	assert_gt(hp, 0)

func test_rarity_and_payout() -> void:
	var state := RarityRules.fresh("verb:kaku")
	for i in 4:
		state = RarityRules.record(state, true, 2000)
	assert_eq(str(state["rarity"]), "uncommon")
	for i in 4:
		state = RarityRules.record(state, true, 2000)
	assert_eq(str(state["rarity"]), "rare")
	for i in 4:
		state = RarityRules.record(state, true, 2000)
	assert_eq(str(state["rarity"]), "ultra")
	state = RarityRules.record(state, false, 2000)
	state = RarityRules.record(state, false, 2000)
	state = RarityRules.record(state, false, 2000)
	assert_eq(str(state["rarity"]), "rare")
	var pay := PayoutRules.battle(3, [{"correct": true, "rarity": "ultra"}, {"correct": false, "rarity": "rare"}, {"correct": true, "rarity": "common"}], 6)
	assert_eq(int(pay["rarity"]), 7 + 1)
	assert_eq(int(pay["combo"]), 2)
	assert_eq(int(pay["total"]), 3 + 8 + 2)
	assert_gt(RewardRules.offer(Rng.new(4), "boss", 1.0, [{"id": "a", "ruby": "見[み]る"}], []).size(), 2)
