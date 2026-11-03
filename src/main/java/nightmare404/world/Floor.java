package nightmare404.world;

import nightmare404.tile.TileMap;

// One floor of the school: a doubly linked list of hallway screens, from head (leftmost) to tail (rightmost).
// Rooms hang off the hallway screens through doors (see Room.door). A room bigger than one screen,
// like the library, is its own little left/right list, joined to the hallway only by its door.
//
// Floors are linked to each other the same way, through their stairs:
//   floor4.below == floor3   and   floor3.above == floor4
public class Floor {
    public final int number;
    public Room head;       // Leftmost hallway screen
    public Room tail;       // Rightmost hallway screen
    public int size;        // Number of hallway screens

    public Floor above;     // The floor reached by going up the stairs (null = none yet)
    public Floor below;     // The floor reached by going down the stairs (null = none yet)
    public Room stairs;     // The screen with the working stairs on this floor

    public Floor(int number) {
        this.number = number;
    }

    // Adds a hallway screen to the right end of the list and returns it.
    // Same as adding to the tail of any doubly linked list:
    //   1. the old tail points right to the new node
    //   2. the new node points left to the old tail
    //   3. the new node becomes the tail
    public Room append(String name, TileMap map) {
        Room room = new Room(name, this, map);
        if (head == null) {
            head = room;            // First node: it's both the head and the tail
            tail = room;
        } else {
            tail.right = room;
            room.left = tail;
            tail = room;
        }
        size++;
        return room;
    }

    // Adds a room behind a hallway screen (a classroom, the library's entrance...) and links
    // their doors both ways. The room isn't part of the hallway list.
    public Room addRoomBehind(String name, TileMap map, Room hallway) {
        // Rooms linked by a door must both actually have one, or going through it has nowhere to
        // arrive. Catch that mistake now, with a clear message, instead of crashing mid-game.
        if (map.nearestDoor(0) == null) {
            throw new IllegalStateException(name + " is linked to " + hallway.name + " but its map has no working door");
        }
        if (hallway.map.nearestDoor(0) == null) {
            throw new IllegalStateException(hallway.name + " is linked to " + name + " but its map has no working door");
        }
        Room room = new Room(name, this, map);
        room.door = hallway;
        hallway.door = room;
        return room;
    }

    // Adds a room to the left or right of an existing room that isn't in the hallway list,
    // and links them both ways. This is how a room wider than one screen is built:
    //   libraryWest <-> libraryEntrance <-> libraryEast
    public Room addLeftOf(Room existing, String name, TileMap map) {
        if (existing.left != null) {
            throw new IllegalStateException(existing.name + " already has a room on its left");
        }
        Room room = new Room(name, this, map);
        room.right = existing;
        existing.left = room;
        return room;
    }

    public Room addRightOf(Room existing, String name, TileMap map) {
        if (existing.right != null) {
            throw new IllegalStateException(existing.name + " already has a room on its right");
        }
        Room room = new Room(name, this, map);
        room.left = existing;
        existing.right = room;
        return room;
    }

    // Links two floors through their stairs: the second list (floors) works like the first (rooms)
    public static void connectStairs(Floor upper, Floor lower) {
        upper.below = lower;
        lower.above = upper;
    }

    // Walks the list from the head until it finds a screen with this name (null if there isn't one).
    // For each hallway screen with a door, it also searches the row of rooms behind that door.
    public Room find(String name) {
        for (Room r = head; r != null; r = r.right) {
            if (r.name.equals(name)) return r;
            if (r.door != null) {
                for (Room side = leftEnd(r.door); side != null; side = side.right) {
                    if (side.name.equals(name)) return side;
                }
            }
        }
        return null;
    }

    // The leftmost room of the row a room is in: keep following left until there's nothing there
    public static Room leftEnd(Room room) {
        Room r = room;
        while (r.left != null) {
            r = r.left;
        }
        return r;
    }
}
