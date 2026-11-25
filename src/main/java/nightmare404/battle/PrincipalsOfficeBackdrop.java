package nightmare404.battle;

import static nightmare404.battle.BattleScreen.BattlePanel.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import nightmare404.battle.BattleScreen.BattlePanel;

// A battle background, from Lobby/Principals office in the Battle_Screens folder.
// It's the original drawRoom code; the battle calls draw() behind the fighters (see BattlePanel.drawRoom).
public class PrincipalsOfficeBackdrop implements Backdrop {

    public void draw(Graphics2D g, BattlePanel battle) {
        Shape oldClip = g.getClip();
        g.clipRect(0, TOP_H, W, STAGE_BOTTOM - TOP_H);
        Random r = new Random(1952); // same decoration every frame

        // dark green damask wallpaper
        g.setPaint(new GradientPaint(0, TOP_H, new Color(40, 54, 44), 0, 190, new Color(22, 30, 24)));
        g.fillRect(0, TOP_H, W, 190 - TOP_H);
        g.setColor(new Color(60, 78, 62, 70));
        for (int y = TOP_H + 10; y < 190; y += 22)
            for (int x = (y / 22) % 2 * 14; x < W; x += 28) {
                g.fill(el(x - 3, y - 4, 6, 8));
                g.fill(el(x - 6, y - 1, 12, 2));
            }
        for (int i = 0; i < 6; i++) { // water stains
            double sx = r.nextInt(W), sy = TOP_H + 20 + r.nextInt(100), sr = 30 + r.nextInt(45);
            g.setPaint(new RadialGradientPaint(new Point2D.Double(sx, sy), (float) sr, new float[] { 0f, 1f },
                    new Color[] { new Color(70, 58, 30, 60), new Color(70, 58, 30, 0) }));
            g.fill(el(sx - sr, sy - sr, sr * 2, sr * 2));
        }
        // mahogany panelling
        g.setPaint(new GradientPaint(0, 186, new Color(74, 40, 28), 0, FLOOR_TOP, new Color(40, 20, 14)));
        g.fillRect(0, 186, W, FLOOR_TOP - 186);
        g.setColor(new Color(24, 12, 8));
        g.setStroke(new BasicStroke(1.5f));
        for (int x = 10; x < W; x += 60) g.drawRect(x, 196, 48, 40);
        g.setColor(new Color(100, 60, 40));
        g.fillRect(0, 184, W, 4);

        // ---- big window with broken venetian blinds, moonlight leaking through
        g.setColor(new Color(36, 22, 14));
        g.fillRect(232, 70, 336, 118);
        g.setPaint(new GradientPaint(0, 76, new Color(22, 30, 46), 0, 182, new Color(10, 14, 22)));
        g.fillRect(240, 76, 320, 106);
        g.setColor(new Color(214, 216, 196));
        g.fill(el(500, 88, 16, 16));
        for (int k = 0; k < 15; k++) {                                               // slats
            int y = 78 + k * 7;
            boolean broken = k == 6 || k == 11;
            g.setColor(new Color(58, 56, 50));
            if (broken) {                                                            // hanging loose at one end
                Path2D slat = new Path2D.Double();
                slat.moveTo(240, y); slat.lineTo(400, y); slat.lineTo(420, y + 14); slat.lineTo(420, y + 17); slat.lineTo(400, y + 4); slat.lineTo(240, y + 4); slat.closePath();
                g.fill(slat);
            } else g.fillRect(240, y, 320, 5);
            g.setColor(new Color(150, 170, 200, 45));
            g.fillRect(240, y + 5, 320, 1);                                          // thin line of moonlight
        }
        g.setColor(new Color(70, 64, 54));
        g.drawLine(330, 72, 330, 186); g.drawLine(470, 72, 470, 186);                // cords

        // ---- high-backed leather chair and the desk
        g.setPaint(new GradientPaint(0, 148, new Color(90, 26, 26), 0, 210, new Color(48, 12, 14)));
        g.fill(new RoundRectangle2D.Double(360, 146, 80, 70, 24, 24));
        g.setColor(new Color(30, 8, 8));
        for (int[] b : new int[][] { { 380, 162 }, { 400, 162 }, { 420, 162 }, { 390, 176 }, { 410, 176 } }) g.fill(el(b[0] - 2, b[1] - 2, 4, 4));
        g.setPaint(new GradientPaint(0, 198, new Color(92, 52, 32), 0, FLOOR_TOP, new Color(46, 24, 14)));
        g.fillRect(250, 198, 300, FLOOR_TOP - 198);
        g.setPaint(new GradientPaint(0, 192, new Color(120, 72, 44), 0, 200, new Color(80, 46, 28)));
        g.fillRect(244, 192, 312, 8);                                                // desk top
        g.setColor(new Color(26, 14, 8));
        g.drawRect(262, 206, 80, 34); g.drawRect(458, 206, 80, 34);                  // drawers
        g.setColor(new Color(170, 140, 70));
        g.fill(el(298, 220, 6, 4)); g.fill(el(494, 220, 6, 4));
        g.setPaint(new GradientPaint(0, 182, new Color(186, 156, 86), 0, 192, new Color(120, 96, 46)));
        g.fillRect(370, 182, 60, 10);                                                // name plate
        g.setFont(new Font(SERIF, Font.BOLD, 7));
        g.setColor(new Color(40, 28, 10));
        g.drawString("PRINCIPAL", 400 - g.getFontMetrics().stringWidth("PRINCIPAL") / 2, 190);
        for (int k = 0; k < 4; k++) {                                                // stacked files
            if (k % 2 == 0) {
                g.setColor(new Color(196, 168, 112));
            } else {
                g.setColor(new Color(170, 142, 92));
            }
            g.fillRect(470 - k, 184 - k * 3, 40, 3);
        }
        g.setColor(new Color(90, 70, 40));
        g.fill(el(446, 184, 10, 9)); g.setColor(new Color(60, 70, 30)); g.fillRect(450, 181, 2, 4);   // rotten apple
        // green banker's lamp, glowing
        g.setPaint(new RadialGradientPaint(new Point2D.Double(300, 182), 110f, new float[] { 0f, 1f },
                new Color[] { new Color(255, 214, 140, 70), new Color(255, 214, 140, 0) }));
        g.fill(el(190, 72, 220, 220));
        g.setColor(new Color(150, 124, 60));
        g.fillRect(297, 176, 4, 16); g.fillRect(288, 189, 22, 3);
        g.setPaint(new GradientPaint(0, 168, new Color(40, 120, 70), 0, 178, new Color(20, 70, 40)));
        g.fill(new Arc2D.Double(280, 164, 38, 22, 0, 180, Arc2D.CHORD));
        g.setColor(new Color(255, 230, 170));
        g.fillRect(286, 174, 26, 2);

        // ---- filing cabinets (left), one drawer hanging open, files spilling out
        for (int c = 0; c < 2; c++) {
            int x = 14 + c * 46;
            g.setPaint(new GradientPaint(x, 0, new Color(96, 100, 100), x + 44, 0, new Color(56, 58, 60)));
            g.fillRect(x, 130, 44, FLOOR_TOP - 130);
            g.setColor(new Color(24, 26, 28));
            for (int d = 0; d < 4; d++) {
                g.drawRect(x + 3, 134 + d * 27, 38, 24);
                g.setColor(new Color(150, 154, 154)); g.fillRect(x + 16, 144 + d * 27, 12, 3); g.setColor(new Color(24, 26, 28));
            }
        }
        g.setColor(new Color(44, 46, 48));
        g.fillRect(56, 186, 52, 22);                                                 // the open drawer
        g.setColor(new Color(196, 168, 112));
        for (int k = 0; k < 5; k++) g.fillRect(60 + k * 9, 180 - (k % 2) * 3, 7, 8);
        for (int[] f : new int[][] { { 70, 230, 20 }, { 96, 236, -15 } }) {         // files on the floor
            Graphics2D fg = (Graphics2D) g.create();
            fg.rotate(Math.toRadians(f[2]), f[0], f[1]);
            fg.setColor(new Color(186, 158, 100)); fg.fillRect(f[0] - 10, f[1] - 6, 20, 12);
            fg.dispose();
        }
        // portrait of a past principal, face scratched out
        g.setColor(new Color(130, 100, 50));
        g.fillRect(28, 72, 50, 56);
        g.setPaint(new GradientPaint(0, 78, new Color(70, 56, 44), 0, 122, new Color(36, 28, 22)));
        g.fillRect(33, 77, 40, 46);
        g.setColor(new Color(20, 16, 14));
        g.fill(new RoundRectangle2D.Double(40, 104, 26, 22, 12, 12));               // shoulders
        g.setColor(new Color(150, 124, 100));
        g.fill(el(44, 84, 18, 22));                                                  // face
        g.setColor(new Color(230, 224, 210));
        g.setStroke(new BasicStroke(1.5f));
        for (int k = 0; k < 6; k++) g.drawLine(43 + k * 3, 86 + (k % 2) * 3, 49 + k * 3, 104 - (k % 2) * 3);

        // ---- crooked diplomas (right)
        int[][] frames = { { 590, 84, -4 }, { 640, 92, 6 }, { 600, 132, 3 } };
        for (int[] f : frames) {
            Graphics2D dg = (Graphics2D) g.create();
            dg.rotate(Math.toRadians(f[2]), f[0] + 20, f[1] + 15);
            dg.setColor(new Color(40, 28, 18)); dg.fillRect(f[0], f[1], 40, 30);
            dg.setColor(new Color(206, 196, 168)); dg.fillRect(f[0] + 3, f[1] + 3, 34, 24);
            dg.setColor(new Color(120, 100, 70));
            for (int l = 0; l < 3; l++) dg.fillRect(f[0] + 8, f[1] + 8 + l * 5, 24, 1);
            dg.setColor(new Color(170, 30, 30)); dg.fill(el(f[0] + 26, f[1] + 19, 6, 6));   // seal
            dg.dispose();
        }
        // coat stand in the corner, with a long coat and a hat... or is it?
        g.setColor(new Color(50, 30, 20));
        g.fillRect(752, 74, 4, FLOOR_TOP - 74);
        g.drawLine(740, FLOOR_TOP - 2, 768, FLOOR_TOP - 2);
        g.setColor(new Color(24, 22, 26));
        g.fillPolygon(new int[] { 742, 766, 776, 732 }, new int[] { 96, 96, 214, 214 }, 4);
        g.fill(el(740, 76, 28, 10)); g.fillRect(745, 66, 18, 14);                  // hat
        g.setColor(new Color(16, 14, 18));
        g.fill(el(744, 86, 20, 14));                                                 // where a face would be

        // ---- carpet floor with a patterned border
        g.setPaint(new GradientPaint(0, FLOOR_TOP, new Color(70, 22, 22), 0, STAGE_BOTTOM, new Color(46, 14, 16)));
        g.fillRect(0, FLOOR_TOP, W, STAGE_BOTTOM - FLOOR_TOP);
        double vpx = 400, vpy = 120;
        g.setColor(new Color(110, 80, 40, 120));
        g.setStroke(new BasicStroke(2));
        Path2D border = new Path2D.Double();
        border.moveTo(px(vpx, vpy, 40, 252), 252); border.lineTo(px(vpx, vpy, 760, 252), 252);
        border.lineTo(px(vpx, vpy, 760, 286), 286); border.lineTo(px(vpx, vpy, 40, 286), 286); border.closePath();
        g.draw(border);
        for (int k = 0; k < 24; k++) { double x = px(vpx, vpy, 60 + k * 30, 269); g.fill(el(x - 2, 267, 4, 4)); }
        g.setPaint(new GradientPaint(0, FLOOR_TOP, new Color(0, 0, 0, 170), 0, STAGE_BOTTOM, new Color(0, 0, 0, 20)));
        g.fillRect(0, FLOOR_TOP, W, STAGE_BOTTOM - FLOOR_TOP);
        g.setColor(new Color(40, 4, 6, 140));                                       // a dark stain where the monster stands
        g.fill(el(560, 268, 170, 20));
        g.setColor(new Color(14, 8, 6));
        g.fillRect(0, FLOOR_TOP - 2, W, 4);
        for (int i = 0; i < 6; i++) {                                                // scattered papers
            double sx = 20 + r.nextInt(W - 40), sy = FLOOR_TOP + 10 + r.nextInt(36);
            Shape paper = AffineTransform.getRotateInstance(r.nextDouble() * 2 - 1, sx, sy)
                    .createTransformedShape(new Rectangle2D.Double(sx - 9, sy - 6, 18, 12));
            g.setColor(new Color(170, 160, 136, 170));
            g.fill(paper);
        }
        g.setClip(oldClip);
    }
}
