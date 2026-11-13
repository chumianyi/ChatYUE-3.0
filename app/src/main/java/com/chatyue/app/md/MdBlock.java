package com.chatyue.app.md;

import java.util.ArrayList;

/** Markdown 块级元素模型。 */
public class MdBlock {

    public static final int PARAGRAPH = 0;
    public static final int HEADING = 1;
    public static final int BULLET = 2;
    public static final int ORDERED = 3;
    public static final int QUOTE = 4;
    public static final int CODE = 5;
    public static final int RULE = 6;
    public static final int TABLE = 7;

    public int type;
    public String text = "";
    public int level = 0;
    public int number = 0;
    public String lang = "";
    public String code = "";

    public ArrayList<String> head = new ArrayList<String>();
    public ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();

    public static MdBlock paragraph(String t) {
        MdBlock b = new MdBlock();
        b.type = PARAGRAPH;
        b.text = t;
        return b;
    }

    public static MdBlock heading(int level, String t) {
        MdBlock b = new MdBlock();
        b.type = HEADING;
        b.level = level;
        b.text = t;
        return b;
    }

    public static MdBlock code(String lang, String code) {
        MdBlock b = new MdBlock();
        b.type = CODE;
        b.lang = lang == null ? "" : lang;
        b.code = code;
        return b;
    }

    public static MdBlock rule() {
        MdBlock b = new MdBlock();
        b.type = RULE;
        return b;
    }
}
