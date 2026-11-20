class_name WorldView
extends RefCounted

static func build(kind: String) -> Node3D:
	var root := Node3D.new()
	root.name = "World"
	var env_node := WorldEnvironment.new()
	var env := Environment.new()
	env.background_mode = Environment.BG_COLOR
	env.background_color = Color("9ec4a8")
	env.fog_enabled = true
	env.fog_mode = Environment.FOG_MODE_EXPONENTIAL
	env.fog_density = 0.055
	env.fog_light_color = Color("d5e7d4")
	env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	env.ambient_light_color = Color("d7ead8")
	env.ambient_light_energy = 0.7
	env_node.environment = env
	root.add_child(env_node)

	var light := DirectionalLight3D.new()
	light.rotation_degrees = Vector3(-42, -28, 0)
	light.light_color = Color("ffd9a8")
	light.light_energy = 1.45
	light.shadow_enabled = true
	root.add_child(light)

	var cam := Camera3D.new()
	cam.fov = 36
	cam.current = true
	if kind == "battle":
		cam.look_at_from_position(Vector3(0.2, 3.4, 4.6), Vector3(0, 1.1, -0.4), Vector3.UP)
	else:
		cam.look_at_from_position(Vector3(0.8, 6.4, 8.2), Vector3(0, 0.6, -2.0), Vector3.UP)
	root.add_child(cam)

	_plane(root, Vector2(30, 42), Vector3(0, 0, -8), Color("3f7d52"))
	_plane(root, Vector2(3.2, 36), Vector3(0, 0.02, -8), Color("6a5434"))

	if kind == "battle":
		_sprite(root, "res://assets/sprites/enemy_godan.png", Vector3(0, 1.5, -1.2), 0.04)
		_sprite(root, "res://assets/sprites/hero.png", Vector3(-0.2, 1.05, 1.7), 0.03)
		_sprite(root, "res://assets/sprites/tree.png", Vector3(-2.6, 1.3, -2.4), 0.03)
		_sprite(root, "res://assets/sprites/tree.png", Vector3(2.4, 1.2, 0.2), 0.026)
	else:
		_sprite(root, "res://assets/sprites/hero.png", Vector3(0.15, 1.35, 0.6), 0.034)
		_sprite(root, "res://assets/sprites/house.png", Vector3(-2.1, 1.2, -2.2), 0.03)
		var spots := [Vector3(-2.4, 1.45, 2.2), Vector3(2.3, 1.4, -0.4), Vector3(-2.2, 1.3, -4.6), Vector3(2.5, 1.25, -8.0), Vector3(-2.0, 1.15, -11.2), Vector3(1.8, 1.05, -14.0)]
		for spot in spots:
			_sprite(root, "res://assets/sprites/tree.png", spot, 0.03)
	return root

static func _plane(root: Node3D, size: Vector2, at: Vector3, color: Color) -> void:
	var mesh := PlaneMesh.new()
	mesh.size = size
	var body := MeshInstance3D.new()
	body.mesh = mesh
	var mat := StandardMaterial3D.new()
	mat.albedo_color = color
	mat.roughness = 1.0
	body.material_override = mat
	body.position = at
	root.add_child(body)

static func _sprite(root: Node3D, path: String, at: Vector3, pixel: float) -> void:
	var texture = load(path)
	if texture == null:
		return
	var sprite := Sprite3D.new()
	sprite.texture = texture
	sprite.billboard = BaseMaterial3D.BILLBOARD_ENABLED
	sprite.pixel_size = pixel
	sprite.position = at
	sprite.shaded = true
	root.add_child(sprite)

static func tint_enemy(root: Node3D, group: String) -> void:
	var path := "res://assets/sprites/enemy_%s.png" % group
	var texture = load(path)
	if texture == null:
		return
	for child in root.get_children():
		if child is Sprite3D and child.texture != null and str(child.texture.resource_path).contains("enemy"):
			child.texture = texture
