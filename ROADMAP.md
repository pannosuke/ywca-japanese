# よるべん roadmap

Short sessions, one hand, often offline. A run can end. The study record and the banked gems do not.

The learner is a save. Class notes live in a content pack. There is no server in these phases.

Checkboxes: `[x]` done in the current tree, `[ ]` not built yet.

## Released

- **v0.1.0** — study app. Lessons, practice, class prep, read-only AnkiDroid, furigana.
- **v0.2.0** — forest adventure on the Kotlin app. Conjugation cave, seeded runs, relics, village shop.
- **v0.3.0** — the installable app becomes Godot. Package `dev.ericallerton.yoruben`. The Kotlin tree stays in git history under `archive/kotlin-app`.

v0.3.0 was a menu of study screens and a 2.5D forest. After that playtest, v0.4 replaces the loop. The package and the signing key stay.

## v0.4 — one act of a deckbuilder

This is the game from here on. A commute session is one act: a map, a hand of cards, a boss.

- [x] Title with one button, 出発. A fight on the second tap. Skippable coach marks
- [x] Seeded map, seven rows, no crossing edges, every node reachable, boss at the top
- [x] Node types: battle, elite tiles, group sort, speed, rest, shop, event, treasure, reading, boss
- [x] Cards from the class pack: verbs, words, phrases, grammar. Energy, a hand of five, play-to-answer
- [x] Rarity from use: Common, Uncommon, Rare, Ultra. Misses can drop a tier. Higher rarity pays more gems
- [x] Pick 1 of 3 after a node. Payout screen splits base, rarity, and combo
- [x] Relics, one weapon with durability, HP across the act, gems kept after a loss
- [x] Yokai enemies and a binder. Graded-reader stories, furigana, tap-to-define, English hidden. 茨木童子 is the act boss
- [x] Versioned class packs (`tools/build_card_pack.py`) and a booster when a new pack id appears
- [x] Homework JSON, the No.89 sheet, turn-in rewards, and weak-verb cards on the next run
- [x] Tests for the map, conjugation (including ない), rarity, payout, deck, yokai stories, drop rates, and homework
- [x] Furigana on every kanji unless it is in the profile's `known_kanji` list. Eric's list is empty
- [ ] Camera or OCR grading of a paper worksheet. The sheet is graded outside the app and imported as JSON until then
- [ ] More than one act, and more than the thirteen yokai

## v0.5.0 — art pass

This is the next build after v0.4.0. The act stays. The pictures and the feel catch up.

- [ ] Real card frames, not a flat panel and a border
- [ ] Foil and holo shaders by rarity, so an Ultra Rare card looks dramatically different from a Common one
- [ ] Yokai illustrations in place of the ink silhouettes
- [ ] Battle juice: hits, shakes, and the combo should read at a glance

## After the art pass

AnkiDroid, FSRS, and class-day bosses come after v0.5.0.

- [ ] AnkiDroid read-only progress. The pack still stores the deck name and the `class::` tags. Mastery counts are shaped so a later feed can write them
- [ ] FSRS-style respawns on top of the weak-verb cards homework already creates
- [ ] Class-day bosses built from that day's pack
- [ ] Village quests beyond the homework turn-in
- [ ] Shared packs, sync, leaderboards, co-op
- [ ] More biomes and more cosmetics
- [ ] Speech, last. A commute is often the wrong place for a microphone
