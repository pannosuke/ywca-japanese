# Changelog

## Unreleased

v0.4.0 is not tagged. `versionCode` in the project is 4 so the next signed build can replace 0.3.0 in place. Package name stays `dev.ericallerton.yoruben`.

- The game is a one-act deckbuilder. 出発 opens a seeded map. The second tap is a battle. Coach marks can be skipped.
- Class words, verbs, and grammar points are cards. A turn has energy and a hand. Playing a card answers that card.
- Rarity climbs with use and can fall after three misses. A payout lists base gems, rarity gems, and combo gems.
- Enemies are yokai drawn in the game. Defeating one can add it to the binder, with a graded-reader story. 茨木童子 is the act boss.
- `tools/build_card_pack.py` writes a versioned pack per class date. A new pack id opens as a small booster.
- Homework is imported as JSON. No.89 (2026-11-13, 大内, てください) is the sample: 11 correct of 14 items, with the three named misses kept. Turning it in pays gems, boosts the cards that were right, and puts the missed verbs in the next run. A perfect sheet would add a card back. Photographing a worksheet is not in this build.
- The save file is `user://yoruben-v4.json`.

## 0.3.0

The installable app is a Godot 4.7 project. The Kotlin app is archived under `archive/kotlin-app` and is no longer what Obtainium installs. Package name stays `dev.ericallerton.yoruben`. `versionCode` is 3.

- 2.5D forest: billboard sprites, a tilted camera, light, and fog. Portrait, one-handed, offline.
- Lessons, practice, and class prep, with ruby furigana on kanji.
- The evening-class notes are a swappable content pack. The save is a learner profile. Commute and hometown are read from the pack.
- Verb enemies. A correct conjugation is the attack. Stats, levels, a small skill tree, gear slots, seeded loot, durability, repair, and a lost run that drops only unbanked gear and gems.
- A neglected lesson region clouds over until those verbs are reviewed. The study log is kept.
- A fight can be resumed. Music and effects have their own volumes and can be muted. Subtitles and a short vibration stay.
- Three CC0 loops, credited in the app and in `assets/music/CREDITS.md`.
- Local friend code only. Sync, leaderboards, and co-op stay closed.
- AnkiDroid read-only progress is deferred. The pack still stores the deck name and tags.

## 0.2.0

Forest adventure on the Kotlin app. Conjugation cave, seeded runs, relics, village shop, gems called ほたる玉. Study screens from 0.1.0 stayed. `versionCode` 2.

## 0.1.0

Study app. Lessons, practice, class prep, and a read-only AnkiDroid progress screen. Furigana on kanji. `versionCode` 1.
