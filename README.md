# よるべん

Evening-class companion for Eric Allerton's Japanese at Osaka YWCA, evening class 2. Tuesday is Tsuji-sensei (dialogues). Friday is Ōuchi-sensei (grammar worksheets). Class is 19:00–20:30. Eric lives in Ibaraki-shi and installs this app himself with [Obtainium](https://github.com/ImranR98/Obtainium).

Kanji study has not started. Every kanji on screen has furigana above it. Definitions are short Japanese, in the style of a children's dictionary, with a smaller English line underneath.

## Progress

Version 0.4.0, not tagged yet, is a one-act deckbuilder. Open the app and press 出発. The map appears. Tap the glowing node. That is the fight, two taps from the title. The design is in [DESIGN.md](DESIGN.md). The checklist is in [ROADMAP.md](ROADMAP.md). [CHANGELOG.md](CHANGELOG.md) has the unreleased notes.

AnkiDroid, FSRS, and class-day bosses wait until after this act. Speech is still last.

## A run

1. **出発** — start the act. The coach marks can be skipped.
2. **The map** — tap a lit neighbor. The top node is the boss.
3. **A fight** — play a card, answer it, then end the turn or keep playing. After the win, pick one reward and read the gem payout.
4. **ずかん** — word cards and any yokai that dropped. A yokai card opens its story.
5. **宿題** — on the map, or at the campfire. Turning in a graded sheet pays gems and marks the missed verbs for the next run. It is not on the path to the first fight.

## Cards from class

```bash
python3 tools/build_card_pack.py
```

That writes one pack per lesson date under `content/packs/ywca-evening/releases/` and a `catalog.json`. A kanji without furigana fails the build. `python3 tools/write_yokai.py` rewrites the yokai stories. Homework sheets live in `content/packs/ywca-evening/homework/`. The sample is No.89: Eric named three misses and eleven right answers, and the file keeps all of them, so the score is 11 of 14.

## His commute

JR A-line from 茨木駅 to 大阪駅. The rapid, if he catches it. Then the underground exit M16 and E Street to the YWCA. The app does not call that destination Umeda, and it does not name stations in between. He has not said which ones that train stops at. 吹田 and 梅田 remain in the word list only because they were printed on a handout.

## 頻度

The Chinese word 頻率 on the 20 October sheet was the teacher's gloss for Chinese-speaking classmates. It is not treated as Eric's mistake, and there is no error drill on ひんどう. 頻度 is ordinary class vocabulary.

## Data

Bundled assets, schema version 1:

- `content/packs/ywca-evening/profile.json` — student, school, schedule, commute, weak spots
- `content/packs/ywca-evening/lessons.json` — one object per class
- `content/packs/ywca-evening/words.json` — the word bank (`study` or `known`)
- `content/packs/ywca-evening/verbs.json` — the conjugation list

The Kotlin copies of those files are archived with the old app. `tools/build_assets.py` from that tree is at `archive/kotlin-app/tools/build_assets.py`.

Japanese in those files uses Anki brackets when a word has kanji: `夕日[ゆうひ]`, `厳[きび]しい`. Gloss fields (`gloss_ja`, `explanation_ja`, `reminder_ja`) are kana only, so a dropped-in file cannot show a bare kanji by accident. To add a class later, edit the pack JSON and ship a new release. Obtainium will not see asset edits until a release exists.

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

Godot 4.7.2. Open the project, or from a machine that has the editor:

```bash
godot --headless --path . --import --quit
godot --headless --path . -s addons/gut/gut_cmdln.gd -gdir=res://tests -gprefix=test_ -gexit
```

The Android export preset is `Android` in `export_presets.cfg`. Package `dev.ericallerton.yoruben`, version name `0.4.0`, version code `4`, arm64-v8a only. The preset exports an unsigned APK. CI signs it. Bump `version/code` and `version/name` there, and `config/version` in `project.godot`, before the next tag.

The previous Kotlin project is in `archive/kotlin-app`. It is not the CI build.

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

The Godot export is `0.4.0` (`versionCode` 4). Bump `export_presets.cfg` before the next tag. Tags are pushed by hand after merge. Do not tag v0.4.0 until it has been reviewed.

## AnkiDroid

0.1.0 and 0.2.0 read the deck through the content provider (`content://com.ichi2.anki.flashcards`, permission `com.ichi2.anki.permission.READ_WRITE_DATABASE`). That screen is not in the Godot build. The pack still names the deck `YWCA` and the tags `class::YYYY-MM-DD`. The plugin waits until after v0.4, and it stays read-only.

## 灯森

v0.4 keeps the night forest, the three CC0 loops in `assets/music/CREDITS.md`, and the name ほたる玉. Fights ask for ます, ません, ました, ませんでした, て-form, or ない. The ない form uses the あ-row, and う becomes わ (あう → あわない). 行く in the て-form is still いって.

Wrong answers wear the brush. The shop repairs it. Dying keeps mastery and gems already paid out. It does not keep the rest of that run. Nothing in the shop costs real money.

## Known gaps

- v0.1.0, v0.2.0, and v0.3.0 are already released. v0.4.0 is not tagged. Tags are pushed by hand after review.
- Photographing a worksheet, AnkiDroid, FSRS, class-day bosses, shared packs, and speech are not in this build. See [ROADMAP.md](ROADMAP.md).
- Lesson 8's date (2026-11-13) and Ōuchi-sensei are inferred from the Friday worksheet, not from a printed header. The No.89 homework file uses that same date and teacher.
- New lessons are a new pack file and a release, not an in-app download.
- The Japanese glosses and the yokai stories were written for this app. They are not copied from a published dictionary or a graded-reader series.
- There is no kanji-study mode.
