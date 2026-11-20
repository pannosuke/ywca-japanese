# Design

0.5.0 is a study tool. Paper, ink, and one quiet accent. No mascot, no score fireworks, no chrome that does not answer something.

The game is in `archive/godot-game`. This file describes the app that replaced it.

## Palette

Light is warm paper `#F4F1EA` and ink `#1A1916`. The accent is `#2C4A3E`, used for the primary button, a selected filter, and a correct answer. Incorrect is `#7A3E36`, and only then.

Dark follows the phone. The page is `#11110F`, the ink is `#F3EFE6`, and the accent is `#B7CBBF`. Incorrect is `#E0B2A8`.

There is no second brand color and no card shadow. Rows are separated by a hairline.

## Type

Noto Sans JP. Titles are 32sp with room under them. Body is 18sp on a 30sp line. Furigana is 11sp in the muted ink, close to the character, present and not loud. A kanji with no reading does not ship. The check scans display fields and the Kotlin sources.

Chrome is English. Japanese is the material being studied.

## Home

Five rows, in this order: Review, Quiz, Conjugation, Read, Stats. Each row has one true subtitle: a due count, a weak count, or the fact that there is no streak yet. **Start review** sits at the bottom, in the thumb's reach. The same zone holds Show answer, the four grades, Check, Next, Grade, and Share progress.

## Motion

A revealed answer and a correct or incorrect line fade in over a short moment. Nothing else moves.

## Review

The word, then the meaning, the Japanese definition, the sentence, and the English. Again, Hard, Good, and Easy match Anki. Good is the filled button. A sideways swipe grades Again or Good, and the screen says so.

## Feedback

Correct or Incorrect, then the expected answer when it was wrong, then the rule in one sentence. Counts are the numbers that happened. An empty queue says that nothing is due.

## Offline

The bundled JSON is enough. A newer manifest from GitHub is cached and used next time, including with no network.
