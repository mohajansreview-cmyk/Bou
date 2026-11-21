# Pending: AI mode, branding and public release

Status: not started. Owner said "not now" on 2026-11-21. Do not implement until the owner says so.

## 1. AI mode (Claude-like quality)

Goal: Rambler output is rewritten by an LLM that understands the whole sentence, so Bengali
words come out in Bengali script, English words stay English, and regional speech is kept as
spoken.

Plan:
- Model: Gemini Flash-Lite via Google AI Studio, each user's own free API key (entered in
  PixelBoard settings, stored only on the phone, never shipped in the APK or repo).
- Prompt: "Convert Banglish to Bengali script. Keep English words in English. Keep regional
  dialect words exactly as spoken. Do not translate or rephrase."
- Flow: commit the offline conversion immediately (current `BanglishTransliterator`), then
  replace it with the AI result when it arrives, only if the user has not edited that text.
  Never block `GboardRamblerDictationRuntime.filterVoiceText` on the network.
- Fallback: no key, no internet, or timeout (~1.5 s) keeps the offline result.

Code starting points:
- `pixelboard-patches/extensions/extension/src/main/java/com/akshaykadam/pixelboard/extension/rambler/GboardRamblerDictationRuntime.java` (`convertBanglish`)
- `.../rambler/BanglishTransliterator.java`
- Settings UI: `.../settings/GboardPatchesSettingsFeatureRegistry.java`

Checklist:
- [ ] Spike: can the committed span be replaced later through Gboard's input connection?
- [ ] Settings screen: API key field + on/off switch
- [ ] Async request with timeout, no text logged
- [ ] Replace only if the field still holds the offline text
- [ ] Phone test on owner's HONOR 200

## 2. Branding and public release

Decided so far:
- Owner is leaning to the name **Bou** (keyboard), slogan "Bou: বলো, লেখা হয়ে যাবে।".
  Avoid jokes like "বউ কথা শোনে". Do not claim regional support until phone tests pass.
- Names checked and taken: Kontho, Kotha, Ekushey, Borno, Akkhor, Bolo, Bolchi.

Open decisions:
- [ ] Final name (Bou / Gola / other)
- [ ] Distribution: APK on GitHub Releases, patch-only (Morphe Manager), or both (recommended: both,
      patch guide as the main path so the app survives a DMCA takedown of the APK)
- [ ] Vercel download site in Bangla with a step-by-step patch guide

Legal must-keeps (GPL-3.0):
- Keep `LICENSE`, Akshay Kadam's copyright and credits, and state our changes.
- No "Google", "Gboard" or "Pixel" in the app name or logo.
- Keep data credits: FrequencyWords (CC BY-SA 4.0), ONUBAD (CC BY 4.0).

## 3. More regional data

- [ ] Ask RegSpeech12 authors (BRAC University, Md. Rezuwan Hassan, Farig Sadeque) for permission
      to use their 12-region transcripts (Kaggle license shows "Unknown").
- [ ] Noakhali, Rajshahi, Rangpur, Mymensingh still need a licensed source.
- Do not use Vashantor / Unified-Bangla: non-commercial licence conflicts with GPL.

Current measured quality: 81.8% word accuracy on 2,940 romanized ONUBAD dialect sentences.
