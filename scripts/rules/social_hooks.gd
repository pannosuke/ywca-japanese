class_name SocialHooks
extends RefCounted

static func friend_code(profile_id: String) -> String:
	var cleaned := profile_id.replace("-", "").to_upper()
	if cleaned.length() < 4:
		cleaned = "LOCAL"
	return "YB-" + cleaned.substr(0, 6)

static func sync_available() -> bool:
	return false

static func leaderboard_available() -> bool:
	return false

static func party_available() -> bool:
	return false
