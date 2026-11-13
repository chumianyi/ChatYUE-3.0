package com.chatyue.app.ui.screen;

import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Ids;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.data.Provider;
import com.chatyue.app.data.Providers;
import com.chatyue.app.net.ChatClient;
import com.chatyue.app.net.RequestQueue;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.overlay.Dialog;
import com.chatyue.app.ui.widget.BounceScrollView;
import com.chatyue.app.ui.widget.IconButton;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.RoundBox;
import com.chatyue.app.ui.widget.SoftButton;

/** 第三方模型配置页。内置的 ChatYUE 不会走到这里。 */
public class ProviderEditPage extends Page {

    private String editingId;
    private EditText nameField;
    private EditText urlField;
    private EditText keyField;
    private EditText modelField;

    public ProviderEditPage(MainActivity a, String id) {
        super(a);
        // 内置 ChatYUE 的地址与密钥是托管的，任何入口都不允许进入编辑态
        editingId = Providers.BUILTIN_ID.equals(id) ? null : id;

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        addView(root, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));

        FrameLayout header = new FrameLayout(a);
        IconButton back = new IconButton(a, Icons.BACK, 0, Palette.INK, 38, 20);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.pop();
            }
        });
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(Ui.dp(38), Ui.dp(38));
        blp.leftMargin = Ui.dp(12);
        blp.topMargin = Ui.dp(8);
        header.addView(back, blp);
        Label title = new Label(a, editingId == null ? "添加模型" : "编辑模型", 22f, Palette.INK, true);
        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.LEFT | Gravity.BOTTOM);
        tlp.leftMargin = Ui.dp(18);
        tlp.bottomMargin = Ui.dp(6);
        header.addView(title, tlp);
        root.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, Ui.dp(88)));

        BounceScrollView scroll = new BounceScrollView(a);
        LinearLayout form = new LinearLayout(a);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(Ui.dp(18), Ui.dp(6), Ui.dp(18), Ui.dp(24));

        form.addView(hint("任何 OpenAI 兼容服务都可以接入：填写 Base URL、API Key 与模型名即可。"),
                margin(0, 0, 0, 14));

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
        form.addView(actions, margin(0, 6, 0, 0));

        SoftButton test = new SoftButton(a, "测试连接", 14f, Palette.INK, Palette.BG_DEEP);
        test.pad(12, 12).radius(15);
        test.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doTest(false);
            }
        });
        LinearLayout.LayoutParams tlp2 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tlp2.rightMargin = Ui.dp(5);
        actions.addView(test, tlp2);

        SoftButton save = new SoftButton(a, "保存并设为默认", 14f, 0xFFFFFFFF, Palette.DARK);
        save.pad(12, 12).radius(15);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doSave();
            }
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        slp.leftMargin = Ui.dp(5);
        actions.addView(save, slp);

        if (editingId != null) {
            SoftButton del = new SoftButton(a, "删除这个提供方", 13.6f, Palette.RED, 0xFFFFF1F1);
            del.pad(12, 11).radius(14);
            del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Dialog.confirmDanger(act.root().overlay, "删除提供方？", "相关配置会被移除。", "删除",
                            new Runnable() {
                                public void run() {
                                    Providers.remove(act, editingId);
                                    act.pop();
                                    act.toast("已删除");
                                }
                            });
                }
            });
            form.addView(del, margin(0, 10, 0, 0));
        }

        scroll.addView(form, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, 0, 1f));
    }

    private View hint(String text) {
        return new Label(act, text, 12.4f, Palette.INK_3, false);
    }

    private LinearLayout.LayoutParams margin(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = Ui.dp(l);
        lp.topMargin = Ui.dp(t);
        lp.rightMargin = Ui.dp(r);
        lp.bottomMargin = Ui.dp(b);
        return lp;
    }

    private EditText field(LinearLayout parent, String caption, String hintText, String value, int variation) {
        RoundBox box = new RoundBox(act);
        box.setFill(Palette.CARD);
        box.setRadius(16);
        box.setShadow(Palette.SHADOW, 6);
        box.setPadding(Ui.dp(14), Ui.dp(10), Ui.dp(14), Ui.dp(10));

        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        box.addView(col, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        col.addView(new Label(act, caption, 11.2f, Palette.INK_3, false),
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        EditText edit = new EditText(act);
        edit.setBackgroundDrawable(null);
        edit.setPadding(0, Ui.dp(4), 0, Ui.dp(2));
        edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.4f);
        edit.setTextColor(Palette.INK);
        edit.setHintTextColor(Palette.INK_4);
        edit.setHint(hintText);
        edit.setSingleLine(true);
        int type = InputType.TYPE_CLASS_TEXT;
        if (variation != 0) type = type | variation;
        edit.setInputType(type);
        if (value != null && value.length() > 0) edit.setText(value);
        col.addView(edit, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        parent.addView(box, margin(0, 0, 0, 12));
        return edit;
    }

    private Provider collect() {
        Provider p = new Provider();
        p.id = editingId == null ? Ids.next("p") : editingId;
        p.name = nameField.getText().toString().trim();
        p.baseUrl = urlField.getText().toString().trim();
        p.apiKey = keyField.getText().toString().trim();
        p.model = modelField.getText().toString().trim();
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

    private void doTest(boolean unused) {
        final Provider p = collect();
        if (!validate(p)) return;
        act.toast("正在测试连接…");
        ChatClient.ping(p, new ChatClient.PingCallback() {
            public void onResult(final boolean ok, final String message) {
                RequestQueue.get().postMain(new Runnable() {
                    public void run() {
                        Dialog.alert(act.root().overlay, ok ? "连接成功" : "连接失败", message);
                    }
                });
            }
        });
    }
}
