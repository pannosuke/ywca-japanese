class_name Backdrop
extends Control

var mood := "night"

func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	set_anchors_preset(Control.PRESET_FULL_RECT)

func set_mood(next: String) -> void:
	mood = next
	queue_redraw()

func _process(_delta: float) -> void:
	var target := get_viewport_rect().size
	if size != target:
		position = Vector2.ZERO
		size = target
		queue_redraw()

func _draw() -> void:
	var area := size
	if area.x < 200.0:
		area = get_viewport_rect().size
	var night := Color("1a1410")
	var sky := Color("3d2a22")
	if mood == "battle":
		sky = Color("5a3028")
	draw_rect(Rect2(Vector2.ZERO, area), night)
	for i in 16:
		var t := float(i) / 16.0
		draw_rect(Rect2(0, area.y * t, area.x, area.y / 16.0 + 2), night.lerp(sky, t * 0.9))
	var moon := Vector2(area.x * 0.78, 118)
	draw_circle(moon, 86, Color(0.95, 0.78, 0.42, 0.16))
	draw_circle(moon, 48, Color("f3ddb0"))
	draw_circle(moon + Vector2(14, -8), 40, sky.lerp(night, 0.35))
	var ground_y := area.y * 0.78
	draw_rect(Rect2(0, ground_y, area.x, area.y - ground_y), Color("1d3328"))
	for i in 8:
		var x := -20.0 + float(i) * (area.x / 7.0)
		var h := 120.0 + float((i * 37) % 90)
		var top := ground_y - h
		draw_colored_polygon(PackedVector2Array([
			Vector2(x, ground_y + 8),
			Vector2(x + 28, top),
			Vector2(x + 56, ground_y + 8),
		]), Color("163528").lerp(Color("2f5d40"), float(i % 3) * 0.25))
	draw_rect(Rect2(0, ground_y + 8, area.x, area.y), Color("24362c"))
	var lamp := Vector2(area.x * 0.18, ground_y - 36)
	draw_line(lamp, Vector2(lamp.x, ground_y + 8), Color("5c4030"), 5)
	draw_circle(lamp, 16, Color("ffd27a"))
	draw_circle(lamp, 36, Color(0.89, 0.55, 0.2, 0.28))
	var lamp2 := Vector2(area.x * 0.86, ground_y - 18)
	draw_line(lamp2, Vector2(lamp2.x, ground_y + 8), Color("5c4030"), 4)
	draw_circle(lamp2, 12, Color("e39a3c"))
	draw_circle(lamp2, 28, Color(0.89, 0.55, 0.2, 0.22))
