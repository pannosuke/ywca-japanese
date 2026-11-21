# Content

Study material is JSON. The app bundles this folder and, on launch with a network, reads `content/manifest.json` from `main` on GitHub. A higher `version` replaces the cached copy. An older build ignores a form id it does not know.

Kanji that a person sees must be written `漢字[よみ]`, unless every character in that run is listed in `profile.json` `known_kanji`. Eric's list is empty. Ids and English glosses may contain bare kanji. A deck `note` is shown on the review back. Japanese in that note uses furigana. The English mnemonic names the kanji it is talking about.

After class, add or edit a file, list any new path in `manifest.json` `files`, and add 1 to `version`. No app release is required for new words, sheets, scores, or stories.

## manifest.json

`schema` is 1. `version` is an integer. `files` is the list of JSON files below, relative to this folder.

## profile.json

`id`, `display_name`, `known_kanji` (array of characters), `deck_name`, `note_type`, `note_fields`. One profile is shown. The file can hold another id later.

## lessons.json

`lessons[]`: `id`, `date` (`YYYY-MM-DD`), `teacher_id`, `topic_ruby`, `topic_en`, `anki_tag`.

## vocab.json

`items[]`: `id`, `lesson_id`, `ruby`, `kana`, `gloss_ja`, `gloss_en`, `sentence_ruby`, `sentence_en`, `note`.

## verbs.json

`verbs[]`: `id`, `ruby`, `kana`, `group` (`godan`, `ichidan`, or `irregular`), `gloss_ja`, `gloss_en`, `lesson_id`.

## adjectives.json

`adjectives[]`: `id`, `kind` (`i` or `na`), `ruby`, `kana`, `gloss_ja`, `gloss_en`, `lesson_id`.

## forms.json

`forms[]`: `id`, `label_ruby`, `prompt_ruby`, `applies_to` (`verb`, `i-adjective`, or `na-adjective`), `enabled`.

An enabled form is drilled. Ids the app can grade: `masu`, `dict`, `masen`, `mashita`, `masen_deshita`, `te`, `te_kudasai`, `ta`, `nai`, `i_negative`, `i_past`, `i_past_negative`, `na_negative`, `na_past`, `na_past_negative`. A new pattern needs an app update. Until then, leave it out, or the installed app skips it. A typed answer may be kana or the kanji spelling of that card.

## grammar.json

`points[]`: `id`, `lesson_id`, `pattern_ruby`, `cloze_ruby`, `answer`, `accept`, `gloss_en`. A one-kana particle answer is asked as a particle. Anything longer is a blank.

## exceptions.json

`verbs` maps dictionary kana to the form the rule engine must produce. `group` is included. The check fails if the engine disagrees. This is the record of real exceptions, including `いく` → `いって`.

## worksheets/*.json

A sheet Eric can grade in the app. `id`, `date`, `teacher_id`, `title_ruby`, `lesson_id`, `items[]` with `id`, `prompt_ruby`, `expected`, `accept`, `verb_id`, `kind`.

## homework/*.json

A graded result. `id`, `worksheet_id`, `date`, `teacher_id`, `title_ruby`, `note`, `correct`, `total`, `items[]` with `id`, `prompt_ruby`, `expected`, `answer`, `correct`, `error_type`, `verb_id`. Misses are weighted in quizzes and drills. `correct` must equal the number of items with `"correct": true`.

## quizzes/*.json

A class quiz used as a question source. `id`, `lesson_id`, `title_ruby`, `items[]` with `id`, `kind` (`choose_form`, `fill_blank`, `meaning_to_word`, `word_to_meaning`, `fill_particle`), `prompt_ruby`, `answer`, `choices`. Optional `correct` and `total` record a score already given. Omit them when the file is only the questions.

## readers/*.json

`id`, `level` (0 or 1), `title_ruby`, `lesson_id`, `body_ruby`, `questions` (3 to 5). Each question has `id`, `prompt_ruby`, and `choices[]` of `{ "ruby", "correct" }`. One choice is correct.

## deck.json

The offline copy of the YWCA deck. `deck_name`, `note_type`, `cards[]` with `id`, `lesson_id`, `word`, `definition`, `meaning`, `sentence`, `sentence_en`, `note`. `note` is the Anki Note field: the class note, then a `<div class="kanji-mnemonic">` block. The review back shows that block as Kanji. Used when AnkiDroid is missing or the permission is refused. Scheduling then stays on the phone with FSRS.
