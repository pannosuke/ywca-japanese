class_name MusicCredits
extends RefCounted

const TRACKS := [
	{
		"id": "forest",
		"file": "res://assets/music/forest.ogg",
		"title": "Iremos Forest Theme Loop",
		"author": "beardalaxy",
		"source": "https://opengameart.org/content/iremos-forest-theme-loop",
		"license": "CC0",
		"use_ruby": "林[はやし]の きょく",
	},
	{
		"id": "study",
		"file": "res://assets/music/study.ogg",
		"title": "Dark Woods Loop",
		"author": "beardalaxy",
		"source": "https://opengameart.org/content/dark-woods-loop",
		"license": "CC0",
		"use_ruby": "しょさいの きょく",
	},
	{
		"id": "autumn",
		"file": "res://assets/music/autumn.ogg",
		"title": "Aimless Autumn",
		"author": "Icerider007",
		"source": "https://opengameart.org/content/aimless-autumn",
		"license": "CC0",
		"use_ruby": "あきの きょく",
	},
]

static func for_region(index: int) -> Dictionary:
	return TRACKS[posmod(index, TRACKS.size())]
