package nightmare404.story;

import java.util.ArrayList;
import java.util.List;

import nightmare404.entity.Clue;
import nightmare404.entity.Loot;
import nightmare404.world.Floor;
import nightmare404.world.Room;
import nightmare404.world.World;

// Every floor's quest and where its clues lie, like Encounters does for the monsters. Each floor has
// two or three clues, in the side rooms the player could otherwise walk straight past:
//
//   Floor 4  The Janitor's Logbook   a torn page in Room 402, another in Room 403 (both needed: read
//                                    to the boulder on the stairs, they wake the Brute; see SleepingBoss)
//   Floor 3  Overdue                 three checkout cards: two in Library (Entrance), one in Library (West)
//                                    (all three needed: their names make the Whisperer solid; see Whisperer)
//   Floor 2  Project REMEMBER        a lab report page in the Chemistry Lab, another in the Biology Lab
//   Floor 1  Permanent Record        three books: the Staff Handbook (its memo tucked inside) in the
//                                    Faculty Room, the register and the yearbook in the Records Room
//                                    (all three needed: the Principal won't see Kai until he has them)
//
// Each lies at the far end of its room from the door, on a walkable tile, near the monsters' patrols:
// the player can sneak in and out or fight. (Run QuestCheck after changing anything here.)
// What the clues say is in Story.
public class Quests {
    private final int ts;

    private Quests(int ts) {
        this.ts = ts;
    }

    public static void populate(World world, int ts) {
        Quests q = new Quests(ts);

        Quest f4 = q.quest(world.floor4, "The Janitor's Logbook", "Pages");
        q.clue(f4, "4F-1", "Torn logbook page", "Room 402", 21, 8, Story.LOGBOOK_OCT_3, 1, 0, 0, 0);
        q.clue(f4, "4F-2", "Torn logbook page", "Room 403", 21, 9, Story.LOGBOOK_OCT_9, 0, 0, 1, 0);

        Quest f3 = q.quest(world.floor3, "Overdue", "Checkout cards");
        q.clue(f3, "3F-1", "Checkout card", "Library (Entrance)", 4, 13, Story.CARD_ROOM_404, 0, 1, 0, 0);
        q.clue(f3, "3F-2", "Checkout card", "Library (West)", 3, 12, Story.CARD_NIGHT_CLASS, 1, 0, 0, 0);
        q.clue(f3, "3F-3", "Checkout card", "Library (Entrance)", 20, 9, Story.CARD_LAST_BELL, 0, 0, 1, 0);

        Quest f2 = q.quest(world.floor2, "Project REMEMBER", "Lab report pages");
        q.clue(f2, "2F-1", "Lab report page", "Chemistry Lab", 21, 9, Story.REPORT_PAGE_1, 0, 0, 0, 1);
        q.clue(f2, "2F-2", "Lab report page", "Biology Lab", 3, 6, Story.REPORT_PAGE_40, 1, 0, 0, 0);

        Quest f1 = q.quest(world.floor1, "Permanent Record", "Books");
        q.clue(f1, "1F-1", "Staff Handbook", "Faculty Room", 21, 6, Story.STAFF_HANDBOOK, 0, 0, 1, 0);
        q.clue(f1, "1F-2", "Attendance register", "Records Room", 3, 8, Story.REGISTER, 1, 0, 0, 0);
        q.clue(f1, "1F-3", "Yearbook", "Records Room", 21, 8, Story.YEARBOOK, 0, 1, 0, 0);
    }

    private Quest quest(Floor floor, String title, String things) {
        Quest quest = new Quest(floor, title, things);
        floor.quest = quest;
        return quest;
    }

    // A clue lying at (col, row) in the room called roomName, with the supplies found with it
    private void clue(Quest quest, String id, String title, String roomName, int col, int row, List<Line> pages,
                      int kits, int textbooks, int redPens, int hallPasses) {
        Room room = quest.floor.find(roomName);
        if (room == null) throw new IllegalStateException("Quests: no room called " + roomName + " on Floor " + quest.floor.number);
        Clue clue = new Clue(id, title, room, col, row, ts, pages, new Loot(null, title, kits, textbooks, redPens, hallPasses));
        room.clues.add(clue);
        quest.clues.add(clue);
    }

    // Every clue in the game, floor by floor from the top
    public static List<Clue> all(World world) {
        List<Clue> all = new ArrayList<>();
        for (Floor f = world.top; f != null; f = f.below) {
            if (f.quest != null) all.addAll(f.quest.clues);
        }
        return all;
    }
}
