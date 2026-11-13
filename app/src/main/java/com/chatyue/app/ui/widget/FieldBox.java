package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;

/**
 * 卡片式输入框：白色圆角容器 + 小标题 + 无边框 EditText。
 * 系统 EditText 默认的下划线与内边距在这里被彻底去掉，避免露出原生外观。
 */
public class FieldBox extends RoundBox {

    public final EditText edit;

    public FieldBox(Context c, String label, String hint, String value, int lines) {
        super(c);
        setFill(Palette.CARD);
        setRadius(18);

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(16), Ui.dp(11), Ui.dp(16), Ui.dp(11));

        if (label != null && label.length() > 0) {
            Label l = new Label(c, label, 11.4f, Palette.INK_3, false);
            col.addView(l, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        edit = new EditText(c);
        edit.setBackgroundDrawable(null);
        edit.setPadding(0, Ui.dp(5), 0, 0);
        edit.setTextSize(14.2f);
        edit.setTextColor(Palette.INK);
        edit.setHintTextColor(Palette.INK_4);
        edit.setHint(hint == null ? "" : hint);
        edit.setTypeface(Typeface.DEFAULT);
        if (value != null && value.length() > 0) edit.setText(value);
        edit.setInputType(InputType.TYPE_CLASS_TEXT
                | (lines > 1 ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
        if (lines > 1) {
            edit.setSingleLine(false);
            edit.setGravity(Gravity.TOP | Gravity.LEFT);
            edit.setMinLines(lines);
        } else {
            edit.setSingleLine(true);
        }
        col.addView(edit, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        addView(col, new FrameLayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public String text() {
        return edit.getText().toString().trim();
    }

    private static class FrameLayoutParams extends android.widget.FrameLayout.LayoutParams {
        FrameLayoutParams(int w, int h) {
            super(w, h);
        }
    }
}
