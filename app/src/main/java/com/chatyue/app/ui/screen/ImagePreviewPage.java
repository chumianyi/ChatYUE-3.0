package com.chatyue.app.ui.screen;

import android.provider.MediaStore;
import android.view.View;
import android.widget.FrameLayout;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.core.Palette;
import com.chatyue.app.core.Ui;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.widget.IconButton;
import com.chatyue.app.ui.widget.PhotoView;
import com.chatyue.app.ui.widget.SoftButton;

/** 全屏看图：黑底、可拖动缩放、保存到相册。 */
public class ImagePreviewPage extends Page {

    private final PhotoView photo;
    private final String path;

    public ImagePreviewPage(MainActivity a, String path) {
        super(a);
        this.path = path;
        setBackgroundColor(0xFF0D0D0F);

        photo = new PhotoView(a);
        photo.setPath(path);
        addView(photo, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));

        IconButton close = new IconButton(a, Icons.CLOSE, 0x33FFFFFF, 0xFFFFFFFF, 38, 17);
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.pop();
            }
        });
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(Ui.dp(38), Ui.dp(38));
        clp.leftMargin = Ui.dp(16);
        clp.topMargin = Ui.dp(14);
        addView(close, clp);

        SoftButton save = new SoftButton(a, "保存到相册", 13.4f, 0xFFFFFFFF, 0x33FFFFFF);
        save.pad(16, 10).radius(15);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveToGallery();
            }
        });
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        slp.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
        slp.bottomMargin = Ui.dp(30);
        addView(save, slp);
    }

    private void saveToGallery() {
        try {
            String uri = MediaStore.Images.Media.insertImage(act.getContentResolver(), path,
                    "ChatYUE_" + System.currentTimeMillis(), "来自 ChatYUE");
            if (uri != null) {
                act.toast("已保存到相册");
            } else {
                act.toast("保存失败：系统未返回相册地址");
            }
        } catch (Throwable t) {
            // 高版本系统没有 WRITE_EXTERNAL_STORAGE 授权，insertImage 会失败
            act.toast("保存失败：需要相册写入权限");
        }
    }

    @Override
    public boolean onBack() {
        act.pop();
        return true;
    }
}
