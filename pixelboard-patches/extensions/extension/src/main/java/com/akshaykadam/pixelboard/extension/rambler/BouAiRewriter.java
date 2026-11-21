package com.akshaykadam.pixelboard.extension.rambler;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Bou AI mode: asks Gemini (the user's own free Google AI Studio key) to turn Rambler's Banglish
 * into Bengali script with full sentence context, which handles regional speech and misheard
 * words far better than the offline rules. Off unless the user turns it on and adds a key.
 * Any failure (no network, timeout, bad reply) returns null and the offline result is used.
 * Dictated text is never logged.
 */
public final class BouAiRewriter {

    private static final String TAG = "BouAi";
    static final String PREFS = "bou_ai_mode";
    static final String KEY_ENABLED = "enabled";
    static final String KEY_API_KEY = "api_key";
    static final String MODEL = "gemini-2.5-flash-lite";
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent";
    /** Longest the keyboard waits for the AI before falling back to the offline result. */
    static final long TIMEOUT_MS = 1800;

    static final String INSTRUCTIONS =
            "You convert Bengali speech that a speech recognizer wrote in Latin letters (Banglish) "
            + "into Bengali script.\n"
            + "Rules:\n"
            + "1. Write every Bengali word in Bengali script, spelled the way it was spoken. Keep "
            + "regional dialect words (Chittagonian, Sylheti, Noakhali, Barishal, Dhaka and others) "
            + "as spoken. Never change dialect into standard Bengali.\n"
            + "2. English words the speaker used as English (office, meeting, call, laptop) stay in "
            + "Latin letters exactly as given.\n"
            + "3. Do not translate, summarize, add, drop or reorder words. If a Bengali word was "
            + "clearly misheard, fix it from the meaning of the sentence.\n"
            + "4. Keep the punctuation. End Bengali sentences with । instead of a period.\n"
            + "5. Reply with the converted text only, nothing else.";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "BouAiRewriter");
        t.setDaemon(true);
        return t;
    });

    private BouAiRewriter() {
    }

    static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context context) {
        if (context == null) {
            return false;
        }
        SharedPreferences p = preferences(context);
        return p.getBoolean(KEY_ENABLED, false) && !p.getString(KEY_API_KEY, "").trim().isEmpty();
    }

    /**
     * Returns Gemini's Bengali-script version of {@code banglish}, or null to use {@code draft}
     * (the offline conversion). Blocks the caller for at most {@link #TIMEOUT_MS}.
     */
    public static String rewrite(Context context, String banglish, String draft) {
        if (!isEnabled(context) || banglish == null || banglish.trim().isEmpty()) {
            return null;
        }
        final String apiKey = preferences(context).getString(KEY_API_KEY, "").trim();
        Future<String> future = EXECUTOR.submit(() -> call(apiKey, banglish, draft));
        try {
            return accept(banglish, future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } catch (Throwable t) {
            future.cancel(true);
            Log.w(TAG, "AI mode skipped: " + t.getClass().getSimpleName());
            return null;
        }
    }

    /** Rejects replies that are empty or clearly not a conversion of the input. */
    static String accept(String banglish, String reply) {
        if (reply == null) {
            return null;
        }
        String out = reply.trim();
        if (out.startsWith("\"") && out.endsWith("\"") && out.length() > 1) {
            out = out.substring(1, out.length() - 1).trim();
        }
        if (out.isEmpty() || out.indexOf('\n') >= 0 || out.length() > banglish.length() * 3 + 20
                || !BanglishTransliterator.containsBengali(out)) {
            return null;
        }
        return out;
    }

    static String buildRequestBody(String banglish, String draft) throws Exception {
        JSONObject body = new JSONObject();
        body.put("systemInstruction", new JSONObject().put("parts",
                new JSONArray().put(new JSONObject().put("text", INSTRUCTIONS))));
        String user = "Banglish: " + banglish
                + (draft != null && !draft.isEmpty() ? "\nOffline draft (may be wrong): " + draft : "");
        body.put("contents", new JSONArray().put(new JSONObject()
                .put("role", "user")
                .put("parts", new JSONArray().put(new JSONObject().put("text", user)))));
        body.put("generationConfig", new JSONObject().put("temperature", 0).put("maxOutputTokens", 256));
        return body.toString();
    }

    static String parseReply(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONArray candidates = root.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            return null;
        }
        JSONArray parts = candidates.getJSONObject(0).optJSONObject("content") == null ? null
                : candidates.getJSONObject(0).getJSONObject("content").optJSONArray("parts");
        if (parts == null) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            text.append(parts.getJSONObject(i).optString("text", ""));
        }
        return text.toString();
    }

    private static String call(String apiKey, String banglish, String draft) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout((int) TIMEOUT_MS);
            conn.setReadTimeout((int) TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            // Key in a header, never in the URL, so it does not end up in logs or proxies.
            conn.setRequestProperty("x-goog-api-key", apiKey);
            byte[] payload = buildRequestBody(banglish, draft).getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }
            int code = conn.getResponseCode();
            if (code != 200) {
                Log.w(TAG, "AI mode HTTP " + code);
                return null;
            }
            try (InputStream in = conn.getInputStream()) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] chunk = new byte[4096];
                int n;
                while ((n = in.read(chunk)) > 0) {
                    buf.write(chunk, 0, n);
                }
                return parseReply(new String(buf.toByteArray(), StandardCharsets.UTF_8));
            }
        } finally {
            conn.disconnect();
        }
    }
}
