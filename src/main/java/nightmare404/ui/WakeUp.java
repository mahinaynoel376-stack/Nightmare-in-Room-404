package nightmare404.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

import nightmare404.main.GamePanel;

// The opening scene, before anything is said: Kai wakes up at his desk in Room 404.
//   The screen is black, and somewhere a clock ticks.
//   His eyes open a crack and fall shut again, then open properly on Room 404, dark at first:
//   Kai asleep on his desk, breathing, a few z's drifting up.
//   He stirs, sits up with his eyes still shut, opens them, looks back over his shoulder at the
//   empty room, then stands up and steps out into the aisle (where the game starts).
// Then the story begins (Story.INTRO, see Director). It can't be skipped.
// The frames of him at the desk are made by tools/art/make_desk_wakeup.py.
public class WakeUp {
    // When each part starts, in frames (60 a second)
    private static final int CRACK = 150;           // Black until here, the clock ticking
    private static final int SHUT = 200;            // His eyes open a crack, and shut again...
    private static final int OPEN = 250;            // ...then open properly
    private static final int WIDE = 340;            // ...all the way
    private static final int LIGHT = 430;           // The room is as light as it gets
    private static final int STIR = 430;            // He stirs
    private static final int SIT = 480;             // Sits up, eyes shut
    private static final int AWAKE = 540;           // Opens his eyes
    private static final int LOOK = 580;            // Looks back over his shoulder
    private static final int LOOK_AHEAD = 640;      // Looks ahead again
    private static final int STAND = 670;           // Stands up beside his chair
    private static final int STEP = 690;            // Steps into the aisle
    private static final int TURN = 712;            // Turns round to face the room
    public static final int LENGTH = 730;           // The end (about 12 seconds)

    // His chair in Room 404 (the desk is the next tile to the right). The frames are 2 x 2 tiles:
    // the chair and desk, and the row above them.
    private static final int CHAIR_COL = 12, CHAIR_ROW = 11;

    private static final Font TICK = new Font(Font.SERIF, Font.ITALIC, 18);
    private static final Font ZZZ = new Font(Font.SERIF, Font.BOLD | Font.ITALIC, 12);

    private final GamePanel gp;
    private final BufferedImage sleep1, sleep2, stir, drowsy, awake, lookBack;   // At the desk
    private final BufferedImage standing, stepping, facingUp, facingDown;       // Getting up (his walking sprites)
    private int ticks;

    public WakeUp(GamePanel gp) {
        this.gp = gp;
        sleep1 = load("desk_sleep_1");
        sleep2 = load("desk_sleep_2");
        stir = load("desk_stir");
        drowsy = load("desk_drowsy");
        awake = load("desk_awake");
        lookBack = load("desk_look_back");
        standing = load("right_idle");
        stepping = load("up_walk1");
        facingUp = load("up_idle");
        facingDown = load("down_idle");
        // With no screen (the checks and debug tools) nobody would see it, so it's already over
        ticks = GraphicsEnvironment.isHeadless() ? LENGTH : 0;
    }

    private BufferedImage load(String name) {
        String path = "/characters/player/" + name + ".png";
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new IllegalStateException("Missing sprite: " + path);
            return ImageIO.read(is);
        } catch (IOException e) {
            throw new IllegalStateException("Can't read " + path, e);
        }
    }

    // Plays it from the start (the screenshot tools use this, since it's skipped without a screen)
    public void start() {
        ticks = 0;
    }

    public boolean playing() {
        return ticks < LENGTH;
    }

    // One frame
    public void update() {
        ticks++;
    }

    // Kai, at his desk or getting up from it (drawn in the world, in place of the player)
    public void drawKai(Graphics2D g) {
        int ts = gp.tileSize;
        int x = CHAIR_COL * ts, y = (CHAIR_ROW - 1) * ts;
        if (ticks >= STAND) {                       // Up, and out into the aisle (where the player starts)
            BufferedImage image = ticks < STEP ? standing : ticks < STEP + 10 ? stepping : ticks < TURN ? facingUp : facingDown;
            int top = ticks < STEP ? y - 22 : ticks < STEP + 10 ? y - 28 : y - ts;
            g.drawImage(image, x, top, ts, ts * 2, null);
            return;
        }
        BufferedImage image;
        if (ticks < STIR) image = (ticks / 45) % 2 == 0 ? sleep1 : sleep2;   // Asleep, breathing
        else if (ticks < SIT) image = stir;
        else if (ticks < AWAKE) image = drowsy;
        else if (ticks < LOOK || ticks >= LOOK_AHEAD) image = awake;
        else image = lookBack;
        g.drawImage(image, x, y, ts * 2, ts * 2, null);
        if (ticks < STIR) drawZs(g, x + 50, y + 20);
    }

    // A few z's drifting up from him while he sleeps
    private void drawZs(Graphics2D g2d, int x, int y) {
        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(ZZZ);
        for (int i = 0; i < 2; i++) {
            int age = (ticks + i * 45) % 90;        // Each z lasts 1.5 seconds, then starts again
            float alpha = age < 20 ? age / 20f : (90 - age) / 70f;
            g.setComposite(AlphaComposite.SrcOver.derive(alpha * 0.8f));
            g.setColor(new Color(200, 206, 228));
            g.drawString("z", x + age / 6, y - age / 3);
        }
        g.dispose();
    }

    // Over everything: the dark, his eyelids, and the ticking
    public void drawOverlay(Graphics2D g2d, int width, int height) {
        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (ticks < CRACK) {                        // Black, and the clock ticking
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, width, height);
            tick(g, width, height, 20, 75);
            tick(g, width, height, 85, 140);
        } else {
            // How dark the room still looks (his eyes getting used to it)
            float dark = ticks < OPEN ? 0.8f : Math.max(0f, 0.8f * (LIGHT - ticks) / (LIGHT - OPEN));
            g.setColor(new Color(0, 0, 0, Math.round(dark * 255)));
            g.fillRect(0, 0, width, height);
            drawEyelids(g, width, height, eyesOpen());
        }
        g.dispose();
    }

    // "...tick." in the middle of the dark, fading in and out between frames `from` and `to`
    private void tick(Graphics2D g2d, int width, int height, int from, int to) {
        if (ticks < from || ticks >= to) return;
        float t = (ticks - from) / (float) (to - from);
        float alpha = t < 0.3f ? t / 0.3f : (1 - t) / 0.7f;
        Graphics2D g = (Graphics2D) g2d.create();
        g.setComposite(AlphaComposite.SrcOver.derive(alpha * 0.7f));
        g.setFont(TICK);
        g.setColor(new Color(150, 146, 140));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("...tick.", (width - fm.stringWidth("...tick.")) / 2, height / 2);
        g.dispose();
    }

    // How far his eyes are open: 0 = shut, 1 = wide open
    private float eyesOpen() {
        if (ticks < SHUT) {                         // A crack, then shut again
            float t = (ticks - CRACK) / (float) (SHUT - CRACK);
            return 0.25f * (t < 0.5f ? t * 2 : (1 - t) * 2);
        }
        if (ticks < OPEN) return 0;
        if (ticks >= WIDE) return 1;
        float t = (ticks - OPEN) / (float) (WIDE - OPEN);
        return t * t * (3 - 2 * t);                 // Slow at first, slow at the end
    }

    // His eyelids: black from the top and the bottom, with soft edges, leaving a gap in the middle
    private void drawEyelids(Graphics2D g, int width, int height, float open) {
        if (open >= 1) return;
        int soft = 40;                                      // The soft edge
        // How far each lid reaches. Shut, each one's solid black reaches the middle, so no light gets through.
        int lid = Math.round((height / 2f + soft) * (1 - open));
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, lid - soft);
        g.fillRect(0, height - lid + soft, width, lid - soft);
        g.setPaint(new GradientPaint(0, lid - soft, Color.BLACK, 0, lid, new Color(0, 0, 0, 0)));
        g.fillRect(0, lid - soft, width, soft);
        g.setPaint(new GradientPaint(0, height - lid, new Color(0, 0, 0, 0), 0, height - lid + soft, Color.BLACK));
        g.fillRect(0, height - lid, width, soft);
    }
}
