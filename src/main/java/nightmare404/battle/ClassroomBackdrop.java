package nightmare404.battle;

import static nightmare404.battle.BattleScreen.BattlePanel.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import nightmare404.battle.BattleScreen.BattlePanel;

// A battle background, from the Fourth_Floor screens (Room 1, Room 2, Brute) in the Battle_Screens folder.
// It's the original drawRoom code; the battle calls draw() behind the fighters (see BattlePanel.drawRoom).
public class ClassroomBackdrop implements Backdrop {
    /** The abandoned classroom: stained walls, chalkboard, stopped clock, broken window, filthy floor. */
    public void draw(Graphics2D g, BattlePanel battle) {
        Shape oldClip = g.getClip();
        g.clipRect(0, TOP_H, W, STAGE_BOTTOM - TOP_H);
        Random r = new Random(204); // same decoration every frame

        // wall
        g.setPaint(new GradientPaint(0, TOP_H, new Color(50, 60, 52), 0, FLOOR_TOP, new Color(29, 36, 31)));
        g.fillRect(0, TOP_H, W, FLOOR_TOP - TOP_H);
        g.setColor(new Color(0, 0, 0, 24));
        for (int x = 0; x < W; x += 22) g.fillRect(x, TOP_H, 9, FLOOR_TOP - TOP_H);
        for (int i = 0; i < 6; i++) { // water stains
            double sx = r.nextInt(W), sy = TOP_H + 20 + r.nextInt(120), sr = 30 + r.nextInt(50);
            g.setPaint(new RadialGradientPaint(new Point2D.Double(sx, sy), (float) sr, new float[] { 0f, 0.75f, 1f },
                    new Color[] { new Color(78, 64, 36, 55), new Color(66, 52, 30, 35), new Color(66, 52, 30, 0) }));
            g.fill(el(sx - sr, sy - sr, sr * 2, sr * 2));
        }
        g.setStroke(new BasicStroke(1.5f));
        for (int i = 0; i < 5; i++) { // peeling paint
            double px = 20 + r.nextInt(W - 80), py = TOP_H + 14 + r.nextInt(110);
            Path2D p = new Path2D.Double();
            int n = 8;
            for (int k = 0; k < n; k++) {
                double a = k * 2 * Math.PI / n, rr = 8 + r.nextInt(16);
                if (k == 0) p.moveTo(px + Math.cos(a) * rr * 1.5, py + Math.sin(a) * rr); else p.lineTo(px + Math.cos(a) * rr * 1.5, py + Math.sin(a) * rr);
            }
            p.closePath();
            g.setColor(new Color(76, 86, 72));
            g.fill(p);
            g.setColor(new Color(16, 20, 16, 170));
            g.draw(p);
        }
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 10; i++) { // drips from the ceiling
            double dx = 10 + r.nextInt(W - 20), len = 16 + r.nextInt(80);
            g.setColor(new Color(26, 18, 12, 120));
            g.draw(new Line2D.Double(dx, TOP_H, dx + r.nextInt(5) - 2, TOP_H + len));
            g.fill(el(dx - 2.5, TOP_H + len - 3, 5, 7));
        }

        // wooden wainscot
        g.setPaint(new GradientPaint(0, 196, new Color(46, 36, 28), 0, FLOOR_TOP, new Color(26, 20, 16)));
        g.fillRect(0, 196, W, FLOOR_TOP - 196);
        g.setColor(new Color(14, 10, 8, 160));
        g.setStroke(new BasicStroke(1.5f));
        for (int x = 8; x < W; x += 34) g.drawLine(x, 200, x, FLOOR_TOP);
        g.setColor(new Color(72, 58, 44));
        g.fillRect(0, 193, W, 5);

        // chalkboard
        g.setColor(new Color(54, 40, 30));
        g.fillRect(226, 70, 348, 112);
        g.setPaint(new GradientPaint(0, 78, new Color(31, 45, 39), 0, 174, new Color(19, 29, 25)));
        g.fillRect(234, 78, 332, 96);
        for (int i = 0; i < 8; i++) {
            g.setColor(new Color(170, 180, 170, 16));
            g.fill(el(238 + r.nextInt(270), 80 + r.nextInt(70), 40 + r.nextInt(70), 12 + r.nextInt(18)));
        }
        chalk(g, "DON'T LOOK BACK", 242, 93, -3, 11, 95);
        chalk(g, "WHO LET YOU IN?", 448, 168, 2, 11, 85);
        chalk(g, "stay after class", 246, 168, -1, 10, 60);
        g.setColor(new Color(214, 220, 208, 80));
        g.setStroke(new BasicStroke(1.6f));
        for (int i = 0; i < 4; i++) g.drawLine(526 + i * 6, 84, 524 + i * 6, 100);
        g.drawLine(522, 98, 548, 86);
        g.setColor(new Color(40, 30, 22));
        g.fillRect(230, 176, 340, 6);
        g.setColor(new Color(220, 220, 208));
        g.fillRect(470, 173, 14, 4);

        // clock stopped at 3:33 - the second hand twitches back and forth
        double cx = 66, cy = 106;
        g.setColor(new Color(22, 22, 20));
        g.fill(el(cx - 25, cy - 25, 50, 50));
        g.setPaint(new GradientPaint(0, (float) (cy - 21), new Color(186, 178, 150), 0, (float) (cy + 21), new Color(132, 124, 100)));
        g.fill(el(cx - 21, cy - 21, 42, 42));
        g.setColor(new Color(40, 34, 28));
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            g.draw(new Line2D.Double(cx + Math.cos(a) * 16, cy + Math.sin(a) * 16, cx + Math.cos(a) * 19, cy + Math.sin(a) * 19));
        }
        g.setColor(new Color(60, 52, 40, 160));
        g.setStroke(new BasicStroke(1));
        g.draw(new Line2D.Double(cx - 14, cy - 12, cx - 2, cy + 4));
        g.draw(new Line2D.Double(cx - 2, cy + 4, cx + 6, cy + 18));
        g.setColor(new Color(24, 20, 18));
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double hour = (3 + 33 / 60.0) / 12 * 2 * Math.PI - Math.PI / 2, min = 33 / 60.0 * 2 * Math.PI - Math.PI / 2;
        g.draw(new Line2D.Double(cx, cy, cx + Math.cos(hour) * 10, cy + Math.sin(hour) * 10));
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx, cy, cx + Math.cos(min) * 16, cy + Math.sin(min) * 16));
        double sec = ((battle.ticks / 50) % 2 == 0 ? 40 : 41) / 60.0 * 2 * Math.PI - Math.PI / 2;
        g.setColor(new Color(150, 20, 20));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new Line2D.Double(cx, cy, cx + Math.cos(sec) * 18, cy + Math.sin(sec) * 18));

        // broken window with moonlight
        g.setColor(new Color(18, 16, 14));
        g.fillRect(688, 70, 88, 120);
        g.setPaint(new GradientPaint(0, 76, new Color(28, 36, 56), 0, 184, new Color(10, 14, 24)));
        g.fillRect(694, 76, 76, 108);
        g.setColor(new Color(222, 224, 204));
        g.fill(el(742, 86, 18, 18));
        g.setColor(new Color(28, 36, 56));
        g.fill(el(737, 84, 16, 16)); // crescent
        g.setColor(new Color(4, 4, 6));
        Path2D hole = new Path2D.Double();
        hole.moveTo(694, 150); hole.lineTo(708, 140); hole.lineTo(714, 156); hole.lineTo(726, 146);
        hole.lineTo(730, 184); hole.lineTo(694, 184); hole.closePath();
        g.fill(hole);
        g.setColor(new Color(170, 180, 200, 120));
        g.setStroke(new BasicStroke(1));
        g.draw(new Line2D.Double(708, 140, 718, 128)); g.draw(new Line2D.Double(714, 156, 728, 132)); g.draw(new Line2D.Double(726, 146, 732, 156));
        g.setColor(new Color(18, 16, 14));
        g.fillRect(730, 76, 4, 108);
        g.fillRect(694, 128, 76, 4);

        // checkered linoleum floor in perspective
        g.setColor(new Color(30, 32, 30));
        g.fillRect(0, FLOOR_TOP, W, STAGE_BOTTOM - FLOOR_TOP);
        double vpx = 400, vpy = 120;
        double[] rows = { FLOOR_TOP, 251, 260, 272, STAGE_BOTTOM };
        for (int ri = 0; ri < rows.length - 1; ri++) {
            for (int ci = -10; ci < 20; ci++) {
                double y0 = rows[ri], y1 = rows[ri + 1], xb0 = ci * 64 - 120, xb1 = xb0 + 64;
                Path2D q = new Path2D.Double();
                q.moveTo(px(vpx, vpy, xb0, y0), y0); q.lineTo(px(vpx, vpy, xb1, y0), y0);
                q.lineTo(px(vpx, vpy, xb1, y1), y1); q.lineTo(px(vpx, vpy, xb0, y1), y1); q.closePath();
                g.setColor(Math.floorMod(ri + ci, 2) == 0 ? new Color(58, 60, 54) : new Color(34, 36, 33));
                g.fill(q);
            }
        }
        g.setPaint(new GradientPaint(0, FLOOR_TOP, new Color(0, 0, 0, 170), 0, STAGE_BOTTOM, new Color(0, 0, 0, 20)));
        g.fillRect(0, FLOOR_TOP, W, STAGE_BOTTOM - FLOOR_TOP);
        g.setColor(new Color(60, 8, 10, 110)); // a dark stain where the monster stands
        g.fill(el(560, 268, 170, 20));
        g.setColor(new Color(14, 12, 10));
        g.fillRect(0, FLOOR_TOP - 2, W, 4);
        for (int i = 0; i < 7; i++) { // scattered papers
            double sx = 20 + r.nextInt(W - 40), sy = FLOOR_TOP + 10 + r.nextInt(36);
            Shape paper = AffineTransform.getRotateInstance(r.nextDouble() * 2 - 1, sx, sy)
                    .createTransformedShape(new Rectangle2D.Double(sx - 9, sy - 6, 18, 12));
            g.setColor(new Color(150, 146, 128, 170));
            g.fill(paper);
        }
        // moonlight through the window
        Path2D beam = new Path2D.Double();
        beam.moveTo(694, 184); beam.lineTo(770, 184); beam.lineTo(700, STAGE_BOTTOM); beam.lineTo(560, STAGE_BOTTOM); beam.closePath();
        g.setPaint(new GradientPaint(0, 184, new Color(160, 180, 220, 40), 0, STAGE_BOTTOM, new Color(160, 180, 220, 6)));
        g.fill(beam);
        g.setClip(oldClip);
    }
}
