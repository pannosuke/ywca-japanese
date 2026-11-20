extends Node

const SAVE_PATH := "user://yoruben-v4.json"
const INK := Color("241c16")
const PAPER := Color("f6ead2")
const LANTERN := Color("e39a3c")
const MOSS := Color("2f5d40")

var pack: ContentPack
var font: Font
var state: Dictionary = {}
var shot := ""
var stage: Control
var column: VBoxContainer
var backdrop: Backdrop
var flash: ColorRect
var music: AudioStreamPlayer
var sfx: AudioStreamPlayer
var music_id := ""
var correct_sfx: AudioStream
var wrong_sfx: AudioStream
var pending: Dictionary = {}
var built := ""
var guide: Dictionary = {}
var fly_from := Vector2.INF
var fly_piece := ""
var speed_left := 0.0
var speed_running := false
var speed_label: Label
var reading: Dictionary = {}

func _ready() -> void:
	pack = ContentPack.load_default()
	Furigana.prepare(pack)
	font = load("res://assets/fonts/NotoSansJP-Regular.otf")
	correct_sfx = load("res://assets/sfx/correct.wav")
	wrong_sfx = load("res://assets/sfx/wrong.wav")
	shot = OS.get_environment("YORUBEN_SHOT")
	_chrome()
	_load()
	_bind_known()
	if shot != "":
		state["music"] = 0.0
		_prepare_shot()
		_capture()
	else:
		_show_title()

func _process(delta: float) -> void:
	if not speed_running:
		return
	speed_left -= delta
	if speed_label != null:
		speed_label.text = "%d" % ceili(speed_left)
	if speed_left <= 0.0:
		speed_running = false
		_finish_speed()

func _chrome() -> void:
	stage = Control.new()
	stage.set_anchors_preset(Control.PRESET_FULL_RECT)
	stage.theme = Theme.new()
	stage.theme.default_font = font
	stage.theme.default_font_size = 20
	add_child(stage)
	backdrop = Backdrop.new()
	stage.add_child(backdrop)
	var margin := MarginContainer.new()
	margin.set_anchors_preset(Control.PRESET_FULL_RECT)
	margin.add_theme_constant_override("margin_left", 16)
	margin.add_theme_constant_override("margin_right", 16)
	margin.add_theme_constant_override("margin_top", 18)
	margin.add_theme_constant_override("margin_bottom", 16)
	stage.add_child(margin)
	column = VBoxContainer.new()
	column.add_theme_constant_override("separation", 10)
	column.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	column.size_flags_vertical = Control.SIZE_EXPAND_FILL
	margin.add_child(column)
	flash = ColorRect.new()
	flash.set_anchors_preset(Control.PRESET_FULL_RECT)
	flash.mouse_filter = Control.MOUSE_FILTER_IGNORE
	flash.color = Color(1, 1, 1, 0)
	stage.add_child(flash)
	music = AudioStreamPlayer.new()
	add_child(music)
	sfx = AudioStreamPlayer.new()
	add_child(sfx)

func _clear() -> void:
	speed_running = false
	speed_label = null
	for child in column.get_children():
		column.remove_child(child)
		child.free()

func _panel() -> VBoxContainer:
	var frame := PanelContainer.new()
	frame.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	var style := StyleBoxFlat.new()
	style.bg_color = Color("f6ead2")
	style.set_corner_radius_all(18)
	style.set_content_margin_all(12)
	frame.add_theme_stylebox_override("panel", style)
	var box := VBoxContainer.new()
	box.add_theme_constant_override("separation", 6)
	frame.add_child(box)
	column.add_child(frame)
	return box

func _ruby(parent: Node, markup: String, size := 24, color := INK) -> RubyLabel:
	var label := RubyLabel.new()
	label.font = font
	label.font_size = size
	label.color = color
	label.max_width = 500
	label.set_markup(markup)
	parent.add_child(label)
	return label

func _text(parent: Node, value: String, size := 18, color := INK) -> Control:
	var marked := Furigana.apply(value, Furigana.known)
	if marked.find("[") == -1:
		var label := Label.new()
		label.text = marked
		label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
		label.add_theme_font_size_override("font_size", size)
		label.add_theme_color_override("font_color", color)
		parent.add_child(label)
		return label
	return _ruby(parent, marked, size, color)

func _button(parent: Node, markup: String, fill: Color, cb: Callable, height := 72, font_size := 24) -> Button:
	if markup.length() > 48:
		font_size = mini(font_size, 16)
		height = maxi(height, 120)
	var button := Button.new()
	button.text = ""
	button.custom_minimum_size = Vector2(0, height)
	button.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	var style := StyleBoxFlat.new()
	style.bg_color = fill
	style.set_corner_radius_all(16)
	style.set_content_margin_all(8)
	button.add_theme_stylebox_override("normal", style)
	button.add_theme_stylebox_override("hover", style)
	button.add_theme_stylebox_override("pressed", style)
	button.add_theme_stylebox_override("disabled", style)
	var center := CenterContainer.new()
	center.mouse_filter = Control.MOUSE_FILTER_IGNORE
	center.set_anchors_preset(Control.PRESET_FULL_RECT)
	var label := RubyLabel.new()
	label.font = font
	label.font_size = font_size
	label.color = INK if fill == LANTERN else Color("f7efe0")
	label.align_center = true
	label.max_width = 460
	label.mouse_filter = Control.MOUSE_FILTER_IGNORE
	label.set_markup(markup)
	center.add_child(label)
	button.add_child(center)
	button.pressed.connect(cb)
	parent.add_child(button)
	return button

func _show_title() -> void:
	_clear()
	backdrop.set_mood("night")
	_play_music(2)
	var box := _panel()
	_ruby(box, "よるべん", 52)
	_ruby(box, "よるの べんきょう", 22)
	_text(box, "カードで、灯りの もりを あるく。", 18, Color("5c5146"))
	if not bool(state.get("tutorial_done", false)):
		_text(box, "さきに あそびかた。カードを タップして、もじを ならべる。", 16, Color("8a4b12"))
		_button(column, "あそびかた", LANTERN, func() -> void: _begin_guide(true), 88, 28)
		_button(column, "出発[しゅっぱつ]", Color("6a3b12"), _depart, 80, 28)
	else:
		_button(column, "出発[しゅっぱつ]", LANTERN, _depart, 88, 30)
		_button(column, "あそびかた", MOSS, func() -> void: _begin_guide(true), 64, 20)
	var track := MusicCredits.for_region(2)
	_text(column, "%s · %s · %s" % [track["title"], track["author"], track["license"]], 13, Color("f0e2c8"))

func _depart() -> void:
	if not bool(state.get("tutorial_done", false)) and pack.unseen_packs(state["known_packs"]).is_empty():
		pass
	_start_run(int(Time.get_ticks_msec() % 90000) + 11)
	_show_map()

func _show_map() -> void:
	_clear()
	backdrop.set_mood("night")
	_play_music(0)
	var run: Dictionary = state["run"]
	var head := _panel()
	_ruby(head, "第[だい]1幕[まく]  灯[あか]りの 森[もり]", 26)
	_text(head, "HP %d    ほたる玉 %d" % [int(run["hp"]), int(state["gems"])], 18)
	if not bool(state.get("tutorial_done", false)) and (run["path"] as Array).is_empty():
		_ruby(head, "ひかっている ばしょを タップ。", 20, Color("8a4b12"))
		_button(head, "スキップ", Color("5c5146"), _skip_tutorial)
	var view := MapView.new()
	view.chosen.connect(_enter)
	view.bind(run["map"], run["path"], _open_ids(), font)
	column.add_child(view)
	var legend := _panel()
	_text(legend, "せん 戦う    つよ 強い    わけ 分ける", 16)
	_text(legend, "はや 速い    やす 休む    みせ 店", 16)
	_text(legend, "よみ 読む    ぬし 主", 16)
	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", 8)
	column.add_child(row)
	_button(row, "ずかん", MOSS, _show_collection)
	if _pending_sheet().size() > 0:
		_button(row, "宿題[しゅくだい]", LANTERN, _show_homework)

func _skip_tutorial() -> void:
	state["tutorial_done"] = true
	_save()
	_show_map()

func _open_ids() -> Array:
	var run: Dictionary = state["run"]
	var path: Array = run["path"]
	if path.is_empty():
		var ids: Array = []
		for node in run["map"]["rows"][0]:
			ids.append(str(node["id"]))
		return ids
	var node := MapGen.node_by_id(run["map"], str(path[path.size() - 1]))
	return node["next"]

func _enter(node_id: String) -> void:
	var run: Dictionary = state["run"]
	var node := MapGen.node_by_id(run["map"], node_id)
	var kind := str(node["type"])
	if kind in ["battle", "elite", "boss"] and not bool(state.get("tutorial_done", false)):
		state["guide_node"] = node_id
		_begin_guide(false)
		return
	var path: Array = run["path"]
	path.append(node_id)
	if kind in ["battle", "elite", "boss"]:
		_begin_fight(node)
	elif kind == "puzzle":
		_begin_puzzle()
	elif kind == "speed":
		_begin_speed()
	elif kind == "rest":
		_show_rest()
	elif kind == "shop":
		_show_shop()
	elif kind == "event":
		_show_event()
	elif kind == "treasure":
		_grant_treasure()
	else:
		_begin_reading(_story_yokai())

func _begin_fight(node: Dictionary) -> void:
	var run: Dictionary = state["run"]
	var rng := Rng.new(int(run["seed"]) + (run["path"] as Array).size() * 13)
	var kind := str(node["type"])
	var yokai := pack.yokai_by_id("ibaraki") if kind == "boss" else YokaiRules.for_node(kind, rng.next_index(20), pack.yokai)
	var forms: Array = ["masu", "mashita", "masen", "te", "nai"]
	var deck_state := CombatRules.open(run["deck"], rng)
	run["block"] = 0
	run["battle"] = {
		"node_id": node["id"],
		"node_type": kind,
		"yokai_id": yokai["id"],
		"form": forms[rng.next_index(forms.size())],
		"phase": 1,
		"enemy_hp": CombatRules.enemy_hp(kind),
		"enemy_max": CombatRules.enemy_hp(kind),
		"deck_state": deck_state,
		"plays": [],
		"combo": 0,
		"forgiven": false,
		"turn": 1,
		"dropped": false,
	}
	pending = {}
	_show_battle()

func _begin_guide(from_title: bool) -> void:
	var verb := pack.verb_by_id("miru")
	var answer := Conjugation.kana(verb, "masu")
	guide = {
		"from_title": from_title,
		"phase": "pick",
		"form": "masu",
		"built": "",
		"tiles": ChallengeRules.tiles(answer, Rng.new(4)),
		"notice": "",
		"pulse": true,
	}
	_show_guide()

func _show_guide() -> void:
	_clear()
	backdrop.set_mood("battle")
	_play_music(1)
	var phase := str(guide["phase"])
	var yokai := pack.yokai_by_id("yuki")
	var top := _panel()
	_ruby(top, str(yokai["ruby"]), 26)
	_text(top, "つぎの こうげき %d" % CombatRules.enemy_attack("battle"), 16, Color("8a4b12"))
	_text(top, "HP %d    れんしゅう" % CombatRules.max_hp(), 16)
	var step := 1
	if phase == "spell":
		step = 3 if str(guide["built"]).length() == Conjugation.kana(pack.verb_by_id("miru"), "masu").length() else 2
	elif phase == "end" or phase == "done":
		step = 3
	_step_hints(top, step)
	if str(guide.get("notice", "")) != "":
		_text(top, str(guide["notice"]), 16, Color("8a2a1a"))
	if phase == "pick":
		_ruby(top, "この カードを タップ。", 18, Color("8a4b12"))
		var row := HBoxContainer.new()
		row.alignment = BoxContainer.ALIGNMENT_CENTER
		column.add_child(row)
		row.add_child(_guide_card())
	elif phase == "spell":
		var bank: Array = guide["tiles"]
		var pad := _make_pad(pack.verb_by_id("miru"), "masu", str(guide["built"]), bank, bool(guide["pulse"]))
		pad.typed.connect(_guide_type)
		pad.erased.connect(_guide_erase)
		pad.submitted.connect(_guide_submit)
		column.add_child(pad)
		guide["pulse"] = false
	elif phase == "end":
		var dealt := CombatRules.strike_damage(true, "common", [], "ichidan")
		_text(column, "こうげき %d が あたった。" % dealt, 18)
		_text(column, "ターンを おわる と、あいてが %d こうげきする。" % CombatRules.enemy_attack("battle"), 16, Color("8a4b12"))
		var gap := Control.new()
		gap.size_flags_vertical = Control.SIZE_EXPAND_FILL
		column.add_child(gap)
		_button(column, "ターンを おわる", Color("6a3b12"), _guide_end, 88, 26)
	else:
		_text(column, "HP が %d へった。ぼうぎょが あると、へりかたが すくない。" % CombatRules.enemy_attack("battle"), 18)
		_text(column, "わかった。タイトルの あそびかた で、もう いちど。", 16)
		_button(column, "わかった", LANTERN, _finish_guide, 80, 24)
	if phase != "done":
		_button(column, "スキップ", Color("5c5146"), _skip_guide, 52, 16)

func _guide_card() -> Control:
	var card := pack.card_by_id("verb:miru")
	var button := Button.new()
	button.flat = true
	button.custom_minimum_size = Vector2(150, 200)
	var face := CardFace.new()
	face.mouse_filter = Control.MOUSE_FILTER_IGNORE
	face.present(card, "common", font, true)
	face.set_playable(true)
	button.add_child(face)
	button.pressed.connect(func() -> void:
		guide["phase"] = "spell"
		guide["notice"] = ""
		_show_guide()
	)
	return button

func _guide_type(piece: String, origin: Vector2) -> void:
	var bank: Array = guide["tiles"]
	if bank.find(piece) < 0:
		return
	guide["tiles"] = ChallengeRules.take(bank, piece)
	guide["built"] = str(guide["built"]) + piece
	guide["notice"] = ""
	guide["pulse"] = false
	fly_from = origin
	fly_piece = piece
	_show_guide()

func _guide_erase() -> void:
	var text := str(guide["built"])
	if text.length() == 0:
		return
	var piece := text.substr(text.length() - 1, 1)
	guide["built"] = text.substr(0, text.length() - 1)
	guide["tiles"] = ChallengeRules.untake(guide["tiles"], piece)
	guide["notice"] = ""
	_show_guide()

func _guide_submit(answer: String) -> void:
	var verb := pack.verb_by_id("miru")
	var expected := Conjugation.kana(verb, "masu")
	if answer.length() < expected.length():
		guide["notice"] = "はこを ぜんぶ うめて。"
		_show_guide()
		return
	if not ChallengeRules.accepts(answer, verb, "masu"):
		guide["notice"] = "じゅんばんが ちがう。けすで もどせる。"
		_shake()
		_show_guide()
		return
	guide["phase"] = "end"
	guide["notice"] = ""
	_show_guide()

func _guide_end() -> void:
	guide["phase"] = "done"
	guide["notice"] = ""
	_show_guide()

func _finish_guide() -> void:
	var from_title := bool(guide.get("from_title", true))
	var node_id := str(state.get("guide_node", ""))
	guide = {}
	state["tutorial_done"] = true
	state["guide_node"] = ""
	if not from_title and node_id != "" and state.get("run") != null:
		state["run"]["path"].append(node_id)
		_save()
		_show_map()
		return
	_save()
	_show_title()

func _skip_guide() -> void:
	var from_title := bool(guide.get("from_title", true))
	guide = {}
	state["tutorial_done"] = true
	state["guide_node"] = ""
	_save()
	if not from_title and state.get("run") != null:
		_show_map()
	else:
		_show_title()

func _step_hints(parent: Node, active: int) -> void:
	var lines := ["1. カードを タップ", "2. もじを タップして つくる", "3. これで答[こた]える"]
	for i in lines.size():
		var here := i + 1 == active
		_ruby(parent, lines[i], 17 if here else 14, Color("8a4b12") if here else Color("5c5146"))

func _make_pad(verb: Dictionary, form: String, text: String, bank: Array, pulse: bool) -> SpellPad:
	var pad := SpellPad.new()
	pad.fly_from = fly_from
	pad.fly_piece = fly_piece
	fly_piece = ""
	fly_from = Vector2.INF
	pad.present(verb, form, text, bank, font, pulse)
	return pad

func _show_battle() -> void:
	_clear()
	backdrop.set_mood("battle")
	_play_music(1)
	var run: Dictionary = state["run"]
	var battle: Dictionary = run["battle"]
	var yokai := pack.yokai_by_id(str(battle["yokai_id"]))
	var top := _panel()
	if pending.is_empty():
		var art := YokaiView.new()
		art.custom_minimum_size = Vector2(140, 96)
		art.show_id(str(yokai["id"]))
		top.add_child(art)
	_ruby(top, str(yokai["ruby"]), 26)
	_text(top, "つぎの こうげき %d    コンボ %d" % [CombatRules.enemy_attack(str(battle["node_type"])), int(battle["combo"])], 16, Color("8a4b12"))
	var bar := ProgressBar.new()
	bar.max_value = int(battle["enemy_max"])
	bar.value = maxi(int(battle["enemy_hp"]), 0)
	bar.show_percentage = false
	bar.custom_minimum_size = Vector2(0, 14)
	top.add_child(bar)
	_text(top, "HP %d / %d    ぼうぎょ %d    ふで %d" % [int(run["hp"]), int(run["max_hp"]), int(run["block"]), int(run["gear"])], 16)
	var step := 1
	if not pending.is_empty() and ChallengeRules.kind(pending) == "spell":
		var verb := pack.verb_by_id(str(pending["verb_id"]))
		var answer := Conjugation.kana(verb, str(battle["form"]))
		step = 3 if built.length() == answer.length() else 2
	elif not pending.is_empty():
		step = 2
	_step_hints(top, step)
	if pending.is_empty():
		_text(top, "いまの かたち：%s" % ChallengeRules.ask_plain(str(battle["form"])), 16)
		_text(top, "ことばの カードは、いみを えらぶ。", 14, Color("5c5146"))
		_ruby(top, "エナジー %d" % int(battle["deck_state"]["energy"]), 18)
		var scroll := ScrollContainer.new()
		scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_AUTO
		scroll.vertical_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
		scroll.custom_minimum_size = Vector2(0, 210)
		var row := HBoxContainer.new()
		row.add_theme_constant_override("separation", 8)
		scroll.add_child(row)
		for card in battle["deck_state"]["hand"]:
			row.add_child(_card_button(card, battle))
		column.add_child(scroll)
		var gap := Control.new()
		gap.size_flags_vertical = Control.SIZE_EXPAND_FILL
		column.add_child(gap)
		_text(column, "おすと、あいてが こうげきする。", 14, Color("f0e2c8"))
		_button(column, "ターンを おわる", Color("6a3b12"), _end_player_turn, 80, 24)
	else:
		_ask_panel(top)

func _card_button(card: Dictionary, battle: Dictionary) -> Control:
	var wrap := VBoxContainer.new()
	var button := Button.new()
	button.flat = true
	button.custom_minimum_size = Vector2(116, 200)
	var face := CardFace.new()
	face.mouse_filter = Control.MOUSE_FILTER_IGNORE
	var playable := DeckRules.can_play(battle["deck_state"], card)
	face.present(card, _rarity_of(str(card["id"])), font, true)
	face.set_playable(playable)
	button.add_child(face)
	button.pressed.connect(_select_card.bind(card))
	wrap.add_child(button)
	if playable:
		var mark := ColorRect.new()
		mark.custom_minimum_size = Vector2(72, 5)
		mark.color = LANTERN
		wrap.add_child(mark)
	if str(card.get("verb_id", "")) in state["weak_verbs"]:
		var tag := Label.new()
		tag.text = "よわい"
		tag.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
		tag.add_theme_color_override("font_color", Color("f0d7a4"))
		wrap.add_child(tag)
	return wrap

func _select_card(card: Dictionary) -> void:
	var battle: Dictionary = state["run"]["battle"]
	if not DeckRules.can_play(battle["deck_state"], card):
		_shake()
		return
	pending = card
	built = ""
	battle.erase("tiles")
	_show_battle()

func _ask_panel(parent: Node) -> void:
	var card: Dictionary = pending
	var battle: Dictionary = state["run"]["battle"]
	if ChallengeRules.kind(card) == "spell":
		var verb := pack.verb_by_id(str(card["verb_id"]))
		var form := str(battle["form"])
		var answer := Conjugation.kana(verb, form)
		if not battle.has("tiles"):
			battle["tiles"] = ChallengeRules.tiles(answer, Rng.new(answer.length() + int(battle["turn"])))
		var pulse := not bool(battle.get("tiles_seen", false))
		battle["tiles_seen"] = true
		var bank: Array = battle["tiles"]
		var pad := _make_pad(verb, form, built, bank, pulse)
		pad.typed.connect(_type_tile)
		pad.erased.connect(_erase_tile)
		pad.submitted.connect(_answer_verb)
		column.add_child(pad)
		_button(column, "もどる", Color("5c5146"), _cancel_card, 52, 16)
		return
	if str(card["type"]) == "grammar":
		_ruby(parent, str(card.get("cloze_ruby", card["ruby"])), 20)
		for choice in _grammar_choices(card):
			_button(column, str(choice), MOSS, _answer_text.bind(str(choice), str(card.get("cloze_answer", ""))), 88, 16)
	else:
		_ruby(parent, str(card["ruby"]) + " の いみは？", 20)
		_text(parent, "この カードは もじを ならべない。いみを タップ。", 15, Color("8a4b12"))
		for choice in _meaning_choices(card):
			_button(column, str(choice), MOSS, _answer_text.bind(str(choice), str(card["gloss_ja"])), 110, 16)
	_button(column, "もどる", Color("5c5146"), _cancel_card, 52, 16)

func _type_tile(piece: String, origin: Vector2) -> void:
	var battle: Dictionary = state["run"]["battle"]
	var bank: Array = battle["tiles"]
	if bank.find(piece) < 0:
		return
	battle["tiles"] = ChallengeRules.take(bank, piece)
	built += piece
	fly_from = origin
	fly_piece = piece
	_show_battle()

func _erase_tile() -> void:
	if built.length() == 0:
		return
	var battle: Dictionary = state["run"]["battle"]
	var piece := built.substr(built.length() - 1, 1)
	built = built.substr(0, built.length() - 1)
	battle["tiles"] = ChallengeRules.untake(battle["tiles"], piece)
	_show_battle()

func _cancel_card() -> void:
	pending = {}
	built = ""
	var battle: Dictionary = state["run"]["battle"]
	battle.erase("tiles")
	_show_battle()

func _answer_verb(choice: String) -> void:
	var card: Dictionary = pending
	var verb := pack.verb_by_id(str(card["verb_id"]))
	var battle: Dictionary = state["run"]["battle"]
	var expected := Conjugation.kana(verb, str(battle["form"]))
	if choice.length() < expected.length():
		_shake()
		return
	_resolve(ChallengeRules.accepts(choice, verb, str(battle["form"])))

func _answer_text(choice: String, expected: String) -> void:
	_resolve(ChallengeRules.normalize(choice) == ChallengeRules.normalize(expected))

func _resolve(correct: bool) -> void:
	var card: Dictionary = pending
	var run: Dictionary = state["run"]
	var battle: Dictionary = run["battle"]
	var rarity := _rarity_of(str(card["id"]))
	var relics: Array = run["relics"]
	var forgiven: bool = "lantern" in relics and not bool(battle["forgiven"])
	var hit: Dictionary = CombatRules.resolve(correct, card, rarity, relics, forgiven)
	if bool(hit["forgave"]):
		battle["forgiven"] = true
	state["mastery"][str(card["id"])] = RarityRules.record(_mastery(str(card["id"])), correct, 2200)
	var dealt := int(hit["damage"])
	if str(card["effect"]) == "strike" and int(run["gear"]) <= 0 and dealt > 0:
		dealt = maxi(1, dealt - 1)
	battle["enemy_hp"] = int(battle["enemy_hp"]) - dealt
	run["block"] = int(run["block"]) + int(hit["block"])
	if int(hit["draw"]) > 0 and not (battle["deck_state"]["draw"] as Array).is_empty():
		battle["deck_state"]["hand"].append(battle["deck_state"]["draw"].pop_back())
	var plays: Array = battle["plays"]
	plays.append({"correct": correct, "rarity": rarity})
	if correct:
		battle["combo"] = int(battle["combo"]) + 1
		_burst("-%d" % dealt if dealt > 0 else "よし", Color("ffd27a"), true)
	else:
		battle["combo"] = 0
		run["gear"] = maxi(0, int(run["gear"]) - 1)
		_burst("よわい", Color("f0c2b8"), false)
	DeckRules.spend(battle["deck_state"], card)
	pending = {}
	built = ""
	battle.erase("tiles")
	if int(battle["enemy_hp"]) <= 0:
		_win_fight()
		return
	if int(battle["deck_state"]["energy"]) <= 0:
		_enemy_turn()
	else:
		_show_battle()

func _end_player_turn() -> void:
	_enemy_turn()

func _enemy_turn() -> void:
	var run: Dictionary = state["run"]
	var battle: Dictionary = run["battle"]
	var attack := CombatRules.enemy_attack(str(battle["node_type"]))
	var next_hp := CombatRules.player_hit(int(run["hp"]), attack, int(run["block"]))
	run["hp"] = next_hp
	run["block"] = 0
	_shake()
	_burst("-%d" % attack, Color("e7b1a8"), false)
	if next_hp <= 0:
		_defeat()
		return
	battle["turn"] = int(battle["turn"]) + 1
	if str(battle["node_type"]) == "boss":
		var ratio := float(battle["enemy_hp"]) / float(battle["enemy_max"])
		if ratio < 0.66 and str(battle["form"]) == "masu":
			battle["form"] = "te"
			battle["phase"] = 2
		elif ratio < 0.33:
			battle["form"] = "nai"
			battle["phase"] = 3
	DeckRules.end_turn(battle["deck_state"])
	DeckRules.start_turn(battle["deck_state"], Rng.new(int(run["seed"]) + int(battle["turn"]) * 9))
	_show_battle()

func _win_fight() -> void:
	var run: Dictionary = state["run"]
	var battle: Dictionary = run["battle"]
	var plays: Array = battle["plays"]
	var correct := 0
	for play in plays:
		if bool(play["correct"]):
			correct += 1
	var accuracy := 1.0 if plays.is_empty() else float(correct) / float(plays.size())
	var chance := YokaiRules.drop_chance(str(battle["node_type"]), accuracy)
	if YokaiRules.dropped(Rng.new(int(run["seed"]) + plays.size() + 5), chance):
		battle["dropped"] = true
		if str(battle["yokai_id"]) not in state["yokai_found"]:
			state["yokai_found"].append(str(battle["yokai_id"]))
	run["last"] = battle
	run["battle"] = null
	_show_reward()

func _show_reward() -> void:
	_clear()
	var run: Dictionary = state["run"]
	var last: Dictionary = run["last"]
	var plays: Array = last["plays"]
	var correct := 0
	for play in plays:
		if bool(play["correct"]):
			correct += 1
	var accuracy := 1.0 if plays.is_empty() else float(correct) / float(plays.size())
	var box := _panel()
	_ruby(box, "ごほうびを ひとつ", 28)
	if not bool(state.get("tutorial_done", false)):
		_text(box, "3まいから ひとつ。この あと たまの ないわけが 出る。", 16, Color("8a4b12"))
	var offer: Array = RewardRules.offer(Rng.new(int(run["seed"]) + plays.size()), str(last["node_type"]), accuracy, run["deck"], run["relics"])
	for pick in offer:
		var label := str(pick["ruby"])
		if str(pick["kind"]) == "gems":
			label = "ほたる玉[だま] +%d" % int(pick["amount"])
		_button(column, label, LANTERN, _take_reward.bind(pick))

func _take_reward(pick: Dictionary) -> void:
	var run: Dictionary = state["run"]
	if str(pick["kind"]) == "card":
		var card := pack.card_by_id(str(pick["id"]))
		if not card.is_empty():
			run["deck"].append(card)
	elif str(pick["kind"]) == "relic":
		if str(pick["id"]) not in run["relics"]:
			run["relics"].append(str(pick["id"]))
	else:
		state["gems"] = int(state["gems"]) + int(pick.get("amount", 2))
	state["tutorial_done"] = true
	_show_payout()

func _show_payout() -> void:
	_clear()
	var run: Dictionary = state["run"]
	var last: Dictionary = run["last"]
	var pay := PayoutRules.battle(PayoutRules.base_for(str(last["node_type"])), last["plays"], int(last["combo"]))
	var extra := 0
	if "firefly" in run["relics"]:
		for play in last["plays"]:
			if bool(play["correct"]):
				extra += 1
	var total := int(pay["total"]) + extra
	state["gems"] = int(state["gems"]) + total
	var box := _panel()
	_ruby(box, "ほたる玉[だま]", 32)
	_text(box, "きほん  %d" % int(pay["base"]), 22)
	_text(box, "カードの めずらしさ  %d" % int(pay["rarity"]), 22)
	_text(box, "コンボ  %d" % int(pay["combo"]), 22)
	if extra > 0:
		_text(box, "ほたるの お守り  %d" % extra, 22)
	_ruby(box, "あわせて  %d" % total, 30)
	if bool(last.get("dropped", false)):
		var yokai := pack.yokai_by_id(str(last["yokai_id"]))
		_ruby(box, str(yokai["ruby"]) + " の カードを 手[て]に いれた。", 20)
	_save()
	_button(column, "つづける", LANTERN, _after_payout)

func _after_payout() -> void:
	var fresh: Array = pack.unseen_packs(state["known_packs"])
	if fresh.is_empty():
		_show_map()
		return
	_show_booster(fresh[fresh.size() - 1])

func _show_booster(pack_file: Dictionary) -> void:
	_clear()
	var box := _panel()
	_ruby(box, "クラスの あたらしい カード", 28)
	_ruby(box, str(pack_file.get("topic_ruby", "")), 22)
	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", 8)
	var shown := 0
	for card in pack_file["cards"]:
		if shown >= 3:
			break
		if str(card["type"]) != "verb" and shown < 2:
			continue
		var face := CardFace.new()
		face.present(card, "common", font, true)
		row.add_child(face)
		shown += 1
	column.add_child(row)
	for entry in pack.packs:
		if str(entry["id"]) not in state["known_packs"]:
			state["known_packs"].append(str(entry["id"]))
	_save()
	_button(column, "ひらく", LANTERN, _show_map)

func _defeat() -> void:
	_clear()
	state["run"] = null
	_save()
	var box := _panel()
	_ruby(box, "たおれた", 36)
	_text(box, "おぼえた カードと ほたる玉は のこる。", 18)
	_button(column, "タイトルへ", LANTERN, _show_title)

func _begin_puzzle() -> void:
	var run: Dictionary = state["run"]
	run["puzzle_left"] = 3
	run["puzzle_correct"] = 0
	run["puzzle_verb"] = _next_verb()
	_show_puzzle()

func _show_puzzle() -> void:
	_clear()
	var run: Dictionary = state["run"]
	var verb: Dictionary = run["puzzle_verb"]
	var box := _panel()
	_ruby(box, "どの グループ？", 28)
	_ruby(box, str(verb["dict_ruby"]), 32)
	_text(box, "グループ1 五段    グループ2 一段    グループ3 する・くる", 14, Color("5c5146"))
	_button(column, "グループ1", MOSS, _puzzle_pick.bind("godan"))
	_button(column, "グループ2", MOSS, _puzzle_pick.bind("ichidan"))
	_button(column, "グループ3", MOSS, _puzzle_pick.bind("irregular"))

func _puzzle_pick(group_name: String) -> void:
	var run: Dictionary = state["run"]
	var verb: Dictionary = run["puzzle_verb"]
	var ok: bool = str(verb["group"]) == group_name
	run["puzzle_correct"] = int(run["puzzle_correct"]) + (1 if ok else 0)
	run["puzzle_left"] = int(run["puzzle_left"]) - 1
	_burst("せいかい" if ok else "ちがい", Color("ffd27a") if ok else Color("f0c2b8"), ok)
	if int(run["puzzle_left"]) <= 0:
		_finish_count(int(run["puzzle_correct"]), 3, "puzzle")
	else:
		run["puzzle_verb"] = _next_verb()
		_show_puzzle()

func _begin_speed() -> void:
	var run: Dictionary = state["run"]
	run["speed_correct"] = 0
	run["speed_asked"] = 0
	run["speed_combo"] = 0
	speed_left = float(RewardRules.speed_seconds(run["relics"]))
	speed_running = true
	_show_speed()

func _show_speed() -> void:
	_clear()
	speed_running = true
	var run: Dictionary = state["run"]
	var verb := _next_verb()
	run["speed_verb"] = verb
	var form := "masu"
	run["speed_form"] = form
	var box := _panel()
	_ruby(box, "はやく", 28)
	speed_label = _text(box, "%d" % ceili(speed_left), 36)
	_ruby(box, Conjugation.prompt(verb, form), 22)
	var rng := Rng.new(int(run["speed_asked"]) + 4)
	for choice in Conjugation.choices(verb, form, rng):
		_button(column, str(choice), MOSS, _speed_pick.bind(str(choice)))

func _speed_pick(choice: String) -> void:
	if not speed_running:
		return
	var run: Dictionary = state["run"]
	var verb: Dictionary = run["speed_verb"]
	var ok: bool = ChallengeRules.accepts(choice, verb, str(run["speed_form"]))
	run["speed_asked"] = int(run["speed_asked"]) + 1
	if ok:
		run["speed_correct"] = int(run["speed_correct"]) + 1
		run["speed_combo"] = int(run["speed_combo"]) + 1
	else:
		run["speed_combo"] = 0
	_show_speed()

func _finish_speed() -> void:
	speed_running = false
	var run: Dictionary = state["run"]
	_finish_count(int(run.get("speed_correct", 0)), maxi(int(run.get("speed_asked", 1)), 1), "speed")

func _finish_count(correct: int, total: int, node_type: String) -> void:
	var plays: Array = []
	for i in correct:
		plays.append({"correct": true, "rarity": "common"})
	for i in maxi(total - correct, 0):
		plays.append({"correct": false, "rarity": "common"})
	state["run"]["last"] = {
		"node_type": node_type,
		"plays": plays,
		"combo": correct,
		"dropped": false,
		"yokai_id": "",
	}
	_show_reward()

func _show_rest() -> void:
	_clear()
	var box := _panel()
	_ruby(box, "焚[た]き火[び]", 32)
	_text(box, "休むか、よわい ことばを 見なおす。", 18)
	_button(column, "休[やす]む  HP+10", LANTERN, _rest_heal)
	_button(column, "べんきょうする", MOSS, _rest_study)
	if _pending_sheet().size() > 0:
		_button(column, "宿題[しゅくだい]を 出[だ]す", LANTERN, _show_homework)

func _rest_heal() -> void:
	var run: Dictionary = state["run"]
	run["hp"] = mini(int(run["max_hp"]), int(run["hp"]) + 10)
	_back_to_map()

func _rest_study() -> void:
	var run: Dictionary = state["run"]
	if not (run["deck"] as Array).is_empty():
		var card: Dictionary = run["deck"][0]
		for i in 2:
			state["mastery"][str(card["id"])] = RarityRules.record(_mastery(str(card["id"])), true, 2000)
	_back_to_map()

func _show_shop() -> void:
	_clear()
	var box := _panel()
	_ruby(box, "ほたるの みせ", 32)
	_text(box, "ほたる玉 %d" % int(state["gems"]), 18)
	_button(column, "休[やす]む  6", LANTERN, _buy_heal)
	_button(column, "ふでを なおす  4", MOSS, _buy_repair)
	_button(column, "よるの そで  10", Color("6b4c7a"), _buy_sleeve)
	_button(column, "でる", Color("5c5146"), _back_to_map)

func _buy_heal() -> void:
	if int(state["gems"]) < 6:
		_shake()
		return
	state["gems"] = int(state["gems"]) - 6
	var run: Dictionary = state["run"]
	run["hp"] = mini(int(run["max_hp"]), int(run["hp"]) + 8)
	_save()
	_show_shop()

func _buy_repair() -> void:
	if int(state["gems"]) < 4:
		_shake()
		return
	state["gems"] = int(state["gems"]) - 4
	state["run"]["gear"] = 6
	_save()
	_show_shop()

func _buy_sleeve() -> void:
	if int(state["gems"]) < 10:
		_shake()
		return
	state["gems"] = int(state["gems"]) - 10
	state["card_back"] = "night-sleeve"
	if "night-sleeve" not in state["cosmetics"]:
		state["cosmetics"].append("night-sleeve")
	_save()
	_show_shop()

func _show_event() -> void:
	_clear()
	var box := _panel()
	_ruby(box, "みちばたの 灯[あか]り", 28)
	_text(box, "小さな 灯りの そばに、ぬれた ふでが おちている。", 18)
	_button(column, "ひろう", LANTERN, _event_take)
	_button(column, "そのまま あるく", MOSS, _event_leave)

func _event_take() -> void:
	state["run"]["hp"] = maxi(1, int(state["run"]["hp"]) - 4)
	if "lantern" not in state["run"]["relics"]:
		state["run"]["relics"].append("lantern")
	_back_to_map()

func _event_leave() -> void:
	state["gems"] = int(state["gems"]) + 4
	_back_to_map()

func _grant_treasure() -> void:
	var run: Dictionary = state["run"]
	for relic in RewardRules.RELICS:
		if str(relic["id"]) not in run["relics"]:
			run["relics"].append(str(relic["id"]))
			break
	run["last"] = {"node_type": "treasure", "plays": [], "combo": 0, "dropped": false, "yokai_id": ""}
	_show_payout()

func _begin_reading(yokai: Dictionary) -> void:
	var level := 1 if int(state["gems"]) >= 30 else 0
	var story: Dictionary = YokaiRules.story_for(yokai, level)
	reading = {"yokai": yokai, "story": story, "index": 0, "correct": 0, "show_en": false, "gloss": ""}
	_show_reading()

func _show_reading() -> void:
	_clear()
	var yokai: Dictionary = reading["yokai"]
	var story: Dictionary = reading["story"]
	var box := _panel()
	_ruby(box, "語[かた]り部[べ]  " + str(yokai["ruby"]), 26)
	for line in str(story["text_ruby"]).split("。"):
		if line == "":
			continue
		_ruby(box, line + "。", 18)
	var glosses: Array = story["glosses"]
	if glosses.size() > 0:
		var row := HBoxContainer.new()
		row.add_theme_constant_override("separation", 6)
		box.add_child(row)
		for gloss in glosses:
			var button := Button.new()
			button.text = str(gloss["word"])
			button.pressed.connect(_show_gloss.bind(str(gloss["ruby"])))
			row.add_child(button)
	if str(reading.get("gloss", "")) != "":
		_text(box, str(reading["gloss"]), 16, Color("5c5146"))
	if not bool(reading["show_en"]):
		_button(box, "えいご", Color("8a8175"), _toggle_en)
	else:
		_text(box, str(yokai["en"]), 16, Color("5c5146"))
	var questions: Array = story["questions"]
	var index := int(reading["index"])
	if index >= questions.size():
		_finish_count(int(reading["correct"]), questions.size(), "story")
		return
	var question: Dictionary = questions[index]
	_ruby(box, str(question["prompt_ruby"]), 22)
	for choice in question["choices"]:
		_button(column, str(choice["ruby"]), MOSS, _read_pick.bind(bool(choice["correct"])))

func _show_gloss(text: String) -> void:
	reading["gloss"] = text
	_show_reading()

func _toggle_en() -> void:
	reading["show_en"] = true
	_show_reading()

func _read_pick(correct: bool) -> void:
	if correct:
		reading["correct"] = int(reading["correct"]) + 1
		if not (state["run"]["deck"] as Array).is_empty():
			var card: Dictionary = state["run"]["deck"][0]
			state["mastery"][str(card["id"])] = RarityRules.record(_mastery(str(card["id"])), true, 2500)
	reading["index"] = int(reading["index"]) + 1
	reading["gloss"] = ""
	_show_reading()

func _show_collection() -> void:
	_clear()
	var box := _panel()
	_ruby(box, "ずかん", 32)
	_text(box, "うら: %s" % _back_name(str(state.get("card_back", "paper"))), 16, Color("5c5146"))
	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", 8)
	column.add_child(row)
	var shown := 0
	for card in pack.cards:
		if shown >= 4:
			break
		if str(card["type"]) != "verb":
			continue
		var face := CardFace.new()
		face.present(card, _rarity_of(str(card["id"])), font, true)
		row.add_child(face)
		shown += 1
	_ruby(box, "ようかい", 24)
	var found: Array = state["yokai_found"]
	if found.is_empty():
		_text(box, "まだ いない。たたかいに 勝つと、たまに カードが おちる。", 16)
	else:
		for yokai_id in found:
			var yokai := pack.yokai_by_id(str(yokai_id))
			_button(column, str(yokai["ruby"]), MOSS, _open_yokai.bind(yokai))
	_button(column, "もどる", Color("5c5146"), _back_from_collection)

func _open_yokai(yokai: Dictionary) -> void:
	_clear()
	var box := _panel()
	var art := YokaiView.new()
	art.custom_minimum_size = Vector2(220, 180)
	art.show_id(str(yokai["id"]))
	box.add_child(art)
	_ruby(box, str(yokai["ruby"]), 32)
	for line in str(yokai["fact_ruby"]).split("。"):
		if line != "":
			_ruby(box, line + "。", 18)
	_button(column, "はなしを よむ", LANTERN, _begin_reading.bind(yokai))
	_button(column, "もどる", Color("5c5146"), _show_collection)

func _show_homework() -> void:
	_clear()
	var sheet := _pending_sheet()
	var box := _panel()
	if sheet.is_empty():
		_ruby(box, "宿題[しゅくだい]は もう 出した。", 26)
		_button(column, "もどる", LANTERN, _back_from_collection)
		return
	var reward := HomeworkRules.reward_for(sheet)
	_ruby(box, "宿題[しゅくだい]を 出[だ]す", 30)
	_ruby(box, "%s   %s" % [str(sheet["title_ruby"]), str(sheet["teacher_ruby"])], 22)
	_text(box, "%s    %d / %d" % [str(sheet["date"]), int(reward["correct"]), int(reward["total"])], 20)
	for item in sheet["items"]:
		if bool(item["correct"]):
			continue
		_ruby(box, str(item["prompt_ruby"]), 18)
		_text(box, "%s → %s    %s" % [str(item["answer"]), str(item["expected"]), _error_name(str(item["error_type"]))], 16, Color("8c3a32"))
	_ruby(box, "ほたる玉[だま] +%d" % int(reward["gems"]), 24)
	_text(box, "できた ことばは、カードが 少し めずらしさに ちかづく。", 16)
	if (reward["weak_verbs"] as Array).size() > 0:
		_text(box, "まちがえた どうしは、つぎの たびで よわい カードになる。", 16)
	_button(column, "出[だ]す", LANTERN, _turn_in_homework.bind(sheet))

func _turn_in_homework(sheet: Dictionary) -> void:
	var reward := HomeworkRules.reward_for(sheet)
	state["gems"] = int(state["gems"]) + int(reward["gems"])
	for card_id in reward["boost_cards"]:
		for i in 4:
			state["mastery"][str(card_id)] = RarityRules.record(_mastery(str(card_id)), true, 2000)
	for verb_id in reward["weak_verbs"]:
		if str(verb_id) not in state["weak_verbs"]:
			state["weak_verbs"].append(str(verb_id))
	if str(reward["cosmetic_back"]) != "":
		state["card_back"] = str(reward["cosmetic_back"])
		if str(reward["cosmetic_back"]) not in state["cosmetics"]:
			state["cosmetics"].append(str(reward["cosmetic_back"]))
	if str(reward["relic"]) != "":
		state["bonus_relic"] = "perfect-brush"
	state["seen_homework"].append(str(sheet["id"]))
	_save()
	if state.get("run") == null:
		_show_title()
	else:
		_show_map()

func _pending_sheet() -> Dictionary:
	for sheet in pack.homework:
		if str(sheet["id"]) not in state["seen_homework"]:
			return sheet
	return {}

func _back_to_map() -> void:
	_save()
	_show_map()

func _back_from_collection() -> void:
	if state.get("run") == null:
		_show_title()
	else:
		_show_map()

func _start_run(seed_value: int) -> void:
	var relics: Array = []
	if str(state.get("bonus_relic", "")) != "":
		relics.append(str(state["bonus_relic"]))
	state["run"] = {
		"seed": seed_value,
		"map": MapGen.generate(seed_value),
		"path": [],
		"hp": CombatRules.max_hp(),
		"max_hp": CombatRules.max_hp(),
		"block": 0,
		"deck": _starter(),
		"relics": relics,
		"gear": 6,
		"battle": null,
	}

func _starter() -> Array:
	var latest: Dictionary = pack.packs[pack.packs.size() - 1]
	var deck: Array = []
	var verbs := 0
	var others := 0
	for card in latest["cards"]:
		if str(card["type"]) == "verb" and verbs < 6:
			deck.append(card)
			verbs += 1
		elif str(card["type"]) in ["grammar", "noun", "adjective", "phrase"] and others < 3:
			deck.append(card)
			others += 1
	for verb_id in state["weak_verbs"]:
		var card := pack.card_by_id("verb:" + str(verb_id))
		if card.is_empty():
			continue
		deck.append(card)
		deck.append(card)
	return deck

func _next_verb() -> Dictionary:
	var run: Dictionary = state["run"]
	for card in run["deck"]:
		if str(card["type"]) == "verb":
			var verb := pack.verb_by_id(str(card["verb_id"]))
			if not verb.is_empty():
				run["deck"].pop_at(run["deck"].find(card))
				run["deck"].append(card)
				return verb
	return pack.verbs[0]

func _story_yokai() -> Dictionary:
	var found: Array = state["yokai_found"]
	if found.is_empty():
		return pack.yokai[0]
	return pack.yokai_by_id(str(found[0]))

func _mastery(card_id: String) -> Dictionary:
	var all: Dictionary = state["mastery"]
	if not all.has(card_id):
		all[card_id] = RarityRules.fresh(card_id)
	return all[card_id]

func _rarity_of(card_id: String) -> String:
	return str(_mastery(card_id)["rarity"])

func _back_name(back_id: String) -> String:
	match back_id:
		"gold-margin":
			return "きんの ふち"
		"night-sleeve":
			return "よるの そで"
		_:
			return "かみ"

func _error_name(kind: String) -> String:
	match kind:
		"dakuten":
			return "だくてん"
		"sokuon":
			return "そくおん"
		"word-choice":
			return "ことば"
		_:
			return kind

func _grammar_choices(card: Dictionary) -> Array:
	var out: Array = [str(card.get("cloze_answer", ""))]
	for other in pack.cards:
		if str(other["type"]) != "grammar":
			continue
		var answer := str(other.get("cloze_answer", ""))
		if answer != "" and answer not in out:
			out.append(answer)
		if out.size() >= 3:
			break
	return out

func _meaning_choices(card: Dictionary) -> Array:
	var out: Array = [str(card["gloss_ja"])]
	for other in pack.cards:
		var gloss := str(other.get("gloss_ja", ""))
		if gloss != "" and gloss not in out:
			out.append(gloss)
		if out.size() >= 3:
			break
	return out

func _burst(text: String, color: Color, good: bool) -> void:
	_float(text, color)
	_flash(Color(1, 0.86, 0.45, 1) if good else Color(0.7, 0.2, 0.16, 1))
	_play_sfx(good)
	if not good:
		Input.vibrate_handheld(30)

func _float(text: String, color: Color) -> void:
	var label := Label.new()
	label.text = text
	label.position = Vector2(230, 300)
	label.add_theme_font_size_override("font_size", 36)
	label.add_theme_color_override("font_color", color)
	stage.add_child(label)
	var tween := create_tween()
	tween.tween_property(label, "position", label.position + Vector2(0, -70), 0.55)
	tween.parallel().tween_property(label, "modulate:a", 0.0, 0.55)
	tween.tween_callback(label.queue_free)

func _flash(color: Color) -> void:
	flash.color = Color(color.r, color.g, color.b, 0.4)
	var tween := create_tween()
	tween.tween_property(flash, "color:a", 0.0, 0.28)

func _shake() -> void:
	var tween := create_tween()
	tween.tween_property(stage, "position", Vector2(8, 0), 0.04)
	tween.tween_property(stage, "position", Vector2(-6, 2), 0.04)
	tween.tween_property(stage, "position", Vector2.ZERO, 0.05)

func _play_sfx(good: bool) -> void:
	var stream := correct_sfx if good else wrong_sfx
	if stream == null or float(state.get("sfx", 0.8)) <= 0.01:
		return
	sfx.stream = stream
	sfx.volume_db = linear_to_db(maxf(float(state["sfx"]), 0.001))
	sfx.play()

func _play_music(index: int) -> void:
	var track := MusicCredits.for_region(index)
	var volume := float(state.get("music", 0.7))
	if volume <= 0.01:
		music.stop()
		music_id = ""
		return
	if music_id != str(track["id"]):
		var stream: AudioStream = load(str(track["file"]))
		if stream is AudioStreamOggVorbis:
			stream = stream.duplicate()
			stream.loop = true
		music.stream = stream
		music_id = str(track["id"])
	music.volume_db = linear_to_db(maxf(volume, 0.001))
	if not music.playing:
		music.play()

func _load() -> void:
	state = _fresh()
	if shot != "" or not FileAccess.file_exists(SAVE_PATH):
		return
	var parsed = JSON.parse_string(FileAccess.get_file_as_string(SAVE_PATH))
	if typeof(parsed) == TYPE_DICTIONARY:
		state = parsed

func _fresh() -> Dictionary:
	return {
		"pack_id": pack.id,
		"music": 0.7,
		"sfx": 0.8,
		"gems": 0,
		"mastery": {},
		"known_packs": [],
		"seen_homework": [],
		"cosmetics": ["paper"],
		"card_back": "paper",
		"tutorial_done": false,
		"weak_verbs": [],
		"yokai_found": [],
		"bonus_relic": "",
		"known_kanji": pack.known_kanji.duplicate(),
		"run": null,
	}

func _bind_known() -> void:
	if not state.has("known_kanji") or not (state["known_kanji"] is Array):
		state["known_kanji"] = pack.known_kanji.duplicate()
	Furigana.known = state["known_kanji"]

func _save() -> void:
	if shot != "":
		return
	var file := FileAccess.open(SAVE_PATH, FileAccess.WRITE)
	if file == null:
		return
	file.store_string(JSON.stringify(state))

func _prepare_shot() -> void:
	match shot:
		"map":
			_start_run(17)
			_show_map()
		"battle":
			_start_run(17)
			state["tutorial_done"] = true
			_begin_fight(state["run"]["map"]["rows"][0][0])
		"tutorial":
			state["tutorial_done"] = false
			_begin_guide(true)
			guide["phase"] = "spell"
			guide["pulse"] = true
			_show_guide()
		"spell":
			_start_run(17)
			state["tutorial_done"] = true
			var node: Dictionary = state["run"]["map"]["rows"][0][0].duplicate(true)
			node["type"] = "elite"
			_begin_fight(node)
			var battle: Dictionary = state["run"]["battle"]
			battle["yokai_id"] = "yuki"
			battle["form"] = "mashita"
			battle["node_type"] = "elite"
			pending = pack.card_by_id("verb:oyogu")
			built = ""
			battle.erase("tiles")
			_show_battle()
		"yokai":
			_start_run(21)
			var node: Dictionary = state["run"]["map"]["rows"][0][0].duplicate(true)
			node["type"] = "elite"
			_begin_fight(node)
		"cards":
			_show_compare()
		"collection":
			_seed_collection()
			_show_collection()
		"payout":
			_start_run(17)
			state["run"]["last"] = {
				"node_type": "elite",
				"plays": [
					{"correct": true, "rarity": "ultra"},
					{"correct": true, "rarity": "rare"},
					{"correct": false, "rarity": "common"},
					{"correct": true, "rarity": "uncommon"},
				],
				"combo": 6,
				"dropped": true,
				"yokai_id": "kappa",
			}
			state["yokai_found"] = ["kappa"]
			_show_payout()
		"reading":
			_start_run(17)
			_begin_reading(pack.yokai_by_id("kappa"))
		"yokai_card":
			state["yokai_found"] = ["kappa"]
			_open_yokai(pack.yokai_by_id("kappa"))
		"homework":
			_show_homework()
		_:
			_show_title()

func _show_compare() -> void:
	_clear()
	var box := _panel()
	_ruby(box, "おなじ ことば、ちがう めずらしさ", 24)
	var card := pack.card_by_id("verb:miru")
	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", 12)
	column.add_child(row)
	var common := CardFace.new()
	common.present(card, "common", font, false)
	var ultra := CardFace.new()
	ultra.present(card, "ultra", font, false)
	row.add_child(common)
	row.add_child(ultra)
	_text(box, "コモンは うすい わく。ウルトラは あつい 金の わく。", 16)

func _seed_collection() -> void:
	state["yokai_found"] = ["kappa", "tengu", "kitsune"]
	var card := pack.card_by_id("verb:miru")
	if not card.is_empty():
		var mastered := RarityRules.fresh(str(card["id"]))
		for i in 12:
			mastered = RarityRules.record(mastered, true, 2000)
		state["mastery"][str(card["id"])] = mastered

func _capture() -> void:
	await get_tree().process_frame
	await get_tree().process_frame
	await get_tree().process_frame
	await get_tree().create_timer(0.55).timeout
	var path := OS.get_environment("YORUBEN_SHOT_PATH")
	var image := get_viewport().get_texture().get_image()
	image.save_png(path)
	get_tree().quit()
