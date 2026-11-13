package com.chatyue.app.ui.screen;

import android.view.Gravity;
import android.widget.LinearLayout;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Theme;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.ui.widget.CardList;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.RoundBox;
import com.chatyue.app.ui.widget.Stepper;
import com.chatyue.app.ui.widget.ThemeGrid;

/** 外观：13 套主题 + 字号 + 实时预览。 */
public class AppearancePage extends BasePage {

    public AppearancePage(MainActivity a) {
        super(a, "外观", Theme.name(Prefs.theme(a)));

        heading("换个喜欢的模样", "主题对全部自绘控件即时生效：气泡、按钮、浮层、代码块都会跟着变。");

        ThemeGrid grid = new ThemeGrid(act);
        grid.selected(Prefs.theme(act)).listener(new ThemeGrid.Listener() {
            public void onPick(int index) {
                apply(index);
            }
        });
        CardList themeCard = newCard(8);
        themeCard.padding(8, 12);
        themeCard.add(grid);

        section("文字");
        stepperRow(Icons.DOC, "字体大小", "对话正文与界面文字一起缩放",
                Prefs.fontPercent(act), 85, 130, 5, "%", new Stepper.Listener() {
                    public void onValue(int value) {
                        Prefs.setFontPercent(act, value);
                        act.toast("字号已设为 " + value + "%");
                    }
                });
        note("字号会同时影响消息正文、代码块与说明文字；已打开的历史消息会按新字号重新排版。");

        section("预览");
        previewCard();

        note("整套视觉没有使用 Material 组件：圆角、阴影、按压反馈、浮层动效都由 Canvas 绘制，"
                + "所以换主题时不会出现「系统控件不跟随」的割裂感。");
    }

    private void previewCard() {
        CardList card = newCard(6);
        card.padding(14, 16);

        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);

        Label who = new Label(act, "你", 11f, Palette.INK_3, false);
        who.setPadding(Ui.dp(4), 0, 0, Ui.dp(5));
        who.setAlign(Label.RIGHT);
        col.addView(who, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        RoundBox user = new RoundBox(act);
        user.setFill(Palette.USER_BUBBLE);
        user.setRadius(18);
        user.corners(18, 18, 6, 18);
        Label ut = new Label(act, "这个主题挺好看。", 14f, Palette.INK, false);
        ut.setPadding(Ui.dp(14), Ui.dp(10), Ui.dp(14), Ui.dp(10));
        user.addView(ut);
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ulp.gravity = Gravity.RIGHT;
        col.addView(user, ulp);

        RoundBox bot = new RoundBox(act);
        bot.setFill(Palette.CARD);
        bot.setStroke(Palette.LINE, 1);
        bot.setRadius(18);
        bot.corners(18, 18, 18, 6);
        Label bt = new Label(act, "换了主题之后，连代码块和浮层都会跟着变。", 14f, Palette.INK, false);
        bt.setPadding(Ui.dp(14), Ui.dp(10), Ui.dp(14), Ui.dp(10));
        bot.addView(bt);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        blp.topMargin = Ui.dp(10);
        blp.gravity = Gravity.LEFT;
        col.addView(bot, blp);

        card.add(col);
    }

    private void apply(int index) {
        Prefs.setTheme(act, index);
        Palette.apply(index);
        act.toast("已切换：" + Theme.name(index));
        // 主题决定整套页面：经典主题进 M3，其他主题回默认手绘页面
        act.rebuildAfterTheme();
    }

}
