package com.chatyue.app.data;

import android.content.Context;

import com.chatyue.app.util.FileUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * 会话持久化：一条会话一个 JSON 文件 + 一份索引文件。
 * 不依赖 SQLite（低版本虽有，但文件形式更轻、更容易做迁移与容错）。
 */
public final class SessionStore {

    private static final Object LOCK = new Object();

    private SessionStore() {
    }

    public static File dir(Context c) {
        File d = new File(c.getFilesDir(), "sessions");
        FileUtil.mkdirs(d);
        return d;
    }

    private static File indexFile(Context c) {
        return new File(dir(c), "index.json");
    }

    private static File sessionFile(Context c, String id) {
        return new File(dir(c), id + ".json");
    }

    /** 只读索引（不含消息体），用于会话列表。 */
    public static ArrayList<Session> list(Context c) {
        synchronized (LOCK) {
            ArrayList<Session> out = new ArrayList<Session>();
            try {
                File f = indexFile(c);
                if (!f.exists()) return out;
                String raw = FileUtil.readText(f);
                JSONArray arr = new JSONArray(raw);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    Session s = new Session();
                    s.id = o.optString("id", "");
                    s.title = o.optString("title", "新会话");
                    s.createdAt = o.optLong("createdAt", 0);
                    s.updatedAt = o.optLong("updatedAt", 0);
                    if (s.id != null && s.id.length() > 0) out.add(s);
                }
            } catch (Exception ignored) {
            }
            sort(out);
            return out;
        }
    }

    private static void sort(ArrayList<Session> list) {
        Collections.sort(list, new Comparator<Session>() {
            public int compare(Session a, Session b) {
                if (a.updatedAt == b.updatedAt) return 0;
                return a.updatedAt > b.updatedAt ? -1 : 1;
            }
        });
    }

    public static Session load(Context c, String id) {
        synchronized (LOCK) {
            try {
                File f = sessionFile(c, id);
                if (!f.exists()) return null;
                return Session.fromJson(FileUtil.readText(f));
            } catch (Exception e) {
                return null;
            }
        }
    }

    public static void save(Context c, Session s) {
        if (s == null || s.id == null) return;
        synchronized (LOCK) {
            try {
                s.updatedAt = System.currentTimeMillis();
                FileUtil.writeText(sessionFile(c, s.id), s.toJson());
                writeIndex(c, s);
            } catch (Exception ignored) {
            }
        }
    }

    private static void writeIndex(Context c, Session s) {
        ArrayList<Session> all = list(c);
        ArrayList<Session> next = new ArrayList<Session>();
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++) {
            Session old = all.get(i);
            if (old.id.equals(s.id)) {
                next.add(s);
                replaced = true;
            } else {
                next.add(old);
            }
        }
        if (!replaced) next.add(s);
        sort(next);
        try {
            JSONArray arr = new JSONArray();
            for (int i = 0; i < next.size(); i++) {
                Session it = next.get(i);
                JSONObject o = new JSONObject();
                o.put("id", it.id);
                o.put("title", it.title);
                o.put("createdAt", it.createdAt);
                o.put("updatedAt", it.updatedAt);
                arr.put(o);
            }
            FileUtil.writeText(indexFile(c), arr.toString());
        } catch (Exception ignored) {
        }
    }

    public static void delete(Context c, String id) {
        synchronized (LOCK) {
            FileUtil.delete(sessionFile(c, id));
            ArrayList<Session> all = list(c);
            JSONArray arr = new JSONArray();
            try {
                for (int i = 0; i < all.size(); i++) {
                    Session it = all.get(i);
                    if (it.id.equals(id)) continue;
                    JSONObject o = new JSONObject();
                    o.put("id", it.id);
                    o.put("title", it.title);
                    o.put("createdAt", it.createdAt);
                    o.put("updatedAt", it.updatedAt);
                    arr.put(o);
                }
                FileUtil.writeText(indexFile(c), arr.toString());
            } catch (Exception ignored) {
            }
        }
    }

    public static Session create(Context c) {
        Session s = new Session();
        save(c, s);
        return s;
    }

    public static Session latest(Context c) {
        ArrayList<Session> all = list(c);
        if (all.isEmpty()) return null;
        return load(c, all.get(0).id);
    }

    public static File imageDir(Context c) {
        File d = new File(c.getFilesDir(), "images");
        FileUtil.mkdirs(d);
        return d;
    }
}
