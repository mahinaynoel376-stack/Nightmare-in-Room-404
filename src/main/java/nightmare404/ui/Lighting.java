package nightmare404.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.RadialGradientPaint;

import nightmare404.world.Room;

// How light it is, drawn as a layer of darkness over the world (the map, the monsters and the
// player), under the room name, the notices and the bottom bar.
//
//   The hallways (and the stairwells at their ends) are lit by old ceiling lights that FLICKER:
//   steady most of the time, then now and then a burst where they cut out and come back.
//   The rooms behind the doors are DIMMER: darkest at the edges of the view, a little lighter
//   around the player, as if their eyes were getting used to the dark.
//   Outside (the quadrangle) is left as it is.
//
// Change the numbers below to make it lighter or darker (0 = no darkness, 1 = black).
public final class Lighting {
    private static final float HALLWAY_LIGHTS_ON = 0.12f;    // The hallway, lights on
    private static final float HALLWAY_LIGHTS_OFF = 0.62f;   // ...and when they cut out
    private static final float ROOM_NEAR = 0.30f;            // A room, around the player...
    private static final float ROOM_FAR = 0.60f;             // ...and at the edges of the view
    private static final float ROOM_LIGHT_RADIUS = 460;      // How far the darkness takes to build up, in pixels

    private static final int STRETCH = 150;      // A flicker can come once in every stretch of 150 frames (2.5 s)
    private static final int[] NIGHT = {6, 8, 18};   // The dark is a little blue

    private Lighting() { }

    // Darkens a view `width` x `height` pixels for the player in `room`, standing at (playerX, playerY)
    // on screen. ticks: frames since the game began (the flicker follows them).
    public static void draw(Graphics2D g2d, Room room, int playerX, int playerY, long ticks, int width, int height) {
        if (room.outdoors) return;
        Graphics2D g = (Graphics2D) g2d.create();
        if (room.floor.indexOf(room) >= 0) {        // A hallway screen (or a stairwell): in the hallway list
            g.setColor(night(hallwayDarkness(ticks)));
            g.fillRect(0, 0, width, height);
        } else {
            g.setPaint(new RadialGradientPaint(playerX, playerY, ROOM_LIGHT_RADIUS, new float[] {0f, 0.2f, 1f},
                    new Color[] {night(ROOM_NEAR), night(ROOM_NEAR), night(ROOM_FAR)}, CycleMethod.NO_CYCLE));
            g.fillRect(0, 0, width, height);
        }
        g.dispose();
    }

    // How dark the hallway is at this frame. In about half the 2.5-second stretches there's a burst,
    // at a random moment and 0.2-0.7 seconds long, where every few frames the lights are off or on
    // at random. The rest of the time they're steady. (The same frame always gives the same answer.)
    public static float hallwayDarkness(long ticks) {
        long stretch = mix(ticks / STRETCH);
        boolean burstHere = (stretch & 1) == 0;
        int start = (int) Math.floorMod(stretch >> 8, (long) STRETCH - 45);
        int length = 12 + (int) Math.floorMod(stretch >> 24, 31L);
        int at = (int) Math.floorMod(ticks, (long) STRETCH);
        boolean inBurst = burstHere && at >= start && at < start + length;
        if (inBurst && (mix(ticks / 3) & 1) == 0) return HALLWAY_LIGHTS_OFF;   // Off, for 3 frames
        return HALLWAY_LIGHTS_ON;
    }

    private static Color night(float darkness) {
        return new Color(NIGHT[0], NIGHT[1], NIGHT[2], Math.round(darkness * 255));
    }

    // Scrambles a number (so the flicker looks random but needs nothing stored)
    private static long mix(long x) {
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return x;
    }
}
