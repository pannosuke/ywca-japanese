extends GutTest

func test_map_is_a_fair_path() -> void:
	for seed_value in [1, 7, 19, 42, 99]:
		var map := MapGen.generate(seed_value)
		var rows: Array = map["rows"]
		assert_eq(rows.size(), 7)
		assert_eq(rows[0][0]["type"], "battle")
		assert_eq(rows[rows.size() - 1][0]["type"], "boss")
		assert_true(MapGen.reachable(map), "seed %d" % seed_value)
		assert_true(MapGen.parents_and_children(map), "seed %d links" % seed_value)
		for r in rows.size() - 1:
			assert_false(MapGen.crosses(rows[r], rows[r + 1]), "seed %d row %d" % [seed_value, r])
			assert_gte((rows[r] as Array).size(), 2)
			assert_lte((rows[r] as Array).size(), 4)

func test_same_seed_same_map() -> void:
	var first := JSON.stringify(MapGen.generate(17))
	var again := JSON.stringify(MapGen.generate(17))
	assert_eq(first, again)
	assert_ne(first, JSON.stringify(MapGen.generate(18)))
