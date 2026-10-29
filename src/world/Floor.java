package world;

import tile.TileMap;

// One floor of the school: a doubly linked list of hallway screens, from head (leftmost) to tail (rightmost).
// Classrooms hang off the hallway screens (see Room.door).
//
// Floors will be linked to each other the same way, through their stairs:
//   floor4.below == floor3   and   floor3.above == floor4
// Only Floor 4 exists so far, so above and below are still null.
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

    // Adds a classroom behind a hallway screen and links their doors both ways.
    // The classroom isn't part of the left/right list: its left and right stay null.
    public Room addClassroom(String name, TileMap map, Room hallway) {
        Room classroom = new Room(name, this, map);
        classroom.door = hallway;
        hallway.door = classroom;
        return classroom;
    }

    // Walks the list from the head until it finds a screen with this name (null if there isn't one)
    public Room find(String name) {
        for (Room r = head; r != null; r = r.right) {
            if (r.name.equals(name)) return r;
            if (r.door != null && r.door.name.equals(name)) return r.door;
        }
        return null;
    }
}
