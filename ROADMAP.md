# よるべん roadmap

Short sessions, one hand, often offline. Study notes are never what a run can take. Gear and currency are.

The learner is a save, not a name baked into the rules. Class notes, the commute, and the hometown live in a content pack that can be swapped. There is no server in these phases.

Checkboxes: `[x]` done, `[ ]` planned. Phase 1 items that still need a later plugin are left open.

## Released

- **v0.1.0** — study app. Lessons, practice, class prep, read-only AnkiDroid, furigana.
- **v0.2.0** — forest adventure. Conjugation cave, seeded runs, relics, village shop. That build was the Kotlin app. From v0.3.0 the installable app is Godot. The Kotlin tree stays in git history and is archived in this repo so it is not the thing Obtainium installs.

## Phase 1 — Godot 2.5D commute RPG

Godot is the game. 2D sprites stand in a 3D world: depth, light, fog, and a tilted camera.

- [x] Godot 4.x project, portrait, one-handed reach, offline
- [x] 2.5D overworld and battle (billboards, fog, light, camera)
- [x] Lessons, practice, and class prep carried over, with ruby furigana on every kanji
- [x] Curriculum, words, and verbs as a swappable content pack
- [x] Learner profile in the save. Commute and hometown come from the pack. AnkiDroid stays optional and off until a plugin exists
- [x] Hooks only: a local friend code, and closed doors for sync, leaderboards, and a party
- [x] Verb enemies. The group picks the type. A correct conjugation is the attack
- [x] Stats (ATK, SPD, DEF, LUK, WIS), levels, stat points, a small skill tree
- [x] Gear slots (weapon, shield, armor, accessory, charm), rarity, seeded affixes, loot from battles and bosses
- [x] Cosmetics remain visual
- [x] Consequences: a lost run drops unbanked loot and unbanked gems; wrong answers wear durability; the village repairs it; a streak is a buff; a neglected region corrupts until those verbs are reviewed. The study log is not deleted
- [x] Resume a fight after the app is closed
- [x] Music and effects can be muted. Subtitles and haptics stay. Ambient loops are CC0, credited, one mood per region, separate volumes
- [x] Android export keeps package `dev.ericallerton.yoruben`, versionCode 3, one signed APK on a version tag
- [ ] AnkiDroid read-only progress (needs an Android plugin; the pack still stores the deck and tags)

## Phase 2 — memory and class day

- [ ] FSRS-style respawns: weak verbs return as enemies on a schedule
- [ ] Class-day bosses built from that lesson
- [ ] AnkiDroid plugin, still read-only, still per learner

## Phase 3 — village quests

- [ ] NPCs give short Japanese quests that stay inside grammar the pack already covers

## Phase 4 — classmates

- [ ] Share a content pack
- [ ] Optional sync
- [ ] Optional leaderboards
- [ ] Co-op and friend codes that actually connect

## Phase 5 — polish

- [ ] More biomes
- [ ] More cosmetics
- [ ] More original scenery

## Phase 6 — speech, last

- [ ] Spoken spells. Last, because a commute is often the wrong place for a microphone, and every phase before this works without one.
