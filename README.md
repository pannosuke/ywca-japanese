# よるべん

A study app for Eric Allerton's Japanese at Osaka YWCA, evening class 2. Tuesday is Tsuji-sensei. Friday is Ōuchi-sensei. Class is 19:00–20:30. He installs it himself with [Obtainium](https://github.com/ImranR98/Obtainium).

Version 0.5.1 is a review, quiz, and drill app. The deckbuilder is archived under `archive/godot-game`. The 0.2 Kotlin prototype is under `archive/kotlin-app`.

It is built from class material only. Kanji study has not started. Every kanji on screen has furigana unless it is in `known_kanji`. That list is empty.

## How a session goes

Home is five rows: Review, Quiz, Conjugation, Read, Stats. **Start review** is the button at the thumb.

Review pulls due cards from the AnkiDroid deck `YWCA`, note type `YWCA Vocab`, and sends Again, Hard, Good, or Easy back, so AnkiWeb stays in step. If AnkiDroid is not installed or the permission is refused, the same cards come from `content/deck.json` and a local FSRS schedule.

Quiz is 5, 8, or 10 questions from the deck, a worksheet, graded homework, or a class quiz. A miss is asked once more at the end.

Conjugation is typed. Kana and the kanji spelling of the card both pass, including 座る and 行って. Tiles stay kana. The rule after the answer names the direction and the verb group, including the `いく` te-form exception and u-verbs that end in る. Weak forms and verb groups are asked more often.

Read is a short story with three to five questions. A choice fills the row, stays selected, and is marked after Check. Stats keeps the streak, retention, and the misses. **Share progress** sends a JSON file.

A worksheet with an answer key can be filled in and graded on the phone. Photograph grading stays outside the app. The result is a file under `content/homework/`.

## Content

The shapes are in [content/SCHEMA.md](content/SCHEMA.md). On launch, with a network, the app fetches `content/manifest.json` from `main`. A higher `version` replaces the cached copy, so a new class file does not need a new APK.

```text
https://raw.githubusercontent.com/pannosuke/ywca-japanese/main/content/manifest.json
```

## Build

Package `dev.ericallerton.yoruben`. `versionCode` 7, `versionName` 0.5.1. JDK 17, Android SDK 35.

```bash
./gradlew testDebugUnitTest assembleDebug
```

A tagged release is signed with the existing keystore secrets and published as `yoruben-<version>.apk`. This branch does not add a tag.
