package nightmare404.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import nightmare404.main.GamePanel;
import nightmare404.world.Room;

// Where the player is, in the top-left corner, as an old brass plaque like the ones on the school's
// doors: the room's name engraved on it, and the floor (or "Outside") under it, with a screw at
// each end. Walking into another room, the new plaque slides down into place.
public class LocationSign {
    private static final Font NAME = new Font(Font.SERIF, Font.BOLD, 17);
    private static final Font FLOOR = new Font(Font.SANS_SERIF, Font.BOLD, 10);
    private static final Color BRASS = new Color(164, 132, 70);
    private static final Color BRASS_LIGHT = new Color(214, 184, 112);   // The bevel, catching the light
    private static final Color BRASS_DARK = new Color(104, 80, 38);      // ...and in shadow
    private static final Color EDGE = new Color(36, 26, 12);
    private static final Color ENGRAVED = new Color(44, 30, 14);         // The letters, cut into the brass
    private static final Color TARNISH = new Color(70, 96, 70, 70);      // Green tarnish creeping in

    private static final int X = 14, Y = 10, H = 44;
    private static final int SLIDE = 18;            // Frames the plaque takes to slide into place

    private final GamePanel gp;
    private Room shown;                             // The room on the plaque
    private long arrived;                           // When the player got there (gp.ticks)

    public LocationSign(GamePanel gp) {
        this.gp = gp;
    }

    public void draw(Graphics2D g2d) {
        Room room = gp.currentRoom;
        if (room != shown) {                        // Somewhere new: the plaque slides in again
            shown = room;
            arrived = gp.ticks;
        }
        String name = room.name.toUpperCase();
        String floor = room.outdoors ? "OUTSIDE" : "FLOOR " + room.floor.number;

        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(NAME);
        int w = g.getFontMetrics().stringWidth(name) + 56;

        // Sliding in: from above the screen down to its place, slowing at the end
        float t = Math.min(1f, (gp.ticks - arrived) / (float) SLIDE);
        float ease = 1 - (1 - t) * (1 - t);
        int y = Math.round(Y - (1 - ease) * (H + Y + 6));

        // The plaque: a shadow, the brass with a bevelled edge, and a little tarnish in its corners
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(X + 3, y + 3, w, H, 10, 10);
        g.setColor(BRASS_DARK);
        g.fillRoundRect(X, y, w, H, 10, 10);
        g.setColor(BRASS_LIGHT);
        g.fillRoundRect(X, y, w - 2, H - 2, 10, 10);
        g.setColor(BRASS);
        g.fillRoundRect(X + 2, y + 2, w - 4, H - 4, 8, 8);
        g.setColor(TARNISH);
        g.fillOval(X + w - 22, y + H - 14, 18, 12);
        g.fillOval(X + 4, y + 4, 14, 9);
        g.setColor(EDGE);
        g.drawRoundRect(X, y, w, H, 10, 10);
        g.setColor(BRASS_DARK);                     // An engraved line round the inside
        g.drawRoundRect(X + 5, y + 5, w - 10, H - 10, 6, 6);

        // A screw at each end
        for (int sx : new int[] {X + 13, X + w - 13}) {
            int sy = y + H / 2;
            g.setColor(BRASS_DARK);
            g.fillOval(sx - 4, sy - 4, 8, 8);
            g.setColor(BRASS_LIGHT);
            g.fillOval(sx - 3, sy - 3, 6, 6);
            g.setColor(EDGE);
            g.drawLine(sx - 2, sy + 2, sx + 2, sy - 2);   // Its slot
        }

        // The writing, engraved: a light line under each letter makes it look cut in
        int cx = X + w / 2;
        engrave(g, name, NAME, cx, y + 23);
        engrave(g, floor, FLOOR, cx, y + 36);
        g.dispose();
    }

    private static void engrave(Graphics2D g, String text, Font font, int centreX, int baseline) {
        g.setFont(font);
        int x = centreX - g.getFontMetrics().stringWidth(text) / 2;
        g.setColor(BRASS_LIGHT);
        g.drawString(text, x + 1, baseline + 1);
        g.setColor(ENGRAVED);
        g.drawString(text, x, baseline);
    }
}
