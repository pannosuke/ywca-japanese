# よるべん

Evening-class companion for Eric Allerton's Japanese at Osaka YWCA, evening class 2. Tuesday is Tsuji-sensei (dialogues). Friday is Ōuchi-sensei (grammar worksheets). Class is 19:00–20:30. Eric lives in Ibaraki-shi and installs this app himself with [Obtainium](https://github.com/ImranR98/Obtainium).

Kanji study has not started. Every kanji on screen has furigana above it. Definitions are short Japanese, in the style of a children's dictionary, with a smaller English line underneath.

## Screens

1. **レッスン** — class sessions by date and teacher. Each one opens onto the words, the grammar point (pattern, explanation, example), and his notes.
2. **れんしゅう** — short Japanese-first drills, multiple choice and fill-in, on grammar patterns and the words marked `study`. The mix leans toward the spots that actually tripped him up: ひま vs たいくつ, 物価 vs 値段, small kana such as ディ, and おもしろかった (the ろ stays). Results stay on the phone in Room.
3. **すすみ** — reads the AnkiDroid deck `YWCA` through the AnkiDroid content provider and shows per-lesson card counts for tags such as `class::2026-10-27`, next to the quiz tally. Read-only. If AnkiDroid is missing or the permission is denied, the screen says so and the quiz tally still works.
4. **じゅんび** — a pre-class brief (last class, what to look at again, and the trip to YWCA) and a post-class debrief. Debriefs are stored on the phone and can be written out as JSON.

## His commute

JR A-line from 茨木駅 to 大阪駅. The rapid, if he catches it. Then the underground exit M16 and E Street to the YWCA. The app does not call that destination Umeda, and it does not name stations in between. He has not said which ones that train stops at. 吹田 and 梅田 remain in the word list only because they were printed on a handout.

## 頻度

The Chinese word 頻率 on the 20 October sheet was the teacher's gloss for Chinese-speaking classmates. It is not treated as Eric's mistake, and there is no error drill on ひんどう. 頻度 is ordinary class vocabulary.

## Data

Bundled assets, schema version 1:

- `app/src/main/assets/data/profile.json` — student, school, schedule, commute, weak spots
- `app/src/main/assets/data/lessons.json` — one object per class
- `app/src/main/assets/data/words.json` — the word bank (`study` or `known`)

Japanese in those files uses Anki brackets when a word has kanji: `夕日[ゆうひ]`, `厳[きび]しい`. Gloss fields (`gloss_ja`, `explanation_ja`, `reminder_ja`) are kana only, so a dropped-in file cannot show a bare kanji by accident. `tools/build_assets.py` rebuilds the assets from `tools/source/` and checks those rules. To add a class later, edit the JSON (or the source export plus the script) and ship a new release. Obtainium will not see asset edits until a release exists.

A lesson object looks like this:

```json
{
  "id": "2026-11-10",
  "date": "2026-11-10",
  "weekday": "Tue",
  "teacher_id": "tsuji",
  "anki_tag": "class::2026-11-10",
  "topic_ruby": "欲[ほ]しいと たい",
  "topic_en": "Wanting a thing, and wanting to do something.",
  "grammar": [],
  "notes": []
}
```

Words point at a lesson with `lesson_date`. `status` is `study` or `known`.

## Build

Android Studio, or the command line. JDK 17. `minSdk` 26, `targetSdk` 35. His phone is a Galaxy S24 Ultra on Android 14 or newer.

```bash
./gradlew lint test assembleDebug
```

## Obtainium

1. Install Obtainium from [F-Droid](https://f-droid.org/packages/dev.imranr.obtainium/) or from its GitHub releases.
2. Add App → GitHub.
3. Paste `https://github.com/pannosuke/ywca-japanese`.
4. Leave "include prereleases" off. Obtainium tracks the APK attached to the latest GitHub Release.
5. When a signed `yoruben-x.y.z.apk` is on a release, install it. Later tags update in place only if they are signed with the same key.

There is no Play Store listing. The app does not phone home.

## Signing

The release workflow signs with one keystore, read from Actions secrets, so every Obtainium update matches the first install. The keystore and the passwords are not in this repo. Do not commit a `.jks`, a `.keystore`, or a base64 dump.

Create the key once and keep the file somewhere safe (a password manager or a backup disk). Losing it means the next APK cannot update the installed app.

```bash
keytool -genkeypair -v \
  -storetype PKCS12 \
  -keystore yoruben-release.jks \
  -alias yoruben \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=Eric Allerton, OU=Yoruben, O=Personal, L=Ibaraki, ST=Osaka, C=JP"
```

`keytool` asks for a store password and a key password. They can be the same.

Encode the file as one line:

```bash
# Linux
base64 -w 0 yoruben-release.jks

# macOS
base64 -i yoruben-release.jks | tr -d '\n'
```

In GitHub: Settings → Secrets and variables → Actions → New repository secret. Add all four:

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | the one-line base64 of `yoruben-release.jks` |
| `KEYSTORE_PASSWORD` | the store password |
| `KEY_ALIAS` | `yoruben` |
| `KEY_PASSWORD` | the key password |

Then, on a commit that already contains the app:

```bash
git tag v0.1.0
git push origin v0.1.0
```

The tag workflow runs lint and tests, builds a signed release APK, and attaches `yoruben-0.1.0.apk` to the GitHub Release. If the secrets are missing, that job fails on purpose and does not upload an unsigned APK. An unsigned file, or a file signed with a different key, would block Obtainium from updating later.

Re-run the failed workflow after the secrets exist, or push the same tag again (`git push origin v0.1.0` cannot repeat a tag; delete the remote tag first only if no release was published, or move on to `v0.1.1`).

Version name in the app is `0.1.0` (`versionCode` 1). Bump both in `app/build.gradle.kts` before the next tag.

## AnkiDroid

The progress screen uses the official content provider (`content://com.ichi2.anki.flashcards`, permission `com.ichi2.anki.permission.READ_WRITE_DATABASE`), requested at runtime. It queries decks, notes, and cards. It never writes. Notes are matched on the `class::YYYY-MM-DD` tag. Deck names `YWCA` and `YWCA::…` are the ones that count.

Anki's review "due" value is not a calendar date unless you also know the collection's day cutoff, which this provider does not expose. The screen shows queue counts instead: new, learning, review, suspended.

## Known gaps

- No signed `v0.1.0` APK until the four secrets above exist and the tag is pushed.
- Lesson 8's date (2026-11-13) and Ōuchi-sensei are inferred from the Friday worksheet, not from a printed header.
- New lessons are a new build and release, not an in-app import.
- The Japanese glosses were written for this app. They are not copied from a published dictionary.
- Debrief JSON export does not write back into the lesson assets.
- Quiz scores are not sent to Anki.
- There is no kanji-study mode.
