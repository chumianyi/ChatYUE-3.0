package com.chatyue.app.ui.screen;

import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.AgentTools;
import com.chatyue.app.data.Experiment;
import com.chatyue.app.data.ShellRunner;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.widget.BounceScrollView;
import com.chatyue.app.ui.widget.IconButton;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.RoundBox;
import com.chatyue.app.ui.widget.SoftButton;
import com.chatyue.app.ui.widget.TopBar;

import java.io.File;

/**
 * Android 系统终端：固定 /system/bin/sh，不依赖 Termux。
 *
 * 同一个 ShellRunner 也被 Agent 的 run_command 工具复用；终端页只是把它
 * 变成可交互的 UI。Python 只有在系统或 Termux 里存在时才会被识别。
 */
public class TerminalPage extends Page {

    private final TextView output;
    private final EditText command;
    private final BounceScrollView scroll;
    private final Label status;
    private File cwd;
    private boolean running;

    public TerminalPage(MainActivity a) {
        super(a);
        setBackgroundColor(Palette.BG);

        TopBar bar = new TopBar(a, "终端", "Android 系统 shell", Icons.TRASH);
        bar.left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hideKeyboard();
                act.pop();
            }
        });
        if (bar.right != null) {
            bar.right.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    output.setText(header());
                    act.toast("已清空终端");
                }
            });
        }
        addView(bar, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, Ui.dp(66)));

        LinearLayout body = new LinearLayout(a);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(12), Ui.dp(2), Ui.dp(12), Ui.dp(8));

        scroll = new BounceScrollView(a);
        scroll.setBounceEnabled(true);
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(0), Ui.dp(0), Ui.dp(0), Ui.dp(8));

        status = new Label(a, "", 11f, Palette.INK_3, false);
        status.setPadding(Ui.dp(4), Ui.dp(4), Ui.dp(4), Ui.dp(6));
        col.addView(status, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        RoundBox terminal = new RoundBox(a);
        terminal.setFill(0xFF11161B);
        terminal.setStroke(0xFF2A333B, 0.8f);
        terminal.setRadius(16);
        terminal.setPadding(Ui.dp(12), Ui.dp(12), Ui.dp(12), Ui.dp(12));
        output = new TextView(a);
        output.setTextColor(0xFFD8F3D2);
        output.setTextSize(11.6f);
        output.setTypeface(Typeface.MONOSPACE);
        output.setLineSpacing(Ui.dpf(2), 1f);
        if (Experiment.sdkInt() >= 11) output.setTextIsSelectable(true);
        output.setText(header());
        terminal.addView(output, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        col.addView(terminal, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        scroll.addView(col, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        body.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, 0, 1f));

        LinearLayout quick = new LinearLayout(a);
        quick.setOrientation(LinearLayout.HORIZONTAL);
        quick.setPadding(0, Ui.dp(4), 0, Ui.dp(6));
        quick.addView(chip("ls", "ls -la"), lpW(0));
        quick.addView(chip("pwd", "pwd"), lpW(6));
        quick.addView(chip("python", "command -v python3 || command -v python"), lpW(6));
        quick.addView(chip("help", "help"), lpW(6));
        body.addView(quick, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        RoundBox inputBox = new RoundBox(a);
        inputBox.setFill(Palette.CARD);
        inputBox.setStroke(Palette.LINE, 0.8f);
        inputBox.setRadius(22);
        inputBox.setPadding(Ui.dp(12), Ui.dp(5), Ui.dp(6), Ui.dp(5));
        LinearLayout inputRow = new LinearLayout(a);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);

        Label prompt = new Label(a, "$", 14f, Palette.PINK_DEEP, true);
        prompt.setPadding(0, 0, Ui.dp(6), 0);
        inputRow.addView(prompt, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        command = new EditText(a);
        command.setBackgroundDrawable(null);
        command.setHint("输入命令，回车执行");
        command.setHintTextColor(Palette.INK_4);
        command.setTextColor(Palette.INK);
        command.setTextSize(13.2f);
        command.setTypeface(Typeface.MONOSPACE);
        command.setSingleLine(true);
        command.setInputType(InputType.TYPE_CLASS_TEXT);
        command.setImeOptions(EditorInfo.IME_ACTION_SEND);
        command.setPadding(0, Ui.dp(4), 0, Ui.dp(4));
        command.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND
                        || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                    run(command.getText().toString());
                    return true;
                }
                return false;
            }
        });
        inputRow.addView(command, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        IconButton send = new IconButton(a, Icons.SEND, Palette.PINK, 0xFFFFFFFF, 34, 15);
        send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                run(command.getText().toString());
            }
        });
        inputRow.addView(send, new LinearLayout.LayoutParams(Ui.dp(34), Ui.dp(34)));

        inputBox.addView(inputRow, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        body.addView(inputBox, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Label note = new Label(a,
                "命令在 " + ShellRunner.shellPath() + " 中执行；默认 15 秒超时、64 KB 输出。"
                        + " AI 可在 Agent 模式下调用 run_command。",
                10.4f, Palette.INK_3, false);
        note.setPadding(Ui.dp(2), Ui.dp(6), Ui.dp(2), 0);
        body.addView(note, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        FrameLayout.LayoutParams bodyLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT);
        bodyLp.topMargin = Ui.dp(66);
        addView(body, bodyLp);

        cwd = AgentTools.root(a);
        status.setText("shell " + ShellRunner.shellPath() + " · cwd " + cwd.getAbsolutePath() + " · 输入 help 查看示例");
    }

    private String header() {
        return "ChatYUE Terminal 3.0\n"
                + "shell  " + ShellRunner.shellPath() + "\n"
                + "cwd    " + (cwd == null ? AgentTools.root(act).getAbsolutePath() : cwd.getAbsolutePath()) + "\n"
                + "输入 help 查看示例；危险命令会被拦截。\n\n";
    }

    private View chip(String title, final String cmd) {
        SoftButton b = new SoftButton(act, title, 11.4f, Palette.INK_2, Palette.CARD);
        b.radius(12).pad(10, 6).stroke(Palette.LINE, 0.7f);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                command.setText(cmd);
                command.setSelection(command.getText().length());
                command.requestFocus();
                showKeyboard();
            }
        });
        return b;
    }

    private LinearLayout.LayoutParams lpW(int left) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.leftMargin = Ui.dp(left);
        return p;
    }

    private void run(String raw) {
        if (running) {
            act.toast("上一条命令还在执行");
            return;
        }
        final String line = raw == null ? "" : raw.trim();
        if (line.length() == 0) return;
        command.setText("");
        hideKeyboard();
        output.append("$ " + line + "\n");
        scrollToBottom();

        if ("clear".equals(line)) {
            output.setText(header());
            return;
        }
        if ("help".equals(line)) {
            output.append("示例：\n"
                    + "  ls -la             列出当前目录\n"
                    + "  cat a.txt          查看文本\n"
                    + "  mkdir -p pages     新建目录\n"
                    + "  command -v python3 探测 Python\n"
                    + "  python3 -c \"print(1+1)\"  有 Python 时执行\n"
                    + "  cd /sdcard         切换终端工作目录\n"
                    + "  cd ..              返回上一级\n\n");
            scrollToBottom();
            return;
        }
        if (line.startsWith("cd ") || line.equals("cd")) {
            String arg = line.length() <= 3 ? "" : line.substring(3).trim();
            File next = arg.length() == 0 ? AgentTools.root(act) : new File(arg);
            if (!next.isAbsolute()) next = new File(cwd, arg);
            try {
                next = new File(next.getCanonicalPath());
            } catch (Throwable ignored) {
            }
            if (next.exists() && next.isDirectory()) {
                cwd = next;
                output.append("cwd " + cwd.getAbsolutePath() + "\n\n");
            } else {
                output.append("cd: 目录不存在：" + arg + "\n\n");
            }
            scrollToBottom();
            return;
        }

        running = true;
        status.setText("正在执行…");
        final String cmd = line;
        final File dir = cwd;
        new Thread(new Runnable() {
            public void run() {
                final ShellRunner.Result r = ShellRunner.run(act, cmd, dir, ShellRunner.DEFAULT_TIMEOUT);
                act.runOnUiThread(new Runnable() {
                    public void run() {
                        running = false;
                        String text = r.output == null ? "" : r.output;
                        if (text.length() > 0) output.append(text);
                        if (!text.endsWith("\n")) output.append("\n");
                        output.append("[exit " + r.exit + (r.timeout ? " · timeout" : "") + "]\n\n");
                        status.setText("cwd " + dir.getAbsolutePath() + " · shell " + ShellRunner.shellPath());
                        scrollToBottom();
                    }
                });
            }
        }).start();
    }

    private void scrollToBottom() {
        output.post(new Runnable() {
            public void run() {
                scroll.scrollToBottomNow();
            }
        });
    }

    private void showKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) act.getSystemService(
                    android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(command, 0);
        } catch (Throwable ignored) {
        }
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) act.getSystemService(
                    android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(command.getWindowToken(), 0);
        } catch (Throwable ignored) {
        }
    }
}
