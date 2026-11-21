package com.akshaykadam.pixelboard.extension.rambler;

import org.junit.Assert;
import org.junit.Test;

public final class BanglishTransliteratorTest {

    private static void assertConverts(String banglish, String expected) {
        Assert.assertEquals(banglish, expected, BanglishTransliterator.convert(banglish));
    }

    @Test
    public void convertsBengaliAndKeepsEnglishWords() {
        assertConverts("ami office e meeting e jabo", "আমি office এ meeting এ যাবো");
        assertConverts("amar laptop ta kaj korche na", "আমার laptop টা কাজ করছে না");
        assertConverts("tomar mobile number ta dao", "তোমার mobile number টা দাও");
        assertConverts("ekhon ami busy pore call dibo", "এখন আমি busy পরে call দিব");
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
    public void ambiguousWordsFollowTheSentence() {
        assertConverts("to tumi ki bolte chao", "তো তুমি কি বলতে চাও");
        assertConverts("or kache jao", "ওর কাছে যাও");
        assertConverts("she is my friend", "she is my friend");
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
