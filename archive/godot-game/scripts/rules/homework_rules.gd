class_name HomeworkRules
extends RefCounted

static func score(sheet: Dictionary) -> Dictionary:
	var correct := 0
	var items: Array = sheet["items"]
	for item in items:
		if bool(item["correct"]):
			correct += 1
	return {"correct": correct, "total": items.size()}

static func reward_for(sheet: Dictionary) -> Dictionary:
	var counted := score(sheet)
	var boost: Array = []
	var weak: Array = []
	for item in sheet["items"]:
		if bool(item["correct"]):
			if str(item.get("card_id", "")) != "":
				boost.append(item["card_id"])
		elif str(item.get("verb_id", "")) != "":
			weak.append(item["verb_id"])
	var perfect: bool = int(counted["correct"]) == int(counted["total"]) and int(counted["total"]) > 0
	return {
		"gems": 4 + int(counted["correct"]) * 2,
		"boost_cards": boost,
		"weak_verbs": weak,
		"relic": "満点[まんてん]の ふで" if perfect else "",
		"cosmetic_back": "gold-margin" if perfect else "",
		"correct": counted["correct"],
		"total": counted["total"],
	}

static func named_misses(sheet: Dictionary) -> Array:
	var found: Array = []
	for item in sheet["items"]:
		if not bool(item["correct"]):
			found.append(str(item["error_type"]))
	return found
