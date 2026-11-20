class_name YokaiView
extends Control

var yokai_id := "kappa"

func _notification(what: int) -> void:
	if what == NOTIFICATION_RESIZED:
		queue_redraw()

func show_id(next: String) -> void:
	yokai_id = next
	custom_minimum_size = Vector2(220, 220)
	queue_redraw()

func _draw() -> void:
	var center := size / 2
	draw_circle(center + Vector2(0, 8), 86, Color("f4e7cb"))
	draw_circle(center + Vector2(0, 8), 86, Color("8a5a2a"), false, 4.0)
	match yokai_id:
		"tengu":
			_blob(center, Color("8c3a32"))
			draw_colored_polygon(PackedVector2Array([center + Vector2(10, -4), center + Vector2(78, 6), center + Vector2(12, 16)]), Color("f0d2b0"))
		"kitsune":
			_blob(center, Color("d97848"))
			_ear(center + Vector2(-28, -36))
			_ear(center + Vector2(20, -40))
			draw_circle(center + Vector2(-46, 20), 14, Color("f0d2b0"))
			draw_circle(center + Vector2(-62, 8), 12, Color("f0d2b0"))
			draw_circle(center + Vector2(-74, -6), 10, Color("f0d2b0"))
		"tanuki":
			_blob(center, Color("6b4a32"))
			draw_circle(center + Vector2(48, 24), 22, Color("8c6244"))
		"rokuro":
			_blob(center + Vector2(0, 20), Color("d7c4a4"))
			draw_line(center + Vector2(0, -10), center + Vector2(8, -70), Color("d7c4a4"), 10)
			draw_circle(center + Vector2(8, -78), 16, Color("f0d8c0"))
		"karakasa":
			draw_colored_polygon(PackedVector2Array([center + Vector2(-70, -10), center + Vector2(0, -70), center + Vector2(70, -10)]), Color("8c3a32"))
			draw_line(center + Vector2(0, -10), center + Vector2(0, 60), Color("5c4030"), 6)
			draw_circle(center + Vector2(0, -8), 8, Color("1c1612"))
		"kodama":
			draw_colored_polygon(PackedVector2Array([center + Vector2(-50, 50), center + Vector2(0, -70), center + Vector2(50, 50)]), Color("2f5d40"))
			draw_circle(center + Vector2(0, -10), 8, Color("ffd27a"))
		"yuki":
			_blob(center, Color("d5e4ef"))
			draw_circle(center + Vector2(0, 16), 46, Color("2a3a4a"), false, 3.0)
			draw_colored_polygon(PackedVector2Array([center + Vector2(-34, -20), center + Vector2(0, -62), center + Vector2(34, -20)]), Color("f7fbff"))
			draw_line(center + Vector2(-18, 18), center + Vector2(-36, 72), Color("9eb8cc"), 10)
			draw_line(center + Vector2(18, 18), center + Vector2(36, 72), Color("9eb8cc"), 10)
		"kuchisake":
			_blob(center, Color("f0d8c0"))
			draw_line(center + Vector2(-24, 16), center + Vector2(24, 28), Color("8c3a32"), 4)
		"nurarihyon":
			_blob(center, Color("d7c4a4"))
			draw_circle(center + Vector2(0, -46), 22, Color("f7efe0"))
		"azuki":
			_blob(center, Color("8c3a32"))
			for i in 5:
				draw_circle(center + Vector2(-20 + i * 10, 36), 4, Color("6a1f2a"))
		"hyotan":
			draw_circle(center + Vector2(0, -16), 22, Color("c9842a"))
			draw_circle(center + Vector2(0, 24), 34, Color("e0a24a"))
			_blob(center + Vector2(-36, 10), Color("f0d8c0"))
		"ibaraki":
			_blob(center, Color("6a1f2a"))
			draw_colored_polygon(PackedVector2Array([center + Vector2(-20, -40), center + Vector2(-8, -78), center + Vector2(2, -40)]), Color("f7efe0"))
			draw_colored_polygon(PackedVector2Array([center + Vector2(12, -40), center + Vector2(24, -78), center + Vector2(34, -40)]), Color("f7efe0"))
		_:
			_blob(center, Color("3e6b4e"))
			draw_circle(center + Vector2(0, -52), 16, Color("7ec8c0"), false, 4.0)
	draw_circle(center + Vector2(-16, -6), 4, Color("1c1612"))
	draw_circle(center + Vector2(14, -6), 4, Color("1c1612"))

func _blob(center: Vector2, color: Color) -> void:
	draw_circle(center + Vector2(0, 16), 46, color)

func _ear(at: Vector2) -> void:
	draw_colored_polygon(PackedVector2Array([at, at + Vector2(-14, -28), at + Vector2(16, -6)]), Color("d97848"))
