package com.chatyue.app.ui.screen;

import android.text.InputType;
import android.view.View;

import com.chatyue.app.MainActivity;
import com.chatyue.app.core.Icons;
import com.chatyue.app.data.Prefs;
import com.chatyue.app.data.RemoteServer;
import com.chatyue.app.net.RequestQueue;
import com.chatyue.app.ui.overlay.Dialog;
import com.chatyue.app.ui.widget.FieldBox;
import com.chatyue.app.ui.widget.Toggle;

/**
 * 右上角「配置服务器连接」。
 *
 * 服务器需要实现一个很小的 HTTPS JSON 端点；应用只保存地址、令牌和工作目录，
 * 不在客户端保存 SSH 私钥，也不内置 SSH 协议。
 */
public class ServerPage extends BasePage {

    private FieldBox urlField;
    private FieldBox tokenField;
    private FieldBox cwdField;
    private Toggle enabledToggle;
    private Toggle selfSignedToggle;

    public ServerPage(MainActivity a) {
        super(a, "配置服务器连接", "SSL / HTTPS 命令端点");
        build();
    }

    private void build() {
        resetContent();
        heading("让 Agent 操控你的服务器",
                "在服务器上运行一个 HTTPS 命令端点；启用后 Agent 每轮都可以通过 server_exec 执行远程命令。");

        section("连接参数");
        urlField = field("服务器地址", "https://example.com/v1/exec",
                Prefs.serverUrl(act), 1);
        urlField.edit.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_URI);
        tokenField = field("访问令牌", "Bearer token", Prefs.serverToken(act), 1);
        tokenField.edit.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        cwdField = field("远程工作目录", "/root 或 /var/www", Prefs.serverCwd(act), 1);

        section("选项");
        enabledToggle = toggleRow(Icons.SERVER, "启用服务器连接",
                "关闭时 server_exec 不可用，Agent 仍只操作手机本地工作区",
                Prefs.serverEnabled(act), new Toggle.Listener() {
                    public void onToggle(boolean checked) {
                        Prefs.setServerEnabled(act, checked);
                        act.toast(checked ? "服务器连接已启用" : "服务器连接已关闭");
                    }
                });
        selfSignedToggle = toggleRow(Icons.LOCK, "允许自签名证书",
                "仅用于你自己的测试服务器；会跳过证书链校验，公网服务器请保持关闭",
                Prefs.serverSelfSigned(act), new Toggle.Listener() {
                    public void onToggle(boolean checked) {
                        Prefs.setServerSelfSigned(act, checked);
                        if (checked) act.toast("已允许自签名证书，仅建议内网测试");
                    }
                });

        primaryButton("保存并测试连接", new View.OnClickListener() {
            public void onClick(View v) {
                saveAndTest();
            }
        });
        ghostButton("复制服务端协议", new View.OnClickListener() {
            public void onClick(View v) {
                act.copyText(protocol());
                act.toast("协议已复制");
            }
        });

        note("协议：POST 到服务器地址，请求头 Authorization: Bearer <令牌>，请求体 "
                + "{\"command\":\"ls -la\",\"cwd\":\"/root\",\"timeout\":30}；"
                + "返回 {\"ok\":true,\"exit\":0,\"output\":\"...\"}。"
                + "服务器端可用 Python/Node 自行实现，必须开启 HTTPS。");
        note("这是命令端点，不是 SSH 客户端；不要把令牌提交到公开仓库。"
                + "Agent 不限制总轮数，但每轮都会发起一次模型请求，请随时用停止按钮中断。");
    }

    private void saveAndTest() {
        String url = normalize(urlField.text());
        String token = tokenField.text();
        String cwd = cwdField.text();
        if (url.length() == 0) {
            act.toast("请填写服务器地址");
            return;
        }
        if (!url.startsWith("https://")) {
            act.toast("服务器地址必须以 https:// 开头");
            return;
        }
        if (token.length() == 0) {
            act.toast("请填写访问令牌");
            return;
        }
        if (cwd.length() == 0) cwd = "/";
        Prefs.setServerUrl(act, url);
        Prefs.setServerToken(act, token);
        Prefs.setServerCwd(act, cwd);
        Prefs.setServerEnabled(act, true);
        if (enabledToggle != null) enabledToggle.setChecked(true, true);
        act.toast("已保存，正在测试…");
        new Thread(new Runnable() {
            public void run() {
                final RemoteServer.Result r = RemoteServer.test(act);
                RequestQueue.get().postMain(new Runnable() {
                    public void run() {
                        Dialog.alert(act.root().overlay,
                                r.ok() ? "服务器连接成功" : "服务器连接失败",
                                "exit=" + r.exit + "\n" + r.output
                                        + "\n\n如果失败，请检查地址、令牌、证书和端点是否按协议返回 JSON。");
                    }
                });
            }
        }).start();
    }

    private String normalize(String s) {
        String v = s == null ? "" : s.trim();
        if (v.length() == 0) return "";
        if (v.indexOf("://") < 0) v = "https://" + v;
        return v;
    }

    private String protocol() {
        return "ChatYUE 3.0 远程服务器协议\n"
                + "POST <服务器地址>\n"
                + "Authorization: Bearer <访问令牌>\n"
                + "Content-Type: application/json\n\n"
                + "请求：{\"command\":\"ls -la\",\"cwd\":\"/root\",\"timeout\":30}\n"
                + "响应：{\"ok\":true,\"exit\":0,\"output\":\"...\",\"timeout\":false}\n"
                + "要求：HTTPS（或你明确允许的自签名证书），令牌只保存在本机。"
                + "失败时返回非 2xx 或 {ok:false}。\n";
    }
}
