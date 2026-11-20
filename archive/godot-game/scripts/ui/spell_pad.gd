class_name SpellPad
extends VBoxContainer

signal typed(piece: String, origin: Vector2)
signal erased
signal submitted(answer: String)

var fly_from := Vector2.INF
var fly_piece := ""

func present(verb: Dictionary, form: String, built: String, bank: Array, font: Font, pulse: bool) -> void:
	for child in get_children():
		child.queue_free()
	add_theme_constant_override("separation", 8)
	size_flags_horizontal = Control.SIZE_EXPAND_FILL
	size_flags_vertical = Control.SIZE_EXPAND_FILL
	var answer := Conjugation.kana(verb, form)
	var frame := PanelContainer.new()
	frame.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	var paper := StyleBoxFlat.new()
	paper.bg_color = Color("f6ead2")
	paper.set_corner_radius_all(16)
	paper.set_content_margin_all(12)
	frame.add_theme_stylebox_override("panel", paper)
	var box := VBoxContainer.new()
	box.add_theme_constant_override("separation", 6)
	frame.add_child(box)
	add_child(frame)
	var lead := RubyLabel.new()
	lead.font = font
	lead.font_size = 20
	lead.color = Color("241c16")
	lead.max_width = 470
	lead.set_markup(ChallengeRules.instruction(verb, form))
	box.add_child(lead)
	var english := Label.new()
	english.text = ChallengeRules.instruction_en(form)
	english.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	english.add_theme_font_size_override("font_size", 13)
	english.add_theme_color_override("font_color", Color("5c5146"))
	box.add_child(english)
	var decoy := Label.new()
	decoy.text = "つかわない もじも まざっている。"
	decoy.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	decoy.add_theme_font_size_override("font_size", 15)
	decoy.add_theme_color_override("font_color", Color("8a4b12"))
	box.add_child(decoy)
	var slots := _slots(answer, built, font)
	box.add_child(slots)
	var gap := Control.new()
	gap.size_flags_vertical = Control.SIZE_EXPAND_FILL
	gap.custom_minimum_size = Vector2(0, 8)
	add_child(gap)
	var grid := GridContainer.new()
	grid.columns = 6
	grid.add_theme_constant_override("h_separation", 6)
	grid.add_theme_constant_override("v_separation", 6)
	add_child(grid)
	for item in bank:
		var piece := str(item)
		var tile := Button.new()
		tile.text = piece
		tile.custom_minimum_size = Vector2(52, 52)
		tile.add_theme_font_size_override("font_size", 22)
		tile.add_theme_color_override("font_color", Color("241c16"))
		var style := StyleBoxFlat.new()
		style.bg_color = Color("fff6e4")
		style.border_color = Color("e39a3c")
		style.set_border_width_all(3)
		style.set_corner_radius_all(10)
		tile.add_theme_stylebox_override("normal", style)
		tile.add_theme_stylebox_override("hover", style)
		tile.add_theme_stylebox_override("pressed", style)
		tile.pressed.connect(_on_tile.bind(piece, tile))
		grid.add_child(tile)
		if pulse:
			var tween := create_tween()
			tween.set_loops(8)
			tween.tween_property(tile, "modulate", Color(1, 0.82, 0.45), 0.28)
			tween.tween_property(tile, "modulate", Color(1, 1, 1), 0.28)
	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", 8)
	add_child(row)
	var erase := _action("けす", Color("5c5146"), font, 18, false)
	erase.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	erase.size_flags_stretch_ratio = 1.0
	erase.pressed.connect(func() -> void: erased.emit())
	row.add_child(erase)
	var ready := built.length() == answer.length() and answer.length() > 0
	var caption := "これで答[こた]える" if ready else "もじを つづけて"
	var submit := _action(caption, Color("e39a3c") if ready else Color("8a8175"), font, 20, ready)
	submit.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	submit.size_flags_stretch_ratio = 2.0
	submit.pressed.connect(func() -> void: submitted.emit(built))
	row.add_child(submit)
	if ready:
		var glow := create_tween()
		glow.set_loops(6)
		glow.tween_property(submit, "modulate", Color(1, 0.9, 0.55), 0.24)
		glow.tween_property(submit, "modulate", Color(1, 1, 1), 0.24)
	if fly_piece != "" and built.ends_with(fly_piece):
		var origin := fly_from
		var piece := fly_piece
		fly_piece = ""
		fly_from = Vector2.INF
		_fly(slots, origin, piece, built.length() - 1)

func _on_tile(piece: String, tile: Button) -> void:
	typed.emit(piece, tile.global_position)

func _slots(answer: String, built: String, font: Font) -> HBoxContainer:
	var row := HBoxContainer.new()
	row.alignment = BoxContainer.ALIGNMENT_CENTER
	row.add_theme_constant_override("separation", 4)
	var wide := 44
	if answer.length() > 8:
		wide = 32
	for i in answer.length():
		var slot := PanelContainer.new()
		slot.custom_minimum_size = Vector2(wide, 48)
		var style := StyleBoxFlat.new()
		style.bg_color = Color("fffaf2")
		style.border_color = Color("c9842a")
		style.set_border_width_all(2)
		style.set_corner_radius_all(8)
		slot.add_theme_stylebox_override("panel", style)
		var label := Label.new()
		label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
		label.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
		label.add_theme_font_override("font", font)
		label.add_theme_font_size_override("font_size", 22)
		label.add_theme_color_override("font_color", Color("241c16"))
		if i < built.length():
			label.text = built.substr(i, 1)
		slot.add_child(label)
		row.add_child(slot)
	return row

func _action(markup: String, fill: Color, font: Font, font_size: int, dark_ink: bool) -> Button:
	var button := Button.new()
	button.text = ""
	button.custom_minimum_size = Vector2(0, 72)
	var style := StyleBoxFlat.new()
	style.bg_color = fill
	style.set_corner_radius_all(16)
	style.set_content_margin_all(6)
	button.add_theme_stylebox_override("normal", style)
	button.add_theme_stylebox_override("hover", style)
	button.add_theme_stylebox_override("pressed", style)
	var center := CenterContainer.new()
	center.mouse_filter = Control.MOUSE_FILTER_IGNORE
	center.set_anchors_preset(Control.PRESET_FULL_RECT)
	var label := RubyLabel.new()
	label.font = font
	label.font_size = font_size
	label.color = Color("241c16") if dark_ink else Color("f7efe0")
	label.align_center = true
	label.mouse_filter = Control.MOUSE_FILTER_IGNORE
	label.set_markup(markup)
	center.add_child(label)
	button.add_child(center)
	return button

func _fly(slots: HBoxContainer, origin: Vector2, piece: String, index: int) -> void:
	if index < 0 or index >= slots.get_child_count():
		return
	var slot := slots.get_child(index) as Control
	slot.modulate.a = 0.0
	var ghost := Label.new()
	ghost.text = piece
	ghost.mouse_filter = Control.MOUSE_FILTER_IGNORE
	ghost.add_theme_font_size_override("font_size", 22)
	ghost.add_theme_color_override("font_color", Color("241c16"))
	add_child(ghost)
	ghost.global_position = origin
	var tween := create_tween()
	tween.tween_interval(0.02)
	tween.tween_callback(func() -> void:
		if not is_instance_valid(ghost) or not is_instance_valid(slot):
			if is_instance_valid(ghost):
				ghost.queue_free()
			return
		var glide := create_tween()
		glide.tween_property(ghost, "global_position", slot.global_position, 0.16)
		glide.tween_callback(func() -> void:
			if is_instance_valid(slot):
				slot.modulate.a = 1.0
			if is_instance_valid(ghost):
				ghost.queue_free()
		)
	)
