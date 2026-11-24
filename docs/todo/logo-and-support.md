# Plan: Bou logo everywhere, and SupportKori

Status: plan only. Owner picks a logo concept first; nothing is drawn or changed until then.

## 1. Logo

Where it must appear (one source file, exported to every size):
- App launcher icon (adaptive icon: foreground + violet background, 108 dp, plus monochrome
  layer for Android 13 themed icons). Today the patched app still uses the PixelBoard icon.
- Settings header in the keyboard (Bou section) and the update notification small icon
  (white silhouette, 24 dp).
- Website: `src/app/icon.svg` (favicon), apple-touch icon 180 px, OpenGraph image, header mark.
- GitHub: repo social preview (1280x640) and README header.

Concepts to choose from (words first, images only after the owner picks):
1. **"ব" in a speech bubble**: the Bengali letter ব (first letter of বউ/বলো) inside a rounded
   chat bubble, violet gradient. Reads as "speak Bangla". Works at 16 px.
2. **Mic + ব**: a microphone whose body is the letter ব. Clear "voice typing" meaning.
3. **Sound wave to letters**: three wave bars turning into the letter ব. Modern, but weaker
   at favicon size.

Recommended: concept 1 (simplest shape, survives 16 px favicon and Android's round mask).

Steps after the pick:
- [ ] Draw as SVG (single colour + gradient version), check at 16, 24, 48, 192, 512 px.
- [ ] Android: add adaptive icon resources through a patch (the app resources come from the
      Gboard base, so the icon must be replaced by the resource patch, not a normal res folder).
- [ ] Website: replace `icon.svg`, add apple icon and OG image, deploy.
- [ ] GitHub: upload social preview (Settings, General, Social preview; owner does this).
- [ ] Keep "Gboard", "Google" and "Pixel" marks out of the logo (trademark).

## 2. Support the maker: SupportKori

Owner chose SupportKori (supportkori.com) instead of Buy Me a Coffee: Bangladeshi, bKash /
Nagad / card, 5% fee, free page at supportkori.com/<username>.

- [ ] Owner creates the page and sends the link (Claude does not create accounts).
- [ ] Website: "Bou-কে সাহায্য করুন" button on /contribute, /about and the footer.
- [ ] GitHub: `.github/FUNDING.yml` with `custom: ["https://www.supportkori.com/<username>"]`
      (adds a Sponsor button on the repo) and a line in README.
- [ ] App: one row in the Bou settings section that opens the page.
