class_name CardFace
extends PanelContainer

func present(card: Dictionary, rarity: String, font: Font, compact := false) -> void:
	for child in get_children():
		child.free()
	custom_minimum_size = Vector2(108, 176) if compact else Vector2(210, 280)
	var style := StyleBoxFlat.new()
	style.bg_color = Color("f7efe0")
	style.set_corner_radius_all(14)
	style.set_content_margin_all(8)
	var border := {"common": Color("8a8175"), "uncommon": Color("2f5d40"), "rare": Color("c9842a"), "ultra": Color("e0b15a")}
	style.border_color = border.get(rarity, Color("8a8175"))
	style.set_border_width_all(6 if rarity == "ultra" else 3)
	add_theme_stylebox_override("panel", style)
	var box := VBoxContainer.new()
	box.add_theme_constant_override("separation", 4)
	add_child(box)
	var top := HBoxContainer.new()
	box.add_child(top)
	var cost := Label.new()
	cost.text = "えね%d" % int(card.get("cost", 1))
	cost.add_theme_font_override("font", font)
	cost.add_theme_font_size_override("font_size", 14)
	cost.add_theme_color_override("font_color", Color("6a3b12"))
	top.add_child(cost)
	var kind := Label.new()
	kind.text = _kind(str(card.get("type", "")))
	kind.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	kind.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	kind.add_theme_font_override("font", font)
	kind.add_theme_font_size_override("font_size", 14)
	kind.add_theme_color_override("font_color", Color("5c5146"))
	top.add_child(kind)
	var ruby := RubyLabel.new()
	ruby.font = font
	ruby.font_size = 18 if compact else 28
	ruby.max_width = 92 if compact else 190
	ruby.set_markup(str(card.get("ruby", "")))
	box.add_child(ruby)
	if not compact:
		_line(box, str(card.get("gloss_ja", "")), font, 14, Color("3a332b"))
		_line(box, str(card.get("gloss_en", "")), font, 12, Color("6d6256"))
	var effect := Label.new()
	effect.text = ChallengeRules.effect_line(card, rarity)
	effect.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	effect.add_theme_font_override("font", font)
	effect.add_theme_font_size_override("font_size", 13 if compact else 16)
	effect.add_theme_color_override("font_color", Color("8a2a1a") if str(card.get("effect", "")) == "strike" else Color("2f5d40"))
	box.add_child(effect)
	var mark := Label.new()
	mark.text = _rarity(rarity)
	mark.add_theme_font_override("font", font)
	mark.add_theme_font_size_override("font_size", 13)
	mark.add_theme_color_override("font_color", style.border_color)
	box.add_child(mark)

func set_playable(on: bool) -> void:
	modulate = Color(1, 1, 1, 1.0 if on else 0.38)

func _line(parent: Node, value: String, font: Font, size: int, color: Color) -> void:
	if value == "":
		return
	var marked := Furigana.apply(value, Furigana.known)
	if marked.find("[") == -1:
		var label := Label.new()
		label.text = marked
		label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
		label.add_theme_font_override("font", font)
		label.add_theme_font_size_override("font_size", size)
		label.add_theme_color_override("font_color", color)
		parent.add_child(label)
		return
	var ruby := RubyLabel.new()
	ruby.font = font
	ruby.font_size = size
	ruby.color = color
	ruby.max_width = 190
	ruby.set_markup(marked)
	parent.add_child(ruby)

func _kind(type_name: String) -> String:
	match type_name:
		"verb":
			return "どうし"
		"adjective":
			return "けいようし"
		"grammar":
			return "ぶんぽう"
		"phrase":
			return "フレーズ"
		"yokai":
			return "ようかい"
		_:
			return "めいし"

func _rarity(rarity: String) -> String:
	match rarity:
		"uncommon":
			return "アンコモン"
		"rare":
			return "レア"
		"ultra":
			return "ウルトラ"
		_:
			return "コモン"
