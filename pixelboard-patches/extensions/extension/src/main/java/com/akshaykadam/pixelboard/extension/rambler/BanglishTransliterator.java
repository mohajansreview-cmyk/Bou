package com.akshaykadam.pixelboard.extension.rambler;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Offline Banglish (romanized Bengali) to Bengali script converter.
 *
 * <p>Rambler dictation returns Bengali speech as Roman text ("ami office e jabo"). This class
 * turns the Bengali words into Bengali script and keeps English words in English
 * ("আমি office এ যাবো"), with no network access. Each Latin word is matched against a Bengali
 * and an English word-frequency list ({@link BanglishWordData}); words that could be either
 * follow the rest of the sentence. Unknown words (mostly names) fall back to Avro-style phonetic
 * rules. Bengali text, numbers, links and e-mail addresses pass through unchanged.</p>
 *
 * <p>Pure Java with no Android dependency so it can be unit-tested on the JVM.</p>
 */
public final class BanglishTransliterator {

    private static final String HASANTA = "্";

    /** Independent vowel / vowel sign (kar) pairs, keyed by the Latin spelling. */
    private static final Map<String, String[]> VOWELS = new HashMap<String, String[]>();
    /** Consonant spellings, longest keys are tried first. */
    private static final Map<String, String> CONSONANTS = new HashMap<String, String>();
    /** Latin consonant pairs ("prev|cur") that form a conjunct with a hasanta. */
    private static final Set<String> CLUSTERS = new HashSet<String>();
    /** Consonants that take a following "r" as r-phala (e.g. pr, tr, kr). */
    private static final Set<String> R_PHALA_BASES = new HashSet<String>();
    /** Whole-word overrides for common words the rules spell wrong. */
    static final Map<String, String> DICTIONARY;
    /** Words that are common in both languages; the rest of the sentence decides. */
    private static final Set<String> AMBIGUOUS = new HashSet<String>();
    /** Brand and app names that stay in English even inside a Bengali sentence. */
    private static final Set<String> KEEP_ENGLISH = new HashSet<String>();
    /** Short particles the frequency lists get wrong ("laptop ta" is ল্যাপটপটা, not তা). */
    private static final Map<String, String> FORCE_BENGALI = new HashMap<String, String>();
    /** English words ranked above this are function words, not loanwords. */
    private static final int FUNCTION_WORD_RANK = 300;
    /** Frequent English words that Bengali speakers do mix into Bengali sentences. */
    private static final Set<String> COMMON_LOANWORDS = new HashSet<String>(java.util.Arrays.asList(
            "time", "call", "phone", "ok", "okay", "sorry", "please", "thanks", "thank", "love",
            "good", "nice", "problem", "family", "school", "friend", "life", "money", "job", "work",
            "car", "home", "room", "game", "party", "team", "plan", "show", "boss", "sir", "madam",
            "hello", "hi", "bye", "baby", "mind", "chance", "doctor", "police", "office", "ready",
            "free", "fine", "busy", "class", "exam", "news", "story", "song", "movie", "picture",
            "minute", "hour", "week", "date", "number", "name", "message", "help", "sure", "best",
            "late", "new", "old", "big", "line", "start", "stop", "check", "open", "close", "next"));
    /** Ranks at or above this come from the regional word list (see build_banglish_data.py). */
    private static final int REGIONAL_RANK_BASE = 50000;
    /** A Bengali word must be this many times more frequent to beat an English reading. */
    private static final int BENGALI_RANK_FACTOR = 3;

    static {
        vowel("a", "আ", "া");
        vowel("aa", "আ", "া");
        vowel("i", "ই", "ি");
        vowel("ee", "ঈ", "ী");
        vowel("ii", "ঈ", "ী");
        vowel("u", "উ", "ু");
        vowel("oo", "উ", "ু");
        vowel("uu", "ঊ", "ূ");
        vowel("e", "এ", "ে");
        vowel("oi", "ঐ", "ৈ");
        vowel("ou", "ঔ", "ৌ");
        // "o": inherent vowel after a consonant (handled in convertWord).
        vowel("o", "ও", "ো");

        consonant("ksh", "ক্ষ");
        consonant("cch", "চ্ছ");
        consonant("chh", "ছ");
        consonant("kh", "খ");
        consonant("gh", "ঘ");
        consonant("ch", "চ");
        consonant("jh", "ঝ");
        consonant("th", "থ");
        consonant("dh", "ধ");
        consonant("ph", "ফ");
        consonant("bh", "ভ");
        consonant("sh", "শ");
        consonant("rh", "ঢ়");
        consonant("ng", "ং");
        consonant("k", "ক");
        consonant("g", "গ");
        consonant("c", "চ");
        consonant("j", "জ");
        consonant("z", "জ");
        consonant("t", "ত");
        consonant("d", "দ");
        consonant("n", "ন");
        consonant("p", "প");
        consonant("f", "ফ");
        consonant("b", "ব");
        consonant("v", "ভ");
        consonant("m", "ম");
        consonant("l", "ল");
        consonant("s", "স");
        consonant("h", "হ");
        consonant("q", "ক");
        consonant("x", "ক্স");
        consonant("r", "র");
        consonant("y", "য়");
        consonant("w", "ও");

        for (String pair : new String[] {
                "k|k", "g|g", "t|t", "d|d", "n|n", "m|m", "l|l", "p|p", "b|b", "s|s", "j|j",
                "ch|ch", "n|d", "n|t", "n|dh", "n|th", "s|t", "s|th", "s|k", "s|p", "s|n",
                "k|t", "p|t", "m|b", "m|p", "n|ch", "n|j", "k|l", "p|l", "b|l", "g|l", "f|l",
                "d|dh", "b|d", "t|m", "sh|t", "g|n"}) {
            CLUSTERS.add(pair);
        }
        for (String base : new String[] {
                "k", "g", "t", "d", "p", "b", "bh", "ph", "sh", "s", "dh", "gh", "kh", "th",
                "f", "v"}) {
            R_PHALA_BASES.add(base);
        }

        Map<String, String> d = new HashMap<String, String>();
        // Pronouns and people.
        put(d, "tomar", "তোমার"); put(d, "tomra", "তোমরা"); put(d, "tomake", "তোমাকে");
        put(d, "tomader", "তোমাদের"); put(d, "tor", "তোর"); put(d, "toke", "তোকে");
        put(d, "tora", "তোরা"); put(d, "oi", "ওই"); put(d, "o", "ও"); put(d, "ora", "ওরা");
        put(d, "oder", "ওদের"); put(d, "or", "ওর"); put(d, "oke", "ওকে");
        put(d, "she", "সে"); put(d, "shey", "সে"); put(d, "okhane", "ওখানে");
        put(d, "shekhane", "সেখানে"); put(d, "jekhane", "যেখানে");
        // Question words and particles.
        put(d, "keno", "কেন"); put(d, "kono", "কোনো"); put(d, "kothay", "কোথায়");
        put(d, "kotha", "কথা"); put(d, "koto", "কত"); put(d, "kotokhon", "কতক্ষণ");
        put(d, "ha", "হ্যাঁ"); put(d, "haa", "হ্যাঁ"); put(d, "hae", "হ্যাঁ");
        put(d, "hya", "হ্যাঁ"); put(d, "hmm", "হুম"); put(d, "accha", "আচ্ছা");
        put(d, "acha", "আচ্ছা"); put(d, "achha", "আচ্ছা"); put(d, "jonno", "জন্য");
        put(d, "thik", "ঠিক"); put(d, "shotti", "সত্যি"); put(d, "sotti", "সত্যি");
        put(d, "shob", "সব"); put(d, "sob", "সব"); put(d, "shobai", "সবাই");
        put(d, "sobai", "সবাই"); put(d, "sudhu", "শুধু"); put(d, "besi", "বেশি");
        put(d, "ektu", "একটু"); put(d, "ekta", "একটা"); put(d, "eta", "এটা");
        put(d, "ota", "ওটা"); put(d, "seta", "সেটা"); put(d, "sheta", "সেটা");
        put(d, "eita", "এইটা"); put(d, "oita", "ওইটা"); put(d, "jeta", "যেটা");
        put(d, "duita", "দুইটা"); put(d, "tahole", "তাহলে");
        // "j" words written with য.
        put(d, "ja", "যা"); put(d, "jabo", "যাবো"); put(d, "jao", "যাও"); put(d, "jai", "যাই");
        put(d, "jay", "যায়"); put(d, "jete", "যেতে"); put(d, "jodi", "যদি");
        put(d, "jokhon", "যখন"); put(d, "je", "যে"); put(d, "jemon", "যেমন");
        put(d, "jacchi", "যাচ্ছি"); put(d, "jaccho", "যাচ্ছো"); put(d, "jacche", "যাচ্ছে");
        put(d, "jachi", "যাচ্ছি"); put(d, "jache", "যাচ্ছে");
        // "ch" words written with ছ.
        put(d, "ache", "আছে"); put(d, "achi", "আছি"); put(d, "acho", "আছো");
        put(d, "achen", "আছেন"); put(d, "achis", "আছিস"); put(d, "chilo", "ছিল");
        put(d, "chilam", "ছিলাম"); put(d, "chile", "ছিলে"); put(d, "kichu", "কিছু");
        put(d, "chele", "ছেলে"); put(d, "chobi", "ছবি"); put(d, "chuti", "ছুটি");
        put(d, "choto", "ছোট"); put(d, "kache", "কাছে"); put(d, "pichone", "পিছনে");
        put(d, "mach", "মাছ"); put(d, "maach", "মাছ"); put(d, "choy", "ছয়");
        put(d, "hoyeche", "হয়েছে"); put(d, "geche", "গেছে"); put(d, "gechi", "গেছি");
        put(d, "hoche", "হচ্ছে"); put(d, "bujhso", "বুঝছো");
        // ট / ঠ / ড / ড় / ঢ words.
        put(d, "taka", "টাকা"); put(d, "tk", "টাকা"); put(d, "aat", "আট");
        put(d, "uthbo", "উঠবো"); put(d, "uthe", "উঠে"); put(d, "otho", "ওঠো");
        put(d, "pathabo", "পাঠাবো"); put(d, "pathao", "পাঠাও"); put(d, "pathiye", "পাঠিয়ে");
        put(d, "boro", "বড়"); put(d, "bari", "বাড়ি"); put(d, "barite", "বাড়িতে");
        put(d, "gari", "গাড়ি"); put(d, "pora", "পড়া"); put(d, "pori", "পড়ি");
        put(d, "porte", "পড়তে"); put(d, "porbo", "পড়বো"); put(d, "porashona", "পড়াশোনা");
        put(d, "dhaka", "ঢাকা"); put(d, "daktar", "ডাক্তার");
        // Common words and greetings.
        put(d, "bhalobashi", "ভালোবাসি"); put(d, "valobashi", "ভালোবাসি");
        put(d, "bhalobasi", "ভালোবাসি"); put(d, "bhalobasha", "ভালোবাসা");
        put(d, "dhonnobad", "ধন্যবাদ"); put(d, "dhonnobaad", "ধন্যবাদ");
        put(d, "shondha", "সন্ধ্যা"); put(d, "shomoy", "সময়");
        put(d, "manush", "মানুষ"); put(d, "jinish", "জিনিস"); put(d, "eid", "ঈদ");
        put(d, "pach", "পাঁচ"); put(d, "panch", "পাঁচ"); put(d, "chad", "চাঁদ");
        put(d, "shon", "শোন");
        // Dhaka past tense written with "s": disi = দিছি (gave), not দিচ্ছি (giving).
        put(d, "disi", "দিছি"); put(d, "dise", "দিছে"); put(d, "diso", "দিছো");
        put(d, "nisi", "নিছি"); put(d, "nise", "নিছে"); put(d, "niso", "নিছো"); put(d, "deo", "দেও"); put(d, "dokan", "দোকান"); put(d, "ki", "কি");
        // Regional speech (Chittagong, Sylhet, Noakhali, Barishal, Dhaka, North Bengal).
        put(d, "aai", "আঁই"); put(d, "tuy", "তুঁই"); put(d, "kenge", "কেঙ্গে");
        put(d, "kenne", "কেন্নে"); put(d, "kene", "কেনে"); put(d, "kodde", "কোদ্দে");
        put(d, "hitara", "হিতারা"); put(d, "hite", "হিতে"); put(d, "hiba", "হিবা");
        put(d, "gom", "গম"); put(d, "hoir", "হইর"); put(d, "hoiyum", "হইয়ুম");
        put(d, "hoijjum", "হইজ্জুম"); put(d, "gorir", "গরির"); put(d, "jaibi", "যাইবি");
        put(d, "jaiyum", "যাইয়ুম"); put(d, "jaim", "যাইম"); put(d, "asos", "আছস");
        put(d, "asoni", "আছোনি"); put(d, "kiyolla", "কিয়োল্লা"); put(d, "kiyalla", "কিয়াল্লা");
        put(d, "kitte", "কিত্তে"); put(d, "beggun", "বেগ্গুন"); put(d, "beggune", "বেগ্গুনে");
        put(d, "ekkan", "এক্কান"); put(d, "ugga", "উগ্গা"); put(d, "fua", "ফুয়া");
        put(d, "kita", "কিতা"); put(d, "kunta", "কুনটা"); put(d, "kunu", "কুনু");
        put(d, "kun", "কুন"); put(d, "kitar", "কিতার"); put(d, "jairam", "যাইরাম");
        put(d, "jaitam", "যাইতাম"); put(d, "koiram", "কইরাম"); put(d, "khairam", "খাইরাম");
        put(d, "oibo", "অইবো"); put(d, "bala", "বালা"); put(d, "bhala", "ভালা");
        put(d, "afne", "আফনে"); put(d, "afnar", "আফনার"); put(d, "afnare", "আফনারে");
        put(d, "furi", "ফুড়ি"); put(d, "hetere", "হেতেরে"); put(d, "hete", "হেতে");
        put(d, "hetara", "হেতারা"); put(d, "kitta", "কিত্তা"); put(d, "koros", "করোস");
        put(d, "koilam", "কইলাম"); put(d, "koilo", "কইলো"); put(d, "mui", "মুই");
        put(d, "mor", "মোর"); put(d, "tomgo", "তোমগো"); put(d, "amgo", "আমগো");
        put(d, "jaimu", "যাইমু"); put(d, "khaimu", "খাইমু"); put(d, "kormu", "করমু");
        put(d, "dimu", "দিমু"); put(d, "nimu", "নিমু"); put(d, "jamu", "যামু");
        put(d, "khamu", "খামু"); put(d, "asmu", "আসমু"); put(d, "kortesi", "করতেছি");
        put(d, "kortaso", "করতাছো"); put(d, "kortesos", "করতেছস"); put(d, "kortesen", "করতেছেন");
        put(d, "jaitesi", "যাইতেছি"); put(d, "khaitesi", "খাইতেছি"); put(d, "achos", "আছস");
        put(d, "korsos", "করছস"); put(d, "geso", "গেছো"); put(d, "gesi", "গেছি");
        put(d, "gese", "গেছে"); put(d, "aisi", "আইছি"); put(d, "aiso", "আইছো");
        put(d, "aise", "আইছে"); put(d, "kemne", "কেমনে"); put(d, "hoise", "হইছে");
        put(d, "hoiche", "হইছে"); put(d, "hoiso", "হইছো"); put(d, "koiso", "কইছো");
        put(d, "koisi", "কইছি"); put(d, "koise", "কইছে"); put(d, "hamra", "হামরা");
        put(d, "hamar", "হামার"); put(d, "tore", "তোরে");
        DICTIONARY = Collections.unmodifiableMap(d);

        for (String w : new String[] {
                // Only words that are common in English too; rarer English look-alikes
                // (pore, hoy, bole, mane...) are settled by frequency and come out Bengali.
                "to", "a", "die", "hate", "more", "bad", "man", "tar", "ar", "kal", "ma", "re",
                "boy", "tin", "sat", "tore", "she", "se", "or", "oke", "jai", "ja", "je", "hae", "ha"}) {
            AMBIGUOUS.add(w);
        }
        for (String w : new String[] {
                "bkash", "nagad", "imo", "whatsapp", "messenger", "gmail", "youtube", "facebook",
                "ok", "okay"}) {
            KEEP_ENGLISH.add(w);
        }
        FORCE_BENGALI.put("ta", "টা");
        FORCE_BENGALI.put("ti", "টি");
        FORCE_BENGALI.put("tai", "তাই");
        FORCE_BENGALI.put("e", "এ");
        FORCE_BENGALI.put("o", "ও");
        FORCE_BENGALI.put("dhaka", "ঢাকা");
    }

    private static volatile Map<String, Object[][]> bengaliByKey; // key -> {{word, rank}, ...}
    private static volatile Map<String, Integer> englishRank;

    private BanglishTransliterator() {
    }

    private static void vowel(String key, String independent, String sign) {
        VOWELS.put(key, new String[] {independent, sign});
    }

    private static void consonant(String key, String value) {
        CONSONANTS.put(key, value);
    }

    private static void put(Map<String, String> map, String key, String value) {
        map.put(key, value);
    }

    /** True when the text has Latin letters, i.e. something {@link #convert} would change. */
    public static boolean needsConversion(String text) {
        if (text == null) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (isLatin(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /** True when the text contains any Bengali script character. */
    public static boolean containsBengali(CharSequence text) {
        if (text == null) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 'ঀ' && c <= '৿') {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts the Bengali words in {@code text} to Bengali script and keeps English words.
     * Everything that is not a Latin word is kept as is.
     */
    public static String convert(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // Pass 1: split into whitespace-free chunks and decide every Latin word.
        List<String> chunks = new ArrayList<String>();
        List<List<Word>> chunkWords = new ArrayList<List<Word>>();
        int bengaliVotes = 0;
        int englishVotes = 0;
        int i = 0;
        int n = text.length();
        while (i < n) {
            int end = i;
            boolean space = Character.isWhitespace(text.charAt(i));
            while (end < n && Character.isWhitespace(text.charAt(end)) == space) {
                end++;
            }
            String chunk = text.substring(i, end);
            List<Word> words = space || isProtectedChunk(chunk) ? null : splitWords(chunk);
            if (words != null) {
                for (Word w : words) {
                    if (w.decision == Decision.BENGALI) {
                        bengaliVotes++;
                    } else if (w.decision == Decision.ENGLISH) {
                        englishVotes++;
                    }
                }
            }
            chunks.add(chunk);
            chunkWords.add(words);
            i = end;
        }
        // Pass 2: an ambiguous word follows its nearest decided neighbours ("done hoy nai": the
        // Bengali "nai" makes "hoy" Bengali); with no decided neighbour it follows the sentence,
        // and ties go to Bengali.
        List<Word> all = new ArrayList<Word>();
        for (List<Word> words : chunkWords) {
            if (words != null) {
                all.addAll(words);
            }
        }
        boolean bengaliSentence = bengaliVotes >= englishVotes;
        for (int k = 0; k < all.size(); k++) {
            Word w = all.get(k);
            if (w.decision != Decision.AMBIGUOUS) {
                w.useBengali = w.decision == Decision.BENGALI;
                continue;
            }
            Decision left = nearestDecided(all, k, -1);
            Decision right = nearestDecided(all, k, 1);
            if (w.leanBengali) {
                boolean englishBothSides = left == Decision.ENGLISH && right == Decision.ENGLISH;
                boolean englishOnlySentence = left != Decision.BENGALI && right != Decision.BENGALI
                        && !bengaliSentence;
                w.useBengali = !englishBothSides && !englishOnlySentence;
            } else if (w.strictContext) {
                w.useBengali = left != Decision.ENGLISH && right != Decision.ENGLISH
                        && (left == Decision.BENGALI || right == Decision.BENGALI);
            } else if (left == Decision.BENGALI || right == Decision.BENGALI) {
                w.useBengali = true;
            } else if (left == Decision.ENGLISH || right == Decision.ENGLISH) {
                w.useBengali = false;
            } else {
                w.useBengali = bengaliSentence;
            }
        }
        StringBuilder out = new StringBuilder(text.length() + 8);
        for (int c = 0; c < chunks.size(); c++) {
            List<Word> words = chunkWords.get(c);
            if (words == null) {
                out.append(chunks.get(c));
                continue;
            }
            String chunk = chunks.get(c);
            int pos = 0;
            boolean lastWordBengali = false;
            for (Word w : words) {
                out.append(chunk, pos, w.start);
                boolean bengali = w.useBengali;
                out.append(bengali ? w.bengali : w.latin);
                // "Dr." of a short unknown word keeps its dot; "aniyo." ends a Bengali sentence.
                lastWordBengali = bengali && (w.known || w.latin.length() > 3);
                pos = w.start + w.latin.length();
            }
            String tail = chunk.substring(pos);
            out.append(lastWordBengali && tail.equals(".") ? "।" : tail);
        }
        // One canonical form: the word lists mix precomposed and decomposed য় / ড় / ঢ়.
        return Normalizer.normalize(out, Normalizer.Form.NFC);
    }

    private enum Decision { BENGALI, ENGLISH, AMBIGUOUS }

    /** First non-ambiguous decision walking from {@code k} in direction {@code step}, or null. */
    private static Decision nearestDecided(List<Word> words, int k, int step) {
        for (int j = k + step; j >= 0 && j < words.size(); j += step) {
            Word other = words.get(j);
            if (other.decision != Decision.AMBIGUOUS) {
                return other.decision;
            }
            if (other.strictContext) {
                return Decision.ENGLISH; // a common English word leans English ("I hate you")
            }
        }
        return null;
    }

    private static final class Word {
        final int start;
        final String latin;
        final Decision decision;
        final String bengali;
        /** False when neither word list knows the word and it was spelled phonetically. */
        final boolean known;
        /** Final choice, set in pass 2 of {@link #convert}. */
        boolean useBengali;
        /** Bengali only when no neighbour is English (common English words like "you", "done"). */
        boolean strictContext;
        /** Bengali unless both neighbours are English (unknown words, mostly Bengali speech). */
        boolean leanBengali;

        Word(int start, String latin, Decision decision, String bengali) {
            this(start, latin, decision, bengali, true);
        }

        Word(int start, String latin, Decision decision, String bengali, boolean known) {
            this.start = start;
            this.latin = latin;
            this.decision = decision;
            this.bengali = bengali;
            this.known = known;
        }
    }

    /** Links, e-mails, hyphenated words (e-mail) and letter+digit codes are never converted. */
    private static boolean isProtectedChunk(String chunk) {
        return chunk.indexOf('@') >= 0 || chunk.indexOf('/') >= 0
                || chunk.matches(".*[A-Za-z]\\.[A-Za-z].*")
                || chunk.matches(".*[A-Za-z]-[A-Za-z].*")
                || chunk.matches(".*([A-Za-z][0-9]|[0-9][A-Za-z]).*");
    }

    private static List<Word> splitWords(String chunk) {
        List<Word> words = new ArrayList<Word>();
        int i = 0;
        int n = chunk.length();
        while (i < n) {
            if (!isLatin(chunk.charAt(i))) {
                i++;
                continue;
            }
            int end = i;
            while (end < n && isLatin(chunk.charAt(end))) {
                end++;
            }
            words.add(decide(i, chunk.substring(i, end)));
            i = end;
        }
        return words;
    }

    private static Word decide(int start, String latin) {
        String w = latin.toLowerCase(Locale.ROOT);
        if (KEEP_ENGLISH.contains(w) || isAcronym(latin)) {
            return new Word(start, latin, Decision.ENGLISH, null);
        }
        String forced = FORCE_BENGALI.get(w);
        if (forced != null) {
            return new Word(start, latin, Decision.BENGALI, forced);
        }
        ensureLoaded();
        Integer en = englishRank.get(w);
        String known = DICTIONARY.get(w);
        if (known != null) {
            return new Word(start, latin,
                    AMBIGUOUS.contains(w) ? Decision.AMBIGUOUS : Decision.BENGALI, known);
        }
        Object[] bn = lookupBengali(w, en == null);
        Object[] dialect = en == null ? lookupDhakaDialect(w) : null;
        // The ছ reading wins unless the literal "s" word is far more common (20x).
        if (dialect != null && (bn == null || (Integer) bn[1] * 20 > (Integer) dialect[1])) {
            bn = dialect;
        }
        // A regional Bengali word that is also a less common English word ("lay" = লায়)
        // follows its neighbours instead of always losing to English.
        boolean regionalVsEnglish = bn != null && en != null
                && (Integer) bn[1] >= REGIONAL_RANK_BASE && en > 1000;
        // Very common English words ("are", "he", "no") are rarely mixed into Bengali speech,
        // so when one has a Bengali reading (আরে, হে, নো) its neighbours decide. Loanwords people
        // do mix in (time, phone, call...) are listed in COMMON_LOANWORDS and stay English.
        boolean functionWord = bn != null && en != null && en < FUNCTION_WORD_RANK
                && !COMMON_LOANWORDS.contains(w);
        if (functionWord && !AMBIGUOUS.contains(w) && !regionalVsEnglish) {
            Word word = new Word(start, latin, Decision.AMBIGUOUS, (String) bn[0]);
            word.strictContext = true; // "Thank you bhai" keeps "you"
            return word;
        }
        if (bn != null && en != null && (AMBIGUOUS.contains(w) || regionalVsEnglish)) {
            return new Word(start, latin, Decision.AMBIGUOUS, (String) bn[0]);
        }
        if (bn != null && (en == null || (Integer) bn[1] * BENGALI_RANK_FACTOR < en)) {
            return new Word(start, latin, Decision.BENGALI, (String) bn[0]);
        }
        if (en != null) {
            return new Word(start, latin, Decision.ENGLISH, null);
        }
        // Unknown to both lists (mostly names): spelled phonetically inside a Bengali sentence,
        // kept as written inside an English one.
        Word unknown = new Word(start, latin, Decision.AMBIGUOUS, convertWord(w), false);
        unknown.leanBengali = true; // "check kortesilam": unknown words are usually Bengali
        return unknown;
    }

    /**
     * Exact key first; for words that are not English, also allow Banglish "a" for a silent
     * inherent vowel (subrata).
     */
    private static Object[] lookupBengali(String w, boolean allowLooseVowels) {
        String key = latinKey(w);
        Object[] best = pick(bengaliByKey.get(key), w);
        if (best != null || !allowLooseVowels) {
            return best;
        }
        List<Integer> as = new ArrayList<Integer>();
        for (int i = 1; i < key.length() && as.size() < 3; i++) {
            if (key.charAt(i) == 'a') {
                as.add(i);
            }
        }
        for (int mask = 1; mask < (1 << as.size()); mask++) {
            StringBuilder k = new StringBuilder(key.length());
            for (int i = 0; i < key.length(); i++) {
                int bit = as.indexOf(i);
                if (bit < 0 || (mask & (1 << bit)) == 0) {
                    k.append(key.charAt(i));
                }
            }
            Object[] cand = pick(bengaliByKey.get(k.toString()), w);
            if (cand != null && (best == null || (Integer) cand[1] < (Integer) best[1])) {
                best = cand;
            }
        }
        return best;
    }

    /** A spelling with ো may win if it is at most this many ranks behind the most frequent. */
    private static final int O_ENDING_RANK_FACTOR = 7;

    /**
     * Chooses among the words sharing a key: the most frequent, unless the Banglish ends in "o"
     * after a consonant and a ো spelling is close behind (ekhono -> এখনো, holo -> হলো).
     * Returns {word, rank}; the rank is the best candidate's, for English comparisons.
     */
    private static Object[] pick(Object[][] candidates, String w) {
        if (candidates == null || candidates.length == 0) {
            return null;
        }
        Object[] best = null;
        for (Object[] c : candidates) {
            if (endingMatches(w, (String) c[0])) {
                best = c;
                break;
            }
        }
        if (best == null) {
            return null; // e.g. hitara must not become হিটার, hoir must not become হিরো
        }
        int n = w.length();
        boolean endsWithO = n >= 2 && w.charAt(n - 1) == 'o' && !isVowel(w.charAt(n - 2));
        if (endsWithO && !((String) best[0]).endsWith("ো")) {
            int limit = (Integer) best[1] * O_ENDING_RANK_FACTOR + O_ENDING_RANK_FACTOR;
            for (Object[] c : candidates) {
                if (((String) c[0]).endsWith("ো") && (Integer) c[1] <= limit
                        && endingMatches(w, (String) c[0])) {
                    return new Object[] {c[0], best[1]};
                }
            }
        }
        return best;
    }

    /**
     * Banglish keeps a word's last sound: a final a/e/i/u needs the same vowel at the end of the
     * Bengali word, and a final consonant needs a Bengali word without a final vowel sign.
     * A final "o" matches either (kemon / bolo).
     */
    static boolean endingMatches(String latin, String bengali) {
        // The first sound must agree too: a Banglish vowel start (obay) needs a Bengali vowel
        // start (ওবায়), a consonant start needs a consonant.
        boolean latinVowelStart = isVowel(latin.charAt(0));
        boolean bengaliVowelStart = "অআইঈউঊঋএঐওঔ".indexOf(bengali.charAt(0)) >= 0;
        if (latinVowelStart != bengaliVowelStart) {
            return false;
        }
        char l = latin.charAt(latin.length() - 1);
        char b = bengali.charAt(bengali.length() - 1);
        switch (l) {
            case 'a':
                return b == 'া' || b == 'আ';
            case 'e':
                return b == 'ে' || b == 'এ';
            case 'i':
                return b == 'ি' || b == 'ী' || b == 'ই' || b == 'ঈ';
            case 'u':
                return b == 'ু' || b == 'ূ' || b == 'উ' || b == 'ঊ';
            case 'o':
                return true;
            case 'w':
                return b == 'ো' || b == 'ও'; // kow -> কও
            default:
                return "ািীুূৃেৈোৌআইঈউঊএঐওঔ".indexOf(b) < 0;
        }
    }

    private static boolean isVowel(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
    }

    /** Dhaka speech writes ছ as "s" in verb endings: bolsi -> বলছি, khaiso -> খাইছো. */
    private static Object[] lookupDhakaDialect(String w) {
        java.util.regex.Matcher m = DHAKA_ENDING.matcher(w);
        if (!m.matches()) {
            return null;
        }
        return lookupBengali(m.group(1) + "ch" + m.group(2), true);
    }

    private static final java.util.regex.Pattern DHAKA_ENDING =
            // After a consonant (bolsi), "ai" (khaiso) or consonant+i (disi -> দিছি); not after
            // other vowels: "asi" is আসি.
            java.util.regex.Pattern.compile(
                    "(.*(?:[^aeiou]|ai|[^aeiou]i))s(i|o|e|en|is|ilam|ilo|ile|ila)");

    /**
     * Lossy phonetic key of a Banglish word. Must stay in sync with bn_key/norm in
     * tools/build_banglish_data.py, which builds the Bengali side of the same keys.
     */
    static String latinKey(String word) {
        String w = word.toLowerCase(Locale.ROOT)
                .replaceFirst("^([bcdfghjklmnpqrstvz]h?)y[ae]", "$1e")
                .replaceFirst("^y", "j") // yodi -> যদি (য keys as j)
                .replace("ksh", "kh") // ক্ষ keys as খ
                .replace("chh", "c").replace("ch", "c").replace("sh", "s").replace("f", "ph")
                .replace("v", "bh").replace("z", "j").replace("q", "k").replace("x", "ks")
                .replace("ee", "i").replace("oo", "u").replace("aa", "a");
        w = w.replace("o", "").replace("w", ""); // "o" is the unreliable inherent vowel
        StringBuilder out = new StringBuilder(w.length());
        for (int i = 0; i < w.length(); i++) {
            char c = w.charAt(i);
            if (out.length() > 0 && out.charAt(out.length() - 1) == c) {
                continue; // then doubled letters collapse (fellam -> phelam)
            }
            out.append(c);
        }
        return out.toString();
    }

    private static volatile boolean preloadStarted;

    /** Loads the word lists on a background thread so the first dictation does not wait. */
    public static void preloadAsync() {
        if (preloadStarted || englishRank != null) {
            return;
        }
        preloadStarted = true;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                ensureLoaded();
            }
        }, "PixelBoardBanglishPreload");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private static void ensureLoaded() {
        if (englishRank != null) {
            return;
        }
        synchronized (BanglishTransliterator.class) {
            if (englishRank != null) {
                return;
            }
            Map<String, Object[][]> bn = new HashMap<String, Object[][]>(65536);
            for (String chunk : BanglishWordData.bengali()) {
                for (String line : chunk.split("\n")) {
                    String[] p = line.split("\t");
                    Object[][] cands = new Object[(p.length - 1) / 2][];
                    for (int c = 0; c < cands.length; c++) {
                        cands[c] = new Object[] {p[1 + 2 * c], Integer.valueOf(p[2 + 2 * c])};
                    }
                    bn.put(p[0], cands);
                }
            }
            Map<String, Integer> en = new HashMap<String, Integer>(65536);
            for (String chunk : BanglishWordData.english()) {
                for (String line : chunk.split("\n")) {
                    int tab = line.indexOf('\t');
                    en.put(line.substring(0, tab), Integer.valueOf(line.substring(tab + 1)));
                }
            }
            bengaliByKey = bn;
            englishRank = en;
        }
    }

    /** "MN", "CEO", "PDF": all-caps words of two or more letters stay as spoken. */
    private static boolean isAcronym(String latin) {
        if (latin.length() < 2) {
            return false;
        }
        for (int i = 0; i < latin.length(); i++) {
            if (latin.charAt(i) < 'A' || latin.charAt(i) > 'Z') {
                return false;
            }
        }
        return true;
    }

    private static boolean isLatin(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /** Phonetic (Avro-style) spelling, used for words neither list knows. */
    static String convertWord(String word) {
        // Dhaka continuous tense writes ছ as "s": kortesilam -> করতেছিলাম.
        String w = word.toLowerCase(Locale.ROOT).replaceAll("tes(?=i|o|e)", "techh");
        StringBuilder out = new StringBuilder(w.length() + 4);
        String prevConsonant = null; // Latin key of the previous consonant, null if none
        boolean atStart = true;
        int i = 0;
        int n = w.length();
        while (i < n) {
            String vowelKey = longestMatch(w, i, VOWELS.keySet(), 2);
            String consonantKey = longestMatch(w, i, CONSONANTS.keySet(), 3);
            if (vowelKey != null && (consonantKey == null || vowelKey.length() >= consonantKey.length())) {
                boolean wordEnd = i + vowelKey.length() == n;
                String[] forms = VOWELS.get(vowelKey);
                if (prevConsonant != null) {
                    if ("o".equals(vowelKey)) {
                        // Inherent vowel mid-word; Banglish writes a final ো as "o" (jabo, bolo).
                        if (wordEnd) {
                            out.append("ো");
                        }
                    } else {
                        out.append(forms[1]);
                    }
                } else if ("o".equals(vowelKey) && atStart) {
                    out.append("অ");
                } else if ("a".equals(vowelKey) && !atStart) {
                    out.append("য়া"); // vowel + a glides: fua -> ফুয়া
                } else {
                    out.append(forms[0]);
                }
                prevConsonant = null;
                atStart = false;
                i += vowelKey.length();
                continue;
            }
            if (consonantKey == null) {
                // Not a letter we know; keep the original character.
                out.append(word.charAt(i));
                prevConsonant = null;
                atStart = false;
                i++;
                continue;
            }
            int next = i + consonantKey.length();
            boolean vowelFollows = next < n && longestMatch(w, next, VOWELS.keySet(), 2) != null;
            out.append(consonantText(consonantKey, prevConsonant, vowelFollows, w, next));
            // A "w" with no vowel after it is written as the vowel ও, so it ends the consonant run.
            prevConsonant = "w".equals(consonantKey) && !vowelFollows ? null : consonantKey;
            atStart = false;
            i = next;
        }
        // "Go" verbs start with য, not জ: jaibi -> যাইবি, jete -> যেতে.
        if (out.length() > 0 && out.charAt(0) == 'জ'
                && w.matches("^(ja(i|b|m|o|y|cch|t)|je(t|n)).*")) {
            out.setCharAt(0, 'য');
        }
        return out.toString();
    }

    private static String consonantText(
            String key, String prev, boolean vowelFollows, String word, int next) {
        if ("y".equals(key)) {
            return prev != null && !"y".equals(prev) && !"ng".equals(prev) ? "্য" : "য়";
        }
        if ("w".equals(key)) {
            if (prev != null) {
                return "্ব";
            }
            return vowelFollows ? "ওয়" : "ও";
        }
        if ("r".equals(key) && prev != null && R_PHALA_BASES.contains(prev)) {
            return "্র";
        }
        String base = CONSONANTS.get(key);
        if ("ng".equals(key) && vowelFollows) {
            base = "ঙ"; // angul -> আঙুল; anusvara only before a consonant or at word end
        }
        // Present-continuous verb endings: korchi, bolche, dekhchen -> ছ.
        if ("ch".equals(key) && prev != null && !"n".equals(prev) && vowelFollows
                && isContinuousEnding(word, next)) {
            base = "ছ";
        }
        if (prev != null && !"ng".equals(prev) && CLUSTERS.contains(prev + "|" + key)) {
            return HASANTA + base;
        }
        return base;
    }

    private static boolean isContinuousEnding(String word, int afterCh) {
        String rest = word.substring(afterCh);
        return rest.equals("i") || rest.equals("e") || rest.equals("o") || rest.equals("en")
                || rest.equals("is") || rest.startsWith("il");
    }

    private static String longestMatch(String w, int pos, Set<String> keys, int maxLen) {
        for (int len = Math.min(maxLen, w.length() - pos); len > 0; len--) {
            String candidate = w.substring(pos, pos + len);
            if (keys.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
