package com.chatyue.app.ui.screen;

import android.view.View;

import com.chatyue.app.MainActivity;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.ui.widget.FieldBox;

/** 用户画像：称呼、身份、偏好、表达风格。保存后立即随对话发送。 */
public class ProfilePage extends BasePage {

    public ProfilePage(MainActivity a) {
        super(a, "用户画像", "只保存在本机");

        heading("你是怎样的人", "这些信息会随每次请求一起交给模型，让回答更像在跟你说话。");
        space(12);

        final FieldBox nick = field("称呼", "希望我怎么称呼你", Prefs.getString(act, Prefs.KEY_NICKNAME, ""), 1);
        final FieldBox role = field("身份 / 职业", "例如：产品经理、学生、自由摄影师",
                Prefs.getString(act, Prefs.KEY_PROFILE_ROLE, ""), 1);
        final FieldBox traits = field("偏好与禁忌", "例如：不喜欢客套话；少用感叹号；术语要给出处",
                Prefs.getString(act, Prefs.KEY_PROFILE_TRAITS, ""), 3);
        final FieldBox style = field("表达风格", "例如：先说结论，控制在 200 字以内",
                Prefs.getString(act, Prefs.KEY_PROFILE_STYLE, ""), 2);

        note("留空也可以。画像只影响回答方式，不会被写进会话标题或导出内容。");

        primaryButton("保存并生效", new View.OnClickListener() {
            public void onClick(View v) {
                Prefs.setString(act, Prefs.KEY_NICKNAME, nick.text());
                Prefs.setString(act, Prefs.KEY_PROFILE_ROLE, role.text());
                Prefs.setString(act, Prefs.KEY_PROFILE_TRAITS, traits.text());
                Prefs.setString(act, Prefs.KEY_PROFILE_STYLE, style.text());
                act.toast("已保存画像");
                act.pop();
            }
        });
        ghostButton("清空画像", new View.OnClickListener() {
            public void onClick(View v) {
                nick.edit.setText("");
                role.edit.setText("");
                traits.edit.setText("");
                style.edit.setText("");
                act.toast("已清空，记得保存");
            }
        });
    }

}
