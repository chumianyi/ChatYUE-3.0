package com.chatyue.app.md;

import java.util.ArrayList;

/**
 * 手写 Markdown 解析器（无任何第三方库）。
 *
 * 覆盖聊天场景里真实会出现的语法：标题、段落、有序 / 无序列表、引用、围栏代码块、
 * 分隔线、表格。行内语法（粗体 / 斜体 / 行内代码 / 链接）在 MdInline 中处理。
 */
public final class MdParser {

    private MdParser() {
    }

    public static ArrayList<MdBlock> parse(String src) {
        ArrayList<MdBlock> out = new ArrayList<MdBlock>();
        if (src == null) return out;
        String[] lines = src.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        int i = 0;
        int n = lines.length;
        StringBuilder para = new StringBuilder();

        while (i < n) {
            String line = lines[i];
            String trimmed = line.trim();

            // 围栏代码块
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                flushPara(out, para);
                String fence = trimmed.substring(0, 3);
                String lang = trimmed.substring(3).trim();
                if (lang.startsWith("{")) lang = "";
                StringBuilder code = new StringBuilder();
                i++;
                while (i < n && !lines[i].trim().startsWith(fence)) {
                    if (code.length() > 0) code.append('\n');
                    code.append(lines[i]);
                    i++;
                }
                if (i < n) i++;
                out.add(MdBlock.code(lang, code.toString()));
                continue;
            }

            if (trimmed.length() == 0) {
                flushPara(out, para);
                i++;
                continue;
            }

            // 标题
            if (trimmed.startsWith("#")) {
                int level = 0;
                while (level < trimmed.length() && trimmed.charAt(level) == '#') level++;
                if (level >= 1 && level <= 6 && trimmed.length() > level
                        && trimmed.charAt(level) == ' ') {
                    flushPara(out, para);
                    out.add(MdBlock.heading(level, trimmed.substring(level + 1).trim()));
                    i++;
                    continue;
                }
            }

            // 分隔线
            if (isRule(trimmed)) {
                flushPara(out, para);
                out.add(MdBlock.rule());
                i++;
                continue;
            }

            // 表格：当前行含 |，下一行是分隔行
            if (trimmed.indexOf('|') >= 0 && i + 1 < n && isTableSep(lines[i + 1])) {
                flushPara(out, para);
                MdBlock b = new MdBlock();
                b.type = MdBlock.TABLE;
                b.head = splitRow(trimmed);
                i += 2;
                while (i < n && lines[i].indexOf('|') >= 0 && lines[i].trim().length() > 0) {
                    b.rows.add(splitRow(lines[i]));
                    i++;
                }
                out.add(b);
                continue;
            }

            // 引用
            if (trimmed.startsWith(">")) {
                flushPara(out, para);
                StringBuilder q = new StringBuilder();
                while (i < n && lines[i].trim().startsWith(">")) {
                    String t = lines[i].trim();
                    t = t.substring(1);
                    if (t.startsWith(" ")) t = t.substring(1);
                    if (q.length() > 0) q.append('\n');
                    q.append(t);
                    i++;
                }
                MdBlock b = new MdBlock();
                b.type = MdBlock.QUOTE;
                b.text = q.toString();
                out.add(b);
                continue;
            }

            // 列表
            int indent = indentOf(line);
            String bullet = bulletOf(trimmed);
            if (bullet != null) {
                flushPara(out, para);
                // "- " 与 "1. " 长度可能相同，不能用长度判断有序与否
                boolean ordered = Character.isDigit(bullet.charAt(0));
                MdBlock b = new MdBlock();
                b.type = ordered ? MdBlock.ORDERED : MdBlock.BULLET;
                b.level = indent / 2;
                b.number = ordered ? parseOrder(bullet) : 0;
                b.text = trimmed.substring(bullet.length()).trim();
                out.add(b);
                i++;
                continue;
            }

            // 缩进的续行合并进上一条列表
            if (indent > 0 && !out.isEmpty()) {
                MdBlock last = out.get(out.size() - 1);
                if ((last.type == MdBlock.BULLET || last.type == MdBlock.ORDERED) && para.length() == 0) {
                    last.text = last.text + "\n" + trimmed;
                    i++;
                    continue;
                }
            }

            if (para.length() > 0) para.append('\n');
            para.append(trimmed);
            i++;
        }
        flushPara(out, para);
        return out;
    }

    private static void flushPara(ArrayList<MdBlock> out, StringBuilder para) {
        if (para.length() > 0) {
            out.add(MdBlock.paragraph(para.toString()));
            para.setLength(0);
        }
    }

    private static boolean isRule(String t) {
        if (t.length() < 3) return false;
        char c = t.charAt(0);
        if (c != '-' && c != '*' && c != '_') return false;
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (ch != c && ch != ' ') return false;
        }
        int count = 0;
        for (int i = 0; i < t.length(); i++) if (t.charAt(i) == c) count++;
        return count >= 3;
    }

    private static boolean isTableSep(String line) {
        String t = line.trim();
        if (t.length() == 0) return false;
        int bars = 0;
        int dashes = 0;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '|') bars++;
            else if (c == '-') dashes++;
            else if (c == ':' || c == ' ') continue;
            else return false;
        }
        return bars >= 1 && dashes >= 1;
    }

    private static ArrayList<String> splitRow(String line) {
        String t = line.trim();
        if (t.startsWith("|")) t = t.substring(1);
        if (t.endsWith("|")) t = t.substring(0, t.length() - 1);
        ArrayList<String> cells = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i <= t.length(); i++) {
            if (i == t.length() || t.charAt(i) == '|') {
                cells.add(t.substring(start, i).trim());
                start = i + 1;
            }
        }
        return cells;
    }

    private static int indentOf(String line) {
        int n = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ' ') n++;
            else if (c == '\t') n += 2;
            else break;
        }
        return n;
    }

    /** 返回列表标记（"- " / "1. "），非列表返回 null。 */
    private static String bulletOf(String t) {
        if (t.length() >= 2) {
            char c = t.charAt(0);
            if ((c == '-' || c == '*' || c == '+') && t.charAt(1) == ' ') return t.substring(0, 2);
        }
        int i = 0;
        while (i < t.length() && Character.isDigit(t.charAt(i))) i++;
        if (i > 0 && i + 1 < t.length() && (t.charAt(i) == '.' || t.charAt(i) == ')')
                && t.charAt(i + 1) == ' ') {
            return t.substring(0, i + 2);
        }
        return null;
    }

    private static int parseOrder(String bullet) {
        try {
            int i = 0;
            while (i < bullet.length() && Character.isDigit(bullet.charAt(i))) i++;
            return Integer.parseInt(bullet.substring(0, i));
        } catch (Exception e) {
            return 1;
        }
    }
}
