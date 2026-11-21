# Bou keyboard launch: design

Date: 2026-11-21. Owner: Subrata Mohajan. Status: draft, waiting for owner approval.

## Intent

Bangladeshi people should be able to speak Bengali (with English words and regional speech
mixed in) into the keyboard's Rambler voice typing and get Bengali script for Bengali words and
English for English words. The app is free and launched under the owner's brand, **Bou**, with a
Bangla launch website that promotes Subrata Mohajan as the maker.

## Goals

- Rename the app to **Bou** (launcher name, settings screens, package id).
- Ship two ways to install: a ready APK, and a ReVanced-style patch bundle for Morphe Manager.
- Write down every release step so a release can be repeated without guessing.
- A Next.js launch website on a free Vercel address, in Bangla, with strong SEO, promoting
  Subrata Mohajan.
- Stay inside GPL-3.0 so nobody can get the app or site taken down on licence grounds.

## Non-goals

- AI mode (Gemini) — saved in `docs/todo/ai-mode-and-release.md`, not now.
- A custom domain — later; the site works on the Vercel subdomain first.
- Play Store listing.

## Anti-goals (failure even if it works)

- Relicensing the code as MIT or hiding the source after the APK is public (GPL breach, takedown risk).
- Using "Google", "Gboard" or "Pixel" in the app name, logo or site title.
- Claiming regional-dialect support on the site before phone tests pass.
- A sexist slogan around the name ("বউ কথা শোনে").

## Constraints

- **Licence (decided: comply with GPL-3.0).** Repo stays private during development. On release
  day the source becomes public (GPL requires source for anyone who gets the APK). `LICENSE`
  stays GPL-3.0. The app's About screen and the repo keep a one-line notice:
  "Based on PixelBoard by Akshay Kadam (GPL-3.0)". The website does not need upstream credit.
- Data credits stay in the repo README: FrequencyWords (CC BY-SA 4.0), ONUBAD (CC BY 4.0).
- Base app: Gboard 18.6.3.100 arm64 only (patches are tied to that version).
- Owner does not run commands; Claude builds, tests and deploys, and asks before anything
  public (making the repo public, publishing a release, deploying the site).

## Approaches considered

1. **Comply with GPL (chosen by owner).** Private now, source public at release, full Bou
   branding everywhere, small upstream notice inside the app. Lowest takedown risk.
2. Clean-room own keyboard (rejected): months of work and no Rambler voice typing.
3. Keep source private with public APK (rejected): GPL breach; upstream author could file a DMCA
   notice against both GitHub and Vercel.

## Design

### A. Rebrand to Bou (in this repo)
- `options.json`: `app_display_name` = `Bou`.
- Package id: change `GBOARD_PATCHED_PACKAGE_NAME` (patches `shared/Constants`) from
  `com.akshaykadam.pixelboard` to `com.subratamohajan.bou` and every place that derives from it.
  Existing PixelBoard installs must be uninstalled once (new app id).
- User-visible strings in settings that say "PixelBoard" → "Bou". Internal Java/Kotlin package
  names (`com.akshaykadam.pixelboard.*`) stay as they are: invisible to users, keeps history clean.
- About screen: "Bou by Subrata Mohajan · Based on PixelBoard by Akshay Kadam (GPL-3.0)".
- In-app updater stays removed (done).
- New signing key for Bou (kept out of git, path recorded in `LOCAL-NOTES.md`, backed up).

### B. Release (written to `docs/RELEASING.md`)
- Version tag `vX.Y.Z` on `main`.
- GitHub Release assets: `Bou-X.Y.Z.apk`, `patches-X.Y.Z.mpp`, `patches-bundle.json`.
- Morphe Manager: users add the raw URL of `patches-bundle.json` from the public repo as a patch
  source, pick stock Gboard 18.6.3.100, and patch on the phone.
- Step list covers: build, test, patch, sign, verify, tag, release, update website links.

### C. Launch website (new project)
- Folder `N:\bou-website`, new private GitHub repo `bou-website`, Next.js (App Router) +
  TypeScript + Tailwind, static export, deployed to Vercel at `bou-keyboard.vercel.app`
  (or the closest free name).
- Language: Bangla first, English summary. Pages: home (what it does, demo sentences, download),
  install guide (APK steps and Morphe patch steps with screenshots), FAQ, about the maker.
- Maker section: Subrata Mohajan with links to his portfolio and socials.
- Design process (owner asked for a non-generic look, 2026-11-21): `hallmark` first (anti-slop
  design system and `sloplint`), `design-taste-frontend` while building, `impeccable` audit
  before showing. Owner taste from the portfolio work: dark navy `#030816` with cyan accent,
  real product screenshots and demo text; no glassmorphism, neon glow, particles, fake metrics
  or stock photos.
- Chosen look (owner, 2026-11-21, mockup: claude.ai/code/artifact/b9bfe3e0-5657-43fe-8fde-f52fb0208caf):
  soft light gradient lavender `#F6F1FF` → `#ECE2FF` → peach `#FFF1E2`, deep violet text
  `#22104A`, violet gradient CTA `#7C3AED` → `#A855F7`, small mango dot `#F59E0B`.
  Fonts (all Google Fonts, OFL): headlines Anek Bangla 750 at 85% width, English/brand Plus
  Jakarta Sans, body Hind Siliguri. Buttons built with shadcn/ui `Button` in the real site.
  Hero demo card cycles real Banglish → Bengali examples (standard, mixed English, Chittagong).
- SEO: Bangla and English titles/descriptions, keywords such as "বাংলা ভয়েস টাইপিং",
  "Bangla voice typing keyboard", Open Graph and Twitter images, `sitemap.xml`, `robots.txt`,
  JSON-LD `SoftwareApplication` (author: Person Subrata Mohajan) and `FAQPage`, canonical URLs,
  fast static pages (Lighthouse SEO 100 target).
- Download buttons point to the GitHub "latest release" asset URLs. Until the repo is public they
  show "শীঘ্রই আসছে" (coming soon).

### D. Regional data
- Draft an email from Subrata to the RegSpeech12 authors asking permission; owner sends it.

## Order of work
1. RegSpeech12 email draft.
2. Rebrand (A) → build → phone test.
3. `docs/RELEASING.md` (B).
4. Website (C) → local preview → owner review → Vercel deploy (owner approval).
5. Release day (owner approval): repo public, tag, GitHub Release, site download links live.

## Left to the builder
- Exact Vercel subdomain (first free one of: bou-keyboard, boukeyboard, bou-bd).
- Site visual design, within: simple, fast, Bangla typography, works on cheap phones.
- Screenshot capture for the guide via adb from the owner's phone.
