package nightmare404.battle;

import static nightmare404.battle.BattleScreen.BattlePanel.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import nightmare404.battle.BattleScreen.BattlePanel;

// A battle background, from Lobby/StaffRoom in the Battle_Screens folder.
// It's the original drawRoom code; the battle calls draw() behind the fighters (see BattlePanel.drawRoom).
public class StaffRoomBackdrop implements Backdrop {

    public void draw(Graphics2D g, BattlePanel battle) {
        Shape oldClip = g.getClip();
        g.clipRect(0, TOP_H, W, STAGE_BOTTOM - TOP_H);
        Random r = new Random(1977); // same decoration every frame

        // sickly yellow walls
        g.setPaint(new GradientPaint(0, TOP_H, new Color(92, 84, 56), 0, 190, new Color(50, 46, 30)));
        g.fillRect(0, TOP_H, W, 190 - TOP_H);
        for (int i = 0; i < 7; i++) { // stains
            double sx = r.nextInt(W), sy = TOP_H + 20 + r.nextInt(100), sr = 30 + r.nextInt(50);
            g.setPaint(new RadialGradientPaint(new Point2D.Double(sx, sy), (float) sr, new float[] { 0f, 1f },
                    new Color[] { new Color(60, 44, 20, 70), new Color(60, 44, 20, 0) }));
            g.fill(el(sx - sr, sy - sr, sr * 2, sr * 2));
        }
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 8; i++) { // drips
            double dx = 10 + r.nextInt(W - 20), len = 14 + r.nextInt(70);
            g.setColor(new Color(40, 30, 14, 120));
            g.draw(new Line2D.Double(dx, TOP_H, dx + r.nextInt(5) - 2, TOP_H + len));
        }
        g.setPaint(new GradientPaint(0, 186, new Color(56, 62, 50), 0, FLOOR_TOP, new Color(30, 34, 28)));
        g.fillRect(0, 186, W, FLOOR_TOP - 186);                                      // painted lower wall
        g.setColor(new Color(90, 84, 60));
        g.fillRect(0, 184, W, 4);

        // ---- big window, rain running down it
        g.setColor(new Color(40, 38, 32));
        g.fillRect(232, 70, 336, 118);
        g.setPaint(new GradientPaint(0, 76, new Color(18, 24, 34), 0, 182, new Color(8, 12, 18)));
        g.fillRect(240, 76, 320, 106);
        Shape before = g.getClip();
        g.clipRect(240, 76, 320, 106);
        g.setColor(new Color(4, 6, 10));                                             // dead tree outside
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new QuadCurve2D.Double(530, 182, 520, 130, 500, 100));
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new QuadCurve2D.Double(512, 120, 480, 110, 466, 92)); g.draw(new QuadCurve2D.Double(504, 104, 520, 88, 540, 84));
        g.setStroke(new BasicStroke(1));
        for (int k = 0; k < 40; k++) {                                               // rain, falling
            double rx = 240 + (k * 53) % 320, ry = 76 + ((battle.ticks * (3 + k % 3) + k * 37) % 130) - 12;
            g.setColor(new Color(150, 170, 200, 60 + (k % 3) * 20));
            g.draw(new Line2D.Double(rx, ry, rx - 2, ry + 8));
        }
        for (int k = 0; k < 7; k++) {                                                // drops sliding down the glass
            double rx = 250 + k * 44 + (k % 2) * 9, ry = 80 + ((battle.ticks * 0.3 + k * 19) % 100);
            g.setColor(new Color(170, 190, 220, 90));
            g.fill(el(rx, ry, 3, 4));
            g.draw(new Line2D.Double(rx + 1.5, ry, rx + 1.5, ry - 10));
        }
        g.setClip(before);
        g.setColor(new Color(40, 38, 32));
        g.fillRect(398, 76, 4, 106); g.fillRect(240, 127, 320, 4);                   // window frame

        // ---- long table with abandoned mugs and unmarked papers, chairs pulled out
        for (int cx : new int[] { 290, 360, 440, 510 }) {                            // chair backs behind the table
            g.setColor(new Color(42, 48, 58));
            g.fillRect(cx - 14, 166, 28, 28);
            g.setColor(new Color(26, 30, 36));
            g.drawRect(cx - 14, 166, 28, 28);
        }
        g.setPaint(new GradientPaint(0, 192, new Color(120, 100, 70), 0, 200, new Color(82, 66, 44)));
        g.fillRect(244, 192, 312, 8);                                                // table top
        g.setColor(new Color(40, 32, 22));
        g.fillRect(256, 200, 8, FLOOR_TOP - 200); g.fillRect(536, 200, 8, FLOOR_TOP - 200);
        int[][] mugs = { { 268, 0 }, { 318, 1 }, { 372, 2 }, { 466, 0 }, { 520, 1 } };
        Color[] mugCol = { new Color(170, 60, 50), new Color(200, 196, 180), new Color(60, 90, 140) };
        for (int[] m : mugs) {
            g.setColor(mugCol[m[1]]); g.fillRect(m[0], 182, 10, 10);
            g.setColor(new Color(30, 22, 16)); g.drawRect(m[0], 182, 10, 10);
            g.draw(new Arc2D.Double(m[0] + 8, 184, 6, 6, -90, 180, Arc2D.OPEN));
        }
        g.setColor(new Color(60, 40, 22));                                           // one knocked over, coffee spilling off the edge
        g.fill(new RoundRectangle2D.Double(408, 186, 14, 7, 3, 3));
        g.setColor(new Color(50, 30, 16, 200));
        g.fill(el(396, 192, 24, 4)); g.fillRect(398, 194, 2, 12); g.fill(el(396, 205, 6, 4));
        for (int k = 0; k < 3; k++) {                                                // stack of marking
            g.setColor(k % 2 == 0 ? new Color(214, 206, 180) : new Color(190, 182, 156));
            g.fillRect(430 - k, 188 - k * 2, 26, 3);
        }
        g.setColor(new Color(180, 30, 30)); g.fillRect(452, 183, 8, 1);             // red pen on top

        // ---- kitchenette (left): fridge left open a crack, microwave, coffee machine
        g.setPaint(new GradientPaint(14, 0, new Color(190, 186, 170), 64, 0, new Color(130, 126, 112)));
        g.fillRect(14, 92, 50, FLOOR_TOP - 92);
        g.setColor(new Color(40, 38, 34));
        g.drawRect(14, 92, 50, FLOOR_TOP - 92); g.drawLine(14, 140, 64, 140);
        g.fillRect(58, 104, 3, 24); g.fillRect(58, 150, 3, 30);                      // handles
        g.setPaint(new RadialGradientPaint(new Point2D.Double(66, 190), 40f, new float[] { 0f, 1f },
                new Color[] { new Color(220, 240, 230, 80), new Color(220, 240, 230, 0) }));
        g.fill(el(26, 150, 80, 80));                                                 // cold light from the open door
        g.setColor(new Color(230, 240, 236));
        g.fillRect(64, 144, 3, 90);
        g.setColor(new Color(236, 220, 110));                                         // sticky notes
        g.fillRect(22, 110, 12, 10); g.fillRect(38, 154, 12, 10);
        g.setColor(new Color(200, 30, 30));
        g.drawLine(24, 114, 32, 114); g.drawLine(24, 117, 30, 117); g.drawLine(40, 158, 48, 158);
        g.setPaint(new GradientPaint(0, 172, new Color(110, 92, 66), 0, 180, new Color(76, 62, 42)));
        g.fillRect(64, 172, 64, 8);                                                  // counter
        g.setColor(new Color(60, 48, 34));
        g.fillRect(68, 180, 56, FLOOR_TOP - 180);
        g.setColor(new Color(30, 24, 18));
        g.drawRect(72, 186, 22, 50); g.drawRect(98, 186, 22, 50);
        g.setColor(new Color(70, 72, 74));
        g.fillRect(70, 146, 38, 26);                                                 // microwave
        g.setColor(new Color(16, 18, 20)); g.fillRect(73, 149, 24, 20);
        g.setColor(new Color(200, 40, 30)); g.setFont(new Font(MONO, Font.BOLD, 6)); g.drawString("0:00", 98, 156);
        g.setColor(new Color(36, 34, 34));
        g.fillRect(110, 150, 16, 22);                                                // coffee machine
        g.setColor(new Color(255, 60, 40)); g.fill(el(115, 154, 4, 4));
        g.setColor(new Color(60, 40, 22)); g.fillRect(113, 164, 10, 7);

        // ---- pigeonhole mailboxes (right), stuffed with letters nobody collected
        g.setColor(new Color(76, 56, 36));
        g.fillRect(584, 88, 104, 82);
        for (int row = 0; row < 4; row++)
            for (int col = 0; col < 5; col++) {
                int x = 588 + col * 20, y = 92 + row * 19;
                g.setColor(new Color(24, 16, 10)); g.fillRect(x, y, 17, 16);
                if (r.nextInt(3) > 0) {
                    g.setColor(new Color(206, 198, 172)); g.fillRect(x + 2, y + 6 - r.nextInt(3), 13, 8);
                    if (r.nextInt(4) == 0) { g.setColor(new Color(170, 40, 40)); g.fillRect(x + 4, y + 9, 5, 2); }
                }
                g.setColor(new Color(176, 160, 120)); g.fillRect(x + 4, y + 13, 9, 2);       // name label
            }
        // calendar, every day crossed out in red
        g.setColor(new Color(214, 206, 180));
        g.fillRect(704, 80, 46, 52);
        g.setColor(new Color(150, 30, 30)); g.fillRect(704, 80, 46, 10);
        g.setColor(new Color(180, 20, 20));
        g.setStroke(new BasicStroke(1));
        for (int d = 0; d < 20; d++) {
            int x = 707 + (d % 5) * 9, y = 93 + (d / 5) * 9;
            g.drawLine(x, y, x + 6, y + 6); g.drawLine(x + 6, y, x, y + 6);
        }
        // sagging old couch
        g.setPaint(new GradientPaint(0, 176, new Color(96, 70, 54), 0, FLOOR_TOP, new Color(56, 40, 30)));
        g.fill(new RoundRectangle2D.Double(694, 176, 104, 40, 14, 14));               // back
        g.fill(new RoundRectangle2D.Double(690, 206, 112, 34, 10, 10));               // seat
        g.setColor(new Color(36, 24, 18));
        g.drawLine(730, 180, 730, 212); g.drawLine(766, 180, 766, 212);
        g.setColor(new Color(40, 28, 20, 160)); g.fill(el(744, 214, 30, 12));         // stain
        g.setColor(new Color(140, 120, 90)); g.fill(el(756, 196, 14, 10));           // stuffing poking out

        // ---- worn linoleum floor
        g.setColor(new Color(40, 44, 38));
        g.fillRect(0, FLOOR_TOP, W, STAGE_BOTTOM - FLOOR_TOP);
        double vpx = 400, vpy = 120;
        double[] rows = { FLOOR_TOP, 250, 257, 266, 277, STAGE_BOTTOM };
        for (int ri = 0; ri < rows.length - 1; ri++) {
            for (int ci = -14; ci < 26; ci++) {
                double y0 = rows[ri], y1 = rows[ri + 1], xb0 = ci * 44 - 120, xb1 = xb0 + 44;
                Path2D q = new Path2D.Double();
                q.moveTo(px(vpx, vpy, xb0, y0), y0); q.lineTo(px(vpx, vpy, xb1, y0), y0);
                q.lineTo(px(vpx, vpy, xb1, y1), y1); q.lineTo(px(vpx, vpy, xb0, y1), y1); q.closePath();
                g.setColor(Math.floorMod(ri + ci, 2) == 0 ? new Color(70, 78, 64) : new Color(54, 60, 50));
                g.fill(q);
            }
        }
        g.setPaint(new GradientPaint(0, FLOOR_TOP, new Color(0, 0, 0, 170), 0, STAGE_BOTTOM, new Color(0, 0, 0, 20)));
        g.fillRect(0, FLOOR_TOP, W, STAGE_BOTTOM - FLOOR_TOP);
        g.setColor(new Color(60, 8, 10, 110));                                      // a dark stain where the monster stands
        g.fill(el(560, 268, 170, 20));
        g.setColor(new Color(14, 14, 12));
        g.fillRect(0, FLOOR_TOP - 2, W, 4);
        for (int i = 0; i < 7; i++) {                                                // scattered papers
            double sx = 20 + r.nextInt(W - 40), sy = FLOOR_TOP + 10 + r.nextInt(36);
            Shape paper = AffineTransform.getRotateInstance(r.nextDouble() * 2 - 1, sx, sy)
                    .createTransformedShape(new Rectangle2D.Double(sx - 9, sy - 6, 18, 12));
            g.setColor(new Color(160, 156, 136, 170));
            g.fill(paper);
        }
        g.setClip(oldClip);
    }
}
