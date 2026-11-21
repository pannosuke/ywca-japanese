# Changelog

## 0.5.2

Not tagged in this pull request. `versionCode` is 8, so Obtainium can replace 0.5.1 in place. Package name stays `dev.ericallerton.yoruben`. The release is signed with the same keystore as 0.5.1.

- The review back shows the Anki Note field under the definition and the example. The class note stays as prose. The kanji mnemonic is a Kanji section: the character, its parts, a short English line, and a Japanese line, with furigana. The same field is read from AnkiDroid and from `content/deck.json`.
- The offline deck is the 49 cards Anki is studying. Definitions, meanings, sentences, and notes match those notes, including 生活. Suspended notes stay in the word bank in `vocab.json` and are not review cards. A verb keeps its group in `verbs.json`.

## 0.5.1

Not tagged in this pull request. `versionCode` is 7, so Obtainium can replace 0.5.0 in place. Package name stays `dev.ericallerton.yoruben`. The release is signed with the same keystore as 0.5.0.

- A conjugation explanation names the direction. ます → 辞書形 no longer shows the ます rule. て, た, ない, ます, ません, ました, and ませんでした each say what to change, and a verb that ends in る but is an u-verb says so. 行く stays the って exception.
- 旅行する, 勉強する, and 結婚する conjugate as する. They were being treated as u-verbs that end in る.
- A typed answer accepts kana or the kanji spelling of that card: 座る and すわる, 座って, 行って, 来ない. Spaces and full-width characters are ignored. Tiles stay kana.
- た and ない are drilled, with the same group rules.
- In a story, each question is labeled. A tapped choice stays selected. After Check, the correct choice is marked, and a wrong tap is marked too. Each choice is a full-width row.
- Furigana stays on the same line as the kana that follows it.

## 0.5.0

Not tagged in this pull request. `versionCode` is 6, so Obtainium can replace 0.4.1 in place. Package name stays `dev.ericallerton.yoruben`.

- The deckbuilder is archived under `archive/godot-game`. The installed app is review, quiz, conjugation, reading, and stats, in Kotlin and Jetpack Compose. AnkiDroid's API is a ContentProvider, and the screens are lists and forms.
- Review reads the due cards in the AnkiDroid deck `YWCA` and sends Again, Hard, Good, and Easy back. Without AnkiDroid, or if the permission is refused, `content/deck.json` is reviewed with FSRS on the phone.
- Quiz is 5 to 10 questions from the deck, worksheets, graded homework, and class quizzes. A miss is asked once more, then weighted up.
- Conjugation covers ます, ません, ました, ませんでした, dictionary form, て-form, てください, and the adjective forms class has reached. Answers are typed, or built from tiles. The rule names the verb group, including 行く. Weak forms are asked more often.
- Readers are bundled stories with 3 to 5 questions. Scores are kept.
- Stats shows the streak, retention, today's count, accuracy by lesson, source, form, and verb group, the weakest items, and homework scores. Share progress sends a JSON file. Data is stored with a profile id. The screen does not switch profiles.
- A worksheet with an answer key can be filled in and graded on the phone. No.89 stays 11 of 14, with the three misses kept.
- Content is versioned JSON under `content/`, described in `content/SCHEMA.md`. On launch the app fetches `content/manifest.json` from `main` and caches it. A test checks the schema, furigana on display text, and conjugation answers against the rule engine and `exceptions.json`.

## 0.4.1

Not tagged in this pull request. `versionCode` is 5. Package name stays `dev.ericallerton.yoruben`.

- A verb is answered by tapping kana into one box per character. The prompt says so, in kana and in a short English line. It also says some tiles do not belong. A tapped tile leaves the bank.
- A fight shows three steps, which cards can be played, the card's attack or block, the enemy's next hit, and an end-turn button that says the enemy will attack.
- あそびかた on the title replays a one-turn lesson. The first fight on a profile is that lesson.
- A test checks every card and every form. 行く, する, and 来る stay the exceptions. Words that are not verbs are not asked as verbs. The 36 verb cards had a blank Japanese gloss; each now has a short one.

## 0.4.0

Not tagged in this pull request. `versionCode` is 4 so a signed build can replace 0.3.0 in place. Package name stays `dev.ericallerton.yoruben`.

- The game is a one-act deckbuilder. 出発 opens a seeded map. The second tap is a battle. Coach marks can be skipped.
- Class words, verbs, and grammar points are cards. A turn has energy and a hand. Playing a card answers that card.
- Rarity climbs with use and can fall after three misses. A payout lists base gems, rarity gems, and combo gems.
- Enemies are yokai drawn in the game. Defeating one can add it to the binder, with a graded-reader story. 茨木童子 is the act boss.
- `tools/build_card_pack.py` writes a versioned pack per class date. A new pack id opens as a small booster.
- Homework is imported as JSON. No.89 (2026-11-13, 大内, てください) is the sample: 11 correct of 14 items, with the three named misses kept. Turning it in pays gems, boosts the cards that were right, and puts the missed verbs in the next run. A perfect sheet would add a card back. Photographing a worksheet is not in this build.
- The save file is `user://yoruben-v4.json`.
- Every kanji on screen has furigana unless it is in that profile's `known_kanji` list. Eric's list is empty. One helper applies the reading. A test scans scripts, scenes, and the content pack with an empty list and fails on a bare kanji.

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
