class_name StatRules
extends RefCounted

const STATS := ["atk", "spd", "def", "luk", "wis"]
const SKILLS := {
	"te_form": {"ruby": "て形[けい]の わざ", "stat": "atk"},
	"guard": {"ruby": "かげの たて", "stat": "def"},
	"haste": {"ruby": "はやあし", "stat": "spd"},
	"fortune": {"ruby": "たからの め", "stat": "luk"},
	"wisdom": {"ruby": "ちえの ともしび", "stat": "wis"},
}

static func level(xp: int) -> int:
	return 1 + int(xp / 40.0)

static func unspent(xp: int, points_spent: int) -> int:
	return maxi(0, level(xp) - 1 - points_spent)

static func totals(state: Dictionary) -> Dictionary:
	var out := {}
	for stat in STATS:
		out[stat] = int(state["base"][stat])
	for item in state["equipped"].values():
		if int(item.get("durability", 0)) <= 0:
			continue
		for stat in item.get("affixes", {}).keys():
			out[stat] = int(out.get(stat, 0)) + int(item["affixes"][stat])
	if int(state.get("streak", 0)) >= 3:
		out["def"] = int(out["def"]) + 1
		out["wis"] = int(out["wis"]) + 1
	return out

static func attack_damage(totals: Dictionary, form: String, skills: Array) -> int:
	var bonus := 2 if (form == "te" and "te_form" in skills) else 0
	return 8 + int(totals["atk"]) * 3 + bonus

static func seconds(totals: Dictionary, skills: Array) -> int:
	var extra := 2 if "haste" in skills else 0
	return clampi(12 + int(totals["spd"]) + extra, 8, 30)

static func hurt(totals: Dictionary, skills: Array) -> int:
	var guard := 1 if "guard" in skills else 0
	return maxi(1, 6 - int(totals["def"]) - guard)

static func xp_gain(totals: Dictionary, correct: bool, skills: Array) -> int:
	if not correct:
		return 1
	var bonus := 1 if "wisdom" in skills else 0
	return 5 + int(totals["wis"]) + bonus

static func enemy_hp(group: String, boss: bool) -> int:
	var table := {"godan": 24, "ichidan": 20, "irregular": 28}
	var base: int = int(table.get(group, 22))
	return base * (2 if boss else 1)

static func enemy_ruby(group: String) -> String:
	match group:
		"ichidan":
			return "るを おとす もの"
		"irregular":
			return "する と 来[く]る の まもり"
		_:
			return "うだんの かげ"

static func max_hp(totals: Dictionary) -> int:
	return 12 + int(totals["def"])
