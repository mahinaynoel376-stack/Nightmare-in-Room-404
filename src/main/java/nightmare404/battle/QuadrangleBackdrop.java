package nightmare404.battle;

import java.awt.*;
import java.awt.geom.*;
import java.util.Random;

import nightmare404.battle.BattleScreen.BattlePanel;

// A battle background: the quadrangle at night, for the fights with its trees (see TypingBattle).
// A dark sky with a sick moon, the school's silhouette across the far side of the grass with a few
// windows still lit (one of them flickering, one of them red), the perimeter fence, a low mist,
// and the lawn in front.
public class QuadrangleBackdrop implements Backdrop {
    // For the word battle, if a quadrangle monster is ever fought there
    public void draw(Graphics2D g, BattlePanel battle) {
        Shape oldClip = g.getClip();
        g.clipRect(0, BattlePanel.TOP_H, BattlePanel.W, BattlePanel.STAGE_BOTTOM - BattlePanel.TOP_H);
        paint(g, BattlePanel.W, BattlePanel.TOP_H, BattlePanel.FLOOR_TOP, BattlePanel.STAGE_BOTTOM, battle.ticks);
        g.setClip(oldClip);
    }

    // Draws it between y = top and y = bottom, `width` wide, with the far edge of the grass at y = horizon
    static void paint(Graphics2D g, int width, int top, int horizon, int bottom, long ticks) {
        Random r = new Random(404);   // The same stars and windows every frame

        // Sky
        g.setPaint(new GradientPaint(0, top, new Color(8, 9, 22), 0, horizon, new Color(46, 30, 54)));
        g.fillRect(0, top, width, horizon - top);
        for (int i = 0; i < 70; i++) {
            int sx = r.nextInt(width), sy = top + r.nextInt(Math.max(1, horizon - top - 60));
            int twinkle = (int) (90 + 80 * Math.sin(ticks / 25.0 + i));
            g.setColor(new Color(220, 220, 240, Math.max(30, Math.min(255, twinkle))));
            g.fillRect(sx, sy, 2, 2);
        }
        // The moon, greenish, with a halo
        double mx = width * 0.36, my = top + 62;
        g.setPaint(new RadialGradientPaint(new Point2D.Double(mx, my), 90f, new float[] {0f, 1f},
                new Color[] {new Color(190, 210, 170, 70), new Color(190, 210, 170, 0)}));
        g.fill(new Ellipse2D.Double(mx - 90, my - 90, 180, 180));
        g.setColor(new Color(214, 226, 190));
        g.fill(new Ellipse2D.Double(mx - 26, my - 26, 52, 52));
        g.setColor(new Color(176, 190, 156));
        g.fill(new Ellipse2D.Double(mx - 10, my - 14, 12, 10));
        g.fill(new Ellipse2D.Double(mx + 6, my + 4, 9, 8));

        // The school, across the far side of the grass
        int roof = horizon - 92;
        g.setColor(new Color(16, 14, 22));
        g.fillRect(0, roof, width, horizon - roof);
        g.fillRect((int) (width * 0.44), roof - 26, (int) (width * 0.12), 26);   // The middle block, a floor higher
        for (int floor = 0; floor < 3; floor++) {
            for (int wx = 18; wx < width - 20; wx += 46) {
                int wy = roof + 10 + floor * 26;
                int roll = r.nextInt(12);
                Color c;
                if (roll == 0) c = new Color(220, 190, 110, (ticks / 7 + wx) % 9 < 2 ? 40 : 170);   // Flickering
                else if (roll == 1) c = new Color(150, 20, 24, 170);                                  // Red
                else if (roll < 4) c = new Color(200, 176, 110, 110);                                 // Lit
                else c = new Color(30, 30, 44);                                                        // Dark
                g.setColor(c);
                g.fillRect(wx, wy, 16, 14);
            }
        }
        // The perimeter fence along the far side
        g.setColor(new Color(24, 26, 30));
        g.fillRect(0, horizon - 22, width, 2);
        for (int fx = 0; fx < width; fx += 14) g.fillRect(fx, horizon - 30, 2, 30);

        // The lawn
        g.setPaint(new GradientPaint(0, horizon, new Color(22, 40, 24), 0, bottom, new Color(8, 18, 10)));
        g.fillRect(0, horizon, width, bottom - horizon);
        g.setColor(new Color(34, 58, 32));
        for (int i = 0; i < 160; i++) {                  // Tufts of grass
            int gx = r.nextInt(width), gy = horizon + 4 + r.nextInt(Math.max(1, bottom - horizon - 4));
            g.drawLine(gx, gy, gx - 1, gy - 3);
            g.drawLine(gx + 2, gy, gx + 3, gy - 4);
        }
        // A low mist drifting across the far side
        for (int i = 0; i < 3; i++) {
            double fx = ((ticks * (0.3 + i * 0.15)) + i * 260) % (width + 400) - 200;
            g.setPaint(new RadialGradientPaint(new Point2D.Double(fx, horizon + 6), 220f, new float[] {0f, 1f},
                    new Color[] {new Color(170, 180, 190, 46), new Color(170, 180, 190, 0)}));
            g.fill(new Ellipse2D.Double(fx - 220, horizon - 40, 440, 90));
        }
    }
}
