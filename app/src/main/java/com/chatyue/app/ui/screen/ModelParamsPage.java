package com.chatyue.app.ui.screen;

import android.view.View;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Lang;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.ui.widget.CardList;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.MenuRow;
import com.chatyue.app.ui.widget.Stepper;

/** 模型参数：温度、top_p、单次上限。直接作用于 /chat/completions 请求体。 */
public class ModelParamsPage extends BasePage {

    public ModelParamsPage(MainActivity a) {
        super(a, "模型参数", "作用于每一次请求");

        heading("模型的脾气", "数值越大越发散。日常对话建议温度 0.6–0.8。");

        stepperRow(Icons.SPARKLE, "温度", "低更稳定，高更有想象力",
                Prefs.temperature(act), 0, 150, 5, "", new Stepper.Listener() {
                    public void onValue(int value) {
                        Prefs.setInt(act, Prefs.KEY_TEMP, value);
                    }
                }).display(100, 1);

        stepperRow(Icons.STAR, "Top P", "候选词累计概率上限",
                Prefs.topP(act), 10, 100, 5, "", new Stepper.Listener() {
                    public void onValue(int value) {
                        Prefs.setInt(act, Prefs.KEY_TOPP, value);
                    }
                }).display(100, 1);

        stepperRow(Icons.DOC, "单次上限", "一条回复的最大长度",
                Prefs.maxTokens(act), 512, 8192, 512, "", new Stepper.Listener() {
                    public void onValue(int value) {
                        Prefs.setInt(act, Prefs.KEY_MAXTOKENS, value);
                    }
                });

        note("参数会写进请求体。部分第三方服务会忽略 top_p 或 max_tokens，这不影响对话本身。");

        section("当前请求");
        CardList card = newCard(0);
        card.padding(16, 14);
        card.add(line("模型", Prefs.getString(act, Prefs.KEY_LAST_MODEL, "GLM-4.1V-Thinking-Flash")));
        card.add(line("温度", String.valueOf(Prefs.temperature(act) / 100f)));
        card.add(line("Top P", String.valueOf(Prefs.topP(act) / 100f)));
        card.add(line("最大长度", Prefs.maxTokens(act) + " tokens"));

        primaryButton("恢复默认", new View.OnClickListener() {
            public void onClick(View v) {
                Prefs.setInt(act, Prefs.KEY_TEMP, 70);
                Prefs.setInt(act, Prefs.KEY_TOPP, 90);
                Prefs.setInt(act, Prefs.KEY_MAXTOKENS, 2048);
                act.toast("已恢复默认参数");
                act.resetTo(new HomePage(act), new SettingsPage(act), new ModelParamsPage(act));
            }
        });
    }

    private Label line(String left, String right) {
        Label l = new Label(act, left + "　　" + right, 12.6f, Palette.INK_2, false);
        l.setPadding(0, Ui.dp(5), 0, Ui.dp(5));
        return l;
    }
}
