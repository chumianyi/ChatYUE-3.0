package com.chatyue.app.ui.screen;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Palette;
import com.chatyue.app.data.Skills;
import com.chatyue.app.ui.widget.CardList;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.Toggle;

/** 技能：一组可开关的回答约束。 */
public class SkillPage extends BasePage {

    public SkillPage(MainActivity a) {
        super(a, "技能", Skills.enabledCount(a) + " 项已开启");
        build();
    }

    private void build() {
        resetContent();
        heading("让它按你的规矩来", "技能不是插件，而是一段写进系统提示词的行为约束。");
        space(6);

        Skills.Skill[] list = Skills.all();
        for (int i = 0; i < list.length; i++) {
            final Skills.Skill s = list[i];
            toggleRow(s.icon, s.name, s.desc, Skills.enabled(act, s.id), new Toggle.Listener() {
                public void onToggle(boolean checked) {
                    Skills.setEnabled(act, s.id, checked);
                    bar.setSubtitle(Skills.enabledCount(act) + " 项已开启");
                }
            });
        }

        note("开启越多，提示词越长，模型的注意力越分散。建议同时不超过 3 项。");

        section("实际发送的约束");
        CardList card = newCard(0);
        card.padding(16, 14);
        String text = Skills.compose(act);
        card.add(new Label(act, text.length() == 0 ? "（当前没有开启任何技能）" : text,
                12.2f, Palette.INK_2, false));
    }
}
