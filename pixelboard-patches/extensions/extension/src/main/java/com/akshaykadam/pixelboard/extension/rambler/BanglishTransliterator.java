package com.akshaykadam.pixelboard.extension.rambler;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Offline Banglish (romanized Bengali) to Bengali script converter.
 *
 * <p>Rambler dictation returns Bengali speech as Roman text ("ami bhalo achi"). This class turns
 * it into Bengali script ("আমি ভালো আছি") with no network access: a dictionary of common words
 * first, then Avro-style phonetic rules tuned for casual Banglish (consonants only join into a
 * conjunct for known clusters, a word-final "o" becomes ো, "ch" before a verb ending becomes ছ).
 * Bengali text, numbers, links and e-mail addresses pass through unchanged.</p>
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
        put(d, "shon", "শোন"); put(d, "dokan", "দোকান"); put(d, "ki", "কি");
        // English loanwords Rambler spells in English.
        put(d, "office", "অফিস"); put(d, "phone", "ফোন"); put(d, "mobile", "মোবাইল");
        put(d, "school", "স্কুল"); put(d, "college", "কলেজ"); put(d, "video", "ভিডিও");
        put(d, "ok", "ওকে"); put(d, "okay", "ওকে"); put(d, "internet", "ইন্টারনেট");
        put(d, "facebook", "ফেসবুক"); put(d, "youtube", "ইউটিউব"); put(d, "message", "মেসেজ");
        put(d, "class", "ক্লাস"); put(d, "exam", "এক্সাম"); put(d, "time", "টাইম");
        put(d, "please", "প্লিজ"); put(d, "sorry", "সরি"); put(d, "thanks", "থ্যাংকস");
        put(d, "bus", "বাস"); put(d, "computer", "কম্পিউটার"); put(d, "app", "অ্যাপ");
        put(d, "online", "অনলাইন"); put(d, "call", "কল"); put(d, "doctor", "ডাক্তার");
        put(d, "hospital", "হাসপাতাল"); put(d, "market", "মার্কেট"); put(d, "shop", "শপ");
        put(d, "photo", "ফটো"); put(d, "game", "গেম"); put(d, "price", "প্রাইস");
        DICTIONARY = Collections.unmodifiableMap(d);
    }

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

    /** Converts every Latin word in {@code text}; everything else is kept as is. */
    public static String convert(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                out.append(c);
                i++;
                continue;
            }
            int end = i;
            while (end < n && !Character.isWhitespace(text.charAt(end))) {
                end++;
            }
            out.append(convertChunk(text.substring(i, end)));
            i = end;
        }
        return out.toString();
    }

    /** A whitespace-free chunk: keep links, e-mails and mixed letter+digit codes untouched. */
    private static String convertChunk(String chunk) {
        if (chunk.indexOf('@') >= 0 || chunk.indexOf('/') >= 0 || chunk.indexOf(':') >= 0
                || chunk.matches(".*[A-Za-z]\\.[A-Za-z].*")
                || chunk.matches(".*([A-Za-z][0-9]|[0-9][A-Za-z]).*")) {
            return chunk;
        }
        StringBuilder out = new StringBuilder(chunk.length() + 4);
        boolean converted = false;
        int i = 0;
        int n = chunk.length();
        while (i < n) {
            char c = chunk.charAt(i);
            if (isLatin(c)) {
                int end = i;
                while (end < n && isLatin(chunk.charAt(end))) {
                    end++;
                }
                out.append(convertWord(chunk.substring(i, end)));
                converted = true;
                i = end;
            } else if (c == '.' && converted && i == n - 1) {
                out.append('।');
                i++;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static boolean isLatin(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    static String convertWord(String word) {
        String w = word.toLowerCase(Locale.ROOT);
        String known = DICTIONARY.get(w);
        if (known != null) {
            return known;
        }
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
