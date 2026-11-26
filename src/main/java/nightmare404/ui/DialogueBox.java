package nightmare404.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import nightmare404.battle.BattleScreen;
import nightmare404.story.Line;
import nightmare404.story.Line.Style;

// The dialogue box: a box just above the bottom bar that shows a conversation one Line at a time.
// While it's open the world waits (see GamePanel.update).
//
// A conversation is a QUEUE of lines: the box shows the line at the front, and pressing E, Space or
// Enter takes it off (dequeues it) and shows the next. The text types itself out; pressing while it's
// still typing shows the whole line at once. When the queue is empty the box closes and runs the
// conversation's `then` (for example: a boss's last word, then its battle starts).
//
// The game loop (its own thread) calls update() and Swing calls draw(), so the methods that touch the
// queue are synchronized: only one thread at a time can be inside them.
public class DialogueBox {
    private static final double CHARS_PER_TICK = 1.5;   // Typing speed: 90 letters a second
    private static final int PRESS_DELAY = 10;          // Ticks before a new conversation takes a key press
    private static final int X = 16, W = 768, H = 132;  // The box (its y is just above the bottom bar)
    private static final int PORTRAIT = 100;            // The face beside the text

    private static final Color BOX = new Color(8, 6, 6, 235);
    private static final Color BORDER = new Color(130, 18, 18);
    private static final Color PALE = new Color(206, 200, 184);
    private static final Color GOLD = new Color(232, 184, 70);
    private static final Color PAPER = new Color(226, 218, 196);
    private static final Color PAPER_LINE = new Color(150, 170, 210);
    private static final Color INK = new Color(34, 26, 20);
    private static final Font TEXT = new Font(Font.SERIF, Font.PLAIN, 18);
    private static final Font THOUGHT = new Font(Font.SERIF, Font.ITALIC, 18);
    private static final Font NAME = new Font(Font.SERIF, Font.BOLD, 15);
    private static final Font PROMPT = new Font(Font.SANS_SERIF, Font.BOLD, 12);

    private final ArrayDeque<Line> queue = new ArrayDeque<>();
    private Line current;           // The line on show, or null when the box is closed
    private double shown;           // How many of its letters have been typed out so far
    private int wait;               // Ticks left before a key press counts
    private long ticks;             // For the blinking arrow
    private Runnable then;          // What happens when the conversation ends

    // With no screen (the checks and debug tools run headless) nobody can press a key, so every
    // conversation is skipped at once: its lines are still logged, and its `then` still runs.
    public boolean autoSkip = GraphicsEnvironment.isHeadless();
    private final List<Line> log = new ArrayList<>();   // Every line ever opened, oldest first (for the checks)

    // Starts a conversation (or adds to the one on show). `then` runs when it's over; it may be null.
    public void open(List<Line> lines, Runnable then) {
        synchronized (this) {
            log.addAll(lines);
            if (!autoSkip && !lines.isEmpty()) {
                queue.addAll(lines);
                Runnable before = this.then;
                this.then = before == null ? then : then == null ? before : () -> { before.run(); then.run(); };
                if (current == null) {
                    next();
                    wait = PRESS_DELAY;
                }
                return;
            }
        }
        if (then != null) then.run();   // Nothing to show: carry straight on
    }

    public synchronized boolean isOpen() {
        return current != null;
    }

    // One frame: type out more of the line, and act on a key press (pressed = E/Space/Enter went down
    // since the last frame)
    public void update(boolean pressed) {
        Runnable finished = null;
        synchronized (this) {
            if (current == null) return;
            ticks++;
            if (wait > 0) {
                wait--;
                pressed = false;
            }
            int length = current.text().length();
            if (shown < length) {
                shown = pressed ? length : Math.min(length, shown + CHARS_PER_TICK);
                return;
            }
            if (!pressed) return;
            next();
            if (current == null) {
                finished = then;
                then = null;
            }
        }
        if (finished != null) finished.run();   // Outside the lock: it may start a battle or another conversation
    }

    private void next() {
        current = queue.poll();
        shown = 0;
    }

    // How many lines have been opened so far (for the checks)
    public synchronized int linesSaid() {
        return log.size();
    }

    // True if any line ever opened contains this text (for the checks)
    public synchronized boolean said(String text) {
        for (Line line : log) {
            if (line.text().contains(text)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------------------------ drawing

    public synchronized void draw(Graphics2D g2d, int screenWidth, int bottomBarTop) {
        if (current == null) return;
        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int y = bottomBarTop - H - 10;
        Style style = current.style();
        boolean paper = style == Style.PAPER;

        // The box: dark with a blood-red edge, or a sheet of lined school paper for something written
        if (paper) {
            g.setColor(PAPER);
            g.fillRect(X, y, W, H);
            g.setColor(PAPER_LINE);
            for (int ly = y + 34; ly < y + H; ly += 24) g.drawLine(X, ly + 4, X + W, ly + 4);
            g.setColor(new Color(190, 60, 60));
            g.drawLine(X + 40, y, X + 40, y + H);
            g.setColor(INK);
            g.setStroke(new BasicStroke(2));
            g.drawRect(X, y, W, H);
        } else {
            g.setColor(BOX);
            g.fillRect(X, y, W, H);
            g.setColor(style == Style.TIP ? GOLD.darker() : BORDER);
            g.setStroke(new BasicStroke(2));
            g.drawRect(X, y, W, H);
        }

        // Who's speaking (or what the paper is), on a tab over the box's top-left corner
        String name = switch (style) {
            case SPEECH, PAPER -> current.speaker();
            case TIP -> "HOW TO PLAY";
            default -> null;
        };
        if (name != null) {
            g.setFont(NAME);
            FontMetrics nm = g.getFontMetrics();
            String label = style == Style.SPEECH ? name.toUpperCase() : name;
            int tabW = nm.stringWidth(label) + 24;
            g.setColor(paper ? INK : BOX);
            g.fillRect(X + 12, y - 22, tabW, 24);
            g.setColor(paper ? PAPER : style == Style.TIP ? GOLD : BORDER);
            g.drawRect(X + 12, y - 22, tabW, 24);
            g.setColor(paper ? PAPER : style == Style.TIP ? GOLD : PALE);
            g.drawString(label, X + 24, y - 5);
        }

        // The face, if the speaker has one
        int textX = X + (paper ? 56 : 20);
        BufferedImage face = style == Style.SPEECH && current.portrait() != null
                ? portrait(current.portrait()) : null;
        if (face != null) {
            int px = X + 14, py = y + (H - PORTRAIT) / 2;
            g.setColor(new Color(24, 20, 20));
            g.fillRect(px, py, PORTRAIT, PORTRAIT);
            drawFitted(g, face, px + 4, py + 4, PORTRAIT - 8);
            g.setColor(BORDER);
            g.drawRect(px, py, PORTRAIT, PORTRAIT);
            textX = px + PORTRAIT + 18;
        }

        // The text, wrapped to the box, typed out so far
        g.setFont(style == Style.THOUGHT ? THOUGHT : TEXT);
        g.setColor(paper ? INK : style == Style.TIP ? GOLD : PALE);
        FontMetrics fm = g.getFontMetrics();
        String text = current.text();
        if (style == Style.THOUGHT) text = "(" + text + ")";
        int visible = shown >= current.text().length() ? text.length()
                : (int) shown + (style == Style.THOUGHT ? 1 : 0);
        int lineY = y + 34;
        for (String line : wrap(text, fm, X + W - 24 - textX)) {
            if (visible <= 0) break;
            g.drawString(line.substring(0, Math.min(line.length(), visible)), textX, lineY);
            visible -= line.length() + 1;
            lineY += 24;
        }

        // A blinking arrow once the whole line is out: press to go on
        if (shown >= current.text().length() && (ticks / 20) % 2 == 0) {
            g.setColor(paper ? INK : PALE);
            int ax = X + W - 26, ay = y + H - 22;
            g.fillPolygon(new int[] {ax, ax + 12, ax + 6}, new int[] {ay, ay, ay + 8}, 3);
            g.setFont(PROMPT);
            g.drawString("E", ax - 14, ay + 9);
        }
        g.dispose();
    }

    // The speaker's face: their battle sprite, or for Flor the head of the walking sprite
    private static BufferedImage portrait(String key) {
        BufferedImage img = BattleScreen.Sprites.get(key, false);
        if (img != null && key.equals(Line.FLOR_FACE)) img = img.getSubimage(0, 0, img.getWidth(), img.getHeight() / 2);
        return img;
    }

    // Draws the image as big as fits in a size x size square, centred, pixel for pixel if it's small
    private static void drawFitted(Graphics2D g, BufferedImage img, int x, int y, int size) {
        double fit = Math.min((double) size / img.getWidth(), (double) size / img.getHeight());
        int w = (int) Math.round(img.getWidth() * fit), h = (int) Math.round(img.getHeight() * fit);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, img.getHeight() <= 128
                ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(img, x + (size - w) / 2, y + (size - h) / 2, w, h, null);
        g2.dispose();
    }

    // Splits text into lines no wider than width pixels, breaking between words
    static List<String> wrap(String text, FontMetrics fm, int width) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String trial = line.length() == 0 ? word : line + " " + word;
                if (fm.stringWidth(trial) > width && line.length() > 0) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(trial);
                }
            }
            lines.add(line.toString());
        }
        return lines;
    }
}
