package com.chatyue.app.ui.screen;

import android.view.View;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.data.Lang;
import com.chatyue.app.ui.widget.CardList;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.MenuRow;

/** 回答语言：切换模型输出语言。 */
public class LanguagePage extends BasePage {

    public LanguagePage(MainActivity a) {
        super(a, "回答语言", Lang.current(a));

        heading("让它说你的语言", "只影响模型回复的语言，界面文案保持中文，避免出现半套翻译。");

        for (int i = 0; i < Lang.NAMES.length; i++) {
            final String name = Lang.NAMES[i];
            MenuRow r = new MenuRow(act, MenuRow.STYLE_CARD);
            r.position(i == 0, false);
            r.icon(i == 0 ? Icons.GLOBE : Icons.DOT, Palette.PINK_SOFT, Palette.PINK_DEEP);
            r.title(name);
            r.checked(name.equals(Lang.current(act)));
            r.chevron(false);
            r.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Lang.set(act, name);
                    act.toast("回答语言：" + name);
                    act.resetTo(new HomePage(act), new SettingsPage(act), new LanguagePage(act));
                }
            });
            card().addRow(r);
        }

        note("切换后，新发出的请求会在系统提示词末尾追加一行语言要求；历史消息保持原样。");

        CardList tip = newCard(18);
        tip.padding(16, 14);
        tip.add(new Label(act, "简体中文 · 繁體中文 · English · 日本語 · 한국어", 12.2f, Palette.INK_3, false));
    }
}
