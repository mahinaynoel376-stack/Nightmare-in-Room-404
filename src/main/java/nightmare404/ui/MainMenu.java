package nightmare404.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

import nightmare404.main.Settings;

/** Landscape main menu: bloody 404 door wrapped in NO ENTRY tape. Pure Java2D, no image files.
 *  It's drawn on a 1280 x 720 stage, scaled to fit the window. In the game's 800 x 608 window the
 *  stage is wider than the window is tall, so the hallway carries on above and below it.
 *  Continue and Journal are greyed out until there's a game to go back to (see Main). */
public class MainMenu extends JPanel {
    private static final int STAGE_W = 1280, STAGE_H = 720;
    private static final Color BLOOD = new Color(156, 15, 23), TAPE = new Color(242, 194, 0);
    private static final Color INK = new Color(207, 216, 210), DIM = new Color(111, 127, 120),
            GREYED = new Color(61, 74, 69);
    private static final String[] ITEMS = {"Start Game", "Continue", "Journal", "Options", "Quit"};
    private static final String[] HINTS = {"Wake up in Room 404", "Return to where you left off",
            "The clues you've found", "Turn timer, flickering lights", "Close the game"};
    private static final int START = 0, CONTINUE = 1, JOURNAL = 2, OPTIONS = 3, QUIT = 4;

    private final Runnable start, cont, journal, quit;
    private boolean inProgress;            // There's a game to continue
    private boolean options;               // The Options list is showing instead of the main one
    private int hover = -1, sel = 0, flicker = 0;
    private float drip = 0f;               // 0..1, blood runs down once
    private final Rectangle[] hit = new Rectangle[ITEMS.length];   // Each line's box, on the stage
    private double scale = 1, offX, offY;  // How the stage was fitted into the window when last drawn
    private final Timer timer;

    public MainMenu(Runnable start, Runnable cont, Runnable journal, Runnable quit) {
        this.start = start;
        this.cont = cont;
        this.journal = journal;
        this.quit = quit;
        setPreferredSize(new Dimension(STAGE_W, STAGE_H));
        setBackground(Color.BLACK);
        setFocusable(true);
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                hover = lineAt(e.getPoint());
                if (hover >= 0 && enabled(hover)) sel = hover;
            }
        });
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                int line = lineAt(e.getPoint());
                if (line >= 0) choose(line);
            }
        });
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if (k == KeyEvent.VK_W || k == KeyEvent.VK_UP) move(-1);
                if (k == KeyEvent.VK_S || k == KeyEvent.VK_DOWN) move(1);
                if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE || k == KeyEvent.VK_E) choose(sel);
                if (k == KeyEvent.VK_ESCAPE && options) showOptions(false);
            }
        });
        timer = new Timer(40, e -> {
            drip = Math.min(1f, drip + 0.004f);
            flicker = (Settings.flicker && Math.random() < 0.015) ? 6 : Math.max(0, flicker - 1);
            repaint();
        });
    }

    /** Whether there's a game in progress, so Continue and Journal can be chosen. Called each time
     *  the menu is shown; Continue starts out chosen if there is one, Start Game if not. */
    public void setGameInProgress(boolean inProgress) {
        this.inProgress = inProgress;
        options = false;
        hover = -1;
        sel = inProgress ? CONTINUE : START;
    }

    // The animation only runs while the menu is on screen
    @Override public void addNotify() { super.addNotify(); timer.start(); }
    @Override public void removeNotify() { timer.stop(); super.removeNotify(); }

    // ---------------------------------------------------------------- choosing

    private String[] lines() {
        if (!options) return ITEMS;
        return new String[] {"Turn timer: " + (Settings.turnTimer ? "On" : "Off"),
                "Flicker: " + (Settings.flicker ? "On" : "Off"), "Back"};
    }

    private String hint(int i) {
        if (!options) return !enabled(i) ? "Start a game first" : HINTS[i];
        return switch (i) {
            case 0 -> "A clock on each of your turns in the word battles";
            case 1 -> "Flickering lights in the hallways and the battles";
            default -> "Back to the menu";
        };
    }

    private boolean enabled(int i) {
        return options || inProgress || (i != CONTINUE && i != JOURNAL);
    }

    // W / S: the next line up or down, skipping any that are greyed out
    private void move(int step) {
        int n = lines().length;
        for (int k = 0; k < n; k++) {
            sel = Math.floorMod(sel + step, n);
            if (enabled(sel)) return;
        }
    }

    private void choose(int i) {
        if (!enabled(i)) return;
        sel = i;
        if (options) {
            switch (i) {
                case 0 -> Settings.turnTimer = !Settings.turnTimer;
                case 1 -> Settings.flicker = !Settings.flicker;
                default -> showOptions(false);
            }
            return;
        }
        switch (i) {
            case START -> start.run();
            case CONTINUE -> cont.run();
            case JOURNAL -> journal.run();
            case OPTIONS -> showOptions(true);
            case QUIT -> quit.run();
            default -> { }
        }
    }

    private void showOptions(boolean show) {
        options = show;
        hover = -1;
        sel = show ? 0 : OPTIONS;
    }

    // Which line is under the mouse (-1 for none). The lines' boxes are on the stage, so the mouse's
    // place in the window is turned into a place on the stage first.
    private int lineAt(Point p) {
        double sx = (p.x - offX) / scale, sy = (p.y - offY) / scale;
        for (int i = 0; i < lines().length; i++) if (hit[i] != null && hit[i].contains(sx, sy)) return i;
        return -1;
    }

    // ---------------------------------------------------------------- drawing

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        scale = Math.min(getWidth() / (double) STAGE_W, getHeight() / (double) STAGE_H);   // scale to any window
        offX = (getWidth() - STAGE_W * scale) / 2;
        offY = (getHeight() - STAGE_H * scale) / 2;
        g.translate(offX, offY);
        g.scale(scale, scale);
        // All of the window, on the stage: more than 1280 x 720 if the window's a different shape
        Rectangle view = new Rectangle((int) Math.floor(-offX / scale), (int) Math.floor(-offY / scale),
                (int) Math.ceil(getWidth() / scale) + 2, (int) Math.ceil(getHeight() / scale) + 2);
        // The same layers as the HTML design: the hallway and the door, the flicker, the vignette,
        // and the words on top, so the dark never swallows them
        drawHallway(g, view); drawDoor(g);
        if (flicker > 0) { g.setColor(new Color(0, 0, 0, 120)); g.fill(view); }
        g.setPaint(new RadialGradientPaint(new Point2D.Float(900, 360), 760, new float[]{0.35f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 225)}));
        g.fill(view);
        drawTitle(g); drawMenu(g); drawFooter(g, view);
        g.dispose();
    }

    private void drawHallway(Graphics2D g, Rectangle view) {
        int top = view.y, bottom = view.y + view.height, left = view.x, right = view.x + view.width;
        g.setPaint(new GradientPaint(0, 0, new Color(10, 18, 16), 1280, 0, new Color(15, 26, 23)));
        g.fill(view);
        g.setColor(new Color(26, 40, 36));
        for (int x = Math.floorDiv(left, 66) * 66; x < right; x += 66) g.fillRect(x, top, 62, 560 - top);   // lockers
        g.setColor(new Color(0, 0, 0, 150));
        for (int y = 140 + Math.floorDiv(top - 140, 140) * 140; y < 560; y += 140) if (y > top) g.fillRect(left, y, view.width, 4);
        g.setPaint(new GradientPaint(0, 560, new Color(13, 21, 19), 0, Math.max(720, bottom), new Color(3, 5, 5)));
        g.fillRect(left, 560, view.width, bottom - 560);                          // floor
        g.setColor(new Color(0, 0, 0)); g.fillRect(left, 558, view.width, 3);
        // the pale light on the wall behind the door
        g.setPaint(new RadialGradientPaint(new Point2D.Float(922, 396), 480, new float[]{0f, 1f},
                new Color[]{new Color(120, 150, 130, 64), new Color(120, 150, 130, 0)}));
        g.fill(view);
    }

    private void drawTitle(Graphics2D g) {
        g.setFont(new Font("Serif", Font.ITALIC, 22)); g.setColor(DIM);
        g.drawString("Top floor. Locked from the outside.", 80, 90);
        g.setFont(new Font("Serif", Font.BOLD, 64));
        g.setColor(Color.BLACK); g.drawString("Nightmare in", 84, 168);
        g.setColor(new Color(230, 236, 231)); g.drawString("Nightmare in", 80, 164);
        g.setFont(new Font("Serif", Font.BOLD, 92));
        g.setColor(Color.BLACK); g.drawString("Room 404", 83, 262);
        g.setColor(BLOOD); g.drawString("Room 404", 80, 259);
    }

    private void drawMenu(Graphics2D g) {
        String[] lines = lines();
        java.util.Arrays.fill(hit, null);
        if (options) {
            g.setFont(new Font("Monospaced", Font.BOLD, 20)); g.setColor(TAPE);
            g.drawString("OPTIONS", 80, 310);
        }
        for (int i = 0; i < lines.length; i++) {
            int y = 352 + i * 56;
            boolean on = i == sel && enabled(i);
            hit[i] = new Rectangle(50, y - 34, 470, 54);
            if (on) { g.setColor(BLOOD); g.fillRect(56, y - 14, 26, 5); }
            g.setFont(new Font("Monospaced", Font.PLAIN, 32));
            g.setColor(on ? Color.WHITE : enabled(i) ? INK : GREYED);
            g.drawString(lines[i], on ? 96 : 80, y);
            if (i == sel) {                                     // what the chosen line does, under it
                g.setFont(new Font("Monospaced", Font.PLAIN, 17)); g.setColor(DIM);
                g.drawString(hint(i), on ? 98 : 82, y + 22);
            }
        }
    }

    // The keys, along the bottom of the window
    private void drawFooter(Graphics2D g, Rectangle view) {
        int bottom = view.y + view.height;
        g.setFont(new Font("Monospaced", Font.PLAIN, 20));
        int x = 80, y = bottom - 74;
        String[][] keys = {{"W", "A", "S", "D", "walk"}, {"Shift", "run"}, {"J", "journal"}, {"Esc", "menu"}};
        FontMetrics fm = g.getFontMetrics();
        for (String[] group : keys) {
            for (int i = 0; i < group.length - 1; i++) {        // each key in a little box
                int w = fm.stringWidth(group[i]) + 14;
                g.setColor(new Color(58, 73, 68)); g.setStroke(new BasicStroke(1.6f));
                g.drawRoundRect(x, y - 19, w, 26, 6, 6);
                g.setColor(INK); g.drawString(group[i], x + 7, y);
                x += w + 5;
            }
            g.setColor(DIM); g.drawString(group[group.length - 1], x + 4, y);
            x += fm.stringWidth(group[group.length - 1]) + 30;
        }
        g.drawString("Find every clue for the good ending.", 80, y + 34);
    }

    private void drawDoor(Graphics2D g) {
        int x = 800, y = 60, w = 300, h = 600;
        AffineTransform old = g.getTransform();                                              // red glow beneath:
        g.translate(x + w / 2.0, y + h + 4); g.scale(1, 0.17);                               // a circle, squashed
        g.setPaint(new RadialGradientPaint(new Point2D.Float(0, 0), 250, new float[]{0f, 1f},
                new Color[]{new Color(200, 40, 40, 80), new Color(200, 40, 40, 0)}));
        g.fill(new Ellipse2D.Float(-250, -250, 500, 500));
        g.setTransform(old);
        g.setPaint(new Color(0, 0, 0, 140)); g.fillOval(x - 60, y + h - 20, w + 120, 50);   // shadow
        g.setColor(new Color(10, 15, 13)); g.fillRect(x - 14, y - 14, w + 28, h + 14);       // frame
        g.setPaint(new GradientPaint(x, 0, new Color(43, 58, 51), x + w, 0, new Color(16, 24, 20)));
        g.fillRect(x, y, w, h);
        g.setColor(new Color(10, 17, 14)); g.setStroke(new BasicStroke(5));
        g.drawRect(x + 36, y + 40, w - 72, 190); g.drawRect(x + 36, y + 270, w - 72, 290);
        g.setColor(new Color(207, 216, 210)); g.fillRoundRect(x + 95, y + 56, 110, 26, 4, 4);   // plate
        g.setColor(new Color(26, 26, 26)); g.setFont(new Font("Monospaced", Font.BOLD, 16));
        g.drawString("ROOM 404", x + 112, y + 75);
        // blood 404
        g.setFont(new Font("Serif", Font.BOLD, 150));
        FontMetrics fm = g.getFontMetrics();
        int tx = x + (w - fm.stringWidth("404")) / 2, ty = y + 215;
        g.setColor(new Color(92, 7, 12)); g.drawString("404", tx + 3, ty + 3);
        g.setColor(BLOOD); g.drawString("404", tx, ty);
        int[][] d = {{62, 150}, {118, 80}, {166, 190}, {218, 110}};         // x offset, length
        for (int[] r : d) g.fill(new RoundRectangle2D.Float(x + r[0], ty - 8, 9, r[1] * drip, 9, 9));
        g.setColor(new Color(150, 156, 140)); g.fillOval(x + w - 56, y + 300, 22, 22);       // knob
        // tape bands (rotated around door centre)
        drawTape(g, x + w / 2, y + 250, -24, "NO ENTRY   DO NOT CROSS   NO ENTRY   DO NOT CROSS   ");
        drawTape(g, x + w / 2, y + 400, 21, "DO NOT CROSS   NO ENTRY   DO NOT CROSS   NO ENTRY   ");
        drawTape(g, x + w / 2, y + 320, -3, "NO ENTRY   NO ENTRY   NO ENTRY   NO ENTRY   NO ENTRY   ");
        drawSign(g, x + 190, y + 360);
    }

    private void drawTape(Graphics2D g, int cx, int cy, double deg, String txt) {
        AffineTransform old = g.getTransform();
        Shape oldClip = g.getClip();
        g.translate(cx, cy); g.rotate(Math.toRadians(deg));
        g.setColor(new Color(0, 0, 0, 120)); g.fillRect(-300, 6, 600, 44);
        g.setColor(TAPE); g.fillRect(-300, -2, 600, 44);
        g.setColor(new Color(22, 19, 10)); g.setFont(new Font("Monospaced", Font.BOLD, 24));
        g.clipRect(-300, -2, 600, 44); g.drawString(txt + txt, -290, 30);
        g.setTransform(old); g.setClip(oldClip);   // (the clip is put back in the space it was taken in)
    }

    private void drawSign(Graphics2D g, int x, int y) {
        AffineTransform old = g.getTransform();
        g.rotate(Math.sin(System.currentTimeMillis() / 800.0) * 0.08, x + 45, y);   // swings
        g.setColor(new Color(34, 34, 34)); g.setStroke(new BasicStroke(3)); g.drawLine(x + 45, y, x + 45, y + 16);
        g.setColor(TAPE); g.fillRoundRect(x, y + 16, 90, 100, 8, 8);
        g.setColor(BLOOD); g.setStroke(new BasicStroke(8));
        g.drawOval(x + 20, y + 30, 50, 50); g.drawLine(x + 28, y + 38, x + 62, y + 72);
        g.setColor(new Color(22, 19, 10)); g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.drawString("NO ENTRY", x + 10, y + 106);
        g.setTransform(old);
    }

    /** Quick test: run this file on its own to preview the menu (in its own 1280 x 720 window). */
    public static void main(String[] a) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Nightmare in Room 404");
            Runnable r = () -> System.out.println("clicked");
            MainMenu menu = new MainMenu(r, r, r, () -> System.exit(0));
            f.setContentPane(menu);
            f.pack(); f.setLocationRelativeTo(null); f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setVisible(true);
            menu.requestFocusInWindow();
        });
    }
}
