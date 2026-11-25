# Plan: better hearing than Rambler (Bou Voice)

Status: plan, waiting for the owner's pick (2026-11-26).

## The limit today

Rambler is Google's closed speech model. It hears Bengali speech and writes Latin letters
(Banglish); Bou then guesses the Bengali word. Errors stack up: Rambler mishears, then the guess
from Latin letters can pick the wrong word. Bou cannot change how Rambler hears.

## Measured (FLEURS Bengali dev, 30 read sentences, CC BY 4.0)

- Gemini flash-lite listening to the audio directly, prompt "Bengali in Bengali script, English in
  Latin": 82.8% word accuracy (17.2% WER), about 4 s per sentence. Spelling variants such as
  জরুরী/জরুরি count as errors, so the real figure is higher.
- Published: mozilla-ai/whisper-large-v3-bn (Apache-2.0, used by Delwer's SilkMic on Mac):
  9.65% WER self-reported, offline, but a ~1 GB model.
- 99.99% is not reachable by any speech engine today; the best English engines make a few
  mistakes per 100 words.

## Options

1. **Bou Voice (online, recommended first).** Bou records the voice itself (Android
   AudioRecord inside the keyboard) and sends the audio to Gemini with the user's own key. Gemini
   writes Bengali in Bengali script and English in English in one step: no Banglish guessing.
   Rambler stays as the offline fallback. Cost: internet, ~2-4 s wait, the user's free key.
2. **Offline Bengali Whisper.** whisper.cpp on Android with a Bengali fine-tune (large-v3-bn or a
   smaller one). Bengali script directly, no internet. Risk: a large download and slow on
   mid-range phones; English words come out in Bengali script unless mapped back. Spike first on
   the owner's HONOR 200.
3. **Noise removal before hearing** (DeepFilterNet, as in SilkMic). Only with Bou Voice or
   Whisper (Rambler's mic path is closed). SilkMic's own notes say it may help or hurt; measure.

## SilkMic (Delwer Hossain, GPL-3.0, macOS Swift)

Not code we can drop into an Android keyboard, but reusable: the model choice
(whisper-large-v3-bn), the whisper.cpp route, and DeepFilterNet settings. Credit Delwer if used.

## Checklist for option 1

- [ ] Record button mode "Bou Voice" in the keyboard; mic permission is Gboard's own.
- [ ] Send 16 kHz mono audio to Gemini with the Bengali/English prompt; timeout and fallback.
- [ ] Phone test with the owner's own voice: standard, mixed English, Chittagong speech.
- [ ] Show locally, release only after the owner's OK.
