"""Generate BanglishWordData.java from word-frequency lists.

Usage:
    python tools/build_banglish_data.py bn_50k.txt en_50k.txt

Inputs are the 2018 OpenSubtitles frequency lists from
https://github.com/hermitdave/FrequencyWords (content licensed CC-BY-SA-4.0):
content/2018/bn/bn_50k.txt and content/2018/en/en_50k.txt.

Each Bengali word is reduced to a lossy phonetic key that matches how the same word is
spelled in Banglish (see BanglishTransliterator.latinKey, which must stay in sync with
norm() below). The most frequent Bengali word wins each key.
"""
import os
import re
import sys

OUT = os.path.join(
    os.path.dirname(os.path.abspath(__file__)), "..", "pixelboard-patches", "extensions",
    "extension", "src", "main", "java", "com", "akshaykadam", "pixelboard", "extension",
    "rambler", "BanglishWordData.java")

BN = {}
for chars, v in [("অ", "o"), ("আা", "a"), ("ইঈিী", "i"), ("উঊুূ", "u"), ("ঋৃ", "ri"),
                 ("এে", "e"), ("ঐৈ", "oi"), ("ওো", "o"), ("ঔৌ", "ou"),
                 ("ক", "k"), ("খ", "kh"), ("গ", "g"), ("ঘ", "gh"), ("ঙং", "ng"), ("চছ", "c"),
                 ("জযঝ", "j"), ("ঞ", "n"), ("টত", "t"), ("ঠথ", "th"), ("ডদ", "d"),
                 ("ঢধ", "dh"), ("ণন", "n"), ("প", "p"), ("ফ", "ph"), ("ব", "b"), ("ভ", "bh"),
                 ("ম", "m"), ("র\u09dc\u09dd", "r"), ("ল", "l"), ("শষস", "s"), ("হ", "h"),
                 ("\u09df", "y"), ("ৎ", "t")]:
    for ch in chars:
        BN[ch] = v
VOWEL_SIGNS = set("ািীুূৃেৈোৌ")
CONS = set("কখগঘঙচছজঝঞটঠডঢণতথদধনপফবভমযরলশষসহ\u09dc\u09dd\u09df")


def norm(s):
    s = s.replace("o", "").replace("w", "")  # "o" is the unreliable inherent vowel
    return re.sub(r"(.)\1+", r"\1", s)  # then doubled letters collapse (phelolam -> phelam)


def bn_key(word):
    w = (word.replace("\u09af\u09bc", "\u09df").replace("\u09a1\u09bc", "\u09dc")
         .replace("\u09a2\u09bc", "\u09dd").replace("ক্ষ", "খ").replace("জ্ঞ", "গ"))
    out = []
    skip = set()
    for i, ch in enumerate(w):
        if ch == "্" or i in skip:
            continue
        nxt = w[i + 1] if i + 1 < len(w) else ""
        if ch == "য" and i > 0 and w[i - 1] == "্":
            if i == 2:  # ya-phala on the first consonant sounds like "e" (byatha -> betha)
                out.append("e")
                if nxt == "া":
                    skip.add(i + 1)
            continue  # elsewhere ya-phala doubles the consonant, which keys collapse
        out.append(BN.get(ch, ""))
        if ch in CONS and nxt and nxt not in VOWEL_SIGNS and nxt != "্" and nxt not in "ঁং":
            out.append("o")
    return norm("".join(out))


def load(path):
    with open(path, encoding="utf-8") as f:
        for rank, line in enumerate(f):
            parts = line.split()
            if len(parts) == 2:
                yield parts[0], rank


def chunks(lines, limit=12000):
    buf, size = [], 0
    for line in lines:
        if size + len(line) > limit and buf:
            yield "\n".join(buf)
            buf, size = [], 0
        buf.append(line)
        size += len(line) + 1
    if buf:
        yield "\n".join(buf)


def java_literal(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n") + '"'


MAX_CANDIDATES = 4
REGIONAL_RANK_BASE = 50000  # after the 50k standard words
WIKI_RANK_BASE = 60000  # after the regional words


def main(bn_path, en_path):
    # Up to MAX_CANDIDATES words per key, most frequent first, so the converter can prefer the
    # spelling whose ending matches the Banglish (ekhono -> \u098f\u0996\u09a8\u09cb over \u098f\u0996\u09a8).
    by_key = {}
    for word, rank in load(bn_path):
        if not re.fullmatch(r"[\u0980-\u09ff]+", word):
            continue
        key = bn_key(word)
        if key:
            cands = by_key.setdefault(key, [])
            if len(cands) < MAX_CANDIDATES:
                cands.append(f"{word}\t{rank}")
    # Regional words (tools/data/onubad_regional_words.txt) rank after every standard word, so a
    # standard spelling wins a shared key and regional spellings fill keys standard Bengali lacks.
    regional_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data",
                                 "onubad_regional_words.txt")
    regional = 0
    if os.path.exists(regional_path):
        with open(regional_path, encoding="utf-8") as f:
            words = [ln.split("\t")[0] for ln in f if ln.strip() and not ln.startswith("#")]
        for i, word in enumerate(words):
            key = bn_key(word)
            cands = by_key.setdefault(key, []) if key else None
            if cands is not None and len(cands) < MAX_CANDIDATES \
                    and not any(c.split("\t")[0] == word for c in cands):
                cands.append(f"{word}\t{REGIONAL_RANK_BASE + i}")
                regional += 1
    print(f"regional words added: {regional}")
    # Wikipedia words the conversational list lacks (place names, formal and compound words),
    # ranked after the regional words. tools/data/bn_wiki_extra.txt comes from the Bengali
    # Wikipedia text in Google Dakshina (CC BY-SA 4.0), words seen at least 5 times.
    wiki_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data", "bn_wiki_extra.txt")
    wiki = 0
    if os.path.exists(wiki_path):
        for i, (word, _) in enumerate(load(wiki_path)):
            key = bn_key(word)
            cands = by_key.setdefault(key, []) if key else None
            if cands is not None and len(cands) < MAX_CANDIDATES:
                cands.append(f"{word}	{WIKI_RANK_BASE + i}")
                wiki += 1
    print(f"wikipedia words added: {wiki}")
    bn_lines = [key + "\t" + "\t".join(c) for key, c in by_key.items()]
    en_lines = [f"{w}\t{r}" for w, r in load(en_path) if re.fullmatch(r"[a-z']+", w)]
    # Real Latin spellings people use for common Bengali words (tools/prepare_spelling_pairs.py).
    pairs_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data",
                              "spelling_pairs_bn.tsv")
    exact_lines = []
    if os.path.exists(pairs_path):
        with open(pairs_path, encoding="utf-8") as f:
            exact_lines = [ln.rstrip("\n") for ln in f if ln.strip() and not ln.startswith("#")]

    # Bangladesh place names (tools/data/bd_places.tsv): every spelling -> the Bengali name.
    places_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data", "bd_places.tsv")
    place_lines = []
    if os.path.exists(places_path):
        with open(places_path, encoding="utf-8") as f:
            for ln in f:
                if ln.strip() and not ln.startswith("#"):
                    spellings, bengali = ln.rstrip("\n").split("\t")
                    place_lines += [f"{s}\t{bengali}" for s in spellings.split("|")]

    parts = ["// GENERATED by tools/build_banglish_data.py - do not edit by hand.",
             "// Word frequencies: OpenSubtitles 2018 via github.com/hermitdave/FrequencyWords",
             "// (CC-BY-SA-4.0). Spelling pairs: AI4Bharat Aksharantar (CC BY 4.0), which",
             "// includes Google Dakshina (CC BY-SA 4.0).",
             "package com.akshaykadam.pixelboard.extension.rambler;", "",
             "final class BanglishWordData {", "    private BanglishWordData() {", "    }", ""]
    for name, lines in (("bengali", bn_lines), ("english", en_lines), ("exact", exact_lines), ("places", place_lines)):
        pieces = list(chunks(lines))
        parts.append(f"    /** {len(lines)} lines in {len(pieces)} chunks. */")
        parts.append(f"    static String[] {name}() {{")
        parts.append(f"        return new String[] {{")
        for p in pieces:
            parts.append(f"            {java_literal(p)},")
        parts.append("        };")
        parts.append("    }")
        parts.append("")
    parts.append("}")
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(parts) + "\n")
    print(f"bengali keys={len(bn_lines)} english={len(en_lines)} exact={len(exact_lines)} places={len(place_lines)} -> {os.path.normpath(OUT)}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
