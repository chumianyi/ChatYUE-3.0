package com.chatyue.app.ui.m3;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.core.Ui;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.RoundBox;

/** M3 表单字段：标签 + 浅色填充输入框，聚焦时只变描边，不出现系统下划线。 */
public class M3Field extends LinearLayout {

    public final EditText edit;

    public M3Field(Context c, String caption, String hint, String value, int variation) {
        super(c);
        setOrientation(VERTICAL);

        Label label = new Label(c, caption, 10.8f, M3.ON_SURFACE_2, true);
        label.setPadding(Ui.dp(4), 0, 0, Ui.dp(7));
        addView(label, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

        RoundBox box = new RoundBox(c);
        box.setFill(M3.SURFACE_LOW);
        box.setRadius(20);
        box.setStroke(M3.OUTLINE_VARIANT, 1f);
        box.setPadding(Ui.dp(16), Ui.dp(4), Ui.dp(16), Ui.dp(4));

        edit = new EditText(c);
        edit.setBackgroundDrawable(null);
        edit.setPadding(0, Ui.dp(8), 0, Ui.dp(8));
        edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.4f);
        edit.setTextColor(M3.ON_SURFACE);
        edit.setHintTextColor(M3.ON_SURFACE_3);
        edit.setHint(hint == null ? "" : hint);
        edit.setSingleLine(true);
        edit.setGravity(Gravity.CENTER_VERTICAL);
        int type = InputType.TYPE_CLASS_TEXT;
        if (variation != 0) type = type | variation;
        edit.setInputType(type);
        if (value != null && value.length() > 0) edit.setText(value);
        box.addView(edit, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, Ui.dp(48)));
        addView(box, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
    }
}
