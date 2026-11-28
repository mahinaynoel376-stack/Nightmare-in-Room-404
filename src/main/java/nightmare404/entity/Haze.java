package nightmare404.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import nightmare404.tile.Staircase;
import nightmare404.world.Room;

// A thick, cold haze over a stairwell's way down: Floor 3's east stairs, while the Whisperer is still
// there (see Encounters). The player can't walk into it, and walking up to it, they hear the library
// calling them back (see Director.hazeCalls). Once the floor's boss is beaten, it's gone.
public class Haze {
    public final Room room;
    private final Rectangle area;               // In pixels: the landing and the flight down
    private boolean touchedBefore;              // The player was up against it last time we looked

    public Haze(Room room) {
        this.room = room;
        Staircase stairs = room.map.stairs;
        Rectangle landing = stairs.downZone;
        area = new Rectangle(landing.x, landing.y, landing.width, stairs.downFoot.y - landing.y);
    }

    // True while the floor's boss is still there
    public boolean active() {
        return room.floor.boss != null && !room.floor.boss.defeated;
    }

    // True if this box (the player's hitbox) would walk into it
    public boolean blocks(Rectangle box) {
        return active() && area.intersects(box);
    }

    // True the moment the player walks up to it (once, until they step away again)
    public boolean walkedInto(Rectangle box) {
        Rectangle near = new Rectangle(area);
        near.grow(4, 4);
        boolean touching = active() && near.intersects(box);
        boolean walkedUp = touching && !touchedBefore;
        touchedBefore = touching;
        return walkedUp;
    }

    // Grey clouds drifting slowly over the stairs
    public void draw(Graphics2D g, long ticks) {
        if (!active()) return;
        int clouds = 10;
        for (int i = 0; i < clouds; i++) {
            int w = 70 + (i * 13) % 30, h = 40 + (i * 7) % 20;
            int cx = area.x + area.width / 2 + ((i * 23) % 40) - 20 + (int) (Math.sin((ticks + i * 37) / 40.0) * 10);
            int cy = area.y + i * area.height / clouds + (int) (Math.cos((ticks + i * 19) / 50.0) * 6);
            int alpha = 46 + (int) (20 * Math.sin((ticks + i * 11) / 30.0));
            g.setColor(new Color(176, 180, 198, alpha));
            g.fillOval(cx - w / 2, cy - h / 2, w, h);
        }
    }
}
