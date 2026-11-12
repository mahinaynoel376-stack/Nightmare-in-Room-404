package nightmare404.world;

import nightmare404.map.Classroom;
import nightmare404.map.Ground;
import nightmare404.map.Hallway;
import nightmare404.map.Lab;
import nightmare404.map.Library;
import nightmare404.map.Office;
import nightmare404.map.Quadrangle;
import nightmare404.map.Stairs;
import nightmare404.map.Stairs.Flight;
import nightmare404.tile.TileMap;
import nightmare404.tile.TileSet;

// Builds every room in the game and links them together.
//
// Floor 4, left to right:
//
//              [Room 402]     [Room 403]     [Room 404]          <- classrooms (linked by door)
//                  |              |              |
//   [West Stairs]<->[Hallway 402]<->[Hallway 403]<->[Hallway 404]<->[East Stairs]   <- hallway (left/right)
//         |                                                       (destroyed)
//      (stairs: floor4.below == floor3, down the WEST stairs)
//         |
// Floor 3:
//                 [Library W]<->[Library Entrance]<->[Library E]      <- one room, 3 screens (left/right)
//                                      |  (big double door)
//   [West Stairs]<->[Hallway]<->[Hallway Entrance]<->[Hallway]<->[East Stairs]
//                                                                      |
//                                     (stairs: floor3.below == floor2, down the EAST stairs)
//                                                                      |
// Floor 2:
//                 [Chemistry Lab]  [Biology Lab]  [Containment Lab]    <- labs (linked by door)
//                        |              |                |
//   [West Stairs]<->[Hallway]<->[Hallway]<->[Hallway]<->[East Stairs]
//         |
//      (stairs: floor2.below == floor1, down the WEST stairs)
//         |
// Floor 1 (the ground floor):
//                 [Faculty Room]  [Principal's Office]  [Records Room]   <- offices (linked by door)
//                        |              |                  |
//   [West Stairs]<->[Hallway]<->[Hallway]<->[Hallway]<->[East Stairs]
//   (exit boarded up)              |              (exit boarded up)
//                                  |  the main entrance (in the hallway's front wall)
//                             [Quadrangle]   <- outside: one 50 x 36 map
//
// The way down ALTERNATES between the two ends of the building (the other end's flight has
// collapsed), so the player has to cross every floor: Floor 4 west -> Floor 3 east -> Floor 2 west
// -> Floor 1, then east to the exit. Each stairwell's left flight goes down and its right flight up:
//
//              West Stairs                  East Stairs
//   Floor 4    down OPEN,      up BLOCKED   down COLLAPSED, up BLOCKED    (no Floor 5)
//   Floor 3    down COLLAPSED, up OPEN      down OPEN,      up COLLAPSED
//   Floor 2    down OPEN,      up COLLAPSED down COLLAPSED, up OPEN
//   Floor 1    down BLOCKED,   up OPEN      down BLOCKED,   up COLLAPSED  (no basement)
//
public class World {
    public final Floor floor4;
    public final Floor floor3;
    public final Floor floor2;
    public final Floor floor1;
    public final Room groundExit;   // The screen with the way out of the building: the main entrance
    public final Room quadrangle;   // The courtyard outside it (the last stand)
    public final Floor top;     // The highest floor: start here and follow .below to visit them all
    public final Room start;    // Where the game begins

    public World(TileSet tiles, int ts) {
        // ---------- Floor 4: the classrooms ----------
        floor4 = new Floor(4);

        // The classroom maps come first: each hallway screen copies its classroom's doors.
        TileMap room402 = Classroom.build(tiles, ts, Classroom.ROOM_402);
        TileMap room403 = Classroom.build(tiles, ts, Classroom.ROOM_403);
        TileMap room404 = Classroom.build(tiles, ts, Classroom.ROOM_404);

        // 1. The hallway, appended left to right. append() links each new screen to the previous one.
        floor4.append("West Stairs", Stairs.build(tiles, ts, Stairs.Side.WEST, Flight.OPEN, Flight.BLOCKED));
        Room hall402 = floor4.append("Hallway (Room 402)",
                Hallway.outside(tiles, ts, room402, Hallway.Position.FIRST, new int[] {5, 11}, new int[] {8}));
        Room hall403 = floor4.append("Hallway (Room 403)",
                Hallway.outside(tiles, ts, room403, Hallway.Position.MIDDLE, new int[] {5, 8}, new int[] {11}));
        Room hall404 = floor4.append("Hallway (Room 404)",
                Hallway.outside(tiles, ts, room404, Hallway.Position.LAST, new int[] {5, 8, 11}, new int[] {}));
        floor4.append("East Stairs (destroyed)", Stairs.build(tiles, ts, Stairs.Side.EAST, Flight.COLLAPSED, Flight.BLOCKED));

        // 2. The classrooms, each attached to the hallway screen in front of it.
        floor4.addRoomBehind("Room 402", room402, hall402);
        floor4.addRoomBehind("Room 403", room403, hall403);
        Room startRoom = floor4.addRoomBehind("Room 404", room404, hall404);

        // ---------- Floor 3: the library ----------
        floor3 = new Floor(3);

        // 1. The hallway: same layout as Floor 4, so the stairwells sit right under Floor 4's.
        //    You arrive at the west end (up to Floor 4) and leave from the east end (down to Floor 2).
        floor3.append("West Stairs", Stairs.build(tiles, ts, Stairs.Side.WEST, Flight.COLLAPSED, Flight.OPEN));
        floor3.append("Hallway (Library West)", Hallway.libraryFront(tiles, ts, Library.Part.WEST));
        Room hallEntrance = floor3.append("Hallway (Library Entrance)",
                Hallway.libraryFront(tiles, ts, Library.Part.ENTRANCE));
        floor3.append("Hallway (Library East)", Hallway.libraryFront(tiles, ts, Library.Part.EAST));
        floor3.append("East Stairs", Stairs.build(tiles, ts, Stairs.Side.EAST, Flight.OPEN, Flight.COLLAPSED));

        // 2. The library: three screens joined left/right, but only the middle one has a door.
        Room libraryEntrance = floor3.addRoomBehind("Library (Entrance)",
                Library.build(tiles, ts, Library.Part.ENTRANCE), hallEntrance);
        Room libraryWest = floor3.addLeftOf(libraryEntrance, "Library (West)",
                Library.build(tiles, ts, Library.Part.WEST));
        Room libraryEast = floor3.addRightOf(libraryEntrance, "Library (East)",
                Library.build(tiles, ts, Library.Part.EAST));
        // Screen-to-screen like the classrooms: the camera doesn't scroll inside the library
        libraryWest.fixedScreen = true;
        libraryEntrance.fixedScreen = true;
        libraryEast.fixedScreen = true;

        // ---------- Floor 2: the labs ----------
        floor2 = new Floor(2);

        TileMap chemistry = Lab.build(tiles, ts, Lab.Part.CHEMISTRY);
        TileMap biology = Lab.build(tiles, ts, Lab.Part.BIOLOGY);
        TileMap containment = Lab.build(tiles, ts, Lab.Part.CONTAINMENT);

        // 1. The hallway. You arrive at the east end (up to Floor 3) and leave from the west end (down to Floor 1).
        floor2.append("West Stairs", Stairs.build(tiles, ts, Stairs.Side.WEST, Flight.OPEN, Flight.COLLAPSED));
        Room hallChemistry = floor2.append("Hallway (Chemistry Lab)",
                Hallway.labFront(tiles, ts, chemistry, Lab.Part.CHEMISTRY, Hallway.Position.FIRST));
        Room hallBiology = floor2.append("Hallway (Biology Lab)",
                Hallway.labFront(tiles, ts, biology, Lab.Part.BIOLOGY, Hallway.Position.MIDDLE));
        Room hallContainment = floor2.append("Hallway (Containment Lab)",
                Hallway.labFront(tiles, ts, containment, Lab.Part.CONTAINMENT, Hallway.Position.LAST));
        //    A crooked LABORATORY sign hangs over the east stairs: the first thing you see on this floor.
        floor2.append("East Stairs", Stairs.build(tiles, ts, Stairs.Side.EAST, Flight.COLLAPSED, Flight.OPEN,
                TileSet.LABORATORY_SIGN, TileSet.LABORATORY_SIGN + 1, TileSet.LABORATORY_SIGN + 2));

        // 2. The three labs, each behind its own hallway screen (like Floor 4's classrooms)
        floor2.addRoomBehind("Chemistry Lab", chemistry, hallChemistry);
        floor2.addRoomBehind("Biology Lab", biology, hallBiology);
        floor2.addRoomBehind("Containment Lab", containment, hallContainment);

        // ---------- Floor 1: the ground floor, the offices ----------
        floor1 = new Floor(1);

        TileMap faculty = Office.build(tiles, ts, Office.Part.FACULTY);
        TileMap principal = Office.build(tiles, ts, Office.Part.PRINCIPAL);
        TileMap records = Office.build(tiles, ts, Office.Part.RECORDS);

        // 1. The hallway. Nothing below, so the stairs only go up (at the west end; the east flight has
        //    collapsed), and the corridor looks out on grass.
        //    Each end of the building has a side exit between two support pillars, both boarded up:
        //    the way out is the main entrance, in the middle (see Hallway.officeFront).
        floor1.append("West Stairs", Stairs.ground(tiles, ts, Stairs.Side.WEST, Flight.OPEN, Ground.Exit.BLOCKED));
        Room hallFaculty = floor1.append("Hallway (Faculty Room)",
                Hallway.officeFront(tiles, ts, faculty, Office.Part.FACULTY, Hallway.Position.FIRST));
        Room hallPrincipal = floor1.append("Hallway (Principal's Office)",
                Hallway.officeFront(tiles, ts, principal, Office.Part.PRINCIPAL, Hallway.Position.MIDDLE));
        Room hallRecords = floor1.append("Hallway (Records Room)",
                Hallway.officeFront(tiles, ts, records, Office.Part.RECORDS, Hallway.Position.LAST));
        floor1.append("East Stairs", Stairs.ground(tiles, ts, Stairs.Side.EAST, Flight.COLLAPSED, Ground.Exit.BLOCKED));

        // 2. The three offices, each behind its own hallway screen
        floor1.addRoomBehind("Faculty Room", faculty, hallFaculty);
        floor1.addRoomBehind("Principal's Office", principal, hallPrincipal);
        floor1.addRoomBehind("Records Room", records, hallRecords);

        // 3. The way out: the main entrance, in the front wall of the hallway screen in the middle of
        //    the building, is a door like any other, and the quadrangle is on the other side of it.
        //    That hallway screen now has two kinds of doors: the office's (behind it) and this one.
        //    One big outdoor map; the camera follows the player around it.
        groundExit = hallPrincipal;
        quadrangle = floor1.addOutside("Quadrangle", Quadrangle.build(tiles, ts), groundExit);
        quadrangle.outdoors = true;

        // ---------- Link the floors through their stairs (connectStairs checks the flights line up) ----------
        Floor.connectStairs(floor4, floor3);
        Floor.connectStairs(floor3, floor2);
        Floor.connectStairs(floor2, floor1);
        top = floor4;

        showFloorsBelow();
        start = startRoom;
    }

    // Makes the view over every railing show the real floor below.
    // Every floor's hallway has the same 5 screens in the same order, so screen 1 of a floor is
    // directly above screen 1 of the floor below it, screen 2 above screen 2, and so on.
    // So we walk TWO linked lists side by side: `up` along this floor, `down` along the one below,
    // moving both one step right each time.
    private void showFloorsBelow() {
        for (Floor floor = top; floor.below != null; floor = floor.below) {
            Room up = floor.head;
            Room down = floor.below.head;
            while (up != null && down != null) {
                up.map.showFloorBelow(down.map);
                up = up.right;
                down = down.right;
            }
        }
    }

    // Prints every floor by walking both linked lists: floors (top to bottom), then each floor's
    // hallway (left to right), then the rooms behind each door. Handy for checking the links.
    public void print(Room current) {
        for (Floor f = top; f != null; f = f.below) {
            System.out.println("Floor " + f.number + " (" + f.size + " hallway screens):");
            for (Room r = f.head; r != null; r = r.right) {
                System.out.println("  " + r.name + here(r, current));
                for (Room linked : r.linkedRooms()) {
                    for (Room side = Floor.leftEnd(linked); side != null; side = side.right) {
                        String link = side == linked ? "door -> " : "        ";
                        System.out.println("      " + link + side.name + here(side, current));
                    }
                }
            }
        }
    }

    private static String here(Room r, Room current) {
        return r == current ? "   <- you are here" : "";
    }
}
