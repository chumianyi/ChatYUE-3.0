package com.chatyue.app.ui.m3;

import android.graphics.Paint;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Ui;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.widget.IconButton;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.PhotoView;
import com.chatyue.app.ui.widget.RoundBox;

/** 经典主题图片查看页：深色留白 + 大圆角图片卡，避免跳回默认主题的页面。 */
public class M3ImagePreviewPage extends Page {

    public M3ImagePreviewPage(MainActivity a, final String path) {
        super(a);
        setBackgroundColor(0xFF191314);

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        addView(root, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));

        LinearLayout header = new LinearLayout(a);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(Ui.dp(10), Ui.dp(10), Ui.dp(16), Ui.dp(2));

        IconButton back = new IconButton(a, Icons.BACK, 0, 0xFFFFFFFF, 42, 22);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.pop();
            }
        });
        header.addView(back, new LinearLayout.LayoutParams(Ui.dp(42), Ui.dp(42)));

        Label title = new Label(a, "图片", 18f, 0xFFFFFFFF, true);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = Ui.dp(10);
        header.addView(title, tlp);

        Label done = new Label(a, "完成", 13f, 0xFFFFC7CB, true);
        done.setPadding(Ui.dp(10), Ui.dp(8), 0, Ui.dp(8));
        done.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.pop();
            }
        });
        header.addView(done, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, Ui.dp(62)));

        LinearLayout middle = new LinearLayout(a);
        middle.setOrientation(LinearLayout.VERTICAL);
        middle.setGravity(Gravity.CENTER);
        middle.setPadding(Ui.dp(16), 0, Ui.dp(16), Ui.dp(16));
        root.addView(middle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, 0, 1f));

        RoundBox card = new RoundBox(a);
        card.setFill(0xFF2A2224);
        card.setRadius(28);
        card.setPadding(Ui.dp(8), Ui.dp(8), Ui.dp(8), Ui.dp(8));
        PhotoView pv = new PhotoView(a);
        pv.setPath(path);
        card.addView(pv, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        middle.addView(card, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Label hint = new Label(a, "图片只保存在这台设备", 11.4f, 0xFF9E8B8E, false);
        hint.setAlign(Label.CENTER);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = Ui.dp(14);
        middle.addView(hint, hlp);
    }
}
