package com.akshaykadam.pixelboard.extension.rambler;

import org.junit.Assert;
import org.junit.Test;

public final class BanglishTransliteratorTest {

    private static void assertConverts(String banglish, String bengali) {
        Assert.assertEquals(banglish, bengali, BanglishTransliterator.convert(banglish));
    }

    @Test
    public void convertsEverydaySentences() {
        assertConverts("Ami bhalo achi.", "আমি ভালো আছি।");
        assertConverts("Kemon acho tumi?", "কেমন আছো তুমি?");
        assertConverts("Ajke office e jabo na", "আজকে অফিস এ যাবো না");
        assertConverts("Amar nam Subrata", "আমার নাম সুব্রাতা");
        assertConverts("tomar kotha bujhte parchi", "তোমার কথা বুঝতে পারছি");
    }

    @Test
    public void appliesPhoneticRules() {
        assertConverts("bangla", "বাংলা");
        assertConverts("bangladesh", "বাংলাদেশ");
        assertConverts("kintu", "কিন্তু");
        assertConverts("bondhu", "বন্ধু");
        assertConverts("amra", "আমরা");
        assertConverts("prem", "প্রেম");
        assertConverts("hoy", "হয়");
        assertConverts("diye", "দিয়ে");
        assertConverts("korbo", "করবো");
        assertConverts("korte", "করতে");
        assertConverts("onek", "অনেক");
        assertConverts("amma", "আম্মা");
        assertConverts("khawa", "খাওয়া");
        assertConverts("bhai", "ভাই");
    }

    @Test
    public void continuousVerbEndingsUseChho() {
        assertConverts("korchi", "করছি");
        assertConverts("bolche", "বলছে");
        assertConverts("dekhchen", "দেখছেন");
        assertConverts("khacchi", "খাচ্ছি");
        assertConverts("cha", "চা");
    }

    @Test
    public void leavesNonBanglishAlone() {
        assertConverts("আমি ভালো", "আমি ভালো");
        assertConverts("123", "123");
        assertConverts("mail me at a@b.com", "মাইল মে আত a@b.com");
        assertConverts("visit google.com now", "ভিসিত google.com নও");
        assertConverts("5G", "5G");
        assertConverts("", "");
        Assert.assertNull(BanglishTransliterator.convert(null));
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
