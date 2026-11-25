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
    /** Tried in order; "-latest" aliases survive Google retiring a model version. */
    static final String[] MODELS = {"gemini-flash-lite-latest", "gemini-3.5-flash-lite", "gemini-flash-latest"};
    private static final String API = "https://generativelanguage.googleapis.com/v1beta/models/";
    /** Longest the keyboard waits for the AI before falling back to the offline result. */
    static final long TIMEOUT_MS = 2500;
    /** The settings key test may wait longer: it reports a result, it does not block typing. */
    static final int TEST_TIMEOUT_MS = 15000;

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
        Future<String> future = EXECUTOR.submit(() -> call(apiKey, banglish, draft, (int) TIMEOUT_MS));
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
        return buildRequestBody(banglish, draft, true);
    }

    /** Models that answered HTTP 400 to the thinking setting; they get the request without it. */
    private static final java.util.Set<String> NO_THINKING_CONFIG =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<String, Boolean>());

    static String buildRequestBody(String banglish, String draft, boolean thinking) throws Exception {
        JSONObject body = new JSONObject();
        body.put("systemInstruction", new JSONObject().put("parts",
                new JSONArray().put(new JSONObject().put("text", INSTRUCTIONS))));
        String user = "Banglish: " + banglish
                + (draft != null && !draft.isEmpty() ? "\nOffline draft (may be wrong): " + draft : "");
        body.put("contents", new JSONArray().put(new JSONObject()
                .put("role", "user")
                .put("parts", new JSONArray().put(new JSONObject().put("text", user)))));
        // Least thinking and room to answer: thinking used to eat the whole budget, leaving no text.
        // Gemini 3 models reject thinkingBudget 0 with HTTP 400, so use thinkingLevel instead.
        JSONObject config = new JSONObject().put("temperature", 0).put("maxOutputTokens", 1024);
        if (thinking) {
            config.put("thinkingConfig", new JSONObject().put("thinkingLevel", "minimal"));
        }
        body.put("generationConfig", config);
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

    /** Remembers which way of sending the key worked, so later calls try it first. */
    private static volatile int workingAuth = 0;

    /**
     * Sends the request, trying each way Google accepts a key: classic keys (AIza) in the
     * x-goog-api-key header, newer AI Studio keys (AQ.) as a bearer token, then the key query.
     */
    private static volatile int workingModel = 0;

    private static String call(String apiKey, String banglish, String draft, int timeoutMs) throws Exception {
        Exception last = null;
        for (int m = 0; m < MODELS.length; m++) {
            int model = (workingModel + m) % MODELS.length;
            try {
                String reply = callModel(MODELS[model], apiKey, banglish, draft, timeoutMs);
                if (reply != null && !reply.trim().isEmpty()) {
                    workingModel = model;
                    return reply;
                }
            } catch (java.io.IOException e) {
                last = e; // bad request (400), model retired (404) or busy (429/503): try the next one
            }
        }
        if (last != null) {
            throw last;
        }
        return null;
    }

    private static String callModel(String model, String apiKey, String banglish, String draft, int timeoutMs)
            throws Exception {
        Exception last = null;
        for (int i = 0; i < 3; i++) {
            int auth = (workingAuth + i) % 3;
            try {
                String reply = callWith(model, auth, apiKey, banglish, draft, timeoutMs);
                workingAuth = auth;
                return reply;
            } catch (AuthRejected e) {
                last = e;
            }
        }
        throw last;
    }

    /** Result for the settings screen's key test: null when the key works, else a short reason. */
    public static String testKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "key বসানো নেই";
        }
        try {
            String reply = call(apiKey.trim(), "ami bhalo achi", null, TEST_TIMEOUT_MS);
            return reply == null || reply.trim().isEmpty() ? "Google কোনো উত্তর দেয়নি" : null;
        } catch (AuthRejected e) {
            return "key গ্রহণ করেনি (" + e.getMessage() + ")। Copy key দিয়ে আবার কপি করুন।";
        } catch (Exception e) {
            return "Google-এর উত্তর: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    static final class AuthRejected extends Exception {
        AuthRejected(String message) {
            super(message);
        }
    }

    private static String callWith(String model, int auth, String apiKey, String banglish, String draft,
            int timeoutMs) throws Exception {
        String endpoint = API + model + ":generateContent"
                + (auth == 2 ? "?key=" + java.net.URLEncoder.encode(apiKey, "UTF-8") : "");
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            if (auth == 0) {
                conn.setRequestProperty("x-goog-api-key", apiKey);
            } else if (auth == 1) {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
            byte[] payload = buildRequestBody(banglish, draft, !NO_THINKING_CONFIG.contains(model))
                    .getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }
            int code = conn.getResponseCode();
            // 400 is a bad request (e.g. a setting a new model refuses), not a bad key: report its reason.
            if (code == 401 || code == 403) {
                throw new AuthRejected("HTTP " + code);
            }
            if (code != 200) {
                // Surface Google's own reason (wrong model, quota, region) instead of a silent null.
                String reason = "";
                try (InputStream err = conn.getErrorStream()) {
                    if (err != null) {
                        ByteArrayOutputStream eb = new ByteArrayOutputStream();
                        byte[] ch = new byte[2048];
                        int k;
                        while ((k = err.read(ch)) > 0 && eb.size() < 8192) {
                            eb.write(ch, 0, k);
                        }
                        JSONObject e = new JSONObject(new String(eb.toByteArray(), StandardCharsets.UTF_8))
                                .optJSONObject("error");
                        reason = e == null ? "" : e.optString("message", "");
                    }
                } catch (Exception ignored) {
                    // keep the bare code
                }
                Log.w(TAG, "AI mode HTTP " + code + " " + reason);
                if (code == 400 && reason.toLowerCase(java.util.Locale.ROOT).contains("thinking")
                        && NO_THINKING_CONFIG.add(model)) {
                    // This model refuses the thinking setting: ask again without it, once.
                    conn.disconnect();
                    return callWith(model, auth, apiKey, banglish, draft, timeoutMs);
                }
                throw new java.io.IOException("HTTP " + code + (reason.isEmpty() ? "" : ": " + reason));
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
