#!/usr/bin/env python3
"""Build versioned card packs from the evening-class JSON.

Reads content/packs/ywca-evening/{lessons,words,verbs}.json and writes one
pack per class date to content/packs/ywca-evening/releases/ywca-YYYY-MM-DD.json
plus catalog.json. A kanji with no furigana bracket fails the build.

    python3 tools/build_card_pack.py
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "content" / "packs" / "ywca-evening"
KANJI = re.compile(r"[\u4e00-\u9fff々]")


def bare_kanji(text: str) -> list[str]:
    bad = []
    i = 0
    while i < len(text):
        if KANJI.match(text[i]):
            j = i
            while j < len(text) and KANJI.match(text[j]):
                j += 1
            if j >= len(text) or text[j] != "[":
                bad.append(text[i:j])
            i = j
        else:
            i += 1
    return bad


def load(name: str):
    return json.loads((ROOT / name).read_text())


def kind_for(word: dict, verb: dict | None) -> str:
    if verb:
        return "verb"
    kana = word.get("kana") or ""
    ruby = word.get("ruby") or ""
    if kana.endswith(("やか", "かな", "しい", "ない")) or kana.endswith("い"):
        return "adjective"
    if " " in ruby or "の" in ruby:
        return "phrase"
    return "noun"


def effect_for(kind: str) -> str:
    return {"verb": "strike", "grammar": "chant", "phrase": "spark"}.get(kind, "ward")


def card_from_word(word: dict, verb: dict | None) -> dict:
    kind = kind_for(word, verb)
    card = {
        "id": f"verb:{verb['id']}" if verb else f"word:{word['id']}",
        "type": kind,
        "ruby": word.get("ruby") or word.get("word"),
        "gloss_ja": word.get("gloss_ja") or "",
        "gloss_en": word.get("gloss_en") or "",
        "cost": 1,
        "effect": effect_for(kind),
        "lesson_date": word.get("lesson_date"),
        "verb_id": verb["id"] if verb else None,
        "group": verb["group"] if verb else None,
    }
    return card


def card_from_verb(verb: dict) -> dict:
    return {
        "id": f"verb:{verb['id']}",
        "type": "verb",
        "ruby": verb["dict_ruby"],
        "gloss_ja": "",
        "gloss_en": verb.get("gloss_en") or "",
        "cost": 1,
        "effect": "strike",
        "lesson_date": verb.get("lesson_date"),
        "verb_id": verb["id"],
        "group": verb["group"],
    }


def card_from_grammar(point: dict, lesson: dict) -> dict:
    return {
        "id": f"grammar:{point['id']}",
        "type": "grammar",
        "ruby": point.get("pattern_ruby") or "",
        "gloss_ja": point.get("explanation_ja") or "",
        "gloss_en": point.get("explanation_en") or "",
        "cost": 1,
        "effect": "chant",
        "lesson_date": lesson["date"],
        "verb_id": None,
        "group": None,
        "cloze_ruby": point.get("cloze_ruby") or "",
        "cloze_answer": point.get("cloze_answer") or "",
        "example_ruby": point.get("example_ruby") or "",
    }


def check_card(card: dict) -> None:
    for field in ("ruby", "gloss_ja", "cloze_ruby", "example_ruby"):
        if field not in card:
            continue
        bad = bare_kanji(str(card[field]))
        if bad:
            raise SystemExit(f"{card['id']} {field} has kanji without furigana: {bad}")


def main() -> None:
    lessons = load("lessons.json")["lessons"]
    words = load("words.json")["words"]
    verbs = load("verbs.json")
    by_kana = {v["dict_kana"]: v for v in verbs}
    by_ruby = {v["dict_ruby"]: v for v in verbs}
    out_dir = ROOT / "releases"
    out_dir.mkdir(parents=True, exist_ok=True)
    catalog = []
    seen_verb = set()
    for lesson in sorted(lessons, key=lambda item: item["date"]):
        date = lesson["date"]
        cards = []
        for word in words:
            if word.get("lesson_date") != date:
                continue
            verb = by_kana.get(word.get("kana")) or by_ruby.get(word.get("ruby"))
            card = card_from_word(word, verb)
            if verb:
                seen_verb.add(verb["id"])
            check_card(card)
            cards.append(card)
        for verb in verbs:
            if verb.get("lesson_date") != date or verb["id"] in seen_verb:
                continue
            card = card_from_verb(verb)
            seen_verb.add(verb["id"])
            check_card(card)
            cards.append(card)
        for point in lesson.get("grammar", []):
            card = card_from_grammar(point, lesson)
            check_card(card)
            cards.append(card)
        pack_id = f"ywca-{date}"
        payload = {
            "schema": 1,
            "id": pack_id,
            "lesson_date": date,
            "teacher_id": lesson.get("teacher_id"),
            "topic_ruby": lesson.get("topic_ruby"),
            "cards": cards,
        }
        path = out_dir / f"{pack_id}.json"
        path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n")
        catalog.append({"id": pack_id, "file": f"releases/{pack_id}.json", "lesson_date": date, "cards": len(cards)})
        print(f"{pack_id} {len(cards)} cards")
    (ROOT / "catalog.json").write_text(json.dumps({"schema": 1, "packs": catalog}, ensure_ascii=False, indent=2) + "\n")
    print("catalog", len(catalog))


if __name__ == "__main__":
    sys.exit(main())
