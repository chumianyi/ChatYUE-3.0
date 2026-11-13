package com.chatyue.app.ui.m3;

import android.text.InputType;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Ids;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.data.Provider;
import com.chatyue.app.data.Providers;
import com.chatyue.app.net.ChatClient;
import com.chatyue.app.net.RequestQueue;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.widget.BounceScrollView;
import com.chatyue.app.ui.widget.Label;

/** 经典主题的第三方模型配置页，和默认 ProviderEditPage 是两套布局。 */
public class M3ProviderEditPage extends Page {

    private final String editingId;
    private M3Field nameField;
    private M3Field urlField;
    private M3Field keyField;
    private M3Field modelField;

    public M3ProviderEditPage(MainActivity a, String id) {
        super(a);
        editingId = Providers.BUILTIN_ID.equals(id) ? null : id;
        setBackgroundColor(M3.BG);

        M3TopBar bar = new M3TopBar(a, editingId == null ? "添加模型" : "编辑模型",
                "OpenAI 兼容端点", Icons.BACK, 0);
        bar.left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.pop();
            }
        });
        addView(bar, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, Ui.dp(70)));

        BounceScrollView scroll = new BounceScrollView(a);
        LinearLayout form = new LinearLayout(a);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(Ui.dp(18), Ui.dp(4), Ui.dp(18), Ui.dp(24));
        scroll.addView(form, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT);
        slp.topMargin = Ui.dp(70);
        addView(scroll, slp);

        Label hint = new Label(a, "任何 OpenAI 兼容服务都可以接入。填写 Base URL、Key 与模型名即可。",
                11.4f, M3.ON_SURFACE_3, false);
        form.addView(hint, lp(2, 0, 2, 16));

        Provider p = editingId == null ? null : Providers.find(a, editingId);
        nameField = field(form, "名称", "例如：本地 Qwen", p == null ? "" : p.name, 0);
        urlField = field(form, "Base URL", "https://example.com/v1",
                p == null ? "" : p.baseUrl, InputType.TYPE_TEXT_VARIATION_URI);
        keyField = field(form, "API Key", "sk-...", p == null ? "" : p.apiKey,
                InputType.TYPE_TEXT_VARIATION_PASSWORD);
        modelField = field(form, "模型名称", "例如：qwen2.5-72b-instruct",
                p == null ? "" : p.model, 0);

        LinearLayout actions = new LinearLayout(a);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        form.addView(actions, lp(0, 8, 0, 0));

        M3Button test = new M3Button(a, "测试连接", M3Button.TONAL);
        test.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doTest();
            }
        });
        actions.addView(test, new LinearLayout.LayoutParams(0, Ui.dp(48), 1f));

        M3Button save = new M3Button(a, "保存并设为默认", M3Button.FILLED);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doSave();
            }
        });
        LinearLayout.LayoutParams slp2 = new LinearLayout.LayoutParams(0, Ui.dp(48), 1f);
        slp2.leftMargin = Ui.dp(8);
        actions.addView(save, slp2);

        if (editingId != null) {
            M3Button del = new M3Button(a, "删除这个提供方", M3Button.TEXT);
            del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    M3Dialog.confirmDanger(act.root().overlay, "删除提供方？", "相关配置会被移除。", "删除",
                            new Runnable() {
                                public void run() {
                                    Providers.remove(act, editingId);
                                    act.pop();
                                    act.toast("已删除");
                                }
                            });
                }
            });
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, Ui.dp(44));
            dlp.topMargin = Ui.dp(8);
            form.addView(del, dlp);
        }

        M3.reveal(form);
    }

    private M3Field field(LinearLayout parent, String caption, String hint, String value, int variation) {
        M3Field f = new M3Field(act, caption, hint, value, variation);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(14);
        parent.addView(f, lp);
        return f;
    }

    private LinearLayout.LayoutParams lp(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.leftMargin = Ui.dp(left);
        p.rightMargin = Ui.dp(right);
        p.topMargin = Ui.dp(top);
        p.bottomMargin = Ui.dp(bottom);
        return p;
    }

    private Provider collect() {
        Provider p = new Provider();
        p.id = editingId == null ? Ids.next("p") : editingId;
        p.name = nameField.edit.getText().toString().trim();
        p.baseUrl = urlField.edit.getText().toString().trim();
        p.apiKey = keyField.edit.getText().toString().trim();
        p.model = modelField.edit.getText().toString().trim();
        p.builtin = false;
        return p;
    }

    private boolean validate(Provider p) {
        if (p.name.length() == 0) {
            act.toast("请填写名称");
            return false;
        }
        if (p.baseUrl.length() == 0) {
            act.toast("请填写 Base URL");
            return false;
        }
        if (p.model.length() == 0) {
            act.toast("请填写模型名称");
            return false;
        }
        return true;
    }

    private void doSave() {
        Provider p = collect();
        if (!validate(p)) return;
        Providers.saveCustom(act, p);
        Providers.setDefault(act, p.id);
        Prefs.setString(act, Prefs.KEY_LAST_MODEL, p.model);
        act.pop();
        act.toast("已保存并设为默认");
    }

    private void doTest() {
        final Provider p = collect();
        if (!validate(p)) return;
        act.toast("正在测试连接…");
        ChatClient.ping(p, new ChatClient.PingCallback() {
            public void onResult(final boolean ok, final String message) {
                RequestQueue.get().postMain(new Runnable() {
                    public void run() {
                        M3Dialog.alert(act.root().overlay, ok ? "连接成功" : "连接失败", message);
                    }
                });
            }
        });
    }
}
