# よるべん — design

v0.4 is a one-act deckbuilder. A session is a walk through a lantern forest. Words from class are the cards.

## Core loop

The title has one button, 出発. The first tap opens the map. The first node is a battle, so the second tap is a fight. A short coach mark explains the map, the hand, and the reward, and it can be skipped.

Each act is a seeded map of seven rows. The bottom row is the start. The top node is the boss. Rows have two to four nodes, edges do not cross, and every node can be reached. Tap an adjacent node to move. The path you took stays lit.

Node types:

| Node | What you do |
| --- | --- |
| せん | Turn-based fight. Play cards. |
| つよ | Same fight, but a verb is built from kana tiles. |
| わけ | Sort a verb into group 1, 2, or 3. |
| はや | Answer as many ます forms as you can before the clock ends. |
| やす | Heal, or study a card. A finished homework sheet can be turned in here. |
| みせ | Spend ほたる玉 on a heal, a repair, or a sleeve. |
| はなし | A short risk and a short reward. |
| たま | A relic. |
| よみ | Read a yokai story and answer it. |
| ぬし | The act boss, 茨木童子. The form changes as its health falls. |

HP carries for the act. The act ends when you fall or when the boss falls. Gems and mastery stay either way. The save is `user://yoruben-v4.json`, so a v0.3 save is left alone.

## Cards and rarity

Every word, verb, and grammar point in the evening pack is a card. The face shows the word with furigana, a type, a cost, and an effect.

- A verb strikes. Playing it means conjugating into the form the enemy is asking for.
- A grammar card is a cloze. A correct answer draws or blocks.
- Anything else asks for the meaning and wards.

A turn gives 3 energy and a hand of 5. A correct strike deals 7 plus a rarity bonus. A miss deals 2, and the card is still marked as a miss. The relic ともしび lets the first miss of a fight land at full strength. The gem payout still pays the rarity bonus only for a real correct answer.

Rarity is Common, Uncommon, Rare, Ultra. A card records uses, correct answers, and speed.

- Uncommon: 4 uses, accuracy at least 0.75
- Rare: 8 uses, accuracy at least 0.85
- Ultra: 12 uses, accuracy at least 0.9, average under 6 seconds

Three misses in a row drop one tier, never below Common. Higher rarity hits harder and pays more gems when the answer was correct: 1, 2, 4, or 7. The fields are plain counts so a later Anki or FSRS feed can write them. That feed is not in this version.

After a fight you pick 1 of 3 rewards. The offer leans toward cards, and a harder node or a clean fight leans further. New class packs open as a small booster the first time they are seen. The first departure is not blocked by that.

## Economy

ほたる玉 are the only currency. A fight pays a base (3, or 6 for an elite, 10 for the boss, 4 for a treasure), plus the rarity gems of correct cards, plus one gem for every three combo. The payout screen lists those lines. Gems from a payout are kept even if a later fight ends the run. The shop spends them. There is no real-money shop.

One weapon, 木のふで, starts at 6 durability. A wrong answer wears it. At 0, strikes lose a point of damage. The shop repairs it.

Relics in this act: ともしび (one forgiven miss), とけい (+5 seconds on a speed node), にだん (group-2 strikes hit twice), ほたる (+1 gem per correct card).

## Yokai

Enemies are public-domain yokai, drawn in the game. No copied character art. Minor fights are small yokai, elites are mid ones, and the act boss is 茨木童子. A win can drop that yokai into the binder. Bosses always drop. Elites drop more often than a normal fight, and a clean fight raises the chance.

A yokai card has the name with furigana, the drawing, a short fact, and a graded-reader story. Stories are level 0, and 河童, 狐, 雪女, and 茨木童子 also have a level 1 story that opens once 30 gems have been earned. English is behind a button. A gloss word can be tapped. The reading node asks the story's questions. A correct answer also marks a deck card toward its next rarity.

## Content pipeline

Class notes stay in the pack. Rule scripts do not name a student.

```bash
python3 tools/build_card_pack.py
```

That reads `lessons.json`, `words.json`, and `verbs.json` and writes `content/packs/ywca-evening/releases/ywca-YYYY-MM-DD.json` plus `catalog.json`. A kanji with no `[reading]` fails the build. After a class, add the lesson and run the script, then ship a release. The game treats a new pack id as the booster.

`python3 tools/write_yokai.py` rewrites `yokai.json` from the story source. Thirteen yokai are in the pack.

## Homework

Real worksheets are graded outside the app for now. A later version may photograph a sheet. The result is JSON in the pack:

```json
{
  "worksheet_id": "No.89",
  "date": "2026-11-13",
  "teacher_ruby": "大内[おおうち]",
  "items": [
    {"prompt_ruby": "…", "expected": "だして", "answer": "たして", "correct": false, "error_type": "dakuten", "verb_id": "dasu", "card_id": "verb:dasu"}
  ]
}
```

`error_type` is a short tag such as `dakuten`, `sokuon`, or `word-choice`. The score is the count of items, not a separate number.

The first sheet is `content/packs/ywca-evening/homework/no89.json`. Eric named three misses and eleven right answers, and first said 11/13. The file keeps every named item, so the counted score is 11 of 14. The misses are 出す (dakuten: たして for だして), 行く (sokuon: いて for いって), and 話す (word choice: おしえて for はなして).

Turning the sheet in at the campfire, or from the map, pays `4 + 2 × correct` gems. Each correct card takes four successful reviews, which is enough to lift a new card to Uncommon. Each missed verb is added to the next run twice, and the hand marks it よわい. A perfect sheet also grants the card back きんの ふち and the relic 満点のふで. No.89 is not perfect.

The turn-in is not on the way to the first battle. 出発 still opens the map, and the glowing node is still the fight.

## Multi-user

The pack can be swapped. The save stores mastery, gems, relics, and which packs and homework sheets were already seen. Nothing in `scripts/rules` names Eric or the commute. Another learner is another pack and another save.
