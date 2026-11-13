package com.chatyue.app.ui.screen;

import android.view.View;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.data.AgentTools;
import com.chatyue.app.data.Experiment;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.ui.overlay.Dialog;
import com.chatyue.app.ui.widget.CardList;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.MenuRow;
import com.chatyue.app.ui.widget.Toggle;

/** 3.0 实验室：反思 / 多轮 Agent / 全部文件。所有开关默认关闭。 */
public class ExperimentalPage extends BasePage {

    public ExperimentalPage(MainActivity a) {
        super(a, "实验室", "3.0 实验性功能");
        build();
    }

    private void build() {
        resetContent();
        heading("三个还没完全定型的想法",
                "默认全部关闭。开启后行为会变化，但不会影响普通对话。");

        section("反思");
        toggleRow(Icons.SPARKLE, "反思", "答错时额外想一遍，先认错再修复", 
                Prefs.reflectionEnabled(act), new Toggle.Listener() {
                    public void onToggle(boolean checked) {
                        Prefs.setReflectionEnabled(act, checked);
                        act.toast(checked ? "反思已开启：答错时会追加一次修正" : "反思已关闭");
                    }
                });
        note("只在检测到「无法完成 / 出错 / 抱歉」等信号时自动触发；也可以在消息长按菜单里手动让它反思。"
                + "每次反思会多消耗一次模型请求。");

        section("多轮任务");
        toggleRow(Icons.ROBOT, "Agent 多轮任务", "多次思考、调用工具、再继续执行",
                Prefs.agentEnabled(act), new Toggle.Listener() {
                    public void onToggle(boolean checked) {
                        if (checked) {
                            confirmAgent();
                        } else {
                            Prefs.setAgentEnabled(act, false);
                            Prefs.setAllFilesEnabled(act, false);
                            act.toast("Agent 已关闭，全部文件也一并关闭");
                            build();
                        }
                    }
                });
        note("开启后，对话会自动进入 Agent 模式：模型可以列出/读取/创建/修改/删除文件，"
                + "创建文件夹，压缩与解压 zip，访问网页、下载文件，写完 HTML 还能直接预览。"
                + "在终端模式下还可以执行 Android shell 命令（非 root）。");

        section("全部文件");
        if (!Experiment.supportsAllFiles()) {
            rowWithValue(Icons.LOCK, "让 Agent 操控所有文件", "不可用 · 需 Android 5.0+", null);
            note("当前设备低于 Android 5.0。这个开关不会显示为可用，Agent 仍然只能操作应用私有工作区。");
        } else {
            boolean on = Prefs.allFilesEnabled(act);
            boolean granted = Experiment.hasAllFilesPermission(act);
            String desc;
            if (on) {
                desc = granted ? "已授权：Agent 可读写设备存储中的文件" : "已开启，但还没拿到系统授权";
            } else {
                desc = "需先开启 Agent，再授予存储/所有文件权限";
            }
            toggleRow(Icons.FOLDER, "让 Agent 操控所有文件", desc, on, new Toggle.Listener() {
                public void onToggle(boolean checked) {
                    toggleAllFiles(checked);
                }
            });
            if (on && !granted) {
                ghostButton("去系统设置授权", new View.OnClickListener() {
                    public void onClick(View v) {
                        if (!Experiment.openAllFilesSettings(act)) {
                            act.toast("无法打开系统授权页，请在系统设置里手动授予存储权限");
                        }
                    }
                });
            }
            note("仅 Android 5.0 以上可用；Android 11+ 需要「所有文件访问」，Android 6–10 需要存储权限。"
                    + "开启后模型真的可以删除文件，请只在你信任的任务里使用。");
        }

        section("工作目录");
        CardList card = new CardList(act);
        content.addView(card, lp(16, 0, 16, 0));
        MenuRow open = new MenuRow(act, MenuRow.STYLE_CARD);
        open.position(true, false);
        open.icon(Icons.FOLDER, Palette.PINK_SOFT, Palette.PINK_DEEP);
        open.title("打开工作目录");
        open.desc(AgentTools.rootLabel(act) + " · " + AgentTools.root(act).getAbsolutePath());
        open.chevron(true);
        open.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.openWorkspace();
            }
        });
        card.addRow(open);

        MenuRow tools = new MenuRow(act, MenuRow.STYLE_CARD);
        tools.position(false, false);
        tools.divider(false);
        tools.icon(Icons.CODE, Palette.PINK_SOFT, Palette.PINK_DEEP);
        tools.title("Agent 可用工具");
        tools.desc("文件读写 / 压缩解压 / open_url 网页 / download_url 下载 / "
                + "run_command（Android /system/bin/sh）");
        tools.chevron(false);
        card.addRow(tools);

        MenuRow terminal = new MenuRow(act, MenuRow.STYLE_CARD);
        terminal.position(false, true);
        terminal.divider(false);
        terminal.icon(Icons.TERMINAL, Palette.PINK_SOFT, Palette.PINK_DEEP);
        terminal.title("打开终端");
        terminal.desc("Android 系统 shell；AI 在 Agent 模式下也能执行 run_command");
        terminal.chevron(true);
        terminal.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.openTerminal();
            }
        });
        card.addRow(terminal);

        primaryButton("清空工作目录", new View.OnClickListener() {
            public void onClick(View v) {
                Dialog.confirmDanger(act.root().overlay, "清空工作目录？",
                        "里面的文件会全部删除。若已开启「全部文件」，这会指向设备存储根目录，"
                                + "本按钮会拒绝执行。", "清空", new Runnable() {
                            public void run() {
                                if (Experiment.allFilesReady(act)) {
                                    act.toast("全部文件模式下不允许一键清空，请到工作目录里逐个删除");
                                    return;
                                }
                                com.chatyue.app.util.FileUtil.delete(AgentTools.root(act));
                                act.toast("工作目录已清空");
                            }
                        });
            }
        });
        note("实验性功能出错时不会闪退：会被全局错误页接住，显示日志，可以复制、重试或返回。");
    }

    private void confirmAgent() {
        Dialog.confirm(act.root().overlay, "开启 Agent 多轮任务？",
                "模型会自己决定何时创建、修改或删除工作目录里的文件，并且可能要跑好几轮。"
                        + "它可以通过终端执行 shell 命令（非 root），但不能越过工作目录乱删系统文件。",
                "开启", new Runnable() {
                    public void run() {
                        Prefs.setAgentEnabled(act, true);
                        act.toast("Agent 已开启");
                        build();
                    }
                });
    }

    private void toggleAllFiles(boolean checked) {
        if (checked) {
            if (!Prefs.agentEnabled(act)) {
                act.toast("请先开启「Agent 多轮任务」");
                build();
                return;
            }
            if (!Experiment.supportsAllFiles()) {
                act.toast("当前设备低于 Android 5.0，不能开启");
                build();
                return;
            }
            Dialog.confirmDanger(act.root().overlay, "让 Agent 操作所有文件？",
                    "开启并授权后，模型可以读取、修改、删除设备存储里的任意文件。"
                            + "请只在明确知道要做什么的任务里使用。", "开启", new Runnable() {
                        public void run() {
                            Prefs.setAllFilesEnabled(act, true);
                            if (Experiment.sdkInt() >= 30) {
                                if (!Experiment.hasAllFilesPermission(act)) {
                                    if (!Experiment.openAllFilesSettings(act)) {
                                        act.toast("请到系统设置里授予「所有文件」权限");
                                    } else {
                                        act.toast("请在系统设置里允许「所有文件访问」");
                                    }
                                }
                            } else if (Experiment.sdkInt() >= 23) {
                                Experiment.requestPermissions(act, new String[]{
                                        "android.permission.READ_EXTERNAL_STORAGE",
                                        "android.permission.WRITE_EXTERNAL_STORAGE"},
                                        Experiment.REQ_ALL_FILES);
                            }
                            build();
                        }
                    });
        } else {
            Prefs.setAllFilesEnabled(act, false);
            act.toast("已关闭全部文件，工作目录回到应用私有目录");
            build();
        }
    }

    @Override
    public void onPageShown() {
        build();
    }

    public void refreshPermissionState() {
        build();
    }
}
