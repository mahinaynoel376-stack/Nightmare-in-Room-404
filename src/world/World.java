package world;

import tile.Classroom;
import tile.Hallway;
import tile.Stairs;
import tile.TileSet;

// Builds every room in the game and links them together.
//
// Floor 4, left to right:
//
//              [Room 402]     [Room 403]     [Room 404]          <- classrooms (linked by door)
//                  |              |              |
//   [West Stairs]<->[Hallway 402]<->[Hallway 403]<->[Hallway 404]<->[East Stairs]   <- hallway (left/right)
//
public class World {
    public final Floor floor4;
    public final Room start;    // Where the game begins

    public World(TileSet tiles, int ts) {
        floor4 = new Floor(4);

        // 1. The hallway, appended left to right. append() links each new screen to the previous one.
        Room westStairs = floor4.append("West Stairs", Stairs.west(tiles, ts));
        Room hall402 = floor4.append("Hallway (Room 402)",
                Hallway.outside(tiles, ts, Classroom.ROOM_402, new int[] {9, 15}, new int[] {12}));
        Room hall403 = floor4.append("Hallway (Room 403)",
                Hallway.outside(tiles, ts, Classroom.ROOM_403, new int[] {9, 12}, new int[] {15}));
        Room hall404 = floor4.append("Hallway (Room 404)",
                Hallway.outside(tiles, ts, Classroom.ROOM_404, new int[] {9, 12, 15}, new int[] {}));
        floor4.append("East Stairs (destroyed)", Stairs.eastDestroyed(tiles, ts));
        floor4.stairs = westStairs;   // The only stairs that work

        // 2. The classrooms, each attached to the hallway screen in front of it.
        floor4.addClassroom("Room 402", Classroom.build(tiles, ts, Classroom.ROOM_402), hall402);
        floor4.addClassroom("Room 403", Classroom.build(tiles, ts, Classroom.ROOM_403), hall403);
        Room room404 = floor4.addClassroom("Room 404", Classroom.build(tiles, ts, Classroom.ROOM_404), hall404);

        start = room404;
    }

    // Prints every screen on the floor by walking the linked list. Handy for checking the links.
    public void print(Room current) {
        System.out.println("Floor " + floor4.number + " (" + floor4.size + " hallway screens):");
        for (Room r = floor4.head; r != null; r = r.right) {
            String line = "  " + r.name + (r == current ? "   <- you are here" : "");
            if (r.door != null) {
                line += "\n      door -> " + r.door.name + (r.door == current ? "   <- you are here" : "");
            }
            System.out.println(line);
        }
    }
}
