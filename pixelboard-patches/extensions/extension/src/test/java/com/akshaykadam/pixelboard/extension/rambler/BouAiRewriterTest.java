package com.akshaykadam.pixelboard.extension.rambler;

import android.content.Context;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class BouAiRewriterTest {

    @Test
    public void offUntilEnabledAndKeyed() {
        Context context = RuntimeEnvironment.getApplication();
        BouAiRewriter.preferences(context).edit().clear().commit();
        Assert.assertFalse(BouAiRewriter.isEnabled(context));
        Assert.assertNull(BouAiRewriter.rewrite(context, "ami jabo", "আমি যাবো"));

        BouAiRewriter.preferences(context).edit().putBoolean(BouAiRewriter.KEY_ENABLED, true).commit();
        Assert.assertFalse("a switch without a key stays off", BouAiRewriter.isEnabled(context));

        BouAiRewriter.preferences(context).edit().putString(BouAiRewriter.KEY_API_KEY, "  k  ").commit();
        Assert.assertTrue(BouAiRewriter.isEnabled(context));
        Assert.assertFalse(BouAiRewriter.isEnabled(null));
    }

    @Test
    public void requestCarriesInstructionsTextAndDraft() throws Exception {
        JSONObject body = new JSONObject(BouAiRewriter.buildRequestBody("tui kodde jaibi?", "তুই কোদ্দে যাইবি?"));
        String instructions = body.getJSONObject("systemInstruction").getJSONArray("parts")
                .getJSONObject(0).getString("text");
        Assert.assertTrue(instructions.contains("dialect"));
        String user = body.getJSONArray("contents").getJSONObject(0).getJSONArray("parts")
                .getJSONObject(0).getString("text");
        Assert.assertTrue(user.contains("tui kodde jaibi?"));
        Assert.assertTrue(user.contains("তুই কোদ্দে যাইবি?"));
        Assert.assertEquals(0, body.getJSONObject("generationConfig").getInt("temperature"));
        // Gemini 3 models answer HTTP 400 to thinkingBudget 0; thinkingLevel works on them.
        JSONObject thinking = body.getJSONObject("generationConfig").getJSONObject("thinkingConfig");
        Assert.assertEquals("minimal", thinking.getString("thinkingLevel"));
        Assert.assertFalse(thinking.has("thinkingBudget"));
    }

    @Test
    public void parsesGeminiReply() throws Exception {
        String json = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"আমি office এ যাবো।\"}]}}]}";
        Assert.assertEquals("আমি office এ যাবো।", BouAiRewriter.parseReply(json));
        Assert.assertNull(BouAiRewriter.parseReply("{\"candidates\":[]}"));
        Assert.assertNull(BouAiRewriter.parseReply("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}"));
    }

    @Test
    public void rejectsRepliesThatAreNotAConversion() {
        Assert.assertEquals("আমি যাবো", BouAiRewriter.accept("ami jabo", "  \"আমি যাবো\" "));
        Assert.assertNull(BouAiRewriter.accept("ami jabo", null));
        Assert.assertNull(BouAiRewriter.accept("ami jabo", ""));
        Assert.assertNull("no Bengali at all", BouAiRewriter.accept("ami jabo", "I will go"));
        Assert.assertNull("multi-line chatter", BouAiRewriter.accept("ami jabo", "আমি যাবো\nExplanation: ..."));
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            huge.append("আমি ");
        }
        Assert.assertNull("far longer than the input", BouAiRewriter.accept("ami jabo", huge.toString()));
    }
}
