package com.chatyue.app.core;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

/**
 * 矢量图标库：不依赖 PNG / VectorDrawable（低版本都没有），
 * 全部用 Path 在 24x24 的逻辑坐标系里手绘，可任意放大不失真。
 */
public final class Icons {

    public static final int BACK = 1;
    public static final int CLOSE = 2;
    public static final int MORE = 3;
    public static final int MORE_H = 4;
    public static final int PLUS = 5;
    public static final int SEND = 6;
    public static final int STOP = 7;
    public static final int SEARCH = 8;
    public static final int USER = 9;
    public static final int GEAR = 10;
    public static final int COPY = 11;
    public static final int REFRESH = 12;
    public static final int TRASH = 13;
    public static final int CHECK = 14;
    public static final int CHEVRON_RIGHT = 15;
    public static final int CHEVRON_DOWN = 16;
    public static final int CHEVRON_UP = 17;
    public static final int IMAGE = 18;
    public static final int CAMERA = 19;
    public static final int BOOK = 20;
    public static final int CLOCK = 21;
    public static final int EDIT = 22;
    public static final int TRANSLATE = 23;
    public static final int ROBOT = 24;
    public static final int STAR = 25;
    public static final int SHARE = 26;
    public static final int SPARKLE = 27;
    public static final int ARROW_UP = 28;
    public static final int DOC = 29;
    public static final int INFO = 30;
    public static final int LOCK = 31;
    public static final int DOT = 32;
    public static final int LIST = 33;
    public static final int BRUSH = 34;
    public static final int GLOBE = 35;
    public static final int MUTE = 36;
    public static final int WAIT = 37;
    public static final int MESSAGE = 38;
    public static final int SERVER = 39;
    public static final int TERMINAL = 40;
    public static final int FOLDER = 41;
    public static final int CODE = 42;
    public static final int SPEED = 43;
    public static final int PALETTE = 44;
    public static final int DOWNLOAD = 45;
    public static final int HOME = 46;

    private Icons() {
    }

    public static void draw(Canvas c, int id, float cx, float cy, float size, int color, float strokeDp) {
        c.save();
        c.translate(cx - size / 2f, cy - size / 2f);
        float s = size / 24f;
        c.scale(s, s);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1.4f, strokeDp * 24f / Math.max(1f, size)));
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        Path path = new Path();
        RectF r = new RectF();

        switch (id) {
            case BACK:
                c.drawLine(14.5f, 5f, 7.5f, 12f, p);
                c.drawLine(7.5f, 12f, 14.5f, 19f, p);
                break;
            case CLOSE:
                c.drawLine(6f, 6f, 18f, 18f, p);
                c.drawLine(18f, 6f, 6f, 18f, p);
                break;
            case MORE:
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(12f, 5.6f, 1.9f, p);
                c.drawCircle(12f, 12f, 1.9f, p);
                c.drawCircle(12f, 18.4f, 1.9f, p);
                break;
            case MORE_H:
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(5.4f, 12f, 1.9f, p);
                c.drawCircle(12f, 12f, 1.9f, p);
                c.drawCircle(18.6f, 12f, 1.9f, p);
                break;
            case PLUS:
                c.drawLine(12f, 5f, 12f, 19f, p);
                c.drawLine(5f, 12f, 19f, 12f, p);
                break;
            case SEND:
                p.setStyle(Paint.Style.FILL);
                path.moveTo(3.4f, 19.6f);
                path.lineTo(21f, 12f);
                path.lineTo(3.4f, 4.4f);
                path.lineTo(5.9f, 11.2f);
                path.lineTo(14.4f, 12f);
                path.lineTo(5.9f, 12.8f);
                path.close();
                c.drawPath(path, p);
                break;
            case STOP:
                p.setStyle(Paint.Style.FILL);
                r.set(7.2f, 7.2f, 16.8f, 16.8f);
                c.drawRoundRect(r, 2.6f, 2.6f, p);
                break;
            case SEARCH:
                c.drawCircle(10.6f, 10.6f, 6.1f, p);
                c.drawLine(15.2f, 15.2f, 19.6f, 19.6f, p);
                break;
            case USER:
                c.drawCircle(12f, 8.4f, 3.7f, p);
                path.moveTo(4.8f, 20f);
                path.cubicTo(5.4f, 15.6f, 8.4f, 13.6f, 12f, 13.6f);
                path.cubicTo(15.6f, 13.6f, 18.6f, 15.6f, 19.2f, 20f);
                c.drawPath(path, p);
                break;
            case GEAR:
                c.drawCircle(12f, 12f, 3.4f, p);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI / 4 * i + Math.PI / 8;
                    float x1 = (float) (12 + Math.cos(a) * 6.2);
                    float y1 = (float) (12 + Math.sin(a) * 6.2);
                    float x2 = (float) (12 + Math.cos(a) * 8.4);
                    float y2 = (float) (12 + Math.sin(a) * 8.4);
                    c.drawLine(x1, y1, x2, y2, p);
                }
                break;
            case COPY:
                r.set(8.6f, 8.6f, 19.4f, 19.4f);
                c.drawRoundRect(r, 2.4f, 2.4f, p);
                path.moveTo(15.4f, 5.6f);
                path.lineTo(5.2f, 5.6f);
                path.lineTo(5.2f, 15.4f);
                c.drawPath(path, p);
                break;
            case REFRESH: {
                // 3.0 修正：旧版圆弧从 -55° 扫 250°，箭头却画在右上角，方向是反的。
                // 现在圆弧收在 260°，箭头按切线方向贴在弧的末端，朝向缺口。
                float ccx = 12f, ccy = 12f, rad = 7.4f;
                r.set(ccx - rad, ccy - rad, ccx + rad, ccy + rad);
                c.drawArc(r, -40f, 300f, false, p);
                double end = Math.toRadians(260);
                double tangent = end + Math.PI / 2;
                float px = (float) (ccx + rad * Math.cos(end));
                float py = (float) (ccy + rad * Math.sin(end));
                float tx = (float) Math.cos(tangent);
                float ty = (float) Math.sin(tangent);
                float nx = (float) Math.cos(end);
                float ny = (float) Math.sin(end);
                float len = 5.2f, half = 2.5f;
                Path head = new Path();
                head.moveTo(px + tx * len * 0.55f, py + ty * len * 0.55f);
                head.lineTo(px - tx * len * 0.45f + nx * half, py - ty * len * 0.45f + ny * half);
                head.lineTo(px - tx * len * 0.45f - nx * half, py - ty * len * 0.45f - ny * half);
                head.close();
                Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
                fill.setColor(color);
                fill.setStyle(Paint.Style.FILL);
                c.drawPath(head, fill);
                break;
            }
            case TRASH:
                c.drawLine(4.6f, 7f, 19.4f, 7f, p);
                c.drawLine(9.2f, 7f, 9.8f, 4.4f, p);
                c.drawLine(14.8f, 7f, 14.2f, 4.4f, p);
                c.drawLine(9.8f, 4.4f, 14.2f, 4.4f, p);
                path.moveTo(6.4f, 7f);
                path.lineTo(7.4f, 19.8f);
                path.lineTo(16.6f, 19.8f);
                path.lineTo(17.6f, 7f);
                c.drawPath(path, p);
                c.drawLine(10.4f, 10.4f, 10.9f, 16.4f, p);
                c.drawLine(13.6f, 10.4f, 13.1f, 16.4f, p);
                break;
            case CHECK:
                path.moveTo(5f, 12.6f);
                path.lineTo(9.8f, 17.2f);
                path.lineTo(19f, 6.8f);
                c.drawPath(path, p);
                break;
            case CHEVRON_RIGHT:
                path.moveTo(9.4f, 5.4f);
                path.lineTo(16f, 12f);
                path.lineTo(9.4f, 18.6f);
                c.drawPath(path, p);
                break;
            case CHEVRON_DOWN:
                path.moveTo(5.4f, 9.4f);
                path.lineTo(12f, 16f);
                path.lineTo(18.6f, 9.4f);
                c.drawPath(path, p);
                break;
            case CHEVRON_UP:
                path.moveTo(5.4f, 14.6f);
                path.lineTo(12f, 8f);
                path.lineTo(18.6f, 14.6f);
                c.drawPath(path, p);
                break;
            case IMAGE:
                r.set(3.6f, 4.6f, 20.4f, 19.4f);
                c.drawRoundRect(r, 2.6f, 2.6f, p);
                c.drawCircle(8.8f, 9.4f, 1.7f, p);
                path.moveTo(4.6f, 17.2f);
                path.lineTo(9.6f, 12.4f);
                path.lineTo(13.4f, 16f);
                path.lineTo(16.4f, 13.2f);
                path.lineTo(19.4f, 16.6f);
                c.drawPath(path, p);
                break;
            case CAMERA:
                r.set(3.4f, 7.4f, 20.6f, 19.4f);
                c.drawRoundRect(r, 2.8f, 2.8f, p);
                path.moveTo(8.6f, 7.4f);
                path.lineTo(9.8f, 4.6f);
                path.lineTo(14.2f, 4.6f);
                path.lineTo(15.4f, 7.4f);
                c.drawPath(path, p);
                c.drawCircle(12f, 13.4f, 3.4f, p);
                break;
            case BOOK:
                path.moveTo(4.4f, 5.2f);
                path.lineTo(11.4f, 5.2f);
                path.lineTo(11.4f, 19f);
                path.lineTo(4.4f, 19f);
                path.close();
                path.moveTo(12.6f, 5.2f);
                path.lineTo(19.6f, 5.2f);
                path.lineTo(19.6f, 19f);
                path.lineTo(12.6f, 19f);
                c.drawPath(path, p);
                c.drawLine(12f, 5.4f, 12f, 19f, p);
                break;
            case CLOCK:
                c.drawCircle(12f, 12f, 8f, p);
                c.drawLine(12f, 7.4f, 12f, 12.2f, p);
                c.drawLine(12f, 12.2f, 15.6f, 14f, p);
                break;
            case EDIT:
                path.moveTo(5f, 19f);
                path.lineTo(6.2f, 14.4f);
                path.lineTo(15.6f, 5f);
                path.lineTo(19f, 8.4f);
                path.lineTo(9.6f, 17.8f);
                path.close();
                c.drawPath(path, p);
                c.drawLine(13.6f, 7f, 17f, 10.4f, p);
                break;
            case TRANSLATE: {
                p.setStyle(Paint.Style.FILL);
                p.setTypeface(Typeface.DEFAULT_BOLD);
                p.setTextSize(9.6f);
                c.drawText("\u6587", 3.2f, 12.4f, p);
                p.setTextSize(6.4f);
                c.drawText("A", 14.4f, 9.4f, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.3f);
                c.drawLine(12.6f, 16.6f, 19.6f, 16.6f, p);
                break;
            }
            case ROBOT:
                r.set(4.4f, 7.6f, 19.6f, 19.2f);
                c.drawRoundRect(r, 3.4f, 3.4f, p);
                c.drawLine(12f, 4f, 12f, 7.6f, p);
                c.drawCircle(12f, 3.4f, 1.1f, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(9f, 12.4f, 1.3f, p);
                c.drawCircle(15f, 12.4f, 1.3f, p);
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(9.4f, 16f, 14.6f, 16f, p);
                break;
            case STAR:
                for (int i = 0; i < 10; i++) {
                    double a = -Math.PI / 2 + Math.PI / 5 * i;
                    float rad = (i % 2 == 0) ? 8.6f : 3.8f;
                    float x = (float) (12 + Math.cos(a) * rad);
                    float y = (float) (12 + Math.sin(a) * rad);
                    if (i == 0) path.moveTo(x, y);
                    else path.lineTo(x, y);
                }
                path.close();
                c.drawPath(path, p);
                break;
            case SPARKLE:
                path.moveTo(12f, 3.4f);
                path.quadTo(13.4f, 10.6f, 20.6f, 12f);
                path.quadTo(13.4f, 13.4f, 12f, 20.6f);
                path.quadTo(10.6f, 13.4f, 3.4f, 12f);
                path.quadTo(10.6f, 10.6f, 12f, 3.4f);
                path.close();
                p.setStyle(Paint.Style.FILL);
                c.drawPath(path, p);
                break;
            case SHARE:
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(17.6f, 5.8f, 2.6f, p);
                c.drawCircle(6.4f, 12f, 2.6f, p);
                c.drawCircle(17.6f, 18.2f, 2.6f, p);
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(8.8f, 10.8f, 15.2f, 7f, p);
                c.drawLine(8.8f, 13.2f, 15.2f, 17f, p);
                break;
            case ARROW_UP:
                c.drawLine(12f, 19f, 12f, 5.6f, p);
                path.moveTo(6.4f, 11f);
                path.lineTo(12f, 5.4f);
                path.lineTo(17.6f, 11f);
                c.drawPath(path, p);
                break;
            case DOC:
                path.moveTo(6f, 3.8f);
                path.lineTo(14f, 3.8f);
                path.lineTo(18.4f, 8.2f);
                path.lineTo(18.4f, 20.2f);
                path.lineTo(6f, 20.2f);
                path.close();
                c.drawPath(path, p);
                path.moveTo(13.8f, 4f);
                path.lineTo(13.8f, 8.4f);
                path.lineTo(18.2f, 8.4f);
                c.drawPath(path, p);
                c.drawLine(8.8f, 12.6f, 15.6f, 12.6f, p);
                c.drawLine(8.8f, 16f, 15.6f, 16f, p);
                break;
            case INFO:
                c.drawCircle(12f, 12f, 8.2f, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(12f, 7.9f, 1.15f, p);
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(12f, 11f, 12f, 16.4f, p);
                break;
            case LOCK:
                r.set(5f, 10.4f, 19f, 20f);
                c.drawRoundRect(r, 2.4f, 2.4f, p);
                r.set(8.2f, 4.4f, 15.8f, 12.4f);
                c.drawArc(r, 180f, 180f, false, p);
                break;
            case DOT:
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(12f, 12f, 4.6f, p);
                break;
            case LIST:
                c.drawLine(8.6f, 6.6f, 19.4f, 6.6f, p);
                c.drawLine(8.6f, 12f, 19.4f, 12f, p);
                c.drawLine(8.6f, 17.4f, 19.4f, 17.4f, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(4.8f, 6.6f, 1.3f, p);
                c.drawCircle(4.8f, 12f, 1.3f, p);
                c.drawCircle(4.8f, 17.4f, 1.3f, p);
                break;
            case BRUSH:
                path.moveTo(4.8f, 19.2f);
                path.cubicTo(5.4f, 15f, 8f, 14.6f, 9.6f, 16.2f);
                path.cubicTo(11f, 17.6f, 10.4f, 19.6f, 4.8f, 19.2f);
                path.close();
                p.setStyle(Paint.Style.FILL);
                c.drawPath(path, p);
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(9.4f, 15.2f, 18.4f, 6.2f, p);
                break;
            case GLOBE:
                c.drawCircle(12f, 12f, 8.2f, p);
                r.set(7.4f, 3.8f, 16.6f, 20.2f);
                c.drawOval(r, p);
                c.drawLine(3.8f, 12f, 20.2f, 12f, p);
                break;
            case MUTE:
                path.moveTo(4.4f, 9.4f);
                path.lineTo(8.4f, 9.4f);
                path.lineTo(12.8f, 5.6f);
                path.lineTo(12.8f, 18.4f);
                path.lineTo(8.4f, 14.6f);
                path.lineTo(4.4f, 14.6f);
                path.close();
                c.drawPath(path, p);
                c.drawLine(16f, 9.4f, 19.6f, 14.6f, p);
                c.drawLine(19.6f, 9.4f, 16f, 14.6f, p);
                break;
            case WAIT:
                c.drawCircle(12f, 12f, 8.2f, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(8.4f, 12f, 1.2f, p);
                c.drawCircle(12f, 12f, 1.2f, p);
                c.drawCircle(15.6f, 12f, 1.2f, p);
                break;
            case MESSAGE:
                r.set(4.4f, 4.6f, 19.6f, 16.4f);
                c.drawRoundRect(r, 3.4f, 3.4f, p);
                path.moveTo(8.2f, 16.4f);
                path.lineTo(7.2f, 20.2f);
                path.lineTo(12.2f, 16.4f);
                c.drawPath(path, p);
                c.drawLine(8.2f, 9.2f, 15.8f, 9.2f, p);
                c.drawLine(8.2f, 12.6f, 13.4f, 12.6f, p);
                break;
            case SERVER:
                r.set(3.8f, 3.8f, 20.2f, 9.6f);
                c.drawRoundRect(r, 2.2f, 2.2f, p);
                r.set(3.8f, 12.2f, 20.2f, 18f);
                c.drawRoundRect(r, 2.2f, 2.2f, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(7.2f, 6.7f, 1.2f, p);
                c.drawCircle(7.2f, 15.1f, 1.2f, p);
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(11f, 6.7f, 17f, 6.7f, p);
                c.drawLine(11f, 15.1f, 17f, 15.1f, p);
                break;
            case TERMINAL:
                r.set(3.4f, 4.4f, 20.6f, 19.6f);
                c.drawRoundRect(r, 3f, 3f, p);
                path.moveTo(7.4f, 9.6f);
                path.lineTo(10.4f, 12.4f);
                path.lineTo(7.4f, 15.2f);
                c.drawPath(path, p);
                c.drawLine(12.4f, 15.2f, 16.4f, 15.2f, p);
                break;
            case FOLDER:
                path.moveTo(4f, 7.2f);
                path.lineTo(9.2f, 7.2f);
                path.lineTo(10.8f, 9.4f);
                path.lineTo(19.8f, 9.4f);
                path.lineTo(19.8f, 18.4f);
                path.lineTo(4f, 18.4f);
                path.close();
                c.drawPath(path, p);
                break;
            case CODE:
                path.moveTo(9f, 7.4f);
                path.lineTo(4.6f, 12f);
                path.lineTo(9f, 16.6f);
                c.drawPath(path, p);
                path.moveTo(15f, 7.4f);
                path.lineTo(19.4f, 12f);
                path.lineTo(15f, 16.6f);
                c.drawPath(path, p);
                c.drawLine(13.4f, 5.6f, 10.6f, 18.4f, p);
                break;
            case SPEED:
                r.set(4.4f, 4.4f, 19.6f, 19.6f);
                c.drawArc(r, 205f, 310f, false, p);
                c.drawLine(12f, 12f, 16.4f, 7.6f, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(12f, 12f, 1.8f, p);
                break;
            case PALETTE:
                c.drawCircle(12f, 12f, 8.2f, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(8.2f, 9.2f, 1.2f, p);
                c.drawCircle(12f, 7.4f, 1.2f, p);
                c.drawCircle(15.8f, 9.6f, 1.2f, p);
                c.drawCircle(8.6f, 14.2f, 1.2f, p);
                break;
            case DOWNLOAD:
                c.drawLine(12f, 4.4f, 12f, 15.4f, p);
                path.moveTo(7.4f, 11f);
                path.lineTo(12f, 15.6f);
                path.lineTo(16.6f, 11f);
                c.drawPath(path, p);
                c.drawLine(5f, 19.4f, 19f, 19.4f, p);
                break;
            case HOME:
                path.moveTo(3.8f, 11.2f);
                path.lineTo(12f, 4.2f);
                path.lineTo(20.2f, 11.2f);
                c.drawPath(path, p);
                path.moveTo(6f, 10f);
                path.lineTo(6f, 19.6f);
                path.lineTo(18f, 19.6f);
                path.lineTo(18f, 10f);
                c.drawPath(path, p);
                break;
            default:
                c.drawCircle(12f, 12f, 7f, p);
                break;
        }
        c.restore();
    }

    /** 画一个小圆点徽标（未读 / 在线）。 */
    public static void dot(Canvas c, float cx, float cy, float r, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        c.drawCircle(cx, cy, r, p);
    }
}
