package nightmare404.tile;

import java.awt.Point;
import java.awt.Rectangle;

// The working part of a staircase: which areas take you to another floor, and where you
// stand when you arrive. All positions are in pixels.
//
//          [ downZone ][rail][ upZone ]     <- the landing, split in two
//          [ left     ][rail][ right  ]
//          [ flight   ][rail][ flight ]
//           ^ downFoot         ^ upFoot     <- standing just below each flight
//
// Walk up the LEFT flight onto the landing (downZone) to go down a floor.
// Walk up the RIGHT flight onto the landing (upZone) to go up a floor.
// You arrive at the foot of the flight that leads back where you came from.
public class Staircase {
    public final Rectangle downZone;
    public final Rectangle upZone;
    public final Point downFoot;   // Player (x, y) at the bottom of the left flight
    public final Point upFoot;     // Player (x, y) at the bottom of the right flight

    public Staircase(Rectangle downZone, Rectangle upZone, Point downFoot, Point upFoot) {
        this.downZone = downZone;
        this.upZone = upZone;
        this.downFoot = downFoot;
        this.upFoot = upFoot;
    }
}
