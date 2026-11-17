class_name RubyLabel
extends Control

var markup := ""
var font: Font
var font_size := 28
var color := Color("1c2416")
var align_center := false

func set_markup(value: String) -> void:
	markup = value
	_fit()
	queue_redraw()

func _fit() -> void:
	if font == null:
		return
	var width := 0.0
	for part in RubyText.parse(markup):
		var base: String = part["base"]
		var reading: String = part["reading"]
		var base_w := font.get_string_size(base, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size).x
		var read_w := 0.0
		if reading != "":
			read_w = font.get_string_size(reading, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size * 0.46).x
		width += maxf(base_w, read_w) + 6.0
	custom_minimum_size = Vector2(width, font_size * 1.85)

func _draw() -> void:
	if font == null:
		return
	var parts := RubyText.parse(markup)
	var widths: Array = []
	var total := 0.0
	for part in parts:
		var base: String = part["base"]
		var reading: String = part["reading"]
		var base_w := font.get_string_size(base, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size).x
		var read_w := 0.0
		if reading != "":
			read_w = font.get_string_size(reading, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size * 0.46).x
		var piece := maxf(base_w, read_w)
		widths.append(piece)
		total += piece + 6.0
	var x := 0.0
	if align_center and size.x > total:
		x = (size.x - total) * 0.5
	var base_y := font_size * 1.45
	for i in parts.size():
		var part: Dictionary = parts[i]
		var piece: float = widths[i]
		var base: String = part["base"]
		var reading: String = part["reading"]
		var base_w := font.get_string_size(base, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size).x
		if reading != "":
			var read_w := font.get_string_size(reading, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size * 0.46).x
			draw_string(font, Vector2(x + (piece - read_w) * 0.5, font_size * 0.5), reading, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size * 0.46, color)
		draw_string(font, Vector2(x + (piece - base_w) * 0.5, base_y), base, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size, color)
		x += piece + 6.0
