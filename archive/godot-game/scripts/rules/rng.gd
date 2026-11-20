class_name Rng
extends RefCounted

var state: int

func _init(seed_value: int) -> void:
	state = seed_value & 0x7fffffff
	if state == 0:
		state = 1

func next_int() -> int:
	state = (state * 1664525 + 1013904223) & 0x7fffffff
	return state

func next_index(count: int) -> int:
	return next_int() % count

static func hash_text(text: String) -> int:
	var h := 2166136261
	for i in text.length():
		h = (h ^ text.unicode_at(i)) * 16777619
		h = h & 0x7fffffff
	return h
