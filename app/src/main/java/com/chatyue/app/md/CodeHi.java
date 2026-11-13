package com.chatyue.app.md;

import java.util.HashSet;

/**
 * 极简语法高亮器：按字符着色，返回与源码等长的颜色数组。
 * 自己实现词法扫描，不引入任何高亮库。
 */
public final class CodeHi {

    public static final int C_KEY = 0xFFC2437B;
    public static final int C_STR = 0xFF2F8F5B;
    public static final int C_COM = 0xFF9B979F;
    public static final int C_NUM = 0xFFB06A00;
    public static final int C_FUN = 0xFF3C6FBF;
    public static final int C_TAG = 0xFF7A5AC8;

    private static final HashSet<String> KEYWORDS = new HashSet<String>();

    static {
        String[] k = {
                "abstract", "as", "async", "await", "boolean", "break", "byte", "case", "catch",
                "char", "class", "const", "continue", "def", "default", "delete", "do", "double",
                "elif", "else", "enum", "export", "extends", "false", "final", "finally", "float",
                "for", "from", "func", "function", "global", "go", "if", "implements", "import",
                "in", "instanceof", "int", "interface", "is", "lambda", "let", "long", "namespace",
                "new", "nil", "none", "null", "or", "and", "not", "package", "pass", "private",
                "protected", "public", "raise", "return", "select", "short", "static", "struct",
                "super", "switch", "synchronized", "this", "throw", "throws", "trait", "true",
                "try", "type", "typeof", "val", "var", "void", "volatile", "while", "with", "yield",
                "String", "Integer", "Object", "List", "Map", "void", "self", "cls", "match", "case",
                "fn", "impl", "mut", "pub", "use", "crate", "where", "loop", "until", "echo", "then",
                "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "CREATE", "TABLE"
        };
        for (int i = 0; i < k.length; i++) KEYWORDS.add(k[i]);
    }

    private CodeHi() {
    }

    public static int[] run(String code, String lang, int base) {
        int n = code.length();
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = base;
        boolean hashComment = "py".equals(lang) || "python".equals(lang) || "sh".equals(lang)
                || "bash".equals(lang) || "shell".equals(lang) || "yaml".equals(lang)
                || "yml".equals(lang) || "ruby".equals(lang) || "rb".equals(lang)
                || "toml".equals(lang);
        boolean html = "html".equals(lang) || "xml".equals(lang);

        int i = 0;
        while (i < n) {
            char c = code.charAt(i);
            // 行注释
            if (c == '/' && i + 1 < n && code.charAt(i + 1) == '/') {
                int end = code.indexOf('\n', i);
                if (end < 0) end = n;
                paint(out, i, end, C_COM);
                i = end;
                continue;
            }
            if (c == '#' && hashComment) {
                int end = code.indexOf('\n', i);
                if (end < 0) end = n;
                paint(out, i, end, C_COM);
                i = end;
                continue;
            }
            // 块注释
            if (c == '/' && i + 1 < n && code.charAt(i + 1) == '*') {
                int end = code.indexOf("*/", i + 2);
                end = end < 0 ? n : end + 2;
                paint(out, i, end, C_COM);
                i = end;
                continue;
            }
            // 字符串
            if (c == '"' || c == '\'' || c == '`') {
                int end = i + 1;
                while (end < n) {
                    char ch = code.charAt(end);
                    if (ch == '\\') {
                        end += 2;
                        continue;
                    }
                    if (ch == c) {
                        end++;
                        break;
                    }
                    if (ch == '\n' && c != '`') break;
                    end++;
                }
                if (end > n) end = n;
                paint(out, i, end, C_STR);
                i = end;
                continue;
            }
            // 数字
            if (Character.isDigit(c)) {
                int end = i;
                while (end < n && (Character.isDigit(code.charAt(end))
                        || code.charAt(end) == '.' || code.charAt(end) == 'x'
                        || code.charAt(end) == 'a' || code.charAt(end) == 'b'
                        || code.charAt(end) == 'c' || code.charAt(end) == 'd'
                        || code.charAt(end) == 'e' || code.charAt(end) == 'f'
                        || code.charAt(end) == '_')) {
                    end++;
                }
                paint(out, i, end, C_NUM);
                i = end;
                continue;
            }
            // 标识符
            if (Character.isLetter(c) || c == '_' || c == '$' || c == '@') {
                int end = i;
                while (end < n && (Character.isLetterOrDigit(code.charAt(end))
                        || code.charAt(end) == '_' || code.charAt(end) == '$'
                        || code.charAt(end) == '-')) {
                    end++;
                }
                String word = code.substring(i, end);
                if (KEYWORDS.contains(word)) {
                    paint(out, i, end, C_KEY);
                } else {
                    int p = end;
                    while (p < n && code.charAt(p) == ' ') p++;
                    if (p < n && code.charAt(p) == '(') paint(out, i, end, C_FUN);
                }
                i = end;
                continue;
            }
            if (html && c == '<') {
                int end = code.indexOf('>', i);
                if (end < 0) end = n;
                else end++;
                paint(out, i, end, C_TAG);
                i = end;
                continue;
            }
            i++;
        }
        return out;
    }

    private static void paint(int[] out, int from, int to, int color) {
        if (from < 0) from = 0;
        if (to > out.length) to = out.length;
        for (int i = from; i < to; i++) out[i] = color;
    }
}
