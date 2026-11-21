# Contributing to Bou

Thank you for helping make Bangla voice typing better. Bou is made by
[Subrata Mohajan](https://subrata.fedardokan.com); every kind of help is welcome.

## The easiest way to help: report a wrong word

If Bou writes a Bengali word wrong, especially a regional word, open a
[wrong word report](https://github.com/mohajansreview-cmyk/Bou/issues/new?template=wrong-word.yml).
Tell us what you said, what Bou wrote and what it should have written. These reports grow the
regional word list for everyone.

## Other ways

- **Something broken?** [Report a bug](https://github.com/mohajansreview-cmyk/Bou/issues/new?template=bug.yml).
- **An idea?** [Suggest a feature](https://github.com/mohajansreview-cmyk/Bou/issues/new?template=feature.yml).
- **Questions, or want to talk first?** Use
  [Discussions](https://github.com/mohajansreview-cmyk/Bou/discussions), or contact Subrata through
  [subrata.fedardokan.com](https://subrata.fedardokan.com).

## Contributing code

1. Talk first for anything bigger than a small fix: open an issue or a discussion so we agree on
   the approach before you spend time.
2. Fork the repo and create a branch from `main` (`fix/...`, `feat/...`, `docs/...`).
3. Build and test as described in [docs/RELEASING.md](docs/RELEASING.md) (steps 1 to 3). Every test
   must pass. New logic needs a test, like `BanglishTransliteratorTest`.
4. Keep the change small and focused, one topic per pull request, and match the surrounding style.
5. Commit messages follow conventional commits: `feat:`, `fix:`, `docs:`, `chore:`, `refactor:`.
6. Open a pull request against `main` and fill in the template.

### Where things live

- Banglish to Bengali converter:
  `pixelboard-patches/extensions/extension/src/main/java/com/akshaykadam/pixelboard/extension/rambler/BanglishTransliterator.java`
- Word data generator: `tools/build_banglish_data.py` (never edit `BanglishWordData.java` by hand)
- Regional words: `tools/data/onubad_regional_words.txt`
- AI mode: `.../rambler/BouAiRewriter.java`
- Updates: `.../updater/`

### Word data and licences

Only add words from sources whose licence allows redistribution under GPL-3.0 (for example
CC BY or CC BY-SA), and add the credit to the README. Do not add data from non-commercial or
unlicensed datasets.

## Licence

By contributing you agree that your contribution is released under the
[GNU GPL v3.0](LICENSE), like the rest of Bou.

## Be kind

Be respectful and patient. Many people here are new to open source; help them learn.
