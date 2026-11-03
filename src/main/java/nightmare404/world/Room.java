package nightmare404.world;

import nightmare404.tile.TileMap;

// One screen of the game, and one NODE of the linked list.
//
// Hallway screens are linked to each other with left/right, like a doubly linked list:
//   westStairs <-> hall402 <-> hall403 <-> hall404 <-> eastStairs
// A classroom isn't in that list. It hangs off its hallway screen through the door link,
// and the hallway screen links back to it:
//   room404.door == hall404   and   hall404.door == room404
//
// A null link means "nothing there": walking that way just hits a wall.
public class Room {
    public final String name;
    public final Floor floor;
    public final TileMap map;   // What this screen looks like and where its walls are

    public Room left;    // The screen you reach by walking off the left edge
    public Room right;   // The screen you reach by walking off the right edge
    public Room door;    // The screen on the other side of this screen's doors

    // false: the camera follows the player and scrolls into the neighbouring screens (the hallway).
    // true:  the camera stays still; walking off the edge cuts to the next screen (the library).
    public boolean fixedScreen = false;

    // true for places outside the building (the quadrangle): the screen says "Outside", not a floor number
    public boolean outdoors = false;

    Room(String name, Floor floor, TileMap map) {
        this.name = name;
        this.floor = floor;
        this.map = map;
    }

    @Override
    public String toString() {
        return name;
    }
}
