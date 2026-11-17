class_name RubyLabel
extends Control

var markup := ""
var font: Font
var font_size := 28
var color := Color("1c2416")
var align_center := false
var max_width := 0.0

func set_markup(value: String) -> void:
	markup = Furigana.apply(value, Furigana.known)
	_fit()
	queue_redraw()

func _fit() -> void:
	if font == null:
		return
	var lines := _lines()
	var widest := 0.0
	for line in lines:
		var width := 0.0
		for item in line:
			width += float(item["w"]) + 6.0
		widest = maxf(widest, width)
	if max_width > 0.0:
		widest = maxf(widest, 40.0)
	custom_minimum_size = Vector2(widest, font_size * 1.9 * maxi(lines.size(), 1))

func _lines() -> Array:
	var source: Array = RubyText.parse(markup)
	var pieces: Array = []
	for part in source:
		var base := str(part["base"])
		var reading := str(part["reading"])
		if max_width > 0.0 and base.length() > 1 and reading == "":
			var step := 0
			while step < base.length():
				pieces.append({"base": base.substr(step, 1), "reading": ""})
				step += 1
		else:
			pieces.append({"base": base, "reading": reading})
	var lines: Array = []
	var line: Array = []
	var x := 0.0
	for part in pieces:
		var base := str(part["base"])
		var reading := str(part["reading"])
		var base_w := font.get_string_size(base, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size).x
		var read_w := 0.0
		if reading != "":
			read_w = font.get_string_size(reading, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size * 0.46).x
		var piece := maxf(base_w, read_w)
		if max_width > 0.0 and x > 0.0 and x + piece > max_width:
			lines.append(line)
			line = []
			x = 0.0
		line.append({"base": base, "reading": reading, "w": piece, "base_w": base_w, "read_w": read_w})
		x += piece + 6.0
	if line.size() > 0:
		lines.append(line)
	return lines

func _draw() -> void:
	if font == null:
		return
	var lines := _lines()
	var row := 0
	for line in lines:
		var total := 0.0
		for item in line:
			total += float(item["w"]) + 6.0
		var x := 0.0
		if align_center and size.x > total:
			x = (size.x - total) * 0.5
		var base_y := font_size * 1.45 + row * font_size * 1.9
		var read_y := font_size * 0.5 + row * font_size * 1.9
		for item in line:
			var piece := float(item["w"])
			var base := str(item["base"])
			var reading := str(item["reading"])
			var base_w := float(item["base_w"])
			if reading != "":
				var read_w := float(item["read_w"])
				draw_string(font, Vector2(x + (piece - read_w) * 0.5, read_y), reading, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size * 0.46, color)
			draw_string(font, Vector2(x + (piece - base_w) * 0.5, base_y), base, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size, color)
			x += piece + 6.0
		row += 1
