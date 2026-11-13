package com.chatyue.app.ui.screen;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.MainActivity;
import com.chatyue.app.content.FileSharing;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.AgentTools;
import com.chatyue.app.data.Experiment;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.overlay.Dialog;
import com.chatyue.app.ui.overlay.Sheet;
import com.chatyue.app.ui.widget.BounceScrollView;
import com.chatyue.app.ui.widget.CardList;
import com.chatyue.app.ui.widget.EmptyState;
import com.chatyue.app.ui.widget.Label;
import com.chatyue.app.ui.widget.MenuRow;
import com.chatyue.app.ui.widget.SoftButton;
import com.chatyue.app.ui.widget.TopBar;

import java.io.File;
import java.util.ArrayList;

/** 工作目录浏览器：查看 / 新建 / 分享 / 重命名 / 删除，也是 Agent 的落地窗口。 */
public class WorkspacePage extends Page {

    private final File dir;
    private final LinearLayout content;
    private final TopBar bar;

    public WorkspacePage(MainActivity a, File d) {
        super(a);
        dir = d;

        bar = new TopBar(a, titleOf(d), subtitleOf(d), Icons.MORE);
        bar.left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.pop();
            }
        });
        if (bar.right != null) {
            bar.right.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    moreSheet();
                }
            });
        }
        addView(bar, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, Ui.dp(66)));

        BounceScrollView scroll = new BounceScrollView(a);
        content = new LinearLayout(a);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, Ui.dp(2), 0, Ui.dp(26));
        scroll.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT);
        slp.topMargin = Ui.dp(66);
        addView(scroll, slp);

        build();
    }

    private String titleOf(File f) {
        if (f == null) return "工作目录";
        File root = AgentTools.root(act);
        if (same(f, root)) return "工作目录";
        return f.getName();
    }

    private String subtitleOf(File f) {
        try {
            String rel = AgentTools.relative(act, f);
            return (Experiment.allFilesReady(act) ? "全部文件 · " : "应用工作区 · ") + rel;
        } catch (Throwable t) {
            return "文件浏览";
        }
    }

    private boolean same(File a, File b) {
        try {
            return a.getCanonicalPath().equals(b.getCanonicalPath());
        } catch (Throwable t) {
            return false;
        }
    }

    private void build() {
        content.removeAllViews();
        final File root = AgentTools.root(act);
        boolean inside = insideRoot(dir);

        if (!inside || !dir.exists() || !dir.isDirectory()) {
            content.addView(new EmptyState(act, Icons.FOLDER, "目录不可用",
                    "它可能被删除，或者不在当前工作目录里。"), lp(0, 20, 0, 0));
            return;
        }

        if (!same(dir, root)) {
            SoftButton up = new SoftButton(act, "← 返回上一级", 12.8f, Palette.INK_2, Palette.CARD);
            up.radius(14).pad(14, 9).stroke(Palette.LINE, 0.8f);
            up.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    File p = dir.getParentFile();
                    act.pop();
                    if (p != null && insideRoot(p)) act.push(new WorkspacePage(act, p));
                    else act.push(new WorkspacePage(act, root));
                }
            });
            content.addView(up, lp(16, 8, 16, 0));
        }

        Label path = new Label(act, root.equals(dir) ? root.getAbsolutePath()
                : AgentTools.relative(act, dir), 11f, Palette.INK_3, false);
        path.setPadding(Ui.dp(2), 0, 0, 0);
        content.addView(path, lp(20, 12, 20, 6));

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            content.addView(new EmptyState(act, Icons.FOLDER, "这里还是空的",
                    "点右上角「···」新建文件夹或文本文件，或在对话里让 Agent 放点东西进来。"),
                    lp(0, 10, 0, 0));
            return;
        }
        sort(files);

        CardList card = new CardList(act);
        content.addView(card, lp(16, 0, 16, 0));
        for (int i = 0; i < files.length && i < 500; i++) {
            final File f = files[i];
            MenuRow row = new MenuRow(act, MenuRow.STYLE_CARD);
            row.position(i == 0, i == Math.min(files.length, 500) - 1);
            row.icon(iconFor(f), Palette.PINK_SOFT, Palette.PINK_DEEP);
            row.title(f.getName());
            row.desc(descFor(f));
            row.chevron(f.isDirectory());
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (f.isDirectory()) act.push(new WorkspacePage(act, f));
                    else act.openFile(f);
                }
            });
            row.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    fileMenu(f);
                    return true;
                }
            });
            card.addRow(row);
        }

        Label note = new Label(act,
                "长按文件可分享、重命名或删除。当前模式：" + AgentTools.rootLabel(act)
                        + (Experiment.allFilesReady(act) ? "（Agent 可读写全部文件）" : "（仅应用私有工作区）"),
                11.2f, Palette.INK_3, false);
        content.addView(note, lp(22, 10, 22, 0));
    }

    private LinearLayout.LayoutParams lp(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.leftMargin = Ui.dp(l);
        p.topMargin = Ui.dp(t);
        p.rightMargin = Ui.dp(r);
        p.bottomMargin = Ui.dp(b);
        return p;
    }

    private void sort(File[] files) {
        for (int i = 0; i < files.length; i++) {
            for (int j = i + 1; j < files.length; j++) {
                boolean di = files[i].isDirectory();
                boolean dj = files[j].isDirectory();
                if ((!di && dj) || (di == dj && files[j].getName().compareToIgnoreCase(files[i].getName()) < 0)) {
                    File t = files[i];
                    files[i] = files[j];
                    files[j] = t;
                }
            }
        }
    }

    private int iconFor(File f) {
        if (f.isDirectory()) return Icons.FOLDER;
        String ext = AgentTools.ext(f.getName());
        if ("html".equals(ext) || "htm".equals(ext)) return Icons.GLOBE;
        if ("css".equals(ext) || "js".equals(ext) || "json".equals(ext) || "java".equals(ext)
                || "py".equals(ext)) return Icons.CODE;
        if ("png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext) || "gif".equals(ext))
            return Icons.IMAGE;
        return Icons.DOC;
    }

    private String descFor(File f) {
        if (f.isDirectory()) {
            File[] k = f.listFiles();
            return "文件夹" + (k == null ? "" : " · " + k.length + " 项");
        }
        return AgentTools.sizeText(f.length()) + " · " + f.getName().substring(
                Math.max(0, f.getName().lastIndexOf('.') + 1)).toUpperCase();
    }

    private boolean insideRoot(File f) {
        try {
            String root = AgentTools.root(act).getCanonicalPath();
            String p = f.getCanonicalPath();
            return p.equals(root) || p.startsWith(root + File.separator);
        } catch (Throwable t) {
            return false;
        }
    }

    private void moreSheet() {
        Sheet s = new Sheet(act).title("工作目录").subtitle(subtitleOf(dir));
        s.add(Icons.FOLDER, "新建文件夹", new Runnable() {
            public void run() {
                act.createFolderHere(dir, new Runnable() {
                    public void run() {
                        build();
                    }
                });
            }
        });
        s.add(Icons.DOC, "新建文本文件", new Runnable() {
            public void run() {
                act.createFileHere(dir, new Runnable() {
                    public void run() {
                        build();
                    }
                });
            }
        });
        s.add(Icons.REFRESH, "刷新", new Runnable() {
            public void run() {
                build();
            }
        });
        s.show(act.root().overlay);
    }

    private void fileMenu(final File f) {
        Sheet s = new Sheet(act).title(f.getName()).subtitle(AgentTools.sizeText(f.length()));
        s.add(Icons.SHARE, "分享", new Runnable() {
            public void run() {
                ArrayList<File> list = new ArrayList<File>();
                list.add(f);
                FileSharing.share(act, list);
            }
        });
        if (!f.isDirectory()) {
            s.add(Icons.DOWNLOAD, "保存到本地", new Runnable() {
                public void run() {
                    act.saveArtifact(f);
                }
            });
        }
        s.add(Icons.EDIT, "重命名", new Runnable() {
            public void run() {
                Dialog.input(act.root().overlay, "重命名", "新名称", f.getName(),
                        new Dialog.InputCallback() {
                            public void onInput(String text) {
                                String name = com.chatyue.app.data.Attachments.sanitize(text);
                                if (name.length() == 0) return;
                                File to = new File(f.getParentFile(), name);
                                if (!f.renameTo(to)) act.toast("重命名失败");
                                build();
                            }
                        });
            }
        });
        s.addDanger(Icons.TRASH, "删除", new Runnable() {
            public void run() {
                Dialog.confirmDanger(act.root().overlay, "删除 " + f.getName() + "？",
                        "此操作不可恢复。", "删除", new Runnable() {
                            public void run() {
                                com.chatyue.app.util.FileUtil.delete(f);
                                build();
                            }
                        });
            }
        });
        s.show(act.root().overlay);
    }

    @Override
    public void onPageShown() {
        bar.setTitle(titleOf(dir));
        bar.setSubtitle(subtitleOf(dir));
    }
}
