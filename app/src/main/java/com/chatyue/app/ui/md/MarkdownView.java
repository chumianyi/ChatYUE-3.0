package com.chatyue.app.ui.md;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.LeadingMarginSpan;
import android.view.View;
import android.widget.LinearLayout;

import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.md.MdBlock;
import com.chatyue.app.md.MdInline;
import com.chatyue.app.md.MdParser;

import java.util.ArrayList;

/**
 * Markdown 容器：把解析出来的块级元素逐个变成子 View。
 *
 * 流式输出时只有最后一个块在增长，这里做了「原地补丁」优化：
 * 前 n-1 个块没变化就只更新最后一个 Text，避免整段重建导致的跳动与闪烁。
 */
public class MarkdownView extends LinearLayout {

    private final float scale;
    private String last;
    private ArrayList<MdBlock> blocks = new ArrayList<MdBlock>();
    private CodeBox.CopyListener copyListener;
    private CodeBox.PreviewListener previewListener;

    public MarkdownView(Context c, float fontScale) {
        super(c);
        this.scale = fontScale;
        setOrientation(VERTICAL);
    }

    public void setCodeCopyListener(CodeBox.CopyListener l) {
        this.copyListener = l;
    }

    public void setCodePreviewListener(CodeBox.PreviewListener l) {
        this.previewListener = l;
    }

    public void setMarkdown(String md) {
        String src = md == null ? "" : md;
        if (src.equals(last)) return;
        ArrayList<MdBlock> next;
        try {
            next = MdParser.parse(src);
        } catch (Exception e) {
            next = new ArrayList<MdBlock>();
            next.add(MdBlock.paragraph(src));
        }
        if (canPatch(next)) {
            patchLast(next);
        } else {
            rebuild(next);
        }
        blocks = next;
        last = src;
    }

    private boolean canPatch(ArrayList<MdBlock> next) {
        if (next.size() != blocks.size() || next.isEmpty() || getChildCount() != blocks.size()) return false;
        for (int i = 0; i < next.size() - 1; i++) {
            MdBlock a = blocks.get(i);
            MdBlock b = next.get(i);
            if (a.type != b.type || !eq(a.text, b.text) || !eq(a.code, b.code)) return false;
        }
        MdBlock a = blocks.get(next.size() - 1);
        MdBlock b = next.get(next.size() - 1);
        return a.type == b.type && a.type == MdBlock.PARAGRAPH;
    }

    private boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private void patchLast(ArrayList<MdBlock> next) {
        View child = getChildAt(getChildCount() - 1);
        if (child instanceof MdText) {
            MdBlock b = next.get(next.size() - 1);
            ((MdText) child).setText(MdInline.build(b.text, Palette.INK, Palette.PINK_DEEP));
        } else {
            rebuild(next);
        }
    }

    private void rebuild(ArrayList<MdBlock> next) {
        removeAllViews();
        for (int i = 0; i < next.size(); i++) {
            View v = build(next.get(i));
            if (v == null) continue;
            LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = gap(next.get(i));
            addView(v, lp);
        }
    }

    private int gap(MdBlock b) {
        switch (b.type) {
            case MdBlock.HEADING:
                return Ui.dp(b.level <= 2 ? 8 : 5);
            case MdBlock.CODE:
                return Ui.dp(7);
            case MdBlock.TABLE:
                return Ui.dp(7);
            case MdBlock.RULE:
                return Ui.dp(4);
            default:
                return Ui.dp(4);
        }
    }

    private View build(MdBlock b) {
        switch (b.type) {
            case MdBlock.HEADING: {
                float size = b.level == 1 ? 19.5f : (b.level == 2 ? 17.2f : 15.6f);
                TextPaint p = Ui.textPaint(Palette.INK, size * scale, true);
                MdText t = new MdText(getContext(), MdInline.build(b.text, Palette.INK, Palette.PINK_DEEP),
                        p, 0, Ui.dp(b.level == 1 ? 4 : 2), 2f);
                return t;
            }
            case MdBlock.BULLET: {
                SpannableStringBuilder sb = new SpannableStringBuilder();
                sb.append(b.level > 0 ? "   •  " : "•  ");
                sb.append(MdInline.build(b.text, Palette.INK, Palette.PINK_DEEP));
                int hang = Ui.dp(14 + b.level * 14);
                sb.setSpan(new LeadingMarginSpan.Standard(0, hang), 0, sb.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                TextPaint p = Ui.textPaint(Palette.INK, 14.6f * scale, false);
                MdText t = new MdText(getContext(), sb, p, 0, 0, 4f);
                return t;
            }
            case MdBlock.ORDERED: {
                SpannableStringBuilder sb = new SpannableStringBuilder();
                sb.append(String.valueOf(b.number)).append(".  ");
                sb.append(MdInline.build(b.text, Palette.INK, Palette.PINK_DEEP));
                int hang = Ui.dp(20 + b.level * 14);
                sb.setSpan(new LeadingMarginSpan.Standard(0, hang), 0, sb.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                TextPaint p = Ui.textPaint(Palette.INK, 14.6f * scale, false);
                return new MdText(getContext(), sb, p, 0, 0, 4f);
            }
            case MdBlock.QUOTE: {
                TextPaint p = Ui.textPaint(Palette.INK_2, 14.2f * scale, false);
                return new QuoteText(getContext(), MdInline.build(b.text, Palette.INK_2, Palette.PINK_DEEP), p);
            }
            case MdBlock.CODE: {
                CodeBox box = new CodeBox(getContext(), b.lang, b.code, scale);
                box.setCopyListener(copyListener);
                box.setPreviewListener(previewListener);
                return box;
            }
            case MdBlock.TABLE: {
                return new TableBox(getContext(), b.head, b.rows, scale);
            }
            case MdBlock.RULE:
                return new HrView(getContext());
            case MdBlock.PARAGRAPH:
            default: {
                TextPaint p = Ui.textPaint(Palette.INK, 14.6f * scale, false);
                return new MdText(getContext(), MdInline.build(b.text, Palette.INK, Palette.PINK_DEEP),
                        p, 0, 0, 4.2f);
            }
        }
    }
}
