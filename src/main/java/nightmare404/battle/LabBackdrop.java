package nightmare404.battle;

import static nightmare404.battle.BattleScreen.BattlePanel.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import nightmare404.battle.BattleScreen.BattlePanel;

// A battle background, from the Laboratory screens (Lab 2, Lab 3, Test Subject X) in the Battle_Screens folder.
// It's the original drawRoom code; the battle calls draw() behind the fighters (see BattlePanel.drawRoom).
public class LabBackdrop implements Backdrop {
    /** The abandoned lab: filthy wall tiles, an observation window, a specimen tank, steel counters, a cold floor. */
    public void draw(Graphics2D g, BattlePanel battle) {
        Shape oldClip = g.getClip();
        g.clipRect(0, TOP_H, W, STAGE_BOTTOM - TOP_H);
        Random r = new Random(404); // same decoration every frame

        // cold, dirty wall tiles
        g.setPaint(new GradientPaint(0, TOP_H, new Color(64, 74, 74), 0, FLOOR_TOP, new Color(32, 38, 40)));
        g.fillRect(0, TOP_H, W, FLOOR_TOP - TOP_H);
        for (int i = 0; i < 46; i++) { // stained, cracked and missing tiles
            int tx = r.nextInt(40) * 20, ty = TOP_H + 12 + r.nextInt(7) * 20;
            g.setColor(r.nextInt(3) == 0 ? new Color(12, 16, 16, 210) : new Color(96, 100, 70, 45));
            g.fillRect(tx + 1, ty + 1, 19, 19);
        }
        g.setColor(new Color(18, 24, 24, 110));
        g.setStroke(new BasicStroke(1));
        for (int y = TOP_H + 12; y < FLOOR_TOP; y += 20) g.drawLine(0, y, W, y);
        for (int x = 0; x < W; x += 20) g.drawLine(x, TOP_H, x, FLOOR_TOP);
        for (int i = 0; i < 7; i++) { // mould and chemical stains
            double sx = r.nextInt(W), sy = TOP_H + 20 + r.nextInt(120), sr = 30 + r.nextInt(50);
            g.setPaint(new RadialGradientPaint(new Point2D.Double(sx, sy), (float) sr, new float[] { 0f, 1f },
                    new Color[] { new Color(70, 80, 30, 70), new Color(70, 80, 30, 0) }));
            g.fill(el(sx - sr, sy - sr, sr * 2, sr * 2));
        }

        // pipe along the ceiling, dripping
        g.setPaint(new GradientPaint(0, 60, new Color(96, 96, 90), 0, 70, new Color(38, 40, 38)));
        g.fillRect(0, 60, W, 9);
        g.setColor(new Color(28, 30, 28));
        for (int x = 40; x < W; x += 130) g.fillRect(x, 58, 9, 13);
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 7; i++) {
            double dx = 10 + r.nextInt(W - 20), len = 14 + r.nextInt(60);
            g.setColor(new Color(60, 70, 24, 120));
            g.draw(new Line2D.Double(dx, 69, dx + r.nextInt(3) - 1, 69 + len));
        }

        // observation window into a dark room
        g.setColor(new Color(58, 64, 66));
        g.fillRect(222, 72, 356, 116);
        g.setColor(new Color(16, 18, 18));
        g.setStroke(new BasicStroke(2));
        g.drawRect(222, 72, 356, 116);
        g.setColor(new Color(110, 114, 112));
        for (int x = 228; x < 576; x += 28) { g.fill(el(x, 75, 3, 3)); g.fill(el(x, 182, 3, 3)); }
        g.setPaint(new GradientPaint(0, 80, new Color(14, 30, 28), 0, 178, new Color(5, 12, 12)));
        g.fillRect(232, 80, 336, 98);
        Shape before = g.getClip();
        g.clipRect(232, 80, 336, 98);
        g.setColor(new Color(40, 80, 64, 40)); // shapes of old tanks in the dark
        g.fill(new RoundRectangle2D.Double(258, 96, 46, 90, 20, 20));
        g.fill(new RoundRectangle2D.Double(498, 92, 50, 94, 20, 20));
        g.fill(new Rectangle2D.Double(330, 150, 140, 30));
        for (int i = 0; i < 9; i++) { // slow bubbles
            double bx = 270 + (i * 37) % 280, by = 178 - ((battle.ticks * 0.35 + i * 29) % 98);
            g.setColor(new Color(120, 220, 170, 50));
            g.draw(el(bx, by, 5, 5));
        }
        g.setColor(new Color(255, 255, 255, 16)); // reflection on the glass
        Path2D shine = new Path2D.Double();
        shine.moveTo(300, 80); shine.lineTo(340, 80); shine.lineTo(280, 178); shine.lineTo(240, 178); shine.closePath();
        g.fill(shine);
        g.setColor(new Color(210, 230, 220, 90)); // spiderweb crack
        g.setStroke(new BasicStroke(1));
        double ccx = 540, ccy = 104;
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4 + 0.3, len = 18 + r.nextInt(22);
            g.draw(new Line2D.Double(ccx, ccy, ccx + Math.cos(a) * len, ccy + Math.sin(a) * len));
        }
        g.draw(el(ccx - 7, ccy - 6, 14, 12));
        g.draw(el(ccx - 15, ccy - 13, 30, 26));
        g.setClip(before);
        g.setFont(new Font(MONO, Font.BOLD, 9));
        g.setColor(new Color(190, 170, 60, 170));
        g.drawString("OBSERVATION B-2", 236, 186);

        // warning sign, hanging crooked
        Graphics2D ws = (Graphics2D) g.create();
        ws.rotate(Math.toRadians(7), 66, 104);
        Path2D tri = new Path2D.Double();
        tri.moveTo(66, 82); tri.lineTo(91, 124); tri.lineTo(41, 124); tri.closePath();
        ws.setColor(new Color(196, 166, 40));
        ws.fill(tri);
        ws.setColor(new Color(20, 18, 10));
        ws.setStroke(new BasicStroke(3));
        ws.draw(tri);
        ws.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        ws.drawString("!", 61, 120);
        ws.dispose();

        // shelf of specimen jars
        g.setColor(new Color(70, 72, 70));
        g.fillRect(8, 166, 124, 5);
        int[][] jars = { { 16, 34 }, { 50, 26 }, { 82, 40 } };
        for (int j = 0; j < jars.length; j++) {
            int jx = jars[j][0], jh = jars[j][1], jy = 166 - jh;
            g.setColor(new Color(80, 140, 70, 170));
            g.fillRect(jx + 2, jy + jh / 3, 24, jh - jh / 3);
            g.setColor(new Color(20, 30, 18, 200));
            if (j == 1) { // an eye floating in this one
                g.setColor(new Color(220, 214, 196));
                g.fill(el(jx + 8, jy + 12, 12, 12));
                g.setColor(new Color(40, 20, 20));
                g.fill(el(jx + 12 + Math.sin(battle.ticks / 50.0) * 2, jy + 16, 5, 5));
            } else g.fill(el(jx + 6, jy + jh / 2.0, 14, 10));
            g.setColor(new Color(170, 200, 180, 50));
            g.fillRect(jx, jy, 28, jh);
            g.setColor(new Color(180, 200, 190, 140));
            g.drawRect(jx, jy, 28, jh);
            g.setColor(new Color(90, 90, 86));
            g.fillRect(jx - 1, jy - 5, 30, 6);
        }

        // smeared handprint
        g.setColor(new Color(96, 10, 12, 150));
        g.fill(el(588, 104, 16, 20));
        for (int f = 0; f < 4; f++) g.fill(new RoundRectangle2D.Double(585 + f * 5, 86 + Math.abs(f - 1.5) * 3, 4, 20, 4, 4));
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int f = 0; f < 3; f++) g.draw(new Line2D.Double(590 + f * 5, 122, 588 + f * 6, 150 + f * 8));

        // specimen tank with something curled up inside
        g.setPaint(new RadialGradientPaint(new Point2D.Double(732, 150), 110f, new float[] { 0f, 1f },
                new Color[] { new Color(80, 200, 120, 60), new Color(80, 200, 120, 0) }));
        g.fill(el(622, 40, 220, 220));
        g.setColor(new Color(44, 46, 44));
        g.fillRect(712, TOP_H, 8, 18); g.fillRect(744, TOP_H, 8, 18);
        g.setPaint(new GradientPaint(0, 96, new Color(70, 150, 96, 190), 0, 222, new Color(24, 70, 44, 220)));
        g.fillRect(688, 96, 88, 126);
        double bob = Math.sin(battle.ticks / 40.0) * 3;
        g.setColor(new Color(16, 34, 24, 210));
        g.fill(el(708, 132 + bob, 44, 66));
        g.fill(el(716, 110 + bob, 28, 28));
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new QuadCurve2D.Double(712, 150 + bob, 698, 170 + bob, 712, 190 + bob));
        for (int i = 0; i < 7; i++) {
            double bx = 694 + (i * 23) % 76, by = 218 - ((battle.ticks * 0.6 + i * 31) % 120);
            g.setColor(new Color(190, 255, 210, 110));
            g.fill(el(bx, by, 4, 4));
        }
        g.setColor(new Color(255, 255, 255, 40));
        g.fillRect(694, 92, 6, 128);
        g.setColor(new Color(160, 200, 170, 150));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(688, 88, 88, 134);
        g.setPaint(new GradientPaint(0, 74, new Color(110, 112, 108), 0, 88, new Color(50, 52, 50)));
        g.fillRect(682, 74, 100, 14);
        g.setPaint(new GradientPaint(0, 222, new Color(100, 102, 98), 0, FLOOR_TOP, new Color(40, 42, 40)));
        g.fillRect(678, 222, 108, FLOOR_TOP - 222);

        // steel counter and cabinets
        g.setPaint(new GradientPaint(0, 192, new Color(112, 116, 114), 0, 200, new Color(58, 60, 58)));
        g.fillRect(0, 192, 678, 8);
        g.setPaint(new GradientPaint(0, 200, new Color(50, 54, 54), 0, FLOOR_TOP, new Color(26, 28, 28)));
        g.fillRect(0, 200, 678, FLOOR_TOP - 200);
        g.setColor(new Color(14, 16, 16));
        g.setStroke(new BasicStroke(1.5f));
        for (int x = 0; x < 678; x += 84) {
            g.drawRect(x + 4, 204, 76, FLOOR_TOP - 208);
            g.fillRect(x + 36, 214, 14, 3); // handle
        }
        g.setColor(new Color(6, 8, 8));
        g.fillRect(344, 205, 74, FLOOR_TOP - 210); // a door hanging open
        g.setColor(new Color(60, 64, 64));
        g.fill(new Polygon(new int[] { 418, 438, 438, 418 }, new int[] { 205, 200, FLOOR_TOP - 2, FLOOR_TOP - 6 }, 4));
        // glassware on the counter, one flask tipped over and spilling
        g.setColor(new Color(170, 200, 190, 110));
        g.fillRect(250, 176, 12, 16);
        g.fillRect(268, 182, 8, 10);
        Graphics2D fl = (Graphics2D) g.create();
        fl.rotate(Math.toRadians(-80), 556, 188);
        Path2D flask = new Path2D.Double();
        flask.moveTo(552, 172); flask.lineTo(560, 172); flask.lineTo(560, 180); flask.lineTo(570, 194); flask.lineTo(542, 194); flask.lineTo(552, 180); flask.closePath();
        fl.setColor(new Color(170, 200, 190, 120));
        fl.fill(flask);
        fl.dispose();
        g.setColor(new Color(90, 200, 110, 110));
        g.fill(el(520, 189, 46, 5));
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(528, 196, 528, 214));

        // cold tiled floor in perspective
        g.setColor(new Color(30, 34, 34));
        g.fillRect(0, FLOOR_TOP, W, STAGE_BOTTOM - FLOOR_TOP);
        double vpx = 400, vpy = 120;
        double[] rows = { FLOOR_TOP, 251, 260, 272, STAGE_BOTTOM };
        for (int ri = 0; ri < rows.length - 1; ri++) {
            for (int ci = -10; ci < 20; ci++) {
                double y0 = rows[ri], y1 = rows[ri + 1], xb0 = ci * 64 - 120, xb1 = xb0 + 64;
                Path2D q = new Path2D.Double();
                q.moveTo(px(vpx, vpy, xb0, y0), y0); q.lineTo(px(vpx, vpy, xb1, y0), y0);
                q.lineTo(px(vpx, vpy, xb1, y1), y1); q.lineTo(px(vpx, vpy, xb0, y1), y1); q.closePath();
                g.setColor(Math.floorMod(ri + ci, 2) == 0 ? new Color(60, 66, 66) : new Color(46, 50, 50));
                g.fill(q);
                g.setColor(new Color(20, 22, 22));
                g.setStroke(new BasicStroke(1));
                g.draw(q);
            }
        }
        g.setPaint(new GradientPaint(0, FLOOR_TOP, new Color(0, 0, 0, 170), 0, STAGE_BOTTOM, new Color(0, 0, 0, 20)));
        g.fillRect(0, FLOOR_TOP, W, STAGE_BOTTOM - FLOOR_TOP);
        g.setColor(new Color(10, 12, 12)); // floor drain
        g.fill(el(386, 266, 40, 12));
        g.setColor(new Color(70, 74, 72));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(el(386, 266, 40, 12));
        for (int i = 1; i < 5; i++) g.drawLine(386 + i * 8, 267, 386 + i * 8, 277);
        g.setColor(new Color(70, 180, 100, 60)); // glowing puddle under the tank
        g.fill(el(640, 266, 150, 22));
        g.setColor(new Color(60, 8, 10, 110)); // a dark stain where the monster stands
        g.fill(el(560, 268, 170, 20));
        g.setColor(new Color(14, 16, 16));
        g.fillRect(0, FLOOR_TOP - 2, W, 4);
        for (int i = 0; i < 6; i++) { // scattered papers
            double sx = 20 + r.nextInt(W - 40), sy = FLOOR_TOP + 10 + r.nextInt(36);
            Shape paper = AffineTransform.getRotateInstance(r.nextDouble() * 2 - 1, sx, sy)
                    .createTransformedShape(new Rectangle2D.Double(sx - 9, sy - 6, 18, 12));
            g.setColor(new Color(160, 160, 146, 160));
            g.fill(paper);
        }
        g.setClip(oldClip);
    }
}
