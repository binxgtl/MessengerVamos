package io.github.binxgtl.messengervamos;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class SettingsActivity extends Activity {
    private LinearLayout root;
    private TextView serviceStatus;
    private EditText seenApiId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView title = text("MessengerVamos", 24, true);
        root.addView(title);
        root.addView(text("M0-M4 clean-room build · target Messenger " + Config.TARGET_VERSION, 14, false));

        serviceStatus = text("", 14, false);
        root.addView(serviceStatus);

        SharedPreferences p = VamosApp.readPrefs(this);
        addToggle("Disable Seen receipts", Config.KEY_NO_SEEN, p.getBoolean(Config.KEY_NO_SEEN, Config.DEF_NO_SEEN));
        addToggle("Disable Typing indicator", Config.KEY_NO_TYPING, p.getBoolean(Config.KEY_NO_TYPING, Config.DEF_NO_TYPING));
        addToggle("Strict version guard (recommended)", Config.KEY_STRICT_VERSION, p.getBoolean(Config.KEY_STRICT_VERSION, Config.DEF_STRICT_VERSION));
        addToggle("Diagnostics / candidate logging", Config.KEY_DIAGNOSTICS, p.getBoolean(Config.KEY_DIAGNOSTICS, Config.DEF_DIAGNOSTICS));
        addToggle("Message observer (restart Messenger after changing)", Config.KEY_MESSAGE_OBSERVER, p.getBoolean(Config.KEY_MESSAGE_OBSERVER, Config.DEF_MESSAGE_OBSERVER));
        addToggle("Persist message text for unsend recovery", Config.KEY_CACHE_MESSAGE_TEXT, p.getBoolean(Config.KEY_CACHE_MESSAGE_TEXT, Config.DEF_CACHE_MESSAGE_TEXT));

        root.addView(text("Seen API ID", 16, true));
        root.addView(text("Leave -1 for structural auto-detection. If diagnostics identifies a stable API code, enter it here for a stricter match.", 13, false));
        seenApiId = new EditText(this);
        seenApiId.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        seenApiId.setText(String.valueOf(p.getInt(Config.KEY_SEEN_API_ID, Config.DEF_SEEN_API_ID)));
        root.addView(seenApiId);

        Button save = button("Save API ID");
        save.setOnClickListener(v -> saveSeenApiId());
        root.addView(save);

        Button copy = button("Copy expected log path");
        copy.setOnClickListener(v -> {
            String path = "/storage/emulated/0/Android/data/com.facebook.orca/files/MessengerVamos/logs";
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("MessengerVamos logs", path));
            Toast.makeText(this, "Log path copied", Toast.LENGTH_SHORT).show();
        });
        root.addView(copy);

        Button reset = button("Reset defaults");
        reset.setOnClickListener(v -> {
            VamosApp.resetDefaults(this);
            Toast.makeText(this, "Defaults restored. Reopen this screen and restart Messenger.", Toast.LENGTH_LONG).show();
        });
        root.addView(reset);

        root.addView(text("After changing settings, force-stop Messenger and reopen it. If Messenger crashes or a hook cannot be installed, send latest.log + last-hook.txt + last-java-crash.txt (if present). Native crashes still leave the last hook/runtime checkpoint on disk.", 13, false));
        setContentView(scroll);
        refreshStatus();
    }

    private void addToggle(String label, String key, boolean initial) {
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setChecked(initial);
        box.setPadding(0, dp(8), 0, dp(8));
        box.setOnCheckedChangeListener((buttonView, checked) -> VamosApp.putBoolean(this, key, checked));
        root.addView(box);
    }

    private void saveSeenApiId() {
        try {
            int value = Integer.parseInt(seenApiId.getText().toString().trim());
            VamosApp.putInt(this, Config.KEY_SEEN_API_ID, value);
            Toast.makeText(this, "Saved. Restart Messenger.", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter an integer, or -1 for auto", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshStatus() {
        if (serviceStatus == null) return;
        serviceStatus.setText(VamosApp.isServiceReady()
                ? "Xposed service: connected · settings are writing to Remote Preferences"
                : "Xposed service: not connected yet · changes are mirrored locally and will sync when the framework binds");
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(sp);
        tv.setPadding(0, dp(6), 0, dp(6));
        if (bold) tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return tv;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setPadding(dp(12), dp(8), dp(12), dp(8));
        return b;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
