package com.akshaykadam.pixelboard.extension.rambler;

import java.text.Normalizer;
import org.junit.Assert;
import org.junit.Test;

public final class BanglishTransliteratorTest {

    private static void assertConverts(String banglish, String expected) {
        Assert.assertEquals(banglish, Normalizer.normalize(expected, Normalizer.Form.NFC),
                BanglishTransliterator.convert(banglish));
    }

    @Test
    public void convertsBengaliAndKeepsEnglishWords() {
        assertConverts("ami office e meeting e jabo", "আমি office এ meeting এ যাবো");
        assertConverts("amar laptop ta kaj korche na", "আমার laptop টা কাজ করছে না");
        assertConverts("tomar mobile number ta dao", "তোমার mobile number টা দাও");
        assertConverts("ekhon ami busy pore call dibo", "এখন আমি busy পরে call দিবো");
        assertConverts("taka pathiye dao bkash e", "টাকা পাঠিয়ে দাও bkash এ");
        assertConverts("ok ami aschi", "ok আমি আসছি");
    }

    @Test
    public void convertsEverydayBengali() {
        assertConverts("Ami bhalo achi.", "আমি ভালো আছি।");
        assertConverts("Kemon acho tumi?", "কেমন আছো তুমি?");
        assertConverts("ajke amar mon bhalo nei", "আজকে আমার মন ভালো নেই");
        assertConverts("ami kal dhaka jabo", "আমি কাল ঢাকা যাবো");
        assertConverts("amar mathar betha korche", "আমার মাথার ব্যথা করছে");
        assertConverts("apni kothay achen", "আপনি কোথায় আছেন");
        assertConverts("shotti bolchi", "সত্যি বলছি");
        assertConverts("ami tomake bhalobashi", "আমি তোমাকে ভালোবাসি");
    }

    @Test
    public void regionalSpeech() {
        // Chittagong
        assertConverts("Tui kenge asos?", "তুই কেঙ্গে আছস?");
        assertConverts("Tui kodde jaibi?", "তুই কোদ্দে যাইবি?");
        assertConverts("Hitara kenge ase?", "হিতারা কেঙ্গে আসে?");
        // Sylhet
        assertConverts("Tumi kita koro?", "তুমি কিতা করো?");
        assertConverts("Ami bari jairam.", "আমি বাড়ি যাইরাম।");
        assertConverts("Tumi kunta khaiso?", "তুমি কুনটা খাইছো?");
        // Noakhali
        assertConverts("Ami hetere koilam.", "আমি হেতেরে কইলাম।");
        assertConverts("Tui kitta koros?", "তুই কিত্তা করোস?");
        // Barishal, Dhaka, North Bengal
        assertConverts("Mui ekhon jaimu na.", "মুই এখন যাইমু না।");
        assertConverts("Tomgo bari kothay?", "তোমগো বাড়ি কোথায়?");
        assertConverts("Ki kortesos?", "কি করতেছস?");
        assertConverts("Hamra kal jamu.", "হামরা কাল যামু।");
        // Unknown words are spelled out, never matched to an unrelated word.
        assertConverts("Ar fua ekhon ghumar.", "আর ফুয়া এখন ঘুমার।");
        Assert.assertTrue(BanglishTransliterator.endingMatches("kemon", "কেমন"));
        Assert.assertFalse(BanglishTransliterator.endingMatches("hitara", "হিটার"));
    }

    @Test
    public void everydayMixedSentences() {
        assertConverts("Kal sokal e meeting ache.", "কাল সকাল এ meeting আছে।");
        assertConverts("Kono problem nai.", "কোনো problem নাই।");
        assertConverts("Ei mash er salary ekhono pai nai.", "এই মাস এর salary এখনো পাই নাই।");
        assertConverts("Thank you bhai, onek help holo.", "Thank you ভাই, অনেক help হলো।");
        assertConverts("Sorry, ami deri kore fellam.", "Sorry, আমি দেরি করে ফেললাম।");
        assertConverts("Bazar theke dim ar dudh aniyo.", "বাজার থেকে ডিম আর দুধ আনিয়ো।");
        assertConverts("Tumi ki khaiso?", "তুমি কি খাইছো?");
        assertConverts("Ami kaj korsi.", "আমি কাজ করছি।");
        assertConverts("Bhalo theko.", "ভালো থেকো।");
        assertConverts("Internet connection thik nai.", "Internet connection ঠিক নাই।");
        assertConverts("Order ta deliver hoyeche?", "Order টা deliver হয়েছে?");
        assertConverts("Ami ekhon drive korchi, pore kotha boli.",
                "আমি এখন drive করছি, পরে কথা বলি।");
        assertConverts("Ami ghumate jacchi, good night.", "আমি ঘুমাতে যাচ্ছি, good night.");
        assertConverts("bKash e taka pathiye dilam.", "bKash এ টাকা পাঠিয়ে দিলাম।");
        // From the phone: Dhaka past tense and continuous forms.
        assertConverts("Ekta update disi, sei jonno oi update ta check kortesilam.",
                "একটা update দিছি, সেই জন্য ওই update টা check করতেছিলাম।");
        assertConverts("Meeting nei to?", "Meeting নেই তো?");
    }

    @Test
    public void ambiguousWordsFollowTheSentence() {
        assertConverts("to tumi ki bolte chao", "তো তুমি কি বলতে চাও");
        assertConverts("or kache jao", "ওর কাছে যাও");
        assertConverts("she is my friend", "she is my friend");
        assertConverts("Office admin project done hoy nai.", "Office admin project done হয় নাই।");
        assertConverts("Office MN project done hoy nai.", "Office MN project done হয় নাই।");
        assertConverts("Kalke amar client meeting ache, tumi presentation ta ready koro.",
                "কালকে আমার client meeting আছে, তুমি presentation টা ready করো।");
        assertConverts("Amar hate betha", "আমার হাতে ব্যথা");
        assertConverts("I hate you", "I hate you");
        assertConverts("please send me the file", "please send me the file");
    }

    @Test
    public void unknownWordsUsePhoneticRules() {
        Assert.assertEquals("বাংলা", BanglishTransliterator.convertWord("bangla"));
        Assert.assertEquals("কিন্তু", BanglishTransliterator.convertWord("kintu"));
        Assert.assertEquals("বন্ধু", BanglishTransliterator.convertWord("bondhu"));
        Assert.assertEquals("আমরা", BanglishTransliterator.convertWord("amra"));
        Assert.assertEquals("প্রেম", BanglishTransliterator.convertWord("prem"));
        Assert.assertEquals("দিয়ে", BanglishTransliterator.convertWord("diye"));
        Assert.assertEquals("করবো", BanglishTransliterator.convertWord("korbo"));
        Assert.assertEquals("অনেক", BanglishTransliterator.convertWord("onek"));
        Assert.assertEquals("খাওয়া", BanglishTransliterator.convertWord("khawa"));
        Assert.assertEquals("করছি", BanglishTransliterator.convertWord("korchi"));
        Assert.assertEquals("খাচ্ছি", BanglishTransliterator.convertWord("khacchi"));
    }

    @Test
    public void leavesNonBanglishAlone() {
        assertConverts("আমি ভালো", "আমি ভালো");
        assertConverts("123", "123");
        assertConverts("mail me at a@b.com", "mail me at a@b.com");
        assertConverts("ami google.com e jabo", "আমি google.com এ যাবো");
        assertConverts("5G", "5G");
        assertConverts("ami e-mail pathabo", "আমি e-mail পাঠাবো");
        assertConverts("ami Dr. Rahman er kache jabo", "আমি Dr. Rahman এর কাছে যাবো");
        assertConverts("", "");
        Assert.assertNull(BanglishTransliterator.convert(null));
    }

    @Test
    public void keysMatchBetweenBanglishAndBengali() {
        Assert.assertEquals("kemn", BanglishTransliterator.latinKey("kemon"));
        Assert.assertEquals("betha", BanglishTransliterator.latinKey("byatha"));
        Assert.assertEquals("cele", BanglishTransliterator.latinKey("chhele"));
        Assert.assertEquals("jdi", BanglishTransliterator.latinKey("yodi"));
        Assert.assertEquals("khma", BanglishTransliterator.latinKey("kshoma"));
    }

    @Test
    public void needsConversionWhenAnyLatinLetterIsPresent() {
        Assert.assertTrue(BanglishTransliterator.needsConversion("ami bhalo achi"));
        Assert.assertTrue(BanglishTransliterator.needsConversion("আমি bhalo"));
        Assert.assertEquals("আমি ভালো", BanglishTransliterator.convert("আমি bhalo"));
        Assert.assertFalse(BanglishTransliterator.needsConversion("আমি ভালো"));
        Assert.assertFalse(BanglishTransliterator.needsConversion("123 ?"));
        Assert.assertFalse(BanglishTransliterator.needsConversion(null));
        Assert.assertTrue(BanglishTransliterator.containsBengali("ok আমি"));
        Assert.assertFalse(BanglishTransliterator.containsBengali("ok"));
    }
}
