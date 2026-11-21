package com.akshaykadam.pixelboard.extension.rambler;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettingsContract;

import java.util.ArrayList;
import java.util.List;

/** Settings screen for Bou AI mode: on/off switch, the user's own Gemini key, where to get one. */
public final class BouAiSettingsFeature implements GboardPatchesSettingsContract.Feature {

    static final String KEY_PAGE = "https://aistudio.google.com/apikey";
    private static final String TITLE = "Bou AI মোড";
    private static final String SUMMARY = "আঞ্চলিক ভাষা আর ভুল শোনা শব্দ আরও ভালোভাবে ঠিক করে";

    public BouAiSettingsFeature(Context context) {
    }

    @Override
    public String getEntryTitle() {
        return TITLE;
    }

    @Override
    public String getEntrySummary() {
        return SUMMARY;
    }

    @Override
    public GboardPatchesSettingsContract.Screen buildScreen(GboardPatchesSettingsContract.FeatureHost host) {
        Context context = host == null ? null : host.getContext();
        List<GboardPatchesSettingsContract.Row> rows = new ArrayList<GboardPatchesSettingsContract.Row>();
        if (context == null) {
            return new GboardPatchesSettingsContract.Screen(TITLE, "Bou", TITLE, SUMMARY, rows);
        }
        SharedPreferences p = BouAiRewriter.preferences(context);
        String key = p.getString(BouAiRewriter.KEY_API_KEY, "").trim();
        boolean hasKey = !key.isEmpty();

        rows.add(new GboardPatchesSettingsContract.ToggleRow(
                "AI মোড চালু",
                hasKey
                        ? "বলার পর লেখা Google Gemini-তে পাঠিয়ে ঠিক করা হয়। প্রায় ১ সেকেন্ড বেশি লাগে।"
                        : "আগে নিচে আপনার বিনামূল্যের Gemini key বসান।",
                hasKey,
                hasKey && p.getBoolean(BouAiRewriter.KEY_ENABLED, false),
                value -> {
                    p.edit().putBoolean(BouAiRewriter.KEY_ENABLED, value).apply();
                    GboardPatchesSettingsContract.refresh(host);
                }));
        rows.add(new GboardPatchesSettingsContract.NavigationRow(
                "Gemini API key",
                hasKey ? "বসানো আছে (…" + key.substring(Math.max(0, key.length() - 4)) + ")" : "বসানো নেই",
                true,
                () -> showKeyDialog(host, context, value -> {
                            String v = value == null ? "" : value.replaceAll("\\s+", "");
                            SharedPreferences.Editor e = p.edit().putString(BouAiRewriter.KEY_API_KEY, v);
                            if (v.isEmpty()) {
                                e.putBoolean(BouAiRewriter.KEY_ENABLED, false);
                            }
                            e.apply();
                            android.widget.Toast.makeText(context,
                                    v.isEmpty() ? "key মুছে ফেলা হয়েছে" : "key সেভ হয়েছে। এবার পরীক্ষা করুন।",
                                    android.widget.Toast.LENGTH_SHORT).show();
                            GboardPatchesSettingsContract.refresh(host);
                        })));
        rows.add(new GboardPatchesSettingsContract.NavigationRow(
                "key পরীক্ষা করুন",
                "চাপলে Google-এ একটা ছোট পরীক্ষা পাঠিয়ে দেখায় key কাজ করছে কিনা।",
                hasKey,
                () -> {
                    android.widget.Toast.makeText(context, "পরীক্ষা হচ্ছে…", android.widget.Toast.LENGTH_SHORT).show();
                    final String k = p.getString(BouAiRewriter.KEY_API_KEY, "");
                    final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
                    new Thread(() -> {
                        String problem = BouAiRewriter.testKey(k);
                        main.post(() -> android.widget.Toast.makeText(context,
                                problem == null ? "key কাজ করছে। AI মোড চালু করুন।" : problem,
                                android.widget.Toast.LENGTH_LONG).show());
                    }, "BouAiKeyTest").start();
                }));
        rows.add(new GboardPatchesSettingsContract.NavigationRow(
                "বিনামূল্যের key নিন",
                "Google AI Studio খুলবে। Create API key চাপুন, তারপর key কপি করে উপরে বসান।",
                true,
                () -> {
                    Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse(KEY_PAGE));
                    open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(open);
                }));
        rows.add(new GboardPatchesSettingsContract.NavigationRow(
                "গোপনীয়তা",
                "AI মোড চালু থাকলে আপনার বলা লেখা আপনার key দিয়ে Google Gemini-তে যায়। বন্ধ রাখলে কিছুই যায় না।",
                false,
                () -> { }));
        return new GboardPatchesSettingsContract.Screen(TITLE, "Bou", TITLE,
                "ইন্টারনেট না থাকলে বা সমস্যা হলে সাথে সাথে আগের মতো offline পদ্ধতি চলে।",
                rows);
    }

    /**
     * Opens the key box straight on the settings activity. Going through the settings
     * orchestrator dropped the request for this screen, so the box never appeared.
     */
    private static void showKeyDialog(GboardPatchesSettingsContract.FeatureHost host, Context context,
            GboardPatchesSettingsContract.TextValueConsumer consumer) {
        // Our own dialog on whatever Activity hosts this screen (the context may be wrapped).
        android.app.Activity activity = host instanceof android.app.Activity ? (android.app.Activity) host : null;
        Context c = context;
        while (activity == null && c instanceof android.content.ContextWrapper) {
            if (c instanceof android.app.Activity) {
                activity = (android.app.Activity) c;
            }
            Context base = ((android.content.ContextWrapper) c).getBaseContext();
            c = base == c ? null : base;
        }
        if (activity == null || activity.isFinishing()) {
            android.widget.Toast.makeText(context, "Settings বন্ধ করে আবার খুলুন", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        // AlertDialog crashes in this app (AlertController.setupButtons NPE: the patched app's
        // resources lack its layout), so this is a plain Dialog with views we build ourselves.
        final android.app.Dialog dialog = new android.app.Dialog(activity);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        float dp = activity.getResources().getDisplayMetrics().density;
        int pad = (int) (20 * dp);
        android.widget.LinearLayout root = new android.widget.LinearLayout(activity);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad / 2);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xFF2B2B31);
        bg.setCornerRadius(24 * dp);
        root.setBackground(bg);

        android.widget.TextView title = new android.widget.TextView(activity);
        title.setText("Gemini API key");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(20);
        root.addView(title);

        final android.widget.EditText input = new android.widget.EditText(activity);
        input.setHint("এখানে key paste করুন");
        input.setTextColor(0xFFFFFFFF);
        input.setHintTextColor(0xFF9A9AA5);
        input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = pad / 2;
        root.addView(input, lp);

        android.widget.LinearLayout buttons = new android.widget.LinearLayout(activity);
        buttons.setGravity(android.view.Gravity.END);
        android.widget.Button cancel = new android.widget.Button(activity);
        cancel.setText("বাতিল");
        cancel.setOnClickListener(v -> dialog.dismiss());
        android.widget.Button save = new android.widget.Button(activity);
        save.setText("Save");
        save.setOnClickListener(v -> {
            consumer.accept(input.getText() == null ? "" : input.getText().toString());
            dialog.dismiss();
        });
        buttons.addView(cancel);
        buttons.addView(save);
        root.addView(buttons);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout((int) (activity.getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setSoftInputMode(
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dialog.show();
        input.requestFocus();
    }

    /** For tests and the registry: the feature never hides itself. */
    @Override
    public boolean isAvailable(Context context) {
        return true;
    }
}
