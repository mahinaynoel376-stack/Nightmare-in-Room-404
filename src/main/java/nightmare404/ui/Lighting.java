package nightmare404.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.RadialGradientPaint;
import java.util.Random;

import nightmare404.main.Settings;
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

    private static final int[] NIGHT = {6, 8, 18};   // The dark is a little blue

    // The hallway lights' flicker (see hallwayDarkness)
    private static final Random RANDOM = new Random();
    private static int burstLeft;        // Frames left in the burst of flickering going on now (0 = none)
    private static boolean lightsOff;    // During a burst: the lights are out right now
    private static long lastTicks = -1;  // The frame the flicker was last worked out for

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

    // How dark the hallway is at this frame. The lights are steady most of the time. About once every
    // 5 seconds (a 1 in 300 chance each frame) a burst starts, 0.2-0.7 seconds long, where every 3
    // frames the lights go off or come on at random. (Worked out once a frame, however often it's drawn.)
    public static float hallwayDarkness(long ticks) {
        if (!Settings.flicker) return HALLWAY_LIGHTS_ON;   // Turned off on the main menu's Options
        if (ticks != lastTicks) {
            lastTicks = ticks;
            if (burstLeft > 0) {
                burstLeft--;
                if (ticks % 3 == 0) lightsOff = RANDOM.nextBoolean();
            } else {
                lightsOff = false;
                if (RANDOM.nextInt(300) == 0) burstLeft = 12 + RANDOM.nextInt(31);
            }
        }
        if (burstLeft > 0 && lightsOff) return HALLWAY_LIGHTS_OFF;
        return HALLWAY_LIGHTS_ON;
    }

    private static Color night(float darkness) {
        return new Color(NIGHT[0], NIGHT[1], NIGHT[2], Math.round(darkness * 255));
    }
}
