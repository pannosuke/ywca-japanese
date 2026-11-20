class_name RewardRules
extends RefCounted

const RELICS := [
	{"id": "lantern", "ruby": "ともしび", "text": "たたかいの はじめの まちがいを、いちど だけ ゆるす。"},
	{"id": "clock", "ruby": "とけい", "text": "スピードの じかんが 5びょう ふえる。"},
	{"id": "nidan", "ruby": "にだん", "text": "グループ2の カードの ダメージが ばい。"},
	{"id": "firefly", "ruby": "ほたる", "text": "ただしい カードごとに たまが ひとつ ふえる。"},
]

static func offer(rng: Rng, node_type: String, accuracy: float, cards: Array, owned: Array) -> Array:
	var picks: Array = []
	var guard := 0
	while picks.size() < 3 and guard < 40:
		guard += 1
		var roll := rng.next_index(100)
		var gate := 70
		if node_type == "elite":
			gate = 45
		elif node_type == "boss":
			gate = 25
		if accuracy >= 0.9:
			gate -= 15
		if roll > gate and not cards.is_empty():
			var card: Dictionary = cards[rng.next_index(cards.size())]
			picks.append({"kind": "card", "id": card["id"], "ruby": card["ruby"]})
		elif not owned.has("lantern") or rng.next_index(2) == 0:
			var relic: Dictionary = RELICS[rng.next_index(RELICS.size())]
			if not owned.has(relic["id"]):
				picks.append({"kind": "relic", "id": relic["id"], "ruby": relic["ruby"]})
		else:
			picks.append({"kind": "gems", "id": "gems", "ruby": "ほたる玉[だま]", "amount": 3 + rng.next_index(4)})
	while picks.size() < 3:
		picks.append({"kind": "gems", "id": "gems-%d" % picks.size(), "ruby": "ほたる玉[だま]", "amount": 2})
	return picks

static func speed_seconds(relics: Array) -> int:
	return 25 if "clock" in relics else 20
