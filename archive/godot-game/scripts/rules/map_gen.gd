class_name MapGen
extends RefCounted

const ROW_COUNT := 7

static func generate(seed_value: int, row_count: int = ROW_COUNT) -> Dictionary:
	var rng := Rng.new(seed_value)
	var counts: Array[int] = []
	for r in row_count:
		if r == row_count - 1:
			counts.append(1)
		else:
			counts.append(2 + rng.next_index(3))
	var rows: Array = []
	for r in row_count:
		var row: Array = []
		for i in counts[r]:
			row.append({
				"id": "n-%d-%d" % [r, i],
				"row": r,
				"col": i,
				"type": "battle",
				"next": [],
			})
		rows.append(row)
	_types(rows, rng)
	_link(rows)
	return {"seed": seed_value, "rows": rows}

static func node_by_id(map: Dictionary, node_id: String) -> Dictionary:
	for row in map["rows"]:
		for node in row:
			if node["id"] == node_id:
				return node
	return {}

static func column_of(row: Array, node_id: String) -> int:
	for node in row:
		if node["id"] == node_id:
			return int(node["col"])
	return -1

static func crosses(row: Array, next_row: Array) -> bool:
	var edges: Array = []
	for node in row:
		var source := int(node["col"])
		for dest_id in node["next"]:
			edges.append([source, column_of(next_row, str(dest_id))])
	for a in edges.size():
		for b in range(a + 1, edges.size()):
			var left: Array = edges[a]
			var right: Array = edges[b]
			if int(left[0]) > int(right[0]):
				var swap: Array = left
				left = right
				right = swap
			if int(left[0]) < int(right[0]) and int(left[1]) > int(right[1]):
				return true
	return false

static func reachable(map: Dictionary) -> bool:
	var rows: Array = map["rows"]
	var start_ids: Array = []
	for node in rows[0]:
		start_ids.append(node["id"])
	var seen := {}
	var pile: Array = start_ids.duplicate()
	while pile.size() > 0:
		var current: String = pile.pop_back()
		if seen.has(current):
			continue
		seen[current] = true
		var node := node_by_id(map, current)
		for dest in node["next"]:
			pile.append(dest)
	for row in rows:
		for node in row:
			if not seen.has(node["id"]):
				return false
	var boss: Dictionary = rows[rows.size() - 1][0]
	return seen.has(boss["id"])

static func parents_and_children(map: Dictionary) -> bool:
	var rows: Array = map["rows"]
	for r in rows.size():
		var parents := {}
		if r > 0:
			for node in rows[r - 1]:
				for dest in node["next"]:
					parents[str(dest)] = true
		for node in rows[r]:
			if r > 0 and not parents.has(node["id"]):
				return false
			if r < rows.size() - 1 and node["next"].is_empty():
				return false
			if r == rows.size() - 1 and not node["next"].is_empty():
				return false
	return true

static func _types(rows: Array, rng: Rng) -> void:
	var last := rows.size() - 1
	var pool := ["battle", "battle", "elite", "puzzle", "speed", "rest", "shop", "event", "treasure", "story"]
	for r in rows.size():
		for node in rows[r]:
			if r == last:
				node["type"] = "boss"
			elif r == 0:
				node["type"] = "battle" if int(node["col"]) == 0 else ("puzzle" if int(node["col"]) == 1 else "story")
			elif r == last - 1:
				var late := ["rest", "shop", "battle"]
				node["type"] = late[rng.next_index(late.size())]
			else:
				node["type"] = pool[rng.next_index(pool.size())]
	_ensure(rows, "rest")
	_ensure(rows, "shop")

static func _ensure(rows: Array, kind: String) -> void:
	for row in rows:
		for node in row:
			if node["type"] == kind:
				return
	var mid: Array = rows[maxi(rows.size() / 2, 1)]
	mid[0]["type"] = kind

static func _link(rows: Array) -> void:
	for r in rows.size() - 1:
		var cur: Array = rows[r]
		var nxt: Array = rows[r + 1]
		var min_t := 0
		for i in cur.size():
			var ideal := 0
			if cur.size() > 1 and nxt.size() > 1:
				ideal = int(round(float(i) * float(nxt.size() - 1) / float(cur.size() - 1)))
			var target: int = clampi(maxi(ideal, min_t), 0, nxt.size() - 1)
			cur[i]["next"] = [nxt[target]["id"]]
			min_t = target
		for j in nxt.size():
			var dest_id: String = nxt[j]["id"]
			if _has_parent(cur, dest_id):
				continue
			var parent := 0
			for i in cur.size():
				var dest_col := column_of(nxt, str(cur[i]["next"][0]))
				if dest_col <= j:
					parent = i
			_add(cur[parent], dest_id)
		for i in cur.size():
			var cols := _dest_cols(nxt, cur[i]["next"])
			var top: int = cols[cols.size() - 1]
			if top + 1 >= nxt.size():
				continue
			var ok := true
			for k in range(i + 1, cur.size()):
				var later: int = _dest_cols(nxt, cur[k]["next"])[0]
				if later < top + 1:
					ok = false
			if ok:
				_add(cur[i], nxt[top + 1]["id"])

static func _has_parent(row: Array, node_id: String) -> bool:
	for node in row:
		if node_id in node["next"]:
			return true
	return false

static func _add(node: Dictionary, node_id: String) -> void:
	if node_id in node["next"]:
		return
	node["next"].append(node_id)

static func _dest_cols(row: Array, ids: Array) -> Array:
	var cols: Array[int] = []
	for node_id in ids:
		cols.append(column_of(row, str(node_id)))
	cols.sort()
	return cols
