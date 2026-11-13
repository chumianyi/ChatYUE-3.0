package com.chatyue.app.md;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;

import com.chatyue.app.core.Palette;

import java.util.ArrayList;

/**
 * 行内 Markdown：粗体、斜体、行内代码、删除线、链接。
 * 全部基于 API 1 就存在的 Span，无需任何依赖。
 */
public final class MdInline {

    private MdInline() {
    }

    public static CharSequence build(String src, int ink, int accent) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        if (src == null) return sb;
        ArrayList<int[]> marks = new ArrayList<int[]>();
        int i = 0;
        int len = src.length();
        while (i < len) {
            char c = src.charAt(i);
            if (c == '\\' && i + 1 < len) {
                sb.append(src.charAt(i + 1));
                i += 2;
                continue;
            }
            if (c == '`') {
                int end = src.indexOf('`', i + 1);
                if (end > i) {
                    int s = sb.length();
                    sb.append(src, i + 1, end);
                    marks.add(new int[]{s, sb.length(), 1});
                    i = end + 1;
                    continue;
                }
            }
            if (c == '*' && i + 1 < len && src.charAt(i + 1) == '*') {
                int end = src.indexOf("**", i + 2);
                if (end > i + 1) {
                    int s = sb.length();
                    sb.append(src, i + 2, end);
                    marks.add(new int[]{s, sb.length(), 2});
                    i = end + 2;
                    continue;
                }
            }
            if (c == '~' && i + 1 < len && src.charAt(i + 1) == '~') {
                int end = src.indexOf("~~", i + 2);
                if (end > i + 1) {
                    int s = sb.length();
                    sb.append(src, i + 2, end);
                    marks.add(new int[]{s, sb.length(), 5});
                    i = end + 2;
                    continue;
                }
            }
            if (c == '*') {
                int end = src.indexOf('*', i + 1);
                if (end > i + 1 && end - i < 200) {
                    int s = sb.length();
                    sb.append(src, i + 1, end);
                    marks.add(new int[]{s, sb.length(), 3});
                    i = end + 1;
                    continue;
                }
            }
            if (c == '[') {
                int close = src.indexOf(']', i);
                if (close > 0 && close + 1 < len && src.charAt(close + 1) == '(') {
                    int urlEnd = src.indexOf(')', close + 2);
                    if (urlEnd > close) {
                        int s = sb.length();
                        sb.append(src, i + 1, close);
                        marks.add(new int[]{s, sb.length(), 4});
                        i = urlEnd + 1;
                        continue;
                    }
                }
            }
            if (c == 'h' && src.startsWith("http", i)) {
                int end = i;
                while (end < len) {
                    char ch = src.charAt(end);
                    if (ch == ' ' || ch == '\n' || ch == ')' || ch == '，' || ch == '。') break;
                    end++;
                }
                if (end - i > 8) {
                    int s = sb.length();
                    sb.append(src, i, end);
                    marks.add(new int[]{s, sb.length(), 4});
                    i = end;
                    continue;
                }
            }
            sb.append(c);
            i++;
        }
        for (int k = 0; k < marks.size(); k++) {
            int[] m = marks.get(k);
            if (m[0] >= m[1]) continue;
            if (m[2] == 1) {
                sb.setSpan(new TypefaceSpan("monospace"), m[0], m[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new BackgroundColorSpan(Palette.CODE_BG), m[0], m[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new ForegroundColorSpan(Palette.PINK_DEEP), m[0], m[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (m[2] == 2) {
                sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), m[0], m[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new ForegroundColorSpan(ink), m[0], m[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (m[2] == 3) {
                sb.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), m[0], m[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (m[2] == 4) {
                sb.setSpan(new ForegroundColorSpan(accent), m[0], m[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new UnderlineSpan(), m[0], m[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (m[2] == 5) {
                sb.setSpan(new StrikethroughSpan(), m[0], m[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return sb;
    }
}
