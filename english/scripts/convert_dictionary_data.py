#!/usr/bin/env python3
import argparse
import csv
import json
import os
import re
from collections import defaultdict


def read_gt_csv(path):
    with open(path, "r", encoding="utf-8-sig", newline="") as f:
        lines = [line.rstrip("\n") for line in f if line.strip()]
    header = lines[0].split(">")
    rows = []
    for line in lines[1:]:
        parts = line.split(">")
        if len(parts) < len(header):
            parts += [""] * (len(header) - len(parts))
        rows.append(dict(zip(header, parts[: len(header)])))
    return rows


def read_translation_csv(path):
    translations = {}
    with open(path, "r", encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            word = (row.get("word") or "").strip()
            translation = (row.get("translation") or "").strip()
            if word and translation and word not in translations:
                translations[word] = translation
    return translations


def normalize_book_name(name):
    return (name or "").strip()


GRADE_MAP = {
    "一年级": 1,
    "二年级": 2,
    "三年级": 3,
    "四年级": 4,
    "五年级": 5,
    "六年级": 6,
    "七年级": 7,
    "八年级": 8,
    "九年级": 9,
}


def extract_series_name(name):
    name = normalize_book_name(name)
    patterns = [
        r"(.*?)(一年级|二年级|三年级|四年级|五年级|六年级|七年级|八年级|九年级)",
        r"(.*?)(\d+[AB])$",
        r"(.*?)(模块[一二三四五六七八九十十一十二]+)",
    ]
    for pattern in patterns:
        match = re.search(pattern, name)
        if match:
            return match.group(1).strip(" -（）()")
    return name


def extract_grade_order(name):
    name = normalize_book_name(name)
    found = [(name.rfind(grade_name), order) for grade_name, order in GRADE_MAP.items() if grade_name in name]
    if found:
        found.sort(key=lambda item: item[0])
        return found[-1][1]
    match = re.search(r"(\d+)([AB])$", name)
    if match:
        return int(match.group(1))
    match = re.search(r"模块([一二三四五六七八九十]+)", name)
    if match:
        numerals = {
            "一": 1, "二": 2, "三": 3, "四": 4, "五": 5,
            "六": 6, "七": 7, "八": 8, "九": 9, "十": 10,
            "十一": 11, "十二": 12,
        }
        return 100 + numerals.get(match.group(1), 999)
    return 999


def extract_volume_order(name):
    name = normalize_book_name(name)
    if "上册" in name or name.endswith("上") or name.endswith("1A"):
        return 0
    if "下册" in name or name.endswith("下") or name.endswith("1B"):
        return 1
    if name.endswith("2A"):
        return 2
    if name.endswith("2B"):
        return 3
    if "全册" in name or "全一册" in name:
        return 9
    return 5


def book_sort_key(row):
    name = normalize_book_name(row.get("bk_name"))
    return (
        extract_series_name(name),
        extract_grade_order(name),
        extract_volume_order(name),
        float(row.get("bk_order") or 0),
        name,
    )


def main():
    parser = argparse.ArgumentParser(description="Convert DictionaryData CSVs to project sync JSON.")
    parser.add_argument("--source-dir", required=True, help="Directory containing book.csv, relation_book_word.csv, word.csv, word_translation.csv")
    parser.add_argument("--output", required=True, help="Output JSON file path")
    parser.add_argument("--name-contains", default="", help="Optional substring filter for book name")
    parser.add_argument("--limit-books", type=int, default=0, help="Optional limit after filtering")
    args = parser.parse_args()

    source_dir = args.source_dir
    books = read_gt_csv(os.path.join(source_dir, "book.csv"))
    relations = read_gt_csv(os.path.join(source_dir, "relation_book_word.csv"))
    words = read_gt_csv(os.path.join(source_dir, "word.csv"))
    translations = read_translation_csv(os.path.join(source_dir, "word_translation.csv"))

    word_map = {row["vc_id"]: row for row in words if row.get("vc_id")}
    relation_map = defaultdict(list)
    for row in relations:
        book_id = row.get("bv_book_id", "").strip()
        if book_id:
            relation_map[book_id].append(row)

    filtered_books = []
    needle = args.name_contains.strip()
    for row in books:
        if row.get("bk_level") != "2":
            continue
        if int(float(row.get("bk_direct_item_num") or "0")) <= 0:
            continue
        name = normalize_book_name(row.get("bk_name"))
        if needle and needle not in name:
            continue
        filtered_books.append(row)

    filtered_books.sort(key=book_sort_key)
    if args.limit_books > 0:
        filtered_books = filtered_books[: args.limit_books]

    result = {"books": []}
    for book_index, book in enumerate(filtered_books, start=1):
        book_id = book["bk_id"]
        unit_buckets = defaultdict(list)
        for relation in relation_map.get(book_id, []):
            tag = (relation.get("bv_tag") or "").strip() or "词汇"
            unit_buckets[tag].append(relation)

        units = []
        unit_entries = []
        for unit_name, items in unit_buckets.items():
            sorted_items = sorted(items, key=lambda item: int(item.get("bv_order") or 0))
            unit_order = min(int(item.get("bv_order") or 0) for item in sorted_items) if sorted_items else 0
            unit_entries.append((unit_name, unit_order, sorted_items))
        unit_entries.sort(key=lambda item: (item[1], item[0]))

        for unit_sort, (unit_name, _, items) in enumerate(unit_entries, start=1):
            unit_words = []
            seen_words = set()
            for relation in items:
                word_info = word_map.get(relation.get("bv_voc_id"))
                if not word_info:
                    continue
                word_text = (word_info.get("vc_vocabulary") or "").strip()
                if not word_text or word_text in seen_words:
                    continue
                seen_words.add(word_text)
                phonetic = (word_info.get("vc_phonetic_uk") or "").strip() or (word_info.get("vc_phonetic_us") or "").strip()
                meaning = translations.get(word_text, "")
                unit_words.append({
                    "word": word_text,
                    "phonetic": phonetic,
                    "meaning": meaning,
                    "exampleSentence": "",
                    "audioUrl": "",
                    "imageUrl": "",
                    "sortOrder": len(unit_words) + 1,
                })
            units.append({
                "name": unit_name,
                "sortOrder": unit_sort,
                "words": unit_words,
            })

        description_parts = []
        if book.get("bk_book"):
            description_parts.append(book["bk_book"].strip())
        if book.get("bk_comment"):
            description_parts.append(book["bk_comment"].strip())
        result["books"].append({
            "name": normalize_book_name(book.get("bk_name")),
            "coverUrl": "",
            "description": " ".join(part for part in description_parts if part),
            "sortOrder": book_index,
            "units": units,
        })

    os.makedirs(os.path.dirname(args.output), exist_ok=True)
    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False)

    total_units = sum(len(book["units"]) for book in result["books"])
    total_words = sum(len(unit["words"]) for book in result["books"] for unit in book["units"])
    print(json.dumps({
        "output": args.output,
        "books": len(result["books"]),
        "units": total_units,
        "words": total_words,
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
