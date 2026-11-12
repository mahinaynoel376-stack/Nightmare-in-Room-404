package nightmare404.world;

import java.util.List;

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

    public Floor(int number) {
        this.number = number;
    }

    // The stairwell whose way UP works (its right flight), or null if there isn't one.
    // The working flights alternate between the two ends of the building, so the way up and the
    // way down can be different screens: walk the hallway list and look at each screen's stairs.
    public Room upStairs() {
        for (Room r = head; r != null; r = r.right) {
            if (r.map.stairs != null && r.map.stairs.upZone != null) return r;
        }
        return null;
    }

    // The stairwell whose way DOWN works (its left flight), or null if there isn't one
    public Room downStairs() {
        for (Room r = head; r != null; r = r.right) {
            if (r.map.stairs != null && r.map.stairs.downZone != null) return r;
        }
        return null;
    }

    // How far along the hallway list a screen is (0 = head), or -1 if it isn't in it
    public int indexOf(Room room) {
        int i = 0;
        for (Room r = head; r != null; r = r.right, i++) {
            if (r == room) return i;
        }
        return -1;
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
    // their doors both ways: the doors in the hallway's top wall, to the room's doors, in order
    // (the first door to the first door, and so on). The room isn't part of the hallway list.
    public Room addRoomBehind(String name, TileMap map, Room hallway) {
        Room room = new Room(name, this, map);
        linkAll(room, hallway, hallway.map.doorsInTopWall(), "wall");
        room.door = hallway;
        hallway.door = room;
        return room;
    }

    // Adds a place outside the building (the quadrangle), reached through the doors in a hallway
    // screen's FRONT wall (the main entrance), and links them both ways. The hallway screen keeps
    // its room behind as its door; the place outside gets the hallway screen as its door back.
    public Room addOutside(String name, TileMap map, Room hallway) {
        Room room = new Room(name, this, map);
        linkAll(room, hallway, hallway.map.doorsInBottomWall(), "front wall");
        room.door = hallway;
        return room;
    }

    // Links every door of room to the hallway's doors with these numbers, in order.
    // Rooms linked by a door must both actually have one, or going through it has nowhere to arrive,
    // and both sides need the same number, so door i always has a door i to lead to. Catch those
    // mistakes now, with a clear message, instead of crashing mid-game.
    private static void linkAll(Room room, Room hallway, List<Integer> hallDoors, String where) {
        if (room.map.doorCount() == 0) {
            throw new IllegalStateException(room.name + " is linked to " + hallway.name + " but its map has no working door");
        }
        if (hallDoors.isEmpty()) {
            throw new IllegalStateException(hallway.name + " is linked to " + room.name
                    + " but its map has no working door in its " + where);
        }
        if (room.map.doorCount() != hallDoors.size()) {
            throw new IllegalStateException(room.name + " has " + room.map.doorCount() + " working doors but "
                    + hallway.name + " has " + hallDoors.size() + " in its " + where + ": they must match");
        }
        for (int i = 0; i < hallDoors.size(); i++) {
            room.linkDoor(i, hallway, hallDoors.get(i));
        }
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

    // Links two floors through their stairs: the second list (floors) works like the first (rooms).
    // One flight joins the two floors, so the upper floor's way down and the lower floor's way up
    // must be the same stairwell: the same end of the building. Catch a mismatch now, with a clear
    // message, instead of arriving somewhere strange mid-game.
    public static void connectStairs(Floor upper, Floor lower) {
        Room down = upper.downStairs(), up = lower.upStairs();
        if (down == null || up == null) {
            throw new IllegalStateException("Floors " + upper.number + " and " + lower.number
                    + " need a working flight between them (Floor " + upper.number + " down, Floor " + lower.number + " up)");
        }
        if (upper.indexOf(down) != lower.indexOf(up)) {
            throw new IllegalStateException("Floor " + upper.number + "'s way down (" + down.name + ") isn't above Floor "
                    + lower.number + "'s way up (" + up.name + ")");
        }
        upper.below = lower;
        lower.above = upper;
    }

    // Walks the list from the head until it finds a screen with this name (null if there isn't one).
    // For each hallway screen, it also searches the row of rooms behind each of its doors
    // (and the place outside the main entrance).
    public Room find(String name) {
        for (Room r = head; r != null; r = r.right) {
            if (r.name.equals(name)) return r;
            for (Room linked : r.linkedRooms()) {
                for (Room side = leftEnd(linked); side != null; side = side.right) {
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
