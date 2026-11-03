package nightmare404.world;

import nightmare404.map.Classroom;
import nightmare404.map.Ground;
import nightmare404.map.Hallway;
import nightmare404.map.Lab;
import nightmare404.map.Library;
import nightmare404.map.Office;
import nightmare404.map.Quadrangle;
import nightmare404.map.Stairs;
import nightmare404.tile.TileMap;
import nightmare404.tile.TileSet;

// Builds every room in the game and links them together.
//
// Floor 4, left to right:
//
//              [Room 402]     [Room 403]     [Room 404]          <- classrooms (linked by door)
//                  |              |              |
//   [West Stairs]<->[Hallway 402]<->[Hallway 403]<->[Hallway 404]<->[East Stairs]   <- hallway (left/right)
//         |
//      (stairs: floor4.below == floor3)
//         |
// Floor 3:
//                 [Library W]<->[Library Entrance]<->[Library E]      <- one room, 3 screens (left/right)
//                                      |  (big double door)
//   [West Stairs]<->[Hallway]<->[Hallway Entrance]<->[Hallway]<->[East Stairs]
//         |
//      (stairs: floor3.below == floor2)
//         |
// Floor 2:
//                 [Chemistry Lab]  [Biology Lab]  [Containment Lab]    <- labs (linked by door)
//                        |              |                |
//   [West Stairs]<->[Hallway]<->[Hallway]<->[Hallway]<->[East Stairs]
//         |
//      (stairs: floor2.below == floor1)
//         |
// Floor 1 (the ground floor):
//                 [Faculty Room]  [Principal's Office]  [Records Room]   <- offices (linked by door)
//                        |              |                  |
//   [West Stairs]<->[Hallway]<->[Hallway]<->[Hallway]<->[East Stairs]
//   (exit boarded up)                                    (exit open)
//                                                              |  door
//                                                        [Quadrangle]   <- outside: one 50 x 36 map
//
public class World {
    public final Floor floor4;
    public final Floor floor3;
    public final Floor floor2;
    public final Floor floor1;
    public final Room groundExit;   // The screen with the open way out of the building
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
        Room westStairs4 = floor4.append("West Stairs", Stairs.west(tiles, ts, true, false));
        Room hall402 = floor4.append("Hallway (Room 402)",
                Hallway.outside(tiles, ts, room402, new int[] {9, 15}, new int[] {12}));
        Room hall403 = floor4.append("Hallway (Room 403)",
                Hallway.outside(tiles, ts, room403, new int[] {9, 12}, new int[] {15}));
        Room hall404 = floor4.append("Hallway (Room 404)",
                Hallway.outside(tiles, ts, room404, new int[] {9, 12, 15}, new int[] {}));
        floor4.append("East Stairs (destroyed)", Stairs.eastDestroyed(tiles, ts));
        floor4.stairs = westStairs4;   // The only stairs that work

        // 2. The classrooms, each attached to the hallway screen in front of it.
        floor4.addRoomBehind("Room 402", room402, hall402);
        floor4.addRoomBehind("Room 403", room403, hall403);
        Room startRoom = floor4.addRoomBehind("Room 404", room404, hall404);

        // ---------- Floor 3: the library ----------
        floor3 = new Floor(3);

        // 1. The hallway: same layout as Floor 4, so the stairwells sit right under Floor 4's.
        //    Its stairs go both ways: up to Floor 4 and down to Floor 2.
        Room westStairs3 = floor3.append("West Stairs", Stairs.west(tiles, ts, true, true));
        floor3.append("Hallway (Library West)", Hallway.libraryFront(tiles, ts, Library.Part.WEST));
        Room hallEntrance = floor3.append("Hallway (Library Entrance)",
                Hallway.libraryFront(tiles, ts, Library.Part.ENTRANCE));
        floor3.append("Hallway (Library East)", Hallway.libraryFront(tiles, ts, Library.Part.EAST));
        floor3.append("East Stairs (collapsed)", Stairs.eastDestroyed(tiles, ts));
        floor3.stairs = westStairs3;

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

        // 1. The hallway. Its stairs go both ways: up to Floor 3 and down to Floor 1.
        //    A crooked LABORATORY sign hangs over them: the first thing you see on this floor.
        Room westStairs2 = floor2.append("West Stairs", Stairs.west(tiles, ts, true, true,
                TileSet.LABORATORY_SIGN, TileSet.LABORATORY_SIGN + 1, TileSet.LABORATORY_SIGN + 2));
        Room hallChemistry = floor2.append("Hallway (Chemistry Lab)",
                Hallway.labFront(tiles, ts, chemistry, Lab.Part.CHEMISTRY));
        Room hallBiology = floor2.append("Hallway (Biology Lab)",
                Hallway.labFront(tiles, ts, biology, Lab.Part.BIOLOGY));
        Room hallContainment = floor2.append("Hallway (Containment Lab)",
                Hallway.labFront(tiles, ts, containment, Lab.Part.CONTAINMENT));
        floor2.append("East Stairs (collapsed)", Stairs.eastDestroyed(tiles, ts));
        floor2.stairs = westStairs2;

        // 2. The three labs, each behind its own hallway screen (like Floor 4's classrooms)
        floor2.addRoomBehind("Chemistry Lab", chemistry, hallChemistry);
        floor2.addRoomBehind("Biology Lab", biology, hallBiology);
        floor2.addRoomBehind("Containment Lab", containment, hallContainment);

        // ---------- Floor 1: the ground floor, the offices ----------
        floor1 = new Floor(1);

        TileMap faculty = Office.build(tiles, ts, Office.Part.FACULTY);
        TileMap principal = Office.build(tiles, ts, Office.Part.PRINCIPAL);
        TileMap records = Office.build(tiles, ts, Office.Part.RECORDS);

        // 1. The hallway. Nothing below, so the stairs only go up, and the corridor looks out on grass.
        //    Each end of the building has an exit between two support pillars.
        //    The west exit is boarded up; the east one is the way out. (To swap them, swap BLOCKED and OPEN.)
        Room westStairs1 = floor1.append("West Stairs", Stairs.westGround(tiles, ts, Ground.Exit.BLOCKED));
        Room hallFaculty = floor1.append("Hallway (Faculty Room)",
                Hallway.officeFront(tiles, ts, faculty, Office.Part.FACULTY));
        Room hallPrincipal = floor1.append("Hallway (Principal's Office)",
                Hallway.officeFront(tiles, ts, principal, Office.Part.PRINCIPAL));
        Room hallRecords = floor1.append("Hallway (Records Room)",
                Hallway.officeFront(tiles, ts, records, Office.Part.RECORDS));
        Room eastExit = floor1.append("East Stairs (Exit)", Stairs.eastGround(tiles, ts, Ground.Exit.OPEN));
        floor1.stairs = westStairs1;

        // 2. The three offices, each behind its own hallway screen
        floor1.addRoomBehind("Faculty Room", faculty, hallFaculty);
        floor1.addRoomBehind("Principal's Office", principal, hallPrincipal);
        floor1.addRoomBehind("Records Room", records, hallRecords);

        // 3. The way out: the open exit is a door like any other, and the quadrangle is on the
        //    other side of it. One big outdoor map; the camera follows the player around it.
        groundExit = eastExit;
        quadrangle = floor1.addRoomBehind("Quadrangle", Quadrangle.build(tiles, ts), groundExit);
        quadrangle.outdoors = true;

        // ---------- Link the floors through the west stairs ----------
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
                if (r.door != null) {
                    for (Room side = Floor.leftEnd(r.door); side != null; side = side.right) {
                        String link = side == r.door ? "door -> " : "        ";
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
