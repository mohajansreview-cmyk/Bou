# Plan: teach Bou far more Bangla and English words

Status: plan only, waiting for the owner's pick (2026-11-26).

## Why results are still wrong

- Rambler writes Bengali speech in Latin letters; Bou guesses the Bengali word from a lossy
  phonetic key plus a 50k-word frequency list. When the spelling Rambler uses is not one Bou has
  seen, it guesses, and the guess is often a wrong but real word (হয়েসে, গিথুব).
- Bou has no list of how real people actually spell Bengali words in Latin letters.

## Data that fixes this (free, licence fits GPL-3.0)

| Source | What it has | Licence |
|---|---|---|
| Google Dakshina (bn lexicon) | Bengali words with human-written Latin spellings, plus Wikipedia text | CC BY-SA 4.0 |
| AI4Bharat Aksharantar (bn) | Much larger Latin-to-Bengali word pairs, built on Dakshina | CC BY 4.0 |
| Bengali Wikipedia text | Word frequencies for modern written Bengali | CC BY-SA 4.0 |
| FrequencyWords en (already used) | English word list | CC BY-SA 4.0 |

Avoid: Vashantor and other non-commercial (NC) data.

## Options

1. **Recommended: real spelling pairs (offline).** Build an exact "Latin spelling -> Bengali word"
   table from Dakshina + Aksharantar, ranked by Wikipedia frequency; look it up before the
   phonetic guess. Add a Bangladesh word list (districts, upazilas, common names, bKash-style
   brands) that stays correct. Measure on the Dakshina test split before and after.
2. **Stronger AI mode (online).** Better Gemini model and show the offline text first, then
   replace it when the AI answer arrives. High accuracy, needs internet and the user's key.
3. **Own small model on the phone.** IndicXlit-style transliteration model (trained on
   Aksharantar) converted to run on Android. Best long-term, a large project (size, speed).
4. **Learn from the user.** When the user fixes a word right after dictation, remember it on the
   phone. Good later, needs careful IME work.

## Checklist for option 1

- [ ] Download Dakshina bn lexicon and Aksharantar bn; record licences and credits.
- [ ] Extend tools/build_banglish_data.py: exact pair table + frequency ranking; keep APK small.
- [ ] Lookup order: owner fixes -> exact pairs -> phonetic key -> letter-by-letter.
- [ ] Accuracy report: Dakshina test words, ONUBAD regional (must stay >= 81.8%), owner sample.
- [ ] Show results locally; release only after the owner's OK (one commit).

## Done locally (2026-11-26, not released)

Option 1 built: 131,884 real spellings (Aksharantar), 66k Bengali Wikipedia words, 95 Bangladesh
place spellings, leading "a" = অ. Dakshina held-out sentences: 60.1% -> 67.4% word accuracy;
ONUBAD regional 81.8% -> 81.7%. Local APK: output/Bou-1.4.11-local-worddata.apk (84.9 MB).

## Idea from the owner: Google AI Edge Gallery (on-device Gemma)

Google's AI Edge stack (MediaPipe LLM Inference) runs small Gemma models fully offline on the
phone. Could become an offline AI mode: fix whole sentences without internet or an API key.
Costs: model download of hundreds of MB, a few seconds per sentence on mid-range phones, memory
pressure inside the keyboard process. Try as a spike before promising it.
