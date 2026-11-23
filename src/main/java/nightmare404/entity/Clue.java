package nightmare404.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.List;

import nightmare404.story.Line;
import nightmare404.world.Room;

// A clue lying on the floor of a side room: a torn logbook page, a checkout card, a lab report...
// Walking over it picks it up (see GamePanel.update): the player reads it, gets its supplies, and the
// floor's boss starts its fight weaker (see nightmare404.story.Quest). Where they are: story.Quests.
//
// Whether it's been found is the player's business (Player.clues holds the ids), not the clue's: so
// giving up after a lost battle, which puts back the player's clues, makes it lie here again.
public class Clue {
    public final String id;             // Unique, e.g. "4F-1"
    public final String title;          // What it is: "Torn logbook page"
    public final Room room;
    public final int x, y;              // The top-left of the tile it lies on, in pixels
    public final List<Line> pages;      // What the player reads when they pick it up
    public final Loot supplies;         // What else they find with it (its key is always null)

    public Clue(String id, String title, Room room, int col, int row, int ts, List<Line> pages, Loot supplies) {
        this.id = id;
        this.title = title;
        this.room = room;
        this.x = col * ts;
        this.y = row * ts;
        this.pages = pages;
        this.supplies = supplies;
    }

    // Where standing picks it up: the middle of its tile
    public Rectangle area() {
        return new Rectangle(x + 6, y + 8, 20, 20);
    }

    // A scrap of paper, lifting and settling, with a pale glow (the bosses' keys glow gold)
    public void draw(Graphics2D g, long ticks) {
        int cx = x + 16, cy = y + 20;
        float pulse = (float) (0.5 + 0.5 * Math.sin(ticks / 14.0 + x));
        g.setColor(new Color(220, 226, 255, (int) (50 + 70 * pulse)));
        g.fillOval(cx - 15, cy - 9, 30, 18);
        Graphics2D p = (Graphics2D) g.create();
        p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        p.translate(cx, cy - (int) Math.round(2 * pulse));
        p.rotate(Math.toRadians(-12 + 6 * pulse));
        p.setColor(new Color(40, 34, 26));
        p.fillRect(-9, -7, 18, 14);
        p.setColor(new Color(226, 218, 196));
        p.fillRect(-8, -6, 16, 12);
        p.setColor(new Color(120, 140, 190));
        for (int line = -3; line <= 3; line += 3) p.drawLine(-6, line, 6, line);
        p.setColor(new Color(150, 20, 20));
        p.drawLine(-5, -6, -5, 5);
        p.dispose();
    }
}
