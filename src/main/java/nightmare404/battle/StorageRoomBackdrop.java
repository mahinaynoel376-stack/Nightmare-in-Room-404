package nightmare404.battle;

import static nightmare404.battle.BattleScreen.BattlePanel.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import nightmare404.battle.BattleScreen.BattlePanel;

// A battle background, from Lobby/StockRoom in the Battle_Screens folder.
// It's the original drawRoom code; the battle calls draw() behind the fighters (see BattlePanel.drawRoom).
public class StorageRoomBackdrop implements Backdrop {
    /** Inside the school storage room: cinder-block walls, dripping pipes, a chain-link storage cage with its
     *  chain cut, shelves of labelled boxes, a mop bucket, a ball cart, stacked chairs, and something tall
     *  standing under a dusty sheet in the corner. */
    public void draw(Graphics2D g, BattlePanel battle) {
        Shape oldClip = g.getClip();
        g.clipRect(0, TOP_H, W, STAGE_BOTTOM - TOP_H);
        Random r = new Random(1999); // same decoration every frame

        // cinder-block walls
        g.setPaint(new GradientPaint(0, TOP_H, new Color(70, 70, 66), 0, FLOOR_TOP, new Color(34, 34, 32)));
        g.fillRect(0, TOP_H, W, FLOOR_TOP - TOP_H);
        g.setColor(new Color(22, 22, 20, 150));
        g.setStroke(new BasicStroke(1.5f));
        for (int y = TOP_H + 4, row = 0; y < FLOOR_TOP; y += 15, row++) {
            g.drawLine(0, y, W, y);
            for (int x = (row % 2) * 18; x < W; x += 36) g.drawLine(x, y, x, y + 15);
        }
        for (int i = 0; i < 26; i++) { // chipped and darker blocks
            int row = r.nextInt(12), x = r.nextInt(23) * 36 + (row % 2) * 18;
            g.setColor(r.nextBoolean() ? new Color(20, 20, 18, 90) : new Color(110, 106, 96, 40));
            g.fillRect(x + 1, TOP_H + 5 + row * 15, 35, 14);
        }
        for (int i = 0; i < 6; i++) { // damp stains
            double sx = r.nextInt(W), sy = TOP_H + 30 + r.nextInt(120), sr = 30 + r.nextInt(50);
            g.setPaint(new RadialGradientPaint(new Point2D.Double(sx, sy), (float) sr, new float[] { 0f, 1f },
                    new Color[] { new Color(40, 46, 30, 80), new Color(40, 46, 30, 0) }));
            g.fill(el(sx - sr, sy - sr, sr * 2, sr * 2));
        }

        // pipes along the ceiling, one of them dripping
        for (int[] pipe : new int[][] { { 61, 8 }, { 73, 6 } }) {
            g.setPaint(new GradientPaint(0, pipe[0], new Color(110, 100, 86), 0, pipe[0] + pipe[1], new Color(50, 44, 38)));
            g.fillRect(0, pipe[0], W, pipe[1]);
            g.setColor(new Color(36, 32, 28));
            for (int x = 60; x < W; x += 150) g.fillRect(x, pipe[0] - 2, 8, pipe[1] + 4);   // brackets
        }
        g.setColor(new Color(110, 70, 40, 140));                                     // rust streaks down the wall
        for (int x : new int[] { 68, 218, 518, 668 }) g.fillRect(x + 2, 79, 3, 16 + (x % 5) * 6);
        double drop = (battle.ticks * 2) % 170;                                              // drip falling to the floor
        g.setColor(new Color(150, 170, 190, 200));
        if (drop < 166) g.fill(el(676, 79 + drop, 3, 5));
        else { g.setStroke(new BasicStroke(1)); g.draw(el(668, 262, 20, 4)); }

        // ---- shelves of labelled boxes (left)
        g.setColor(new Color(76, 80, 82));
        g.fillRect(10, 84, 5, FLOOR_TOP - 84); g.fillRect(128, 84, 5, FLOOR_TOP - 84);
        String[] labels = { "XMAS", "SPORTS", "LOST", "EXAMS", "KEEP OUT", "CUPS", "CHALK", "FILES" };
        int li = 0;
        for (int shelf = 0; shelf < 4; shelf++) {
            int sy = 128 + shelf * 38;
            g.setColor(new Color(96, 100, 102));
            g.fillRect(10, sy, 123, 4);
            if (shelf == 3) break;
            int x = 17;
            while (x < 118) {
                int w = 26 + r.nextInt(16), h = 22 + r.nextInt(10);
                if (x + w > 126) w = 126 - x;
                if (w < 14) break;
                g.setPaint(new GradientPaint(x, 0, new Color(160, 124, 78), x + w, 0, new Color(112, 84, 50)));
                g.fillRect(x, sy - h, w, h);
                g.setColor(new Color(70, 50, 28));
                g.drawRect(x, sy - h, w, h);
                g.setColor(new Color(196, 174, 120, 160)); g.fillRect(x + w / 2 - 1, sy - h, 3, h);   // tape
                g.setColor(new Color(214, 208, 190)); g.fillRect(x + 3, sy - h + 5, w - 6, 7);      // label
                g.setFont(new Font(MONO, Font.BOLD, 5));
                g.setColor(labels[li % labels.length].equals("KEEP OUT") ? new Color(170, 20, 20) : new Color(40, 34, 28));
                g.drawString(labels[li % labels.length], x + 4, sy - h + 11);
                li++;
                x += w + 3;
            }
        }
        // mop bucket and mop
        g.setColor(new Color(110, 84, 50));
        g.setStroke(new BasicStroke(3));
        g.drawLine(156, 214, 182, 120);
        g.setColor(new Color(150, 146, 130));
        for (int k = 0; k < 6; k++) g.drawLine(150 + k * 3, 214, 146 + k * 4, 226);
        g.setPaint(new GradientPaint(0, 210, new Color(200, 170, 40), 0, FLOOR_TOP, new Color(140, 112, 20)));
        g.fillPolygon(new int[] { 140, 182, 178, 144 }, new int[] { 214, 214, FLOOR_TOP, FLOOR_TOP }, 4);
        g.setColor(new Color(50, 40, 10));
        g.setStroke(new BasicStroke(1.5f));
        g.drawPolygon(new int[] { 140, 182, 178, 144 }, new int[] { 214, 214, FLOOR_TOP, FLOOR_TOP }, 4);
        g.setColor(new Color(60, 70, 40)); g.fillRect(143, 215, 36, 4);             // murky water

        // ---- the storage cage (middle): chain-link, gate hanging open, chain cut
        g.setColor(new Color(10, 10, 12));
        g.fillRect(232, 76, 336, FLOOR_TOP - 76);
        g.setColor(new Color(30, 30, 32));                                           // shapes inside, in the dark
        g.fillRect(250, 150, 60, 94); g.fillRect(256, 126, 46, 24);
        g.fillRect(470, 160, 80, 84); g.fill(el(330, 196, 40, 48));
        g.fillPolygon(new int[] { 380, 420, 440, 370 }, new int[] { 244, 244, 170, 180 }, 4);
        Shape keep = g.getClip();
        g.clipRect(232, 76, 336, FLOOR_TOP - 76);                                   // mesh only inside the cage
        g.setColor(new Color(120, 124, 126, 70));                                    // chain-link mesh
        g.setStroke(new BasicStroke(1));
        for (int k = -20; k < 60; k++) {
            int x0 = 232 + k * 10;
            g.drawLine(x0, 76, x0 + 168, FLOOR_TOP);
            g.drawLine(x0, FLOOR_TOP, x0 + 168, 76);
        }
        g.setClip(keep);
        g.setColor(new Color(90, 94, 96));
        g.setStroke(new BasicStroke(4));
        g.drawRect(232, 76, 336, FLOOR_TOP - 76);                                    // cage frame
        g.drawLine(400, 76, 400, FLOOR_TOP);
        g.setColor(new Color(150, 130, 40));                                         // warning sign
        g.fillRect(372, 80, 56, 10);
        g.setFont(new Font(MONO, Font.BOLD, 7));
        g.setColor(new Color(30, 24, 10));
        g.drawString("STAFF ONLY", 400 - g.getFontMetrics().stringWidth("STAFF ONLY") / 2, 88);
        g.setColor(new Color(140, 136, 128));                                        // cut chain dangling from the gate
        g.setStroke(new BasicStroke(2));
        for (int k = 0; k < 7; k++) g.draw(new Ellipse2D.Double(398 + (k % 2) * 2, 164 + k * 5, 5, 7));
        g.setPaint(new GradientPaint(0, 200, new Color(150, 120, 60), 0, 214, new Color(90, 70, 34)));
        g.fill(new RoundRectangle2D.Double(396, 200, 12, 13, 3, 3));                 // open padlock
        g.setColor(new Color(110, 108, 104));
        g.draw(new Arc2D.Double(397, 192, 10, 12, 20, 180, Arc2D.OPEN));

        // ---- the wall to the right of the cage, then the ball cart, chairs and the sheet
        for (int k = 0; k < 4; k++) {                                                // stacked chairs
            int y = 150 + k * 12;
            g.setColor(new Color(60, 70, 84));
            g.fillRect(598 + k * 2, y, 44, 6);
            g.setColor(new Color(40, 42, 46));
            g.fillRect(600 + k * 2, y + 6, 3, 10); g.fillRect(636 + k * 2, y + 6, 3, 10);
        }
        g.setColor(new Color(40, 42, 46));
        g.fillRect(604, 196, 3, 48); g.fillRect(640, 196, 3, 48);
        g.setColor(new Color(90, 92, 94));                                           // ball cart
        g.setStroke(new BasicStroke(2));
        g.drawRect(650, 206, 60, 30);
        for (int x = 656; x < 710; x += 8) g.drawLine(x, 206, x, 236);
        for (int[] b : new int[][] { { 654, 196 }, { 668, 194 }, { 682, 197 }, { 696, 194 }, { 661, 186 }, { 676, 184 }, { 690, 187 } }) {
            g.setColor(new Color(170, 90, 40)); g.fill(el(b[0], b[1], 13, 13));
            g.setColor(new Color(60, 30, 14)); g.draw(el(b[0], b[1], 13, 13)); g.drawLine(b[0] + 6, b[1], b[0] + 6, b[1] + 13);
        }
        g.setColor(new Color(30, 30, 30));
        g.fill(el(652, 236, 7, 7)); g.fill(el(701, 236, 7, 7));
        // something tall under a dusty sheet
        Path2D sheet = new Path2D.Double();
        sheet.moveTo(744, 96); sheet.curveTo(730, 98, 726, 112, 728, 124);
        sheet.curveTo(716, 134, 718, 180, 712, 244); sheet.lineTo(798, 244);
        sheet.curveTo(790, 180, 794, 134, 780, 124); sheet.curveTo(782, 110, 776, 96, 760, 96); sheet.closePath();
        g.setPaint(new GradientPaint(720, 0, new Color(186, 180, 166), 796, 0, new Color(120, 116, 104)));
        g.fill(sheet);
        g.setColor(new Color(90, 86, 76));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new QuadCurve2D.Double(736, 140, 742, 190, 734, 240));                // folds
        g.draw(new QuadCurve2D.Double(768, 134, 762, 186, 772, 240));
        g.draw(new QuadCurve2D.Double(746, 126, 754, 122, 764, 126));                // the shape of shoulders
        g.setColor(new Color(60, 56, 50, 120));
        g.fill(el(744, 106, 7, 6)); g.fill(el(756, 106, 7, 6));                      // ...and where eyes would be

        // ---- cracked concrete floor
        g.setColor(new Color(46, 46, 44));
        g.fillRect(0, FLOOR_TOP, W, STAGE_BOTTOM - FLOOR_TOP);
        double vpx = 400, vpy = 120;
        g.setColor(new Color(30, 30, 28));
        g.setStroke(new BasicStroke(1));
        for (int ci = -10; ci < 20; ci++) { double xb = ci * 90 - 150; g.draw(new Line2D.Double(px(vpx, vpy, xb, FLOOR_TOP), FLOOR_TOP, xb, STAGE_BOTTOM)); }
        for (double y : new double[] { 254, 268 }) g.drawLine(0, (int) y, W, (int) y);
        g.setColor(new Color(20, 20, 18));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(200, 250, 236, 262); g.drawLine(236, 262, 230, 280); g.drawLine(236, 262, 262, 270);   // cracks
        g.drawLine(480, 256, 520, 252); g.drawLine(520, 252, 540, 266);
        g.setColor(new Color(16, 16, 16)); g.fill(el(386, 266, 30, 8));              // floor drain
        g.setColor(new Color(80, 80, 78));
        for (int k = 1; k < 5; k++) g.drawLine(386 + k * 6, 267, 386 + k * 6, 273);
        g.setColor(new Color(40, 54, 70, 150)); g.fill(el(640, 258, 70, 10));        // puddle under the drip
        g.setColor(new Color(150, 170, 200, 60)); g.drawLine(650, 262, 690, 262);
        g.setPaint(new GradientPaint(0, FLOOR_TOP, new Color(0, 0, 0, 160), 0, STAGE_BOTTOM, new Color(0, 0, 0, 20)));
        g.fillRect(0, FLOOR_TOP, W, STAGE_BOTTOM - FLOOR_TOP);
        g.setColor(new Color(60, 8, 10, 110));                                      // a dark stain where the monster stands
        g.fill(el(560, 268, 170, 20));
        g.setColor(new Color(16, 16, 14));
        g.fillRect(0, FLOOR_TOP - 2, W, 4);
        // a fallen box, spilling
        g.setPaint(new GradientPaint(0, 262, new Color(156, 120, 74), 0, 284, new Color(110, 82, 48)));
        g.fillPolygon(new int[] { 214, 252, 258, 220 }, new int[] { 262, 256, 278, 286 }, 4);
        g.setColor(new Color(70, 50, 28)); g.setStroke(new BasicStroke(1.5f));
        g.drawPolygon(new int[] { 214, 252, 258, 220 }, new int[] { 262, 256, 278, 286 }, 4);
        for (int i = 0; i < 9; i++) {                                                // its contents and loose papers
            double sx = i < 4 ? 262 + i * 12 : 20 + r.nextInt(W - 40), sy = FLOOR_TOP + 12 + r.nextInt(34);
            Shape paper = AffineTransform.getRotateInstance(r.nextDouble() * 2 - 1, sx, sy)
                    .createTransformedShape(new Rectangle2D.Double(sx - 8, sy - 5, 16, 10));
            g.setColor(i < 4 ? new Color(196, 186, 156, 200) : new Color(150, 146, 128, 170));
            g.fill(paper);
        }
        g.setClip(oldClip);
    }
}
