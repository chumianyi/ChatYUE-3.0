package com.chatyue.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.chatyue.app.core.Palette;
import com.chatyue.app.data.Assistants;
import com.chatyue.app.data.Attachments;
import com.chatyue.app.data.Experiment;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.data.AgentTools;
import com.chatyue.app.content.FileSharing;
import com.chatyue.app.core.Tween;
import com.chatyue.app.core.Ui;
import com.chatyue.app.data.Session;
import com.chatyue.app.data.SessionStore;
import com.chatyue.app.net.RequestQueue;
import com.chatyue.app.ui.Page;
import com.chatyue.app.ui.RootLayout;
import com.chatyue.app.ui.overlay.Sheet;
import com.chatyue.app.ui.overlay.Toast;
import com.chatyue.app.ui.screen.AppearancePage;
import com.chatyue.app.ui.screen.ChatPage;
import com.chatyue.app.ui.screen.HomePage;
import com.chatyue.app.ui.screen.HtmlPreviewPage;
import com.chatyue.app.ui.screen.OnboardingPage;
import com.chatyue.app.ui.screen.ProviderEditPage;
import com.chatyue.app.ui.screen.SettingsPage;
import com.chatyue.app.ui.screen.TextPreviewPage;
import com.chatyue.app.ui.screen.WorkspacePage;
import com.chatyue.app.ui.m3.M3ChatPage;
import com.chatyue.app.ui.m3.M3Snackbar;
import com.chatyue.app.ui.m3.M3HomePage;
import com.chatyue.app.ui.m3.M3SettingsPage;
import com.chatyue.app.core.Style;
import com.chatyue.app.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * 唯一 Activity。页面栈、转场、返回键、图片选择都在这里统一调度。
 *
 * 注意：API 3 上 Activity.onBackPressed 还不存在（API 5 才有），
 * 所以返回键走 onKeyDown 拦截，同时在 onBackPressed 里兜一层新系统。
 */
public class MainActivity extends Activity {

    public interface ImagePickCallback {
        void onImage(String path);
    }

    public interface FilePickCallback {
        void onFile(String path);
    }

    private static final int REQ_PICK = 101;
    private static final int REQ_CAMERA = 102;
    private static final int REQ_CAMERA_PERM = 1203;
    private static final int REQ_FILE = 103;

    private static MainActivity sInstance;

    private RootLayout root;
    private final ArrayList<Page> stack = new ArrayList<Page>();
    private ImagePickCallback pendingPick;
    private FilePickCallback pendingFile;
    private Runnable pendingStorageAction;
    private Uri pendingCameraUri;
    private long lastBackAt;

    public static MainActivity get() {
        return sInstance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sInstance = this;
        Ui.init(this);
        root = new RootLayout(this);
        root.setBackgroundColor(Palette.BG);
        setContentView(root);
        // 2.2.0 去掉开屏动画：冷启动直接进入首页/引导，少一次白等。
        if (Prefs.onboarded(this)) {
            showHome();
        } else {
            resetTo(new OnboardingPage(this));
        }
    }

    public void showOnboarding() {
        resetTo(new OnboardingPage(this));
    }

    public void openNewChatWithDraft(String text) {
        Session s = SessionStore.create(this);
        Assistants.Assistant as = Assistants.current(this);
        s.systemPrompt = as.prompt;
        s.title = "新对话";
        SessionStore.save(this, s);
        if (Style.classic(this)) {
            M3ChatPage page = new M3ChatPage(this, s);
            push(page);
            page.setDraft(text);
        } else {
            ChatPage page = new ChatPage(this, s);
            push(page);
            page.setDraft(text);
        }
    }

    /** 清空页面栈并直接放入若干页（最后一页可见）。主题切换、引导步骤切换用它重建界面。 */
    public void resetTo(Page... pages) {
        if (pages == null || pages.length == 0) return;
        stack.clear();
        root.pageHost.removeAllViews();
        for (int i = 0; i < pages.length; i++) {
            stack.add(pages[i]);
            root.pageHost.addView(pages[i], new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
            pages[i].setVisibility(i == pages.length - 1 ? View.VISIBLE : View.GONE);
        }
        Page shown = pages[pages.length - 1];
        shown.onPageShown();
        animateHostIn(shown);
    }

    /** resetTo 的统一入场：整页淡入 + 轻微放大，避免主题切换时硬切。 */
    private void animateHostIn(final Page shown) {
        shown.setPageTransform(0f, 0.985f, Ui.dp(10));
        transition = Tween.of(0, 1, 220, new Tween.Listener() {
            public void onUpdate(float v) {
                shown.setPageTransform(v, 0.985f + 0.015f * v, (int) (Ui.dp(10) * (1f - v)));
            }

            public void onEnd() {
                shown.setPageTransform(1f, 1f, 0);
                transition = null;
            }
        });
        transition.ease(Tween.DECEL).start();
    }

    public RootLayout root() {
        return root;
    }

    public Page top() {
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

    // ---------------------------------------------------------------- 页面栈

    public void showHome() {
        Page home = Style.classic(this) ? new M3HomePage(this) : new HomePage(this);
        stack.clear();
        stack.add(home);
        root.pageHost.removeAllViews();
        root.pageHost.addView(home, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
        home.onPageShown();
        animateHostIn(home);
    }

    /** 主题切换后的整栈重建：经典主题走 M3 页面，其余走默认页面。 */
    public void rebuildAfterTheme() {
        if (Style.classic(this)) {
            resetTo(new M3HomePage(this), new M3SettingsPage(this),
                    new com.chatyue.app.ui.m3.M3AppearancePage(this));
        } else {
            resetTo(new HomePage(this), new SettingsPage(this), new AppearancePage(this));
        }
    }

    private Tween transition;

    private void settleTransition() {
        if (transition != null && transition.isRunning()) transition.cancel();
        transition = null;
        for (int i = 0; i < stack.size(); i++) {
            Page p = stack.get(i);
            p.setPageTransform(1f, 1f, 0);
            p.setVisibility(i == stack.size() - 1 ? View.VISIBLE : View.GONE);
        }
        // 连续快速返回时，上一次 pop 可能已把页面移出栈但还没来得及移除视图
        for (int i = root.pageHost.getChildCount() - 1; i >= 0; i--) {
            View child = root.pageHost.getChildAt(i);
            if (!stack.contains(child)) root.pageHost.removeViewAt(i);
        }
    }

    /**
     * 页面推入：整页淡入 + 从下方轻轻抬 14dp，旧页同步淡出。
     * 比原来的横向位移更像 M3 的 shared-axis 转场，也不会在中低端机上掉帧。
     */
    public void push(final Page page) {
        settleTransition();
        final Page prev = top();
        stack.add(page);
        root.pageHost.addView(page, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
        page.setVisibility(View.VISIBLE);
        page.setPageTransform(0f, 0.985f, Ui.dp(14));
        if (prev != null) prev.setPageTransform(1f, 1f, 0);
        transition = Tween.of(0, 1, 240, new Tween.Listener() {
            public void onUpdate(float v) {
                page.setPageTransform(v, 0.985f + 0.015f * v,
                        (int) (Ui.dp(14) * (1f - v)));
                if (prev != null) {
                    prev.setPageTransform(1f - v, 1f - 0.006f * v, (int) (-Ui.dp(5) * v));
                }
            }

            public void onEnd() {
                page.setPageTransform(1f, 1f, 0);
                if (prev != null) {
                    prev.setPageTransform(1f, 1f, 0);
                    prev.setVisibility(View.GONE);
                    prev.onPageHidden();
                }
                transition = null;
            }
        });
        transition.ease(Tween.DECEL).start();
        page.onPageShown();
    }

    public void pop() {
        if (stack.size() <= 1) return;
        settleTransition();
        final Page top = stack.remove(stack.size() - 1);
        final Page below = top();
        below.setVisibility(View.VISIBLE);
        below.onPageShown();
        below.setPageTransform(0f, 0.985f, Ui.dp(10));
        top.setPageTransform(1f, 1f, 0);
        transition = Tween.of(0, 1, 210, new Tween.Listener() {
            public void onUpdate(float v) {
                top.setPageTransform(1f - v, 1f - 0.004f * v, (int) (Ui.dp(12) * v));
                below.setPageTransform(v, 0.985f + 0.015f * v, (int) (Ui.dp(10) * (1f - v)));
            }

            public void onEnd() {
                top.onPageHidden();
                root.pageHost.removeView(top);
                top.setPageTransform(1f, 1f, 0);
                below.setPageTransform(1f, 1f, 0);
                below.onDataChanged();
                transition = null;
            }
        });
        transition.ease(Tween.DECEL).start();
    }

    public void popAll() {
        while (stack.size() > 1) {
            Page p = stack.remove(stack.size() - 1);
            root.pageHost.removeView(p);
        }
        Page home = top();
        if (home != null) {
            home.setVisibility(View.VISIBLE);
            home.onPageShown();
        }
    }

    // ------------------------------------------------------------ 常见跳转

    public void openChat(Session session) {
        push(Style.classic(this) ? new M3ChatPage(this, session) : new ChatPage(this, session));
    }

    public void openNewChat() {
        Session s = SessionStore.create(this);
        push(Style.classic(this) ? new M3ChatPage(this, s) : new ChatPage(this, s));
    }

    public void openNewChat(String title, String systemPrompt) {
        Session s = SessionStore.create(this);
        s.title = title;
        s.systemPrompt = systemPrompt;
        SessionStore.save(this, s);
        push(Style.classic(this) ? new M3ChatPage(this, s) : new ChatPage(this, s));
    }

    /** 带草稿开新会话（首页快捷入口共用）。 */
    public void openNewChatWithDraft(String title, String draft) {
        Session s = SessionStore.create(this);
        s.title = title;
        SessionStore.save(this, s);
        if (Style.classic(this)) {
            M3ChatPage p = new M3ChatPage(this, s);
            push(p);
            p.setDraft(draft);
        } else {
            ChatPage p = new ChatPage(this, s);
            push(p);
            p.setDraft(draft);
        }
    }

    public void openSettings() {
        push(Style.classic(this) ? new M3SettingsPage(this) : new SettingsPage(this));
    }

    public void openProviderEditor(String providerId) {
        if (Style.classic(this)) {
            push(new com.chatyue.app.ui.m3.M3ProviderEditPage(this, providerId));
        } else {
            push(new ProviderEditPage(this, providerId));
        }
    }

    // -------------------------------------------------------------- 反馈层

    public void toast(String text) {
        if (Style.classic(this)) M3Snackbar.show(root.toastLayer, text);
        else Toast.show(root.toastLayer, text);
    }

    public void copyText(String text) {
        try {
            com.chatyue.app.util.ClipboardCompat.setText(this, text);
            toast("已复制到剪贴板");
        } catch (Throwable t) {
            toast("复制失败");
        }
    }

    // ---------------------------------------------------------- 3.0 文件与预览

    /** 任意文件导入（不限制大小，流式复制到私有附件目录）。 */
    public void pickFile(FilePickCallback cb) {
        pendingFile = cb;
        try {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            if (Experiment.sdkInt() >= 4) i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i, REQ_FILE);
        } catch (Throwable t) {
            toast("当前设备没有可用的文件选择器");
        }
    }

    public void openLink(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Throwable t) {
            toast("没有可用的浏览器");
        }
    }

    public void openFile(final File f) {
        if (f == null || !f.exists()) {
            toast("文件不存在");
            return;
        }
        final String ext = AgentTools.ext(f.getName());
        if (f.isDirectory()) {
            push(new WorkspacePage(this, f));
            return;
        }
        if ("html".equals(ext) || "htm".equals(ext)) {
            push(new HtmlPreviewPage(this, f));
            return;
        }
        if ("png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext)
                || "gif".equals(ext) || "webp".equals(ext) || "bmp".equals(ext)) {
            push(new com.chatyue.app.ui.screen.ImagePreviewPage(this, f.getAbsolutePath()));
            return;
        }
        if (Attachments.isText(f.getName())) {
            push(new TextPreviewPage(this, f));
            return;
        }
        fileSheet(f);
    }

    public void fileSheet(final File f) {
        Sheet s = new Sheet(this).title(f.getName())
                .subtitle(AgentTools.sizeText(f.length()) + " · " + f.getParent());
        s.add(com.chatyue.app.core.Icons.SHARE, "分享", new Runnable() {
            public void run() {
                java.util.ArrayList<File> list = new java.util.ArrayList<File>();
                list.add(f);
                FileSharing.share(MainActivity.this, list);
            }
        });
        s.add(com.chatyue.app.core.Icons.DOWNLOAD, "保存到本地", new Runnable() {
            public void run() {
                saveArtifact(f);
            }
        });
        s.add(com.chatyue.app.core.Icons.INFO, "文件信息", new Runnable() {
            public void run() {
                com.chatyue.app.ui.overlay.Dialog.alert(root.overlay, f.getName(),
                        f.getAbsolutePath() + "\n\n" + AgentTools.sizeText(f.length()));
            }
        });
        s.show(root.overlay);
    }

    /** 代码块里的 HTML 直接预览：写到缓存目录再交给 WebView。 */
    public void previewHtmlCode(String code) {
        File dir = new File(getCacheDir(), "code_preview");
        FileUtil.mkdirs(dir);
        File f = new File(dir, "preview_" + System.currentTimeMillis() + ".html");
        try {
            FileUtil.writeText(f, code == null ? "" : code);
            push(new HtmlPreviewPage(this, f));
        } catch (Throwable t) {
            toast("预览失败：" + t.getMessage());
        }
    }

    public void shareArtifacts(java.util.ArrayList<String> paths) {
        java.util.ArrayList<File> files = new java.util.ArrayList<File>();
        for (int i = 0; i < paths.size(); i++) {
            File f = new File(paths.get(i));
            if (f.exists()) files.add(f);
        }
        if (files.isEmpty()) {
            toast("文件已被删除");
            return;
        }
        FileSharing.share(this, files);
    }

    public void shareArtifact(File f) {
        java.util.ArrayList<File> list = new java.util.ArrayList<File>();
        list.add(f);
        FileSharing.share(this, list);
    }

    /**
     * 保存产物：
     * - Android 10+ 走 MediaStore 的 Download 集合，不需要权限；
     * - Android 6–9 需要写外部存储权限，没有就申请；
     * - 失败或没有权限时退回应用外部目录，至少文件是留住的。
     */
    public void saveArtifact(final File f) {
        if (f == null || !f.exists()) {
            toast("文件不存在");
            return;
        }
        if (Experiment.sdkInt() >= 29) {
            String where = FileSharing.saveViaMediaStore(this, f);
            if (where != null) {
                toast("已保存到 " + where);
                return;
            }
        }
        if (Experiment.canWritePublic(this) && Experiment.sdkInt() < 29) {
            File out = FileSharing.saveToDownloads(this, f);
            if (out != null) {
                toast("已保存到 " + out.getAbsolutePath());
                return;
            }
        }
        if (Experiment.sdkInt() >= 23 && Experiment.sdkInt() < 29
                && !Experiment.granted(this, "android.permission.WRITE_EXTERNAL_STORAGE")) {
            pendingStorageAction = new Runnable() {
                public void run() {
                    saveArtifact(f);
                }
            };
            Experiment.requestPermissions(this, new String[]{
                    "android.permission.WRITE_EXTERNAL_STORAGE"}, Experiment.REQ_STORAGE);
            toast("请允许存储权限后重试保存");
            return;
        }
        File fallback = FileSharing.saveToAppStorage(this, f);
        if (fallback != null) toast("已保存到 " + fallback.getAbsolutePath());
        else toast("保存失败，请检查存储空间");
    }

    public void openInBrowser(File f) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            if (Experiment.sdkInt() >= 24) {
                i.setDataAndType(FileSharing.uriFor(this, f), "text/html");
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                i.setDataAndType(Uri.fromFile(f), "text/html");
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Throwable t) {
            toast("没有可用的浏览器");
        }
    }

    public void openWorkspace() {
        push(new WorkspacePage(this, AgentTools.root(this)));
    }

    public void createFolderHere(final File parent, final Runnable after) {
        com.chatyue.app.ui.overlay.Dialog.input(root.overlay, "新建文件夹", "文件夹名称", "",
                new com.chatyue.app.ui.overlay.Dialog.InputCallback() {
                    public void onInput(String text) {
                        try {
                            String name = Attachments.sanitize(text);
                            if (name.length() == 0) return;
                            File f = new File(parent, name);
                            if (!f.exists()) f.mkdirs();
                            toast("已创建 " + name);
                            if (after != null) after.run();
                        } catch (Throwable t) {
                            toast("创建失败");
                        }
                    }
                });
    }

    public void createFileHere(final File parent, final Runnable after) {
        com.chatyue.app.ui.overlay.Dialog.input(root.overlay, "新建文本文件", "文件名，例如 note.md", "",
                new com.chatyue.app.ui.overlay.Dialog.InputCallback() {
                    public void onInput(String text) {
                        try {
                            String name = Attachments.sanitize(text);
                            if (name.length() == 0) return;
                            File f = new File(parent, name);
                            FileUtil.writeText(f, "");
                            toast("已创建 " + name);
                            if (after != null) after.run();
                        } catch (Throwable t) {
                            toast("创建失败");
                        }
                    }
                });
    }

    // -------------------------------------------------------------- 图片输入

    public void pickImage(ImagePickCallback cb) {
        pendingPick = cb;
        try {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            startActivityForResult(i, REQ_PICK);
        } catch (Throwable t) {
            toast("当前设备没有可用的图片选择器");
        }
    }

    public void takePhoto(ImagePickCallback cb) {
        pendingPick = cb;
        if (Experiment.sdkInt() >= 23 && !Experiment.granted(this, "android.permission.CAMERA")) {
            Experiment.requestPermissions(this, new String[]{"android.permission.CAMERA"}, REQ_CAMERA_PERM);
            return;
        }
        try {
            ContentValues v = new ContentValues();
            v.put(MediaStore.Images.Media.TITLE, "chatyue_" + System.currentTimeMillis());
            v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (uri == null) {
                toast("无法创建拍照文件");
                return;
            }
            pendingCameraUri = uri;
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            startActivityForResult(i, REQ_CAMERA);
        } catch (Throwable t) {
            toast("当前设备不支持拍照");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                importPickedFile(data.getData());
            }
            pendingFile = null;
            return;
        }
        if (resultCode != RESULT_OK) return;
        String path = null;
        if (requestCode == REQ_PICK) {
            if (data != null && data.getData() != null) {
                path = saveImage(data.getData());
            } else if (data != null && data.getExtras() != null && data.getExtras().get("data") != null) {
                Object bmp = data.getExtras().get("data");
                if (bmp instanceof Bitmap) path = saveBitmap((Bitmap) bmp);
            }
        } else if (requestCode == REQ_CAMERA) {
            if (pendingCameraUri != null) path = saveImage(pendingCameraUri);
        }
        if (path != null && pendingPick != null) {
            pendingPick.onImage(path);
        } else if (path == null && pendingPick != null) {
            toast("图片读取失败");
        }
        pendingPick = null;
        pendingCameraUri = null;
    }

    private String saveImage(Uri uri) {
        InputStream in = null;
        try {
            in = getContentResolver().openInputStream(uri);
            if (in == null) return null;
            Bitmap bmp = FileUtil.decodeSampled(in, 1280);
            return saveBitmap(bmp);
        } catch (Throwable t) {
            return null;
        } finally {
            FileUtil.close(in);
        }
    }

    private String saveBitmap(Bitmap bmp) {
        if (bmp == null) return null;
        FileOutputStream out = null;
        try {
            File dir = SessionStore.imageDir(this);
            File f = new File(dir, "img_" + System.currentTimeMillis() + ".jpg");
            out = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.JPEG, 82, out);
            out.flush();
            return f.getAbsolutePath();
        } catch (Throwable t) {
            return null;
        } finally {
            FileUtil.close(out);
        }
    }

    private void importPickedFile(final Uri uri) {
        final String name = Attachments.displayName(this, uri);
        final FilePickCallback cb = pendingFile;
        RequestQueue.get().submit(new Runnable() {
            public void run() {
                try {
                    final File f = Attachments.importUri(MainActivity.this, uri, name);
                    RequestQueue.get().postMain(new Runnable() {
                        public void run() {
                            if (cb != null) cb.onFile(f.getAbsolutePath());
                            else toast("附件已导入：" + f.getName());
                        }
                    });
                } catch (final Throwable t) {
                    RequestQueue.get().postMain(new Runnable() {
                        public void run() {
                            toast("附件导入失败：" + t.getMessage());
                        }
                    });
                }
            }
        });
    }

    /** API 23+ 的运行时权限回调；低版本不会调用到。 */
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Experiment.REQ_STORAGE) {
            Runnable r = pendingStorageAction;
            pendingStorageAction = null;
            if (r != null && Experiment.granted(this, "android.permission.WRITE_EXTERNAL_STORAGE")) {
                r.run();
            } else {
                toast("未获得存储权限，产物会存到应用目录");
            }
        } else if (requestCode == REQ_CAMERA_PERM) {
            if (Experiment.granted(this, "android.permission.CAMERA")) {
                ImagePickCallback cb = pendingPick;
                if (cb != null) takePhoto(cb);
            } else {
                toast("未获得相机权限");
                pendingPick = null;
            }
        } else if (requestCode == Experiment.REQ_ALL_FILES) {
            boolean ok = Experiment.hasAllFilesPermission(this);
            toast(ok ? "全部文件权限已获得" : "未获得存储权限，Agent 仍只能访问应用工作区");
            Page p = top();
            if (p instanceof com.chatyue.app.ui.screen.ExperimentalPage) {
                ((com.chatyue.app.ui.screen.ExperimentalPage) p).refreshPermissionState();
            }
        }
    }

    // ------------------------------------------------------------- 返回键

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return handleBack();
        }
        return super.onKeyDown(keyCode, event);
    }

    /** API 5+ 会走这里；1.5/1.6 走上面的 onKeyDown。不加 @Override 以兼容 API 3 编译。 */
    public void onBackPressed() {
        handleBack();
    }

    public boolean handleBack() {
        if (root.overlay.isOpen()) {
            root.overlay.close();
            return true;
        }
        Page p = top();
        if (p != null && p.onBack()) return true;
        if (stack.size() > 1) {
            pop();
            return true;
        }
        long now = System.currentTimeMillis();
        if (now - lastBackAt < 2000) {
            finish();
        } else {
            lastBackAt = now;
            toast("再按一次退出 ChatYUE");
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        sInstance = null;
        Tween.stopAll();
        super.onDestroy();
    }
}
