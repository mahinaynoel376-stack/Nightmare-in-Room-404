package nightmare404.main;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.*;

import nightmare404.battle.BattleScreen;
import nightmare404.battle.BattleScreen.BattleResult;
import nightmare404.battle.LastStand;
import nightmare404.battle.TypingBattle;
import nightmare404.entity.Clue;
import nightmare404.entity.Monster;
import nightmare404.entity.Player;
import nightmare404.entity.TreeMonster;
import nightmare404.story.Director;
import nightmare404.story.Story;
import nightmare404.tile.TileSet;
import nightmare404.ui.BottomUI;
import nightmare404.ui.DialogueBox;
import nightmare404.ui.EndingScreen;
import nightmare404.ui.Journal;
import nightmare404.ui.Lighting;
import nightmare404.ui.QuestTracker;
import nightmare404.world.Room;
import nightmare404.world.World;

public class GamePanel extends JPanel implements ActionListener {
    // Screen size is derived from the tile grid so the map always fits exactly
    public final int tileSize = 32;
    public final int maxScreenCol = 25;
    public final int maxScreenRow = 19;
    public final int screenWidth = tileSize * maxScreenCol;  // 800
    public final int screenHeight = tileSize * maxScreenRow; // 608

    // The game loop: a Swing timer that calls actionPerformed (below) every 16 ms, about 60 times a
    // second. Swing runs it on the same thread that hears the keys and draws the screen, so a frame
    // of the game, a key press and the drawing never happen at the same time.
    private final Timer loop = new Timer(16, this);
    // Created once and reused: making a new Font every frame (60 times a second) is wasted work
    private static final Font ROOM_NAME_FONT = new Font("Arial", Font.BOLD, 24);
    private static final Font NOTICE_FONT = new Font("Arial", Font.BOLD, 16);

    // Movement tracking booleans
    public boolean upPressed, downPressed, leftPressed, rightPressed;
    public boolean sprintPressed; // Shift held down

    // The other keys. A press is just noted here (see KeyAction), and the game acts on it in its next
    // frame (update):
    //   E, Enter, Space  read on in the dialogue box, read a page in the journal, back to the menu at the end.
    //                    Holding one down makes Windows repeat it, which would race through the lines,
    //                    so a press only counts again after the key's been let go (advanceHeld).
    //   Space            outside: focus (see LastStand). Letters typed while focusing go to `typed`.
    //   J, Esc           open and close the journal (Esc also stops focusing). Backspace drops a word.
    //   Esc              otherwise: the main menu, with the game stopped until Continue.
    //   1-4              outside: use a First Aid Kit, an Old Textbook, a Red Pen, a Hall Pass.
    private boolean advanceHeld, spaceHeld, backHeld;
    private boolean advanceQueued, spaceQueued, journalQueued, backQueued, unlockQueued;
    private int itemQueued;
    private final ArrayList<Character> typed = new ArrayList<>();
    private boolean wasUp, wasDown;   // W and S a frame ago, so the journal moves once per press

    // Entities (public: the UI and the checks in other packages read the player)
    public Player flor;
    BottomUI bottomUI;

    // The story: the dialogue box, the Director that decides when the story happens, the journal (J),
    // the quest tracker (top right), the fight outside, and once Subject Zero is beaten, the ending
    public final DialogueBox dialogue = new DialogueBox();
    public Director director;
    public Journal journal;
    public LastStand lastStand;
    QuestTracker tracker;
    EndingScreen endingScreen;
    public boolean ended;                   // The game is over: the ending screen is showing
    public boolean endedRemembered;         // ...and it's the good ending (every clue found)
    private long endedTicks;
    private boolean talking;                // The dialogue box was open last frame
    private boolean journalFromMenu;        // The journal was opened from the main menu: closing it goes back there

    // The world is a linked list of rooms; currentRoom points at the node the player is in.
    // Moving to another room just means following one of its links (left, right or door).
    public TileSet tiles;
    public World world;
    public Room currentRoom;

    // Battles. Touching a monster opens its battle in this window (see startBattle); while it's on,
    // the world is paused. After a battle the monsters in the room wait a moment (calm), so another
    // one can't grab the player the instant they're back.
    public boolean inBattle;
    public Monster battleMonster;           // The monster in the battle (the checks look at it)
    private JFrame battleWindow;            // The window the battle is showing in
    private int calm;
    private Room lastRoom;
    public long ticks;                      // Frames since the game began (the monsters bob with it)

    // A short message at the bottom of the screen ("The doors won't open...")
    private String notice = "";
    private int noticeTicks;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);

        tiles = new TileSet();                  // Load every tile image once
        world = new World(tiles, tileSize);     // Build and link all the rooms
        currentRoom = world.start;
        flor = new Player(this);
        bottomUI = new BottomUI(this);
        director = new Director(this);
        journal = new Journal(this);
        lastStand = new LastStand(this);
        tracker = new QuestTracker(this);
        endingScreen = new EndingScreen(this);
        setupKeyBindings();
        lastRoom = currentRoom;
    }

    // Every monster beaten and every floor's key in the player's pocket: for the checks that only test walking
    public void peaceful() {
        for (Monster m : world.monsters) {
            m.defeated = true;
            if (m.loot != null) {
                m.lootTaken = true;
                flor.keys.add(m.loot.key());
            }
        }
    }

    public void showNotice(String text) {
        showNotice(text, 150);   // 2.5 seconds
    }

    public void showNotice(String text, int ticks) {
        notice = text;
        noticeTicks = ticks;
    }

    // The notice on screen right now ("" if none)
    public String notice() {
        if (noticeTicks > 0) return notice;
        return "";
    }

    private void releaseKeys() {
        upPressed = false;
        downPressed = false;
        leftPressed = false;
        rightPressed = false;
        sprintPressed = false;
    }

    public void enterRoom(Room room) {
        currentRoom = room;
    }

    private void setupKeyBindings() {
        InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        // A KeyStroke includes the modifiers held at the time, so "pressed W" doesn't fire while
        // Shift is held down (that's "shift pressed W"). Bind both versions so WASD keeps working
        // while sprinting, and so letting go of W mid-sprint still stops the player.
        String[] modifiers = {"", "shift "};
        for (String mod : modifiers) {
            // Bind W, A, S, D Presses ("pressed" modifier is required)
            im.put(KeyStroke.getKeyStroke(mod + "pressed W"), "upPress");
            im.put(KeyStroke.getKeyStroke(mod + "pressed S"), "downPress");
            im.put(KeyStroke.getKeyStroke(mod + "pressed A"), "leftPress");
            im.put(KeyStroke.getKeyStroke(mod + "pressed D"), "rightPress");

            // Bind W, A, S, D Releases
            im.put(KeyStroke.getKeyStroke(mod + "released W"), "upRelease");
            im.put(KeyStroke.getKeyStroke(mod + "released S"), "downRelease");
            im.put(KeyStroke.getKeyStroke(mod + "released A"), "leftRelease");
            im.put(KeyStroke.getKeyStroke(mod + "released D"), "rightRelease");

            // Shift (sprint). Pressing Shift reports the Shift modifier as already down and
            // releasing it reports it as up, so binding both versions covers either case.
            im.put(KeyStroke.getKeyStroke(mod + "pressed SHIFT"), "sprintPress");
            im.put(KeyStroke.getKeyStroke(mod + "released SHIFT"), "sprintRelease");

            im.put(KeyStroke.getKeyStroke(mod + "pressed E"), "advancePress");
            im.put(KeyStroke.getKeyStroke(mod + "released E"), "advanceRelease");
            im.put(KeyStroke.getKeyStroke(mod + "pressed ENTER"), "advancePress");
            im.put(KeyStroke.getKeyStroke(mod + "released ENTER"), "advanceRelease");
            im.put(KeyStroke.getKeyStroke(mod + "pressed SPACE"), "spacePress");
            im.put(KeyStroke.getKeyStroke(mod + "released SPACE"), "spaceRelease");
            im.put(KeyStroke.getKeyStroke(mod + "pressed J"), "journal");
            im.put(KeyStroke.getKeyStroke(mod + "pressed ESCAPE"), "backPress");
            im.put(KeyStroke.getKeyStroke(mod + "released ESCAPE"), "backRelease");
            im.put(KeyStroke.getKeyStroke(mod + "pressed BACK_SPACE"), "unlock");
            for (int n = 1; n <= 4; n++) {
                im.put(KeyStroke.getKeyStroke(mod + "pressed " + n), "item" + n);
                im.put(KeyStroke.getKeyStroke(mod + "pressed NUMPAD" + n), "item" + n);
            }
        }
        // Letters as they're typed (a separate kind of key event from the presses above, so W A S D
        // still walk): they only matter outside, while focusing
        for (char c = 'a'; c <= 'z'; c++) {
            im.put(KeyStroke.getKeyStroke(c), "typed");
            im.put(KeyStroke.getKeyStroke(Character.toUpperCase(c)), "typed");
        }

        // Each name above gets a KeyAction that knows what to do with it
        String[] names = {"upPress", "downPress", "leftPress", "rightPress", "sprintPress",
                "upRelease", "downRelease", "leftRelease", "rightRelease", "sprintRelease",
                "advancePress", "advanceRelease", "spacePress", "spaceRelease", "backPress", "backRelease",
                "journal", "unlock", "item1", "item2", "item3", "item4", "typed"};
        for (String name : names) {
            am.put(name, new KeyAction(name));
        }
    }

    // What a key does. Swing calls actionPerformed when the key bound to `name` is pressed or let go.
    private class KeyAction extends AbstractAction {
        String name;

        KeyAction(String name) {
            this.name = name;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            switch (name) {
                case "upPress": upPressed = true; break;
                case "downPress": downPressed = true; break;
                case "leftPress": leftPressed = true; break;
                case "rightPress": rightPressed = true; break;
                case "sprintPress": sprintPressed = true; break;
                case "upRelease": upPressed = false; break;
                case "downRelease": downPressed = false; break;
                case "leftRelease": leftPressed = false; break;
                case "rightRelease": rightPressed = false; break;
                case "sprintRelease": sprintPressed = false; break;
                case "advancePress":
                    if (!advanceHeld) advanceQueued = true;
                    advanceHeld = true;
                    break;
                case "advanceRelease": advanceHeld = false; break;
                case "spacePress":
                    if (!spaceHeld) spaceQueued = true;
                    spaceHeld = true;
                    break;
                case "spaceRelease": spaceHeld = false; break;
                case "backPress":
                    if (!backHeld) backQueued = true;
                    backHeld = true;
                    break;
                case "backRelease": backHeld = false; break;
                case "journal": journalQueued = true; break;
                case "unlock": unlockQueued = true; break;
                case "item1": itemQueued = 1; break;
                case "item2": itemQueued = 2; break;
                case "item3": itemQueued = 3; break;
                case "item4": itemQueued = 4; break;
                case "typed":
                    String letter = e.getActionCommand();
                    if (letter != null && letter.length() == 1) typed.add(letter.charAt(0));
                    break;
                default:
                    break;
            }
        }
    }

    // Starts the game loop, or starts it again after the main menu (Continue). Keys pressed or let go
    // while the menu was showing never reached the game, so it starts again with none held down.
    public void start() {
        releaseKeys();
        advanceHeld = false;
        spaceHeld = false;
        backHeld = false;
        advanceQueued = false;
        spaceQueued = false;
        journalQueued = false;
        backQueued = false;
        unlockQueued = false;
        itemQueued = 0;
        typed.clear();
        loop.start();
    }

    // Stops the game loop: the game waits (while the menu is showing, or for good after a new game)
    public void stop() {
        loop.stop();
    }

    // One frame of the game loop (the timer calls this every 16 ms)
    @Override
    public void actionPerformed(ActionEvent e) {
        update();
        repaint();
    }

    public void update() {
        if (inBattle) return;            // The world waits while a battle is on
        // The keys pressed since the last frame (and forget them, so each press counts once)
        boolean advance = advanceQueued;
        boolean space = spaceQueued;
        boolean toJournal = journalQueued;
        boolean back = backQueued;
        boolean unlock = unlockQueued;
        int item = itemQueued;
        advanceQueued = false;
        spaceQueued = false;
        journalQueued = false;
        backQueued = false;
        unlockQueued = false;
        itemQueued = 0;
        ArrayList<Character> letters = new ArrayList<>(typed);
        typed.clear();

        if (ended) {                     // The ending screen: Enter goes back to the main menu
            endedTicks++;
            if ((advance || space) && endedTicks > 150) goToMenu();
            return;
        }
        if (dialogue.isOpen()) {         // The world waits while someone's talking
            upPressed = false;
            downPressed = false;
            leftPressed = false;
            rightPressed = false;
            dialogue.update(advance || space);
            if (!dialogue.isOpen()) director.dialogueOver();   // It just closed: what was waiting for it happens now
            talking = true;
            return;
        }
        if (talking) {                   // Just finished reading: the monsters give you a moment
            talking = false;
            calm = Math.max(calm, 30);
        }
        boolean outside = currentRoom.outdoors;
        if (journal.isOpen()) {
            updateJournal(advance || space, toJournal || back);
            return;
        }
        if (toJournal && !(outside && lastStand.focused())) {   // (While focusing, J is just a letter)
            journal.open();
            releaseKeys();
            return;
        }
        if (back && !(outside && lastStand.focused())) {   // (While focusing, Esc stops it instead)
            goToMenu();
            return;
        }
        director.update();               // The opening scene, or somewhere new for the first time
        if (dialogue.isOpen()) return;
        if (outside) {                   // The fight outside: focus, type, use items
            if (space || back && lastStand.focused()) lastStand.toggleFocus();
            if (unlock) lastStand.dropTarget();
            if (item > 0) lastStand.useItem(item);
            for (char c : letters) lastStand.type(c);
            if (lastStand.focused()) releaseKeys();   // Focusing: the player stands still
        }

        ticks++;
        if (noticeTicks > 0) noticeTicks--;
        flor.update();
        if (currentRoom != lastRoom) {   // Just walked into a room: the monsters give you a moment
            for (Monster m : lastRoom.monsters) m.chasing = false;   // The ones left behind stop chasing
            if (lastRoom.outdoors || currentRoom.outdoors) lastStand.reset();   // Going out, or back in
            lastRoom = currentRoom;
            calm = Math.max(calm, 30);
            for (Monster m : currentRoom.monsters) m.playerArrived(flor);   // (The quadrangle's trees start to stir)
        }
        for (Clue clue : currentRoom.clues) {   // Walking over a clue picks it up
            if (!flor.clues.contains(clue.id) && clue.area().intersects(flor.solidArea)) {
                director.clueFound(clue);
                return;
            }
        }
        if (calm > 0) {
            calm--;
            return;
        }
        if (currentRoom.outdoors) {      // Outside, the monsters are fought on the map
            lastStand.update();
            return;
        }
        // The neighbouring screens are in view (the hallway screens are narrower than the view), so
        // their monsters carry on walking their rounds; only the ones on this screen can see the player
        for (Room side : new Room[] {currentRoom.left, currentRoom.right}) {
            if (side == null) continue;
            for (Monster m : side.monsters) m.update(null);
        }
        for (Monster m : currentRoom.monsters) {
            if (m.lootUnder(flor)) {     // Walking over what a boss dropped picks it up
                m.lootTaken = true;
                flor.collect(m.loot);
                director.lootTaken(m);
                return;
            }
            if (m.defeated) continue;
            m.update(flor);
            if (m.touches(flor)) {
                releaseKeys();           // Keys let go during the battle would otherwise stay "held"
                director.beforeBattle(m);   // A floor's boss has its say first, then startBattle
                return;
            }
        }
    }

    // A battle starts (after a floor's boss has had its say: see Director.beforeBattle). The world
    // waits while it's on, and the battle takes this window's place (see openBattle). The checks
    // have no window: they look at inBattle and battleMonster, and call endBattle themselves.
    public void startBattle(Monster m) {
        inBattle = true;
        battleMonster = m;
        JFrame window = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (window != null) openBattle(window, m);
    }

    // The journal is open: W and S choose a clue, E reads it again, J or Esc closes it
    private void updateJournal(boolean advance, boolean close) {
        if (upPressed && !wasUp) journal.move(-1);
        if (downPressed && !wasDown) journal.move(1);
        wasUp = upPressed;
        wasDown = downPressed;
        if (close) {
            journal.close();
            releaseKeys();
            if (journalFromMenu) {       // Opened from the menu: back there
                journalFromMenu = false;
                goToMenu();
            }
        } else if (advance && journal.chosen() != null) {
            journal.close();
            director.reread(journal.chosen());   // It opens the journal again after reading
        }
    }

    // Outside, the player's health ran out (see LastStand): back to the main entrance with the monsters
    // beaten so far still beaten, and at least half health. The trees still standing start over.
    public void loseOutside() {
        flor.respawn();
        flor.health = Math.max(flor.health, LastStand.RESPAWN_HP);
        lastStand.reset();
        for (Monster m : currentRoom.monsters) m.playerArrived(flor);
        lastRoom = currentRoom;
        calm = 60;
        releaseKeys();
        showWakeUpNotice();
    }

    // "You wake up with a gasp...", after going down: by the main entrance outside, or where the player
    // came onto the floor inside
    private void showWakeUpNotice() {
        if (currentRoom.outdoors) {
            showNotice(Story.WAKE_OUTSIDE);
        } else {
            showNotice(Story.WAKE_INSIDE);
        }
    }

    // Subject Zero is beaten and the last words are said: show the ending screen
    public void end(boolean remembered) {
        endedRemembered = remembered;
        endedTicks = 0;
        ended = true;
    }

    // Esc, or Enter after the ending: the game loop stops and the main menu takes the window (see
    // Main). The checks have no window, so for them nothing more happens.
    private void goToMenu() {
        stop();
        releaseKeys();
        if (SwingUtilities.getWindowAncestor(this) != null) Main.showMenu();
    }

    // The menu's Journal: the journal opens, and closing it goes back to the menu (then call start())
    public void openJournalFromMenu() {
        journal.open();
        journalFromMenu = true;
    }

    // Shows the battle in this window instead of the school; when it's over, the battle calls
    // battleOver. (The quadrangle's monsters are fought out on the map instead: see LastStand. The
    // typing race is kept for any tree that does start a battle.) A floor's boss may start weakened
    // (see Director.foeStartHp).
    private void openBattle(JFrame window, Monster m) {
        battleWindow = window;
        if (typingFight(m)) {
            TypingBattle.fight(this, window, m.enemy, flor.health, flor.kits, flor.items);
        } else {
            BattleScreen.fight(this, window, m.enemy, director.foeStartHp(m), flor.health, flor.kits, flor.items);
        }
    }

    // The battle is over (BattleScreen and TypingBattle call this): the school takes the window back
    public void battleOver(BattleResult result) {
        battleWindow.setContentPane(this);
        battleWindow.revalidate();
        battleWindow.repaint();
        requestFocusInWindow();
        endBattle(battleMonster, result);
    }

    // True if this monster's fight is a typing race (TypingBattle) instead of the word battle
    public static boolean typingFight(Monster m) {
        return m instanceof TreeMonster;
    }

    // After a battle. Won: the monster is gone for good (a boss leaves its loot where it stood), and
    // the player keeps their health and kits. Lost (and gave up): everything goes back to how it was
    // when the player came onto this floor (see Player.respawn).
    public void endBattle(Monster m, BattleResult result) {
        if (result.won()) {
            m.defeated = true;
            flor.health = Math.max(1, result.heroHp());
            flor.kits = result.kitsLeft();
            director.afterWin(m);        // A boss leaves its loot where it stood
        } else {
            flor.respawn();
            showWakeUpNotice();
        }
        lastRoom = currentRoom;
        calm = 60;
        releaseKeys();
        inBattle = false;                // Last: the game loop starts updating again after this
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw the world shifted by the camera: the current room, plus the neighbouring
        // room on whichever side has scrolled into view
        int camX = cameraX();
        int camY = cameraY();
        Graphics2D view = (Graphics2D) g2d.create();
        view.translate(-camX, -camY);
        currentRoom.map.draw(view);
        drawMonsters(view, currentRoom, true);
        // The hallway's room-front screens are narrower than the view, so the view can reach into
        // the neighbours on BOTH sides. Follow the links outwards, drawing each neighbour next to
        // the last one, until the view is covered (or the hallway ends).
        int leftEdge = 0;
        for (Room r = currentRoom.left; r != null && camX < leftEdge; r = r.left) {
            leftEdge -= r.map.widthPx();
            drawNeighbour(view, r, leftEdge);
        }
        int rightEdge = currentRoom.map.widthPx();
        for (Room r = currentRoom.right; r != null && camX + screenWidth > rightEdge; r = r.right) {
            drawNeighbour(view, r, rightEdge);
            rightEdge += r.map.widthPx();
        }
        for (Clue clue : currentRoom.clues) {
            if (!flor.clues.contains(clue.id)) clue.draw(view, ticks);
        }
        if (!(currentRoom.outdoors && lastStand.hidePlayer(ticks))) flor.draw(view);   // (Blinks just after a hit)
        drawMonsters(view, currentRoom, false);
        if (currentRoom.outdoors) lastStand.drawWorld(view, camX, camY, ticks);
        view.dispose();

        // The light: flickering in the hallways, dimmer in the rooms (over the world, under the text and UI)
        Lighting.draw(g2d, currentRoom, flor.x + tileSize / 2 - camX, flor.y + tileSize / 4 - camY, ticks,
                screenWidth, screenHeight - tileSize);

        // The room's name stays fixed on screen (drawn after the camera shift is undone)
        g2d.setColor(Color.WHITE);
        g2d.setFont(ROOM_NAME_FONT);
        String where = "Floor " + currentRoom.floor.number;
        if (currentRoom.outdoors) where = "Outside";
        g2d.drawString(currentRoom.name + "  |  " + where, 20, 40);

        if (currentRoom.outdoors && !ended) lastStand.drawHud(g2d);
        tracker.draw(g2d);
        if (noticeTicks > 0 && !dialogue.isOpen()) drawNotice(g2d);
        dialogue.draw(g2d, screenWidth, screenHeight - tileSize);

        // UI in the bottom row (fixed on screen, drawn after the world so nothing covers it)
        bottomUI.draw(g2d);
        journal.draw(g2d);
        if (ended) endingScreen.draw(g2d, endedRemembered, endedTicks, director.cluesFound(), director.cluesInAll());

        // Flushes graphic pipeline (prevents stutter on non-Windows OS)
        Toolkit.getDefaultToolkit().sync();
        // Do NOT call g2d.dispose() here
    }

    // The camera: how far the view is scrolled right, in pixels, from the current room's left edge.
    // It keeps the player centred, but never shows past the edge of the map unless there's a
    // neighbouring room there to show. So one-screen rooms (classrooms) never scroll, the hallway
    // stops at the stairwells, and a big map (the quadrangle) scrolls until its own edges.
    // In the hallway the player is always inside the current room, so the camera moves at most
    // half a screen past its edges. The stairs screens at the two ends are a full screen wide,
    // so stopping at the edge of the end screen is enough to never show past the building.
    public int cameraX() {
        if (currentRoom.fixedScreen) return 0;   // Screen-to-screen rooms (the library) never scroll
        int camX = flor.x + tileSize / 2 - screenWidth / 2;
        if (currentRoom.left == null) camX = Math.max(camX, 0);
        if (currentRoom.right == null) camX = Math.min(camX, currentRoom.map.widthPx() - screenWidth);
        return camX;
    }

    // The same thing up and down. The view is the screen minus the UI bar along the bottom (576 px),
    // and rooms never link up or down, so the camera always stops at the map's top and bottom edges.
    // A one-screen map is exactly 576 px tall, so it never scrolls; only taller maps (the quadrangle) do.
    public int cameraY() {
        if (currentRoom.fixedScreen) return 0;
        int viewHeight = screenHeight - tileSize;
        int camY = flor.y + tileSize / 2 - viewHeight / 2;
        return Math.max(0, Math.min(camY, currentRoom.map.heightPx() - viewHeight));
    }

    private void drawNeighbour(Graphics2D view, Room neighbour, int offsetX) {
        Graphics2D g = (Graphics2D) view.create();
        g.translate(offsetX, 0);
        neighbour.map.draw(g);
        for (Monster m : neighbour.monsters) m.draw(g, ticks);
        g.dispose();
    }

    // A room's monsters, in two passes so they overlap the player the right way round:
    // behind = the ones whose feet are higher up the screen than the player's (drawn before the player)
    private void drawMonsters(Graphics2D view, Room room, boolean behind) {
        for (Monster m : room.monsters) {
            if ((m.y <= flor.y) == behind) m.draw(view, ticks);
        }
    }

    // The notice: a dark box just above the bottom bar (long lines wrap, "\n" starts a new one), fading out at the end
    private void drawNotice(Graphics2D g2d) {
        Graphics2D g = (Graphics2D) g2d.create();
        g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, noticeTicks / 30f)));
        g.setFont(NOTICE_FONT);
        FontMetrics fm = g.getFontMetrics();
        ArrayList<String> lines = new ArrayList<>();
        for (String paragraph : notice.split("\n")) {   // "\n" starts a new line; long lines wrap
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String trial = word;
                if (line.length() > 0) trial = line + " " + word;
                if (fm.stringWidth(trial) > screenWidth - 120 && line.length() > 0) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(trial);
                }
            }
            lines.add(line.toString());
        }
        int w = 0;
        for (String l : lines) w = Math.max(w, fm.stringWidth(l));
        w += 32;
        int h = 14 + 20 * lines.size();
        int x = (screenWidth - w) / 2, y = screenHeight - tileSize - h - 12;
        g.setColor(new Color(8, 6, 6, 220));
        g.fillRect(x, y, w, h);
        g.setColor(new Color(130, 18, 18));
        g.drawRect(x, y, w, h);
        g.setColor(new Color(206, 200, 184));
        for (int i = 0; i < lines.size(); i++) g.drawString(lines.get(i), x + 16, y + 22 + 20 * i);
        g.dispose();
    }
}