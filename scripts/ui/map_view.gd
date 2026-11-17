class_name MapView
extends Control

signal chosen(node_id: String)

const LABEL := {
	"battle": "せん",
	"elite": "つよ",
	"puzzle": "わけ",
	"speed": "はや",
	"rest": "やす",
	"shop": "みせ",
	"event": "はなし",
	"treasure": "たま",
	"story": "よみ",
	"boss": "ぬし",
}
const FILL := {
	"battle": "2f5d40",
	"elite": "8c3a32",
	"puzzle": "3d5c8a",
	"speed": "c9842a",
	"rest": "d97848",
	"shop": "e39a3c",
	"event": "6b4c7a",
	"treasure": "c9a227",
	"story": "2f6b62",
	"boss": "6a1f2a",
}

var font: Font
var _map: Dictionary = {}
var _path: Array = []
var _open: Array = []
var _placing := false

func bind(map: Dictionary, path: Array, open_ids: Array, text_font: Font) -> void:
	_map = map
	_path = path
	_open = open_ids
	font = text_font
	custom_minimum_size = Vector2(500, 560)
	queue_redraw()
	call_deferred("_place")

func _notification(what: int) -> void:
	if what == NOTIFICATION_RESIZED and not _map.is_empty():
		_place()

func _place() -> void:
	if _map.is_empty() or _placing:
		return
	_placing = true
	for child in get_children():
		remove_child(child)
		child.free()
	var rows: Array = _map["rows"]
	var width := maxf(size.x, 500.0)
	var height := maxf(size.y, 560.0)
	var span := (height - 80) / float(maxi(rows.size() - 1, 1))
	for r in rows.size():
		var row: Array = rows[r]
		var y := height - 50 - float(r) * span
		for node in row:
			var x := 40.0 + (float(node["col"]) + 1.0) * (width - 80) / float(row.size() + 1)
			var button := Button.new()
			button.text = LABEL.get(str(node["type"]), "？")
			button.position = Vector2(x - 30, y - 24)
			button.custom_minimum_size = Vector2(64, 48)
			button.add_theme_font_override("font", font)
			button.add_theme_font_size_override("font_size", 16)
			var open := str(node["id"]) in _open
			var visited := str(node["id"]) in _path
			var style := StyleBoxFlat.new()
			style.bg_color = Color(FILL.get(str(node["type"]), "5c5146")).lightened(0.2)
			style.set_corner_radius_all(18)
			style.border_color = Color("ffd27a") if open else Color("f6ead2")
			style.set_border_width_all(5 if open else 2)
			button.add_theme_stylebox_override("normal", style)
			button.add_theme_stylebox_override("hover", style)
			button.add_theme_stylebox_override("pressed", style)
			button.add_theme_stylebox_override("disabled", style)
			button.add_theme_color_override("font_color", Color("f7efe0"))
			button.add_theme_color_override("font_disabled_color", Color("f7efe0"))
			button.disabled = not open
			button.pressed.connect(func(): chosen.emit(str(node["id"])))
			add_child(button)
	_placing = false

func _draw() -> void:
	if _map.is_empty():
		return
	var rows: Array = _map["rows"]
	var width := maxf(size.x, 500.0)
	var height := maxf(size.y, 560.0)
	var span := (height - 80) / float(maxi(rows.size() - 1, 1))
	var points := {}
	for r in rows.size():
		var row: Array = rows[r]
		var y := height - 50 - float(r) * span
		for node in row:
			var x := 40.0 + (float(node["col"]) + 1.0) * (width - 80) / float(row.size() + 1)
			points[node["id"]] = Vector2(x, y)
	for r in rows.size() - 1:
		for node in rows[r]:
			for dest in node["next"]:
				var a: Vector2 = points[node["id"]]
				var b: Vector2 = points[dest]
				var hot: bool = str(node["id"]) in _path and str(dest) in _path
				draw_line(a, b, Color("ffd27a") if hot else Color(0.93, 0.86, 0.72, 0.7), 4.0 if hot else 2.0)
