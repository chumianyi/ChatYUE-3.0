package com.chatyue.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.AgentTools;
import com.chatyue.app.data.Message;

import java.io.File;
import java.util.ArrayList;

/**
 * 本次任务的产物卡片。
 * 默认最多展示 5 个文件；更多的收进「查看全部附件」。每行可单独分享/保存。
 */
public class ArtifactCard extends RoundBox {

    public interface Listener {
        void onOpen(File f);

        void onShare(File f);

        void onShareAll(ArrayList<String> paths);

        void onSave(File f);

        void onSeeAll(ArrayList<String> paths);
    }

    private final MainActivity act;
    private final LinearLayout col;
    private Listener listener;

    public ArtifactCard(MainActivity a, Message msg) {
        super(a);
        act = a;
        setFill(Palette.CARD_SOFT);
        setStroke(Palette.PINK_LINE, 0.8f);
        setRadius(14);
        setPadding(Ui.dp(11), Ui.dp(10), Ui.dp(11), Ui.dp(9));

        col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        addView(col, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        build(msg);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    private void build(Message msg) {
        final ArrayList<File> files = new ArrayList<File>();
        for (int i = 0; i < msg.artifacts.size(); i++) {
            File f = new File(msg.artifacts.get(i));
            if (f.exists()) files.add(f);
        }
        if (files.isEmpty()) {
            setVisibility(GONE);
            return;
        }

        LinearLayout head = new LinearLayout(act);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        FileGlyph glyph = new FileGlyph(act, Icons.FOLDER);
        head.addView(glyph, new LinearLayout.LayoutParams(Ui.dp(28), Ui.dp(28)));

        Label title = new Label(act, "本次产物 · " + files.size() + " 个文件", 11.6f,
                Palette.INK, true);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = Ui.dp(8);
        head.addView(title, tlp);

        IconButton shareAll = new IconButton(act, Icons.SHARE, 0, Palette.PINK_DEEP, 24, 12);
        shareAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (listener != null) {
                    ArrayList<String> all = new ArrayList<String>();
                    for (int i = 0; i < files.size(); i++) all.add(files.get(i).getAbsolutePath());
                    listener.onShareAll(all);
                }
            }
        });
        head.addView(shareAll, new LinearLayout.LayoutParams(Ui.dp(24), Ui.dp(24)));
        col.addView(head);

        int limit = Math.min(5, files.size());
        for (int i = 0; i < limit; i++) {
            final File f = files.get(i);
            col.addView(fileRow(f), rowLp(i == 0 ? 7 : 3));
        }

        if (files.size() > 5) {
            final ArrayList<String> all = new ArrayList<String>(msg.artifacts);
            SoftButton more = new SoftButton(act, "查看全部 " + files.size() + " 个附件", 11.6f,
                    Palette.INK_2, Palette.CARD);
            more.radius(12).stroke(Palette.LINE, 0.8f).pad(12, 7);
            more.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (listener != null) listener.onSeeAll(all);
                }
            });
            LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            mlp.topMargin = Ui.dp(8);
            col.addView(more, mlp);
        }
    }

    private LinearLayout.LayoutParams rowLp(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(top);
        return lp;
    }

    private View fileRow(final File f) {
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        FileGlyph glyph = new FileGlyph(act, iconFor(f.getName()));
        row.addView(glyph, new LinearLayout.LayoutParams(Ui.dp(28), Ui.dp(28)));

        LinearLayout txt = new LinearLayout(act);
        txt.setOrientation(LinearLayout.VERTICAL);
        Label name = new Label(act, f.getName(), 12.2f, Palette.INK, false);
        Label meta = new Label(act, metaFor(f), 10.4f, Palette.INK_3, false);
        txt.addView(name, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        txt.addView(meta, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = Ui.dp(8);
        row.addView(txt, tlp);

        IconButton share = new IconButton(act, Icons.SHARE, 0, Palette.INK_3, 26, 12);
        share.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (listener != null) listener.onShare(f);
            }
        });
        row.addView(share, new LinearLayout.LayoutParams(Ui.dp(26), Ui.dp(26)));

        IconButton save = new IconButton(act, Icons.DOWNLOAD, 0, Palette.INK_3, 26, 12);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (listener != null) listener.onSave(f);
            }
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(Ui.dp(26), Ui.dp(26));
        slp.leftMargin = Ui.dp(2);
        row.addView(save, slp);

        row.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (listener != null) listener.onOpen(f);
            }
        });
        return row;
    }

    private String metaFor(File f) {
        String ext = AgentTools.ext(f.getName());
        String kind;
        if (f.isDirectory()) kind = "文件夹";
        else if ("html".equals(ext) || "htm".equals(ext)) kind = "HTML · 可预览";
        else if ("md".equals(ext)) kind = "Markdown · 可预览";
        else if (AttachmentsType.isText(ext)) kind = ext.toUpperCase() + " · 文本";
        else if ("zip".equals(ext)) kind = "压缩包";
        else kind = ext.length() == 0 ? "文件" : ext.toUpperCase() + " 文件";
        return kind + " · " + AgentTools.sizeText(f.length());
    }

    private int iconFor(String name) {
        String ext = AgentTools.ext(name);
        if ("html".equals(ext) || "htm".equals(ext)) return Icons.GLOBE;
        if ("css".equals(ext) || "js".equals(ext) || "json".equals(ext)
                || "java".equals(ext) || "py".equals(ext)) return Icons.CODE;
        if ("png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext)
                || "gif".equals(ext) || "webp".equals(ext)) return Icons.IMAGE;
        if ("zip".equals(ext)) return Icons.FOLDER;
        return Icons.DOC;
    }

    /** 小文件图标：圆角底色 + 矢量图标，避免额外 PNG 资源。 */
    static class FileGlyph extends View {
        private final int icon;

        FileGlyph(Context c, int iconId) {
            super(c);
            icon = iconId;
        }

        protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            Paint p = Ui.fillPaint(Palette.PINK_SOFT);
            canvas.drawRoundRect(new RectF(0, 0, w, h), Ui.dpf(8), Ui.dpf(8), p);
            Icons.draw(canvas, icon, w / 2f, h / 2f, Math.min(w, h) * 0.62f,
                    Palette.PINK_DEEP, 1.5f);
        }
    }

    /** 只用于判断扩展名，不额外依赖 UI 包。 */
    static final class AttachmentsType {
        static boolean isText(String ext) {
            return "txt".equals(ext) || "log".equals(ext) || "csv".equals(ext)
                    || "xml".equals(ext) || "yml".equals(ext) || "yaml".equals(ext)
                    || "ini".equals(ext) || "gradle".equals(ext) || "sh".equals(ext);
        }
    }
}
