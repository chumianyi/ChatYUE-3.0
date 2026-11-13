package com.chatyue.app.ui.screen;

import android.view.View;

import com.chatyue.app.MainActivity;
import com.chatyue.app.content.FileSharing;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.data.AgentTools;
import com.chatyue.app.ui.overlay.Sheet;

import java.io.File;
import java.util.ArrayList;

/** 产物超过 5 个时，「查看全部附件」进入这里。 */
public class ArtifactListPage extends BasePage {

    private final ArrayList<String> paths;

    public ArtifactListPage(MainActivity a, ArrayList<String> list) {
        super(a, "全部附件", (list == null ? 0 : list.size()) + " 个文件");
        paths = list == null ? new ArrayList<String>() : list;
        build();
    }

    private void build() {
        heading("这次任务产出的全部文件", "点一行可以预览；右侧按钮直接分享或保存。");
        for (int i = 0; i < paths.size(); i++) {
            final File f = new File(paths.get(i));
            if (!f.exists()) continue;
            row(iconFor(f), f.getName(),
                    (f.isDirectory() ? "文件夹" : AgentTools.sizeText(f.length())) + " · "
                            + relative(f), new View.OnClickListener() {
                        public void onClick(View v) {
                            act.openFile(f);
                        }
                    });
        }
        primaryButton("分享全部", new View.OnClickListener() {
            public void onClick(View v) {
                act.shareArtifacts(paths);
            }
        });
        note("产物保存在工作目录里；删除工作目录会一起删除。");
    }

    private String relative(File f) {
        try {
            String p = AgentTools.relative(act, f);
            return p.length() > 48 ? p.substring(0, 48) + "…" : p;
        } catch (Throwable t) {
            return f.getParent();
        }
    }

    private int iconFor(File f) {
        if (f.isDirectory()) return Icons.FOLDER;
        String ext = AgentTools.ext(f.getName());
        if ("html".equals(ext) || "htm".equals(ext)) return Icons.GLOBE;
        if ("css".equals(ext) || "js".equals(ext) || "json".equals(ext)) return Icons.CODE;
        if ("png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext)) return Icons.IMAGE;
        return Icons.DOC;
    }
}
