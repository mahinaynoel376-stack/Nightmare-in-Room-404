package nightmare404.battle;

import static nightmare404.battle.BattleScreen.BattlePanel.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import nightmare404.battle.BattleScreen.BattlePanel;

// A battle background, from the Library screens (Lib 1, Lib 2, Whisperer) in the Battle_Screens folder.
// It's the original drawRoom code; the battle calls draw() behind the fighters (see BattlePanel.drawRoom).
public class LibraryBackdrop implements Backdrop {
    /** The abandoned library: towering shelves, an arched window, guttering candles, a worn rug, fallen books. */
    public void draw(Graphics2D g, BattlePanel battle) {
        Shape oldClip = g.getClip();
        g.clipRect(0, TOP_H, W, STAGE_BOTTOM - TOP_H);
        Random r = new Random(1408); // same decoration every frame
        Color[] spines = { new Color(86, 24, 22), new Color(40, 56, 38), new Color(30, 36, 58), new Color(84, 62, 34),
                new Color(58, 40, 30), new Color(70, 66, 56), new Color(44, 24, 40), new Color(28, 44, 44) };

        // dark wood panelling
        g.setPaint(new GradientPaint(0, TOP_H, new Color(44, 30, 22), 0, FLOOR_TOP, new Color(20, 13, 10)));
        g.fillRect(0, TOP_H, W, FLOOR_TOP - TOP_H);
        g.setColor(new Color(12, 8, 6, 130));
        g.setStroke(new BasicStroke(1.5f));
        for (int x = 236; x < 564; x += 41) g.drawLine(x, TOP_H, x, FLOOR_TOP);

        // tall arched window with dead branches outside
        Path2D arch = new Path2D.Double();
        arch.moveTo(256, 188);
        arch.lineTo(256, 118);
        arch.append(new Arc2D.Double(256, 70, 288, 96, 180, -180, Arc2D.OPEN), true);
        arch.lineTo(544, 188);
        arch.closePath();
        g.setPaint(new GradientPaint(0, 70, new Color(22, 28, 46), 0, 188, new Color(8, 10, 18)));
        g.fill(arch);
        Shape before = g.getClip();
        g.clip(arch);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(470, 102), 90f, new float[] { 0f, 1f },
                new Color[] { new Color(170, 180, 200, 70), new Color(170, 180, 200, 0) }));
        g.fill(el(380, 12, 180, 180));
        g.setColor(new Color(214, 216, 196));
        g.fill(el(458, 90, 24, 24));
        g.setColor(new Color(6, 6, 10));
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new QuadCurve2D.Double(548, 96, 490, 112, 440, 100));
        g.draw(new QuadCurve2D.Double(500, 106, 480, 86, 486, 70));
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new QuadCurve2D.Double(440, 100, 410, 90, 396, 104));
        g.draw(new QuadCurve2D.Double(462, 104, 456, 120, 440, 126));
        g.draw(new Line2D.Double(486, 70, 476, 76));
        g.draw(new QuadCurve2D.Double(548, 150, 520, 156, 506, 172));
        g.setColor(new Color(40, 28, 20)); // window bars
        g.fillRect(346, 70, 5, 118); g.fillRect(449, 70, 5, 118);
        g.fillRect(256, 126, 288, 5);
        g.setClip(before);
        g.setColor(new Color(58, 40, 28));
        g.setStroke(new BasicStroke(9));
        g.draw(arch);
        g.setColor(new Color(24, 16, 12));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(arch);
        g.setColor(new Color(64, 46, 32));
        g.fillRect(246, 186, 308, 7); // sill

        // bookshelves on both sides
        drawShelf(g, r, spines, 0, 238);
        drawShelf(g, r, spines, 562, 800);

        // ladder leaning on the left shelf
        g.setColor(new Color(76, 54, 36));
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(22, FLOOR_TOP + 6, 50, 78));
        g.draw(new Line2D.Double(56, FLOOR_TOP + 6, 84, 78));
        g.setStroke(new BasicStroke(3));
        for (int i = 1; i < 8; i++) {
            double t = i / 8.0;
            g.draw(new Line2D.Double(22 + 28 * t, FLOOR_TOP + 6 - (FLOOR_TOP - 72) * t, 56 + 28 * t, FLOOR_TOP + 6 - (FLOOR_TOP - 72) * t));
        }

        // cobwebs in the corners
        cobweb(g, 0, TOP_H, 1, 1, 60);
        cobweb(g, W, TOP_H, -1, 1, 70);
        cobweb(g, 238, TOP_H + 4, 1, 1, 34);
        cobweb(g, 562, TOP_H + 4, -1, 1, 34);

        // candles on the window sill
        for (int cx : new int[] { 272, 528 }) {
            double flick = Math.sin(battle.ticks / 4.0 + cx) * 1.5;
            g.setPaint(new RadialGradientPaint(new Point2D.Double(cx, 158), 46f, new float[] { 0f, 1f },
                    new Color[] { new Color(255, 170, 70, (int) (60 + 10 * flick)), new Color(255, 170, 70, 0) }));
            g.fill(el(cx - 46, 112, 92, 92));
            g.setColor(new Color(208, 196, 164));
            g.fillRect(cx - 4, 166, 8, 20);
            g.fill(el(cx - 6, 182, 4, 6)); // drip
            g.setColor(new Color(255, 200, 90));
            g.fill(el(cx - 3, 154 - flick, 6, 12 + flick));
            g.setColor(new Color(255, 244, 200));
            g.fill(el(cx - 1.5, 159 - flick / 2, 3, 6));
        }

        // wooden floor in perspective
        g.setColor(new Color(26, 18, 12));
        g.fillRect(0, FLOOR_TOP, W, STAGE_BOTTOM - FLOOR_TOP);
        double vpx = 400, vpy = 120;
        for (int ci = -16; ci < 30; ci++) {
            double xb0 = ci * 36 - 120, xb1 = xb0 + 36;
            Path2D plank = new Path2D.Double();
            plank.moveTo(px(vpx, vpy, xb0, FLOOR_TOP), FLOOR_TOP); plank.lineTo(px(vpx, vpy, xb1, FLOOR_TOP), FLOOR_TOP);
            plank.lineTo(px(vpx, vpy, xb1, STAGE_BOTTOM), STAGE_BOTTOM); plank.lineTo(px(vpx, vpy, xb0, STAGE_BOTTOM), STAGE_BOTTOM); plank.closePath();
            int shade = r.nextInt(3);
            g.setColor(shade == 0 ? new Color(50, 34, 22) : shade == 1 ? new Color(42, 28, 18) : new Color(56, 38, 24));
            g.fill(plank);
            g.setColor(new Color(16, 10, 6));
            g.setStroke(new BasicStroke(1));
            g.draw(plank);
        }
        // worn rug
        Path2D rug = new Path2D.Double();
        rug.moveTo(250, 252); rug.lineTo(550, 252); rug.lineTo(610, 286); rug.lineTo(190, 286); rug.closePath();
        g.setColor(new Color(70, 18, 20));
        g.fill(rug);
        Path2D inner = new Path2D.Double();
        inner.moveTo(266, 256); inner.lineTo(534, 256); inner.lineTo(584, 282); inner.lineTo(216, 282); inner.closePath();
        g.setColor(new Color(112, 64, 32, 160));
        g.setStroke(new BasicStroke(2));
        g.draw(inner);
        g.setColor(new Color(30, 10, 10, 160));
        g.fill(el(330, 262, 34, 8)); g.fill(el(470, 272, 26, 7));
        g.setPaint(new GradientPaint(0, FLOOR_TOP, new Color(0, 0, 0, 170), 0, STAGE_BOTTOM, new Color(0, 0, 0, 20)));
        g.fillRect(0, FLOOR_TOP, W, STAGE_BOTTOM - FLOOR_TOP);
        g.setColor(new Color(60, 8, 10, 110)); // a dark stain where the monster stands
        g.fill(el(560, 268, 170, 20));
        g.setColor(new Color(14, 9, 6));
        g.fillRect(0, FLOOR_TOP - 2, W, 4);
        for (int i = 0; i < 6; i++) { // fallen books
            double sx = 30 + r.nextInt(W - 60), sy = FLOOR_TOP + 14 + r.nextInt(30);
            Shape book = AffineTransform.getRotateInstance(r.nextDouble() * 2 - 1, sx, sy)
                    .createTransformedShape(new Rectangle2D.Double(sx - 12, sy - 7, 24, 14));
            g.setColor(spines[r.nextInt(spines.length)]);
            g.fill(book);
            g.setColor(new Color(10, 6, 4));
            g.draw(book);
        }
        for (int i = 0; i < 7; i++) { // loose pages
            double sx = 20 + r.nextInt(W - 40), sy = FLOOR_TOP + 10 + r.nextInt(36);
            Shape paper = AffineTransform.getRotateInstance(r.nextDouble() * 2 - 1, sx, sy)
                    .createTransformedShape(new Rectangle2D.Double(sx - 8, sy - 5, 16, 10));
            g.setColor(new Color(170, 160, 136, 170));
            g.fill(paper);
        }
        // moonlight through the window
        Path2D beam = new Path2D.Double();
        beam.moveTo(300, 188); beam.lineTo(500, 188); beam.lineTo(540, STAGE_BOTTOM); beam.lineTo(330, STAGE_BOTTOM); beam.closePath();
        g.setPaint(new GradientPaint(0, 188, new Color(160, 180, 220, 30), 0, STAGE_BOTTOM, new Color(160, 180, 220, 5)));
        g.fill(beam);
        g.setClip(oldClip);
    }

    /** One tall bookshelf from x0 to x1, with uneven rows of books, gaps and a few leaning ones. */
    static void drawShelf(Graphics2D g, Random r, Color[] spines, int x0, int x1) {
        g.setColor(new Color(36, 24, 16));
        g.fillRect(x0, TOP_H, x1 - x0, FLOOR_TOP - TOP_H);
        for (int row = 0; row < 4; row++) {
            int top = TOP_H + 12 + row * 46, bottom = top + 40;
            int x = x0 + 8;
            while (x < x1 - 14) {
                int w = 6 + r.nextInt(9), h = 24 + r.nextInt(14);
                if (r.nextInt(9) == 0) { x += 10 + r.nextInt(16); continue; } // gap where books are missing
                Color c = spines[r.nextInt(spines.length)];
                Shape book = new Rectangle2D.Double(x, bottom - h, w, h);
                if (r.nextInt(14) == 0) book = AffineTransform.getRotateInstance(Math.toRadians(-14), x, bottom).createTransformedShape(book);
                g.setColor(c);
                g.fill(book);
                g.setColor(new Color(10, 6, 4));
                g.setStroke(new BasicStroke(1));
                g.draw(book);
                if (r.nextInt(3) == 0) { g.setColor(new Color(150, 120, 60, 90)); g.fillRect(x + 1, bottom - h + 5, w - 2, 2); }
                x += w + 1;
            }
            g.setPaint(new GradientPaint(0, bottom, new Color(78, 54, 34), 0, bottom + 6, new Color(40, 26, 16)));
            g.fillRect(x0, bottom, x1 - x0, 6);
        }
        g.setColor(new Color(58, 40, 26));
        g.fillRect(x0, TOP_H, x1 - x0, 6);
        g.fillRect(x0, TOP_H, 6, FLOOR_TOP - TOP_H);
        g.fillRect(x1 - 6, TOP_H, 6, FLOOR_TOP - TOP_H);
        g.setPaint(new GradientPaint(0, TOP_H, new Color(0, 0, 0, 40), 0, FLOOR_TOP, new Color(0, 0, 0, 150)));
        g.fillRect(x0, TOP_H, x1 - x0, FLOOR_TOP - TOP_H);
    }

    static void cobweb(Graphics2D g, double cx, double cy, int dx, int dy, double size) {
        g.setColor(new Color(200, 200, 190, 55));
        g.setStroke(new BasicStroke(1));
        for (int k = 0; k <= 4; k++) {
            double a = Math.toRadians(k * 22.5);
            g.draw(new Line2D.Double(cx, cy, cx + dx * Math.cos(a) * size, cy + dy * Math.sin(a) * size));
        }
        for (int ring = 1; ring <= 3; ring++) {
            double rr = size * ring / 3.6;
            Path2D p = new Path2D.Double();
            for (int k = 0; k <= 4; k++) {
                double a = Math.toRadians(k * 22.5);
                double x = cx + dx * Math.cos(a) * rr, y = cy + dy * Math.sin(a) * rr;
                if (k == 0) p.moveTo(x, y); else p.quadTo(cx + dx * Math.cos(a - 0.2) * rr * 0.88, cy + dy * Math.sin(a - 0.2) * rr * 0.88, x, y);
            }
            g.draw(p);
        }
    }
}
