package nightmare404.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;

import nightmare404.entity.Player;
import nightmare404.main.GamePanel;
import nightmare404.map.Quadrangle;
import nightmare404.tile.TileMap;

// After Subject Zero: the walk out. The chain on the south gate falls away, and the player walks to
// it while the sun comes up. Walking out through the gate fades the screen out, then the last words
// (Director.walkedOut), then the ending screen.
//   Every clue found (the good ending): the sky over the south gate turns grey, then gold, and the
//   quadrangle brightens, with long rays of sunlight coming up from the south.
//   Some missing (the bad ending): the dawn is red, and Flor is dizzy. The view tilts back and forth,
//   she sees double, the edges of the screen throb dark red, and she walks slowly, swaying (Player.dizzy).
public class Dawn {
    public static final int SUNRISE = 900;          // Frames for the sun to come all the way up (15 s)
    public static final int THOUGHT = 420;          // Flor has a thought on the way, 7 s in
    public static final int FADE = 120;             // Frames to fade out, once through the gate
    // The south gate: 2 tiles wide, in the fence along the bottom of the quadrangle
    public static final int GATE_COL = 24, GATE_ROW = Quadrangle.HEIGHT - 1;

    private final GamePanel gp;
    private boolean on;
    private boolean remembered;                     // The good ending
    private int ticks;                              // Frames since the gate opened
    private int leaving = -1;                       // Frames since walking through the gate (-1: not yet)

    public Dawn(GamePanel gp) {
        this.gp = gp;
    }

    // Subject Zero is down and the last words said: the gate opens, and the sun starts to come up
    public void begin(boolean remembered) {
        this.remembered = remembered;
        on = true;
        ticks = 0;
        leaving = -1;
        TileMap quad = gp.world.quadrangle.map;
        quad.setTile(TileMap.LAYER_OBJECTS, GATE_COL, GATE_ROW, TileMap.EMPTY);
        quad.setTile(TileMap.LAYER_OBJECTS, GATE_COL + 1, GATE_ROW, TileMap.EMPTY);
        gp.flor.dizzy = !remembered;
    }

    public boolean on() {
        return on;
    }

    public boolean leaving() {
        return leaving >= 0;
    }

    // The gate's opening, in pixels: walking into it is walking out
    public Rectangle gate() {
        int ts = gp.tileSize;
        return new Rectangle(GATE_COL * ts, GATE_ROW * ts, 2 * ts, ts);
    }

    // One frame outside
    public void update() {
        ticks++;
        if (leaving >= 0) {                         // Through the gate: fading out, then the last words
            if (leaving < FADE) {
                leaving++;
                if (leaving == FADE) gp.director.walkedOut(remembered);
            }
            return;
        }
        if (ticks == THOUGHT) gp.director.dawnThought(remembered);
        if (gate().intersects(gp.flor.solidArea)) {
            leaving = 0;
            gp.flor.dizzy = false;
        }
    }

    // How far the sun is up: 0 just after the fight, 1 after SUNRISE frames
    private float sunUp() {
        return Math.min(1f, ticks / (float) SUNRISE);
    }

    // How dizzy Flor is: it comes on over the first two seconds (the bad ending only)
    private float dizziness() {
        return remembered ? 0 : Math.min(1f, ticks / 120f);
    }

    // The bad ending: tilts and sways the view of the world (call before the camera's own shift)
    public void tilt(Graphics2D view, int width, int height) {
        if (!on || remembered || !gp.currentRoom.outdoors) return;
        float d = dizziness();
        double angle = Math.toRadians(1.4 * Math.sin(ticks / 50.0) * d);
        double dx = 5 * Math.sin(ticks / 33.0) * d;
        view.translate(width / 2.0 + dx, height / 2.0);
        view.rotate(angle);
        view.scale(1.05, 1.05);                     // (A little bigger, so the tilted corners stay covered)
        view.translate(-width / 2.0, -height / 2.0);
    }

    // The bad ending: Flor seeing double, a faint second Flor drifting beside her (drawn in the world)
    public void drawDouble(Graphics2D view, Player flor) {
        if (!on || remembered || !gp.currentRoom.outdoors) return;
        Graphics2D g = (Graphics2D) view.create();
        g.setComposite(AlphaComposite.SrcOver.derive(0.35f * dizziness()));
        g.translate(6 * Math.sin(ticks / 27.0), 0);
        flor.draw(g);
        g.dispose();
    }

    // The light over the world (in place of Lighting, which leaves the outside as it is)
    public void drawLight(Graphics2D g2d, int width, int height) {
        if (!on || !gp.currentRoom.outdoors) return;
        Graphics2D g = (Graphics2D) g2d.create();
        float sun = sunUp();
        if (remembered) {
            // Grey at first, then gold
            float gold = Math.max(0f, (sun - 0.25f) / 0.75f);
            int r = Math.round(190 + 65 * gold), gr = Math.round(198 + 16 * gold), b = Math.round(214 - 64 * gold);
            g.setColor(new Color(r, gr, b, Math.round(64 * sun)));          // Everything brightens
            g.fillRect(0, 0, width, height);
            g.setPaint(new GradientPaint(0, height, new Color(r, gr, b, Math.round(120 * sun)),
                    0, height * 0.3f, new Color(r, gr, b, 0)));             // Brightest toward the south gate
            g.fillRect(0, 0, width, height);
            for (int i = 0; i < 5; i++) {                                   // Rays of sunlight from the south
                int baseX = width / 2 + (i - 2) * 70;
                int topX = baseX + (i - 2) * 170 + (int) (25 * Math.sin(ticks / 140.0 + i));
                g.setPaint(new GradientPaint(0, height, new Color(255, 232, 176, Math.round(36 * gold)),
                        0, 0, new Color(255, 232, 176, 0)));
                g.fillPolygon(new int[] {baseX - 14, baseX + 14, topX + 40, topX - 40},
                        new int[] {height + 20, height + 20, -20, -20}, 4);
            }
        } else {
            g.setColor(new Color(150, 24, 18, Math.round(70 * sun)));          // A red dawn
            g.fillRect(0, 0, width, height);
            g.setPaint(new GradientPaint(0, height, new Color(220, 70, 40, Math.round(110 * sun)),
                    0, height * 0.4f, new Color(220, 70, 40, 0)));
            g.fillRect(0, 0, width, height);
            // The edges of the screen throb dark red, like a headache
            float throb = 0.5f + 0.5f * (float) Math.sin(ticks / 12.0);
            int edge = Math.round((130 + 80 * throb) * dizziness());
            g.setPaint(new RadialGradientPaint(width / 2f, height / 2f, width * 0.7f, new float[] {0f, 0.5f, 1f},
                    new Color[] {new Color(40, 0, 0, 0), new Color(40, 0, 0, 0), new Color(40, 0, 0, edge)},
                    CycleMethod.NO_CYCLE));
            g.fillRect(0, 0, width, height);
        }
        g.dispose();
    }

    // Through the gate: the screen fades out, into the morning (good) or the dark (bad)
    public void drawFade(Graphics2D g2d, int width, int height) {
        if (leaving < 0) return;
        Graphics2D g = (Graphics2D) g2d.create();
        g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, leaving / (float) FADE)));
        g.setColor(remembered ? new Color(255, 246, 226) : new Color(18, 0, 0));
        g.fillRect(0, 0, width, height);
        g.dispose();
    }
}
