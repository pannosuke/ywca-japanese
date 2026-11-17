extends Node

const LOOKS := [
	{"id": "leaf", "ruby": "わかば", "cost": 0, "color": "7dba6a"},
	{"id": "dusk", "ruby": "ゆうやけ", "cost": 8, "color": "e08a3c"},
	{"id": "ink", "ruby": "すみ", "cost": 8, "color": "355c7d"},
]

var pack: ContentPack
var state: Dictionary
var font: Font
var world: Node3D
var music: AudioStreamPlayer
var music_id := ""
var layer: CanvasLayer
var veil: ColorRect
var panel: Control
var screen := "home"
var timer_label: Label
var fight_timer: Timer
var shot := ""

func _ready() -> void:
	pack = ContentPack.load_default()
	font = load("res://assets/fonts/NotoSansJP-Regular.otf")
	state = SaveGame.load_or_new(pack.id)
	_ensure_save_shape()
	music = AudioStreamPlayer.new()
	add_child(music)
	fight_timer = Timer.new()
	fight_timer.wait_time = 0.2
	fight_timer.timeout.connect(_on_tick)
	add_child(fight_timer)
	fight_timer.start()
	layer = CanvasLayer.new()
	add_child(layer)
	veil = ColorRect.new()
	veil.set_anchors_preset(Control.PRESET_FULL_RECT)
	veil.mouse_filter = Control.MOUSE_FILTER_IGNORE
	layer.add_child(veil)
	panel = Control.new()
	panel.set_anchors_preset(Control.PRESET_FULL_RECT)
	panel.theme = _theme()
	layer.add_child(panel)
	shot = OS.get_environment("YORUBEN_SHOT")
	if shot != "":
		_apply_shot(shot)
	else:
		_show_home()
		_play_music(int(state.get("region", 0)))
	if shot != "":
		_capture_shot()

func _ensure_save_shape() -> void:
	if not state.has("seed"):
		state["seed"] = 20261117
	if not state.has("floor"):
		state["floor"] = 0
	if not state.has("region"):
		state["region"] = 0
	if not state.has("cosmetics_owned"):
		state["cosmetics_owned"] = ["leaf"]
	if not state.has("cosmetic"):
		state["cosmetic"] = "leaf"
	if state.get("equipped") == null:
		state["equipped"] = {"weapon": GearRules.starter_weapon()}

func _theme() -> Theme:
	var theme := Theme.new()
	theme.default_font = font
	theme.default_font_size = 22
	var normal := StyleBoxFlat.new()
	normal.bg_color = Color("1f6b3a")
	normal.set_corner_radius_all(16)
	normal.content_margin_left = 12
	normal.content_margin_right = 12
	normal.content_margin_top = 8
	normal.content_margin_bottom = 8
	theme.set_stylebox("normal", "Button", normal)
	var hover := normal.duplicate()
	hover.bg_color = Color("2f8a4e")
	theme.set_stylebox("hover", "Button", hover)
	theme.set_stylebox("pressed", "Button", hover)
	theme.set_color("font_color", "Button", Color.WHITE)
	theme.set_color("font_color", "Label", Color("1c2416"))
	return theme

func _clear() -> void:
	timer_label = null
	for child in panel.get_children():
		panel.remove_child(child)
		child.free()

func _shell() -> Dictionary:
	_clear()
	var root := VBoxContainer.new()
	root.set_anchors_preset(Control.PRESET_FULL_RECT)
	root.offset_left = 14
	root.offset_top = 16
	root.offset_right = -14
	root.offset_bottom = -16
	root.add_theme_constant_override("separation", 10)
	panel.add_child(root)
	var scroll := ScrollContainer.new()
	scroll.size_flags_vertical = Control.SIZE_EXPAND_FILL
	scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	root.add_child(scroll)
	var body := VBoxContainer.new()
	body.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	body.custom_minimum_size = Vector2(500, 0)
	body.add_theme_constant_override("separation", 8)
	scroll.add_child(body)
	var dock := VBoxContainer.new()
	dock.add_theme_constant_override("separation", 8)
	root.add_child(dock)
	return {"body": body, "dock": dock}

func _paper(on: bool) -> void:
	veil.color = Color("f4e7c8") if on else Color(0, 0, 0, 0)
	if world:
		world.visible = not on

func _set_world(kind: String) -> void:
	if world:
		world.queue_free()
		world = null
	world = WorldView.build(kind)
	add_child(world)
	move_child(world, 0)
	for child in world.get_children():
		if child is Sprite3D and child.texture != null and str(child.texture.resource_path).contains("hero"):
			child.modulate = _look_color()
	_paper(false)

func _look_color() -> Color:
	for look in LOOKS:
		if look["id"] == str(state.get("cosmetic", "leaf")):
			return Color(str(look["color"]))
	return Color.WHITE

func _card() -> VBoxContainer:
	var frame := PanelContainer.new()
	var style := StyleBoxFlat.new()
	style.bg_color = Color(0.97, 0.93, 0.84, 0.9)
	style.set_corner_radius_all(14)
	style.content_margin_left = 12
	style.content_margin_right = 12
	style.content_margin_top = 8
	style.content_margin_bottom = 8
	frame.add_theme_stylebox_override("panel", style)
	var box := VBoxContainer.new()
	box.add_theme_constant_override("separation", 4)
	frame.add_child(box)
	return box

func _ruby(parent: Node, markup: String, size: int, centered := false) -> void:
	var whole := RubyLabel.new()
	whole.font = font
	whole.font_size = size
	whole.align_center = centered
	whole.set_markup(markup)
	if whole.custom_minimum_size.x <= 500:
		parent.add_child(whole)
		return
	whole.free()
	for chunk in markup.split(" "):
		if chunk == "":
			continue
		var label := RubyLabel.new()
		label.font = font
		label.font_size = size
		label.align_center = centered
		label.set_markup(chunk)
		parent.add_child(label)

func _en(parent: Node, text: String) -> void:
	var label := Label.new()
	label.text = text
	label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	label.add_theme_font_size_override("font_size", 16)
	label.add_theme_color_override("font_color", Color("5c5346"))
	parent.add_child(label)

func _plain(parent: Node, text: String, size := 20) -> Label:
	var label := Label.new()
	label.text = text
	label.add_theme_font_size_override("font_size", size)
	label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	parent.add_child(label)
	return label

func _button(parent: Node, markup: String, cb: Callable) -> Button:
	var button := Button.new()
	button.text = ""
	button.custom_minimum_size = Vector2(0, 72)
	button.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	var label := RubyLabel.new()
	label.font = font
	label.font_size = 26
	label.color = Color.WHITE
	label.align_center = true
	label.mouse_filter = Control.MOUSE_FILTER_IGNORE
	label.set_anchors_preset(Control.PRESET_FULL_RECT)
	label.set_markup(markup)
	button.add_child(label)
	button.pressed.connect(cb)
	parent.add_child(button)
	return button

func _save() -> void:
	if shot != "":
		return
	SaveGame.write(state)

func _play_music(index: int) -> void:
	var track := MusicCredits.for_region(index)
	var volume := float(state.get("music", 0.7))
	if volume <= 0.01:
		music.stop()
		music_id = ""
		return
	if music_id != str(track["id"]):
		var stream: AudioStream = load(track["file"])
		if stream is AudioStreamOggVorbis:
			stream = stream.duplicate()
			stream.loop = true
		music.stream = stream
		music_id = str(track["id"])
	music.volume_db = linear_to_db(maxf(volume, 0.001))
	if not music.playing:
		music.play()

func _show_home() -> void:
	screen = "home"
	_paper(true)
	var ui := _shell()
	_ruby(ui.body, "よるべん", 40)
	_ruby(ui.body, str(pack.title_ruby), 22)
	_status(ui.body)
	_en(ui.body, "Study notes stay. Only unbanked gear and gems are at stake.")
	_ruby(ui.body, "AnkiDroid は つぎの だんかい。パックの デッキめいは のこしてある。", 18)
	if state.get("run") != null:
		_button(ui.dock, "つづき", _show_battle)
	_button(ui.dock, "もり", _show_overworld)
	_button(ui.dock, "たたかう", _start_or_resume)
	_button(ui.dock, "レッスン", _show_lessons)
	_button(ui.dock, "れんしゅう", _show_practice_menu)
	_button(ui.dock, "じゅんび", _show_prep)
	_button(ui.dock, "そうび", _show_loot)
	_button(ui.dock, "きろく", _show_sheet)

func _status(parent: Node) -> void:
	var totals := StatRules.totals(state)
	var run = state.get("run")
	var hp := StatRules.max_hp(totals) if run == null else int(run.get("player_hp", 0))
	_plain(parent, "Lv %d   HP %d   ほたる玉 %d   つづけ %d" % [
		StatRules.level(int(state["xp"])), hp, int(state["gems"]), int(state["streak"])
	])
	if int(state.get("unbanked_gems", 0)) > 0 or (state.get("pouch") != null and state["pouch"].size() > 0):
		_ruby(parent, "あずけてない たからが ある", 18)

func _show_overworld() -> void:
	screen = "overworld"
	_set_world("overworld")
	_play_music(int(state.get("region", 0)))
	var ui := _shell()
	var card := _card()
	ui.body.add_child(card.get_parent())
	_ruby(card, "灯[あか]りの 森[もり]", 34)
	var lessons := pack.lessons_sorted()
	var corrupt: Array = Consequences.corrupt_dates(state, int(Time.get_unix_time_from_system()))
	var index := clampi(int(state.get("region", 0)), 0, maxi(lessons.size() - 1, 0))
	if lessons.size() > 0:
		var lesson: Dictionary = lessons[index]
		_ruby(card, str(lesson.get("topic_ruby", lesson["date"])), 24)
		_en(card, "%s  %s" % [lesson["date"], lesson.get("topic_en", "")])
		if str(lesson["date"]) in corrupt:
			_ruby(card, "この くいきは くもっている。ふくしゅうするまで かげが ふかい。", 20)
	var track := MusicCredits.for_region(index)
	_ruby(card, str(track["use_ruby"]), 18)
	_button(ui.dock, "あるく", _walk)
	_button(ui.dock, "たたかう", _fight_region)
	_button(ui.dock, "さと", _show_loot)
	_button(ui.dock, "いえ", _show_home)

func _walk() -> void:
	var count := pack.lessons_sorted().size()
	if count == 0:
		return
	state["region"] = (int(state.get("region", 0)) + 1) % count
	_save()
	_show_overworld()

func _fight_region() -> void:
	var lessons := pack.lessons_sorted()
	var boss := false
	var pool: Array = pack.verbs
	if lessons.size() > 0:
		var lesson: Dictionary = lessons[clampi(int(state.get("region", 0)), 0, lessons.size() - 1)]
		var dated: Array = []
		for verb in pack.verbs:
			if str(verb.get("lesson_date", "")) == str(lesson["date"]):
				dated.append(verb)
		if dated.size() > 0:
			pool = dated
		boss = str(lesson["date"]) in Consequences.corrupt_dates(state, int(Time.get_unix_time_from_system()))
	_begin_fight(pool, boss)

func _start_or_resume() -> void:
	if state.get("run") != null:
		_show_battle()
		return
	_begin_fight(pack.verbs, int(state.get("floor", 0)) > 0 and int(state["floor"]) % 4 == 0)

func _begin_fight(pool: Array, boss: bool) -> void:
	if pool.is_empty():
		return
	var rng := Rng.new((int(state["seed"]) ^ (int(state.get("floor", 0)) * 97) ^ int(Time.get_unix_time_from_system())) & 0x7fffffff)
	var verb: Dictionary = pool[rng.next_index(pool.size())]
	var forms: Array = Conjugation.covered_forms(pack.lessons)
	var form: String = forms[rng.next_index(forms.size())]
	var totals := StatRules.totals(state)
	var player := StatRules.max_hp(totals)
	var enemy := StatRules.enemy_hp(str(verb["group"]), boss)
	state["floor"] = int(state.get("floor", 0)) + 1
	state["run"] = {
		"verb_id": verb["id"],
		"form": form,
		"choices": Conjugation.choices(verb, form, rng),
		"answer": Conjugation.kana(verb, form),
		"enemy_hp": enemy,
		"enemy_max": enemy,
		"player_hp": player,
		"player_max": player,
		"boss": boss,
		"left": StatRules.seconds(totals, state["skills"]),
		"seconds": StatRules.seconds(totals, state["skills"]),
		"subtitle_ruby": "",
		"subtitle_en": "",
		"resolved": false,
		"loot_ruby": "",
	}
	_save()
	_show_battle()

func _show_battle() -> void:
	screen = "battle"
	var run = state.get("run")
	if run == null:
		_show_home()
		return
	_set_world("battle")
	var verb := pack.verb_by_id(str(run["verb_id"]))
	WorldView.tint_enemy(world, str(verb.get("group", "godan")))
	_play_music(int(state.get("region", 0)))
	var ui := _shell()
	var card := _card()
	ui.body.add_child(card.get_parent())
	var totals := StatRules.totals(state)
	if int(totals["wis"]) >= 3 or "wisdom" in state["skills"]:
		_ruby(card, StatRules.enemy_ruby(str(verb["group"])), 18)
	_ruby(card, Conjugation.prompt(verb, str(run["form"])), 28, true)
	_plain(card, "てき %d/%d    じぶん %d/%d" % [int(run["enemy_hp"]), int(run["enemy_max"]), int(run["player_hp"]), int(run["player_max"])])
	timer_label = _plain(card, "%d" % int(ceil(float(run["left"]))), 28)
	if str(run.get("subtitle_ruby", "")) != "":
		_ruby(card, str(run["subtitle_ruby"]), 18)
		_en(card, str(run.get("subtitle_en", "")))
	if str(run.get("loot_ruby", "")) != "":
		_ruby(card, "てに いれた: " + str(run["loot_ruby"]), 20)
	if bool(run.get("resolved", false)):
		if int(run["player_hp"]) <= 0:
			_button(ui.dock, "いえ", _show_home)
		elif int(run["enemy_hp"]) <= 0:
			_button(ui.dock, "さとで あずける", _bank_and_village)
			_button(ui.dock, "つぎの てき", _start_or_resume)
		else:
			_button(ui.dock, "つぎ", _next_question)
		_button(ui.dock, "もどる", _show_home)
		return
	for choice in run["choices"]:
		_button(ui.dock, str(choice), _resolve.bind(str(choice)))

func _on_tick() -> void:
	if screen != "battle" or timer_label == null:
		return
	var run = state.get("run")
	if run == null or bool(run.get("resolved", false)):
		return
	run["left"] = float(run["left"]) - 0.2
	timer_label.text = "%d" % int(ceil(maxf(float(run["left"]), 0)))
	if float(run["left"]) <= 0:
		_resolve("")

func _resolve(choice: String) -> void:
	var run = state.get("run")
	if run == null or bool(run.get("resolved", false)):
		return
	var verb := pack.verb_by_id(str(run["verb_id"]))
	var correct := choice == str(run["answer"])
	var now := int(Time.get_unix_time_from_system())
	SaveGame.touch_day(state, SaveGame.tokyo_day(now))
	Consequences.note_review(state, verb, correct, now)
	var totals := StatRules.totals(state)
	var explained := Conjugation.explain(verb, str(run["form"]))
	run["subtitle_ruby"] = explained["ruby"]
	run["subtitle_en"] = explained["en"]
	run["resolved"] = true
	if correct:
		run["enemy_hp"] = int(run["enemy_hp"]) - StatRules.attack_damage(totals, str(run["form"]), state["skills"])
		state["xp"] = int(state["xp"]) + StatRules.xp_gain(totals, true, state["skills"])
		if int(run["enemy_hp"]) <= 0:
			run["enemy_hp"] = 0
			state["unbanked_gems"] = int(state.get("unbanked_gems", 0)) + (6 if bool(run["boss"]) else 3)
			var loot := GearRules.roll(int(state["seed"]), str(verb["id"]), int(state["floor"]), int(totals["luk"]), bool(run["boss"]))
			if not loot.is_empty():
				state["pouch"].append(loot)
				run["loot_ruby"] = loot["name_ruby"]
	else:
		Input.vibrate_handheld(40)
		run["player_hp"] = int(run["player_hp"]) - StatRules.hurt(totals, state["skills"])
		Consequences.wear_equipped(state)
		state["xp"] = int(state["xp"]) + 1
		if int(run["player_hp"]) <= 0:
			run["player_hp"] = 0
			Consequences.die(state)
	_save()
	_show_battle()

func _next_question() -> void:
	var run = state.get("run")
	if run == null:
		_start_or_resume()
		return
	var verb := pack.verb_by_id(str(run["verb_id"]))
	var rng := Rng.new((int(state["seed"]) ^ int(Time.get_unix_time_from_system()) ^ int(run["enemy_hp"])) & 0x7fffffff)
	var forms: Array = Conjugation.covered_forms(pack.lessons)
	var form: String = forms[rng.next_index(forms.size())]
	var totals := StatRules.totals(state)
	run["form"] = form
	run["choices"] = Conjugation.choices(verb, form, rng)
	run["answer"] = Conjugation.kana(verb, form)
	run["left"] = StatRules.seconds(totals, state["skills"])
	run["resolved"] = false
	run["subtitle_ruby"] = ""
	run["subtitle_en"] = ""
	_save()
	_show_battle()

func _bank_and_village() -> void:
	Consequences.bank(state)
	state["run"] = null
	_save()
	_show_loot()

func _show_sheet() -> void:
	screen = "sheet"
	_paper(true)
	var ui := _shell()
	var totals := StatRules.totals(state)
	_ruby(ui.body, "きろく", 34)
	_plain(ui.body, "Lv %d    けいけん %d    ふりわけ %d" % [StatRules.level(int(state["xp"])), int(state["xp"]), StatRules.unspent(int(state["xp"]), int(state["points_spent"]))])
	_plain(ui.body, "ATK %d   SPD %d   DEF %d   LUK %d   WIS %d" % [totals["atk"], totals["spd"], totals["def"], totals["luk"], totals["wis"]])
	_en(ui.body, "ATK is conjugation power. SPD is time. DEF softens a miss. LUK is loot. WIS is hints and XP. A streak of 3 adds DEF and WIS.")
	_ruby(ui.body, "ともだち コード " + SocialHooks.friend_code(str(state.get("profile_id", "local"))), 20)
	_en(ui.body, "Sync, leaderboards, and co-op are not connected.")
	for stat in StatRules.STATS:
		_button(ui.body, stat + " を あげる", _spend_stat.bind(stat))
	for skill_id in StatRules.SKILLS.keys():
		var owned: bool = skill_id in state["skills"]
		var label: String = StatRules.SKILLS[skill_id]["ruby"]
		if owned:
			_ruby(ui.body, label + " を もっている", 20)
		else:
			_button(ui.body, label, _spend_skill.bind(skill_id))
	_ruby(ui.body, "みため", 24)
	for look in LOOKS:
		var have: bool = look["id"] in state["cosmetics_owned"]
		if have:
			_button(ui.body, str(look["ruby"]), _equip_look.bind(str(look["id"])))
		else:
			_button(ui.body, str(look["ruby"]) + "  %d" % int(look["cost"]), _buy_look.bind(str(look["id"])))
	_ruby(ui.body, "おんりょう", 24)
	_volume(ui.body, "music")
	_volume(ui.body, "sfx")
	_button(ui.dock, "おんがくの クレジット", _show_credits)
	_button(ui.dock, "いえ", _show_home)

func _volume(parent: Node, key: String) -> void:
	_plain(parent, key)
	var slider := HSlider.new()
	slider.min_value = 0
	slider.max_value = 1
	slider.step = 0.05
	slider.value = float(state.get(key, 0.7))
	slider.custom_minimum_size = Vector2(0, 48)
	slider.value_changed.connect(_set_volume.bind(key))
	parent.add_child(slider)

func _set_volume(value: float, key: String) -> void:
	state[key] = value
	_save()
	if key == "music":
		music_id = ""
		_play_music(int(state.get("region", 0)))

func _spend_stat(stat: String) -> void:
	if StatRules.unspent(int(state["xp"]), int(state["points_spent"])) < 1:
		return
	state["base"][stat] = int(state["base"][stat]) + 1
	state["points_spent"] = int(state["points_spent"]) + 1
	_save()
	_show_sheet()

func _spend_skill(skill_id: String) -> void:
	if skill_id in state["skills"]:
		return
	if StatRules.unspent(int(state["xp"]), int(state["points_spent"])) < 1:
		return
	state["skills"].append(skill_id)
	state["points_spent"] = int(state["points_spent"]) + 1
	_save()
	_show_sheet()

func _buy_look(look_id: String) -> void:
	for look in LOOKS:
		if look["id"] != look_id:
			continue
		if look_id in state["cosmetics_owned"]:
			_equip_look(look_id)
			return
		if int(state["gems"]) < int(look["cost"]):
			return
		state["gems"] = int(state["gems"]) - int(look["cost"])
		state["cosmetics_owned"].append(look_id)
		state["cosmetic"] = look_id
		_save()
		_show_sheet()

func _equip_look(look_id: String) -> void:
	state["cosmetic"] = look_id
	_save()
	_show_sheet()

func _show_loot() -> void:
	screen = "loot"
	_paper(true)
	var ui := _shell()
	_ruby(ui.body, "灯[あか]りの 里[さと]", 34)
	_plain(ui.body, "ほたる玉 %d    みあずけ %d" % [int(state["gems"]), int(state.get("unbanked_gems", 0))])
	_ruby(ui.body, "そうび", 24)
	for slot in GearRules.SLOTS:
		if state["equipped"].has(slot):
			_item_row(ui.body, state["equipped"][slot], true)
		else:
			_ruby(ui.body, slot, 18)
	if state["pouch"].size() > 0:
		_ruby(ui.body, "もちあるき", 24)
		for item in state["pouch"]:
			_item_row(ui.body, item, false)
		_button(ui.body, "あずける", _do_bank)
	if state["inventory"].size() > 0:
		_ruby(ui.body, "くら", 24)
		for item in state["inventory"]:
			_item_row(ui.body, item, false)
			_button(ui.body, "そうびする", _equip_item.bind(str(item["id"])))
	_button(ui.dock, "いえ", _show_home)

func _item_row(parent: Node, item: Dictionary, equipped: bool) -> void:
	_ruby(parent, str(item["name_ruby"]), 22)
	var affix := ""
	for key in item["affixes"].keys():
		affix += "%s+%s " % [key, str(item["affixes"][key])]
	_plain(parent, "%s  %s  たいきゅう %d/%d%s" % [item["slot"], item["rarity"], int(item["durability"]), int(item["max_durability"]), "  そうびちゅう" if equipped else ""])
	_plain(parent, affix)
	if int(item["durability"]) < int(item["max_durability"]):
		_button(parent, "しゅうり %d" % GearRules.repair_cost(item), _repair_item.bind(str(item["id"])))

func _do_bank() -> void:
	Consequences.bank(state)
	_save()
	_show_loot()

func _repair_item(item_id: String) -> void:
	Consequences.repair(state, item_id)
	_save()
	_show_loot()

func _equip_item(item_id: String) -> void:
	var found: Dictionary = {}
	var rest: Array = []
	for item in state["inventory"]:
		if item["id"] == item_id and found.is_empty():
			found = item
		else:
			rest.append(item)
	if found.is_empty():
		return
	var slot := str(found["slot"])
	if state["equipped"].has(slot):
		rest.append(state["equipped"][slot])
	state["inventory"] = rest
	state["equipped"][slot] = found
	_save()
	_show_loot()

func _show_lessons() -> void:
	screen = "lessons"
	_paper(true)
	var ui := _shell()
	_ruby(ui.body, "レッスン", 34)
	for lesson in pack.lessons_sorted():
		_button(ui.body, "%s  %s" % [lesson["date"], lesson.get("topic_ruby", "")], _show_lesson.bind(str(lesson["id"])))
	_button(ui.dock, "いえ", _show_home)

func _show_lesson(lesson_id: String) -> void:
	screen = "lesson"
	_paper(true)
	var ui := _shell()
	for lesson in pack.lessons:
		if str(lesson["id"]) != lesson_id:
			continue
		_ruby(ui.body, str(lesson.get("topic_ruby", lesson["date"])), 28)
		_en(ui.body, "%s  %s" % [lesson["date"], lesson.get("topic_en", "")])
		for point in lesson.get("grammar", []):
			_ruby(ui.body, str(point.get("pattern_ruby", "")), 22)
			_ruby(ui.body, str(point.get("explanation_ja", "")), 18)
			_en(ui.body, str(point.get("explanation_en", "")))
			if str(point.get("example_ruby", "")) != "":
				_ruby(ui.body, str(point["example_ruby"]), 20)
		for word in pack.words:
			if str(word.get("lesson_date", "")) == str(lesson["date"]):
				_ruby(ui.body, str(word.get("ruby", word.get("id", ""))), 20)
				if str(word.get("gloss_ja", "")) != "":
					_ruby(ui.body, str(word["gloss_ja"]), 16)
				_en(ui.body, str(word.get("gloss_en", "")))
	_button(ui.dock, "レッスン", _show_lessons)
	_button(ui.dock, "いえ", _show_home)

func _show_practice_menu() -> void:
	screen = "practice"
	_paper(true)
	var ui := _shell()
	_ruby(ui.body, "れんしゅう", 34)
	_en(ui.body, "Practice records the review and does not wear gear. The forest is where a miss costs durability.")
	_button(ui.dock, "かつよう", _start_drill.bind("verb"))
	_button(ui.dock, "クラスの もんだい", _start_drill.bind("class"))
	_button(ui.dock, "いえ", _show_home)

func _start_drill(kind: String) -> void:
	var rng := Rng.new(int(Time.get_unix_time_from_system()) & 0x7fffffff)
	var card := {}
	if kind == "verb":
		var verb: Dictionary = pack.verbs[rng.next_index(pack.verbs.size())]
		var forms: Array = Conjugation.covered_forms(pack.lessons)
		var form: String = forms[rng.next_index(forms.size())]
		card = {
			"kind": "verb",
			"verb_id": verb["id"],
			"form": form,
			"prompt": Conjugation.prompt(verb, form),
			"choices": Conjugation.choices(verb, form, rng),
			"answer": Conjugation.kana(verb, form),
			"explain_ruby": "",
			"explain_en": "",
		}
	else:
		card = _class_card(rng)
	card["resolved"] = false
	card["picked"] = ""
	state["drill"] = card
	_show_drill()

func _class_card(rng: Rng) -> Dictionary:
	var cards: Array = []
	for spot in pack.profile.get("weak_spots", []):
		for item in spot.get("items", []):
			if str(item.get("mode", "")) != "mc":
				continue
			var choices: Array = []
			var answer := ""
			for choice in item.get("choices", []):
				choices.append(str(choice.get("ruby", choice.get("en", ""))))
				if str(choice.get("id", "")) == str(item.get("answer_id", "")):
					answer = str(choice.get("ruby", choice.get("en", "")))
			cards.append({
				"kind": "class",
				"verb_id": "",
				"prompt": str(item.get("prompt_ruby", "")),
				"choices": choices,
				"answer": answer,
				"explain_ruby": str(item.get("explain_ruby", "")),
				"explain_en": str(item.get("explain_en", "")),
			})
	for lesson in pack.lessons:
		for point in lesson.get("grammar", []):
			if str(point.get("cloze_answer", "")) == "":
				continue
			cards.append({
				"kind": "class",
				"verb_id": "",
				"prompt": str(point.get("cloze_ruby", point.get("pattern_ruby", ""))),
				"choices": [],
				"answer": str(point["cloze_answer"]),
				"explain_ruby": str(point.get("explanation_ja", "")),
				"explain_en": str(point.get("explanation_en", "")),
				"pool": "cloze",
			})
	if cards.is_empty():
		return {"kind": "class", "verb_id": "", "prompt": "もんだいが ない", "choices": ["つぎ"], "answer": "つぎ", "explain_ruby": "", "explain_en": ""}
	var card: Dictionary = cards[rng.next_index(cards.size())]
	if card.get("pool", "") == "cloze":
		var answers: Array = [card["answer"]]
		for lesson in pack.lessons:
			for point in lesson.get("grammar", []):
				var other := str(point.get("cloze_answer", ""))
				if other != "" and other not in answers:
					answers.append(other)
		var picked: Array = []
		while picked.size() < 3 and answers.size() > 1:
			var guess: String = answers[rng.next_index(answers.size())]
			if guess != card["answer"] and guess not in picked:
				picked.append(guess)
		picked.append(card["answer"])
		var shuffled: Array = []
		while picked.size() > 0:
			shuffled.append(picked.pop_at(rng.next_index(picked.size())))
		card["choices"] = shuffled
	return card

func _show_drill() -> void:
	screen = "drill"
	_paper(true)
	var card: Dictionary = state.get("drill", {})
	var ui := _shell()
	_ruby(ui.body, "れんしゅう", 28)
	_ruby(ui.body, str(card.get("prompt", "")), 26)
	if bool(card.get("resolved", false)):
		var ok: bool = str(card.get("picked", "")) == str(card.get("answer", ""))
		_ruby(ui.body, "せいかい" if ok else "こたえ: " + str(card.get("answer", "")), 22)
		if str(card.get("explain_ruby", "")) != "":
			_ruby(ui.body, str(card["explain_ruby"]), 18)
			_en(ui.body, str(card.get("explain_en", "")))
		_button(ui.dock, "つぎ", _start_drill.bind(str(card.get("kind", "verb"))))
	else:
		for choice in card.get("choices", []):
			_button(ui.dock, str(choice), _answer_drill.bind(str(choice)))
	_button(ui.dock, "いえ", _show_home)

func _answer_drill(choice: String) -> void:
	var card: Dictionary = state.get("drill", {})
	if bool(card.get("resolved", false)):
		return
	card["picked"] = choice
	card["resolved"] = true
	var ok: bool = choice == str(card.get("answer", ""))
	if not ok:
		Input.vibrate_handheld(30)
	if str(card.get("kind", "")) == "verb":
		var verb := pack.verb_by_id(str(card.get("verb_id", "")))
		var now := int(Time.get_unix_time_from_system())
		SaveGame.touch_day(state, SaveGame.tokyo_day(now))
		Consequences.note_review(state, verb, ok, now)
		var explained := Conjugation.explain(verb, str(card.get("form", "masu")))
		card["explain_ruby"] = explained["ruby"]
		card["explain_en"] = explained["en"]
		state["xp"] = int(state["xp"]) + (StatRules.xp_gain(StatRules.totals(state), true, state["skills"]) if ok else 1)
	state["drill"] = card
	_save()
	_show_drill()

func _show_prep() -> void:
	screen = "prep"
	_paper(true)
	var ui := _shell()
	_ruby(ui.body, "じゅんび", 34)
	var today := SaveGame.tokyo_day(int(Time.get_unix_time_from_system()))
	var weekday := _tokyo_weekday(int(Time.get_unix_time_from_system()))
	_plain(ui.body, today + "  " + weekday)
	for row in pack.profile.get("schedule", []):
		if str(row.get("weekday", "")) != weekday and not _is_next(str(row.get("weekday", "")), weekday):
			continue
		_ruby(ui.body, "%s  %s–%s  %s" % [row["weekday"], row["start"], row["end"], row.get("focus_ruby", "")], 22)
		_en(ui.body, str(row.get("focus_en", "")))
	_ruby(ui.body, str(pack.profile["commute"]["summary_ruby"]), 20)
	_en(ui.body, str(pack.profile["commute"]["summary_en"]))
	_ruby(ui.body, "また みる ところ", 24)
	for spot in pack.profile.get("weak_spots", []):
		_ruby(ui.body, str(spot.get("title_ruby", "")), 20)
		_ruby(ui.body, str(spot.get("reminder_ja", "")), 16)
		_en(ui.body, str(spot.get("reminder_en", "")))
	_ruby(ui.body, "Anki の デッキは パックに のこしてある。よみとりは つぎの だんかい。", 18)
	_button(ui.dock, "いえ", _show_home)

func _tokyo_weekday(unix: int) -> String:
	var shifted: Dictionary = Time.get_datetime_dict_from_unix_time(unix + 9 * 3600)
	return ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"][int(shifted.weekday)]

func _is_next(row_day: String, today: String) -> bool:
	var order := ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]
	var from := order.find(today)
	var to := order.find(row_day)
	if from < 0 or to < 0:
		return false
	var gap := (to - from + 7) % 7
	return gap > 0 and gap <= 3

func _show_credits() -> void:
	screen = "credits"
	_paper(true)
	var ui := _shell()
	_ruby(ui.body, "おんがく", 34)
	for track in MusicCredits.TRACKS:
		_ruby(ui.body, str(track["use_ruby"]), 24)
		_plain(ui.body, "%s — %s" % [track["title"], track["author"]])
		_plain(ui.body, str(track["license"]))
		_en(ui.body, str(track["source"]))
	_ruby(ui.body, "フォント", 24)
	_plain(ui.body, "Noto Sans JP — SIL Open Font License")
	_en(ui.body, "https://github.com/notofonts/noto-cjk")
	_button(ui.dock, "きろく", _show_sheet)
	_button(ui.dock, "いえ", _show_home)

func _apply_shot(which: String) -> void:
	state = SaveGame.fresh(pack.id)
	_ensure_save_shape()
	state["xp"] = 45
	state["streak"] = 3
	state["gems"] = 11
	state["region"] = 2
	if which == "battle":
		state["base"]["wis"] = 3
		var verb := pack.verb_by_id("kaku")
		var rng := Rng.new(17)
		state["run"] = {
			"verb_id": "kaku",
			"form": "te",
			"choices": Conjugation.choices(verb, "te", rng),
			"answer": Conjugation.kana(verb, "te"),
			"enemy_hp": 24,
			"enemy_max": 24,
			"player_hp": 14,
			"player_max": 14,
			"boss": false,
			"left": 16,
			"seconds": 16,
			"subtitle_ruby": "",
			"subtitle_en": "",
			"resolved": false,
			"loot_ruby": "",
		}
		_show_battle()
	elif which == "sheet":
		_show_sheet()
	elif which == "loot":
		var loot := GearRules.roll(17, "kaku", 2, 4, true)
		state["inventory"] = [loot]
		state["pouch"] = [GearRules.roll(19, "iku", 1, 2, true)]
		state["unbanked_gems"] = 3
		_show_loot()
	elif which == "credits":
		_show_credits()
	else:
		_show_overworld()
	_play_music(int(state.get("region", 0)))

func _capture_shot() -> void:
	await get_tree().process_frame
	await get_tree().process_frame
	await get_tree().create_timer(0.45).timeout
	var image := get_viewport().get_texture().get_image()
	var path := OS.get_environment("YORUBEN_SHOT_PATH")
	if path != "":
		image.save_png(path)
	get_tree().quit()
