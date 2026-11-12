package nightmare404.world;

import java.util.ArrayList;
import java.util.List;

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
    public Room door;    // The room behind this hallway screen (or, for a room, the hallway screen in front of it)

    // Where each working door of this screen leads, by its number (TileMap.door(i)): the room on the
    // other side, and which of that room's doors you come out of. Usually all of a screen's doors
    // lead to the same room (door, above), but the hallway screen in the middle of the ground floor
    // also has the school's main entrance, which leads outside. Floor sets these up.
    private final Room[] doorTo;
    private final int[] doorToNumber;

    // false: the camera follows the player and scrolls into the neighbouring screens (the hallway).
    // true:  the camera stays still; walking off the edge cuts to the next screen (the library).
    public boolean fixedScreen = false;

    // true for places outside the building (the quadrangle): the screen says "Outside", not a floor number
    public boolean outdoors = false;

    Room(String name, Floor floor, TileMap map) {
        this.name = name;
        this.floor = floor;
        this.map = map;
        doorTo = new Room[map.doorCount()];
        doorToNumber = new int[map.doorCount()];
    }

    // Links door number mine of this screen with door number theirs of other, both ways
    void linkDoor(int mine, Room other, int theirs) {
        doorTo[mine] = other;
        doorToNumber[mine] = theirs;
        other.doorTo[theirs] = this;
        other.doorToNumber[theirs] = mine;
    }

    // The room that door number i leads to, or null if it doesn't lead anywhere
    public Room doorLeadsTo(int i) {
        return doorTo[i];
    }

    // Which door of that room you come out of
    public int doorArrivesAt(int i) {
        return doorToNumber[i];
    }

    // Every room this screen's doors lead to, each one once
    public List<Room> linkedRooms() {
        List<Room> rooms = new ArrayList<>();
        for (Room r : doorTo) {
            if (r != null && !rooms.contains(r)) rooms.add(r);
        }
        return rooms;
    }

    @Override
    public String toString() {
        return name;
    }
}
