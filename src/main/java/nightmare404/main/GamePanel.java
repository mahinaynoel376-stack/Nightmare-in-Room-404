package nightmare404.main;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import javax.swing.*;

import nightmare404.battle.BattleScreen;
import nightmare404.battle.BattleScreen.BattleResult;
import nightmare404.battle.LastStand;
import nightmare404.battle.TypingBattle;
import nightmare404.entity.Clue;
import nightmare404.entity.FirstBlob;
import nightmare404.entity.Monster;
import nightmare404.entity.Player;
import nightmare404.entity.SubjectZero;
import nightmare404.entity.TreeMonster;
import nightmare404.story.Director;
import nightmare404.story.Story;
import nightmare404.tile.TileSet;
import nightmare404.ui.BottomUI;
import nightmare404.ui.Dawn;
import nightmare404.ui.DialogueBox;
import nightmare404.ui.EndingScreen;
import nightmare404.ui.Journal;
import nightmare404.ui.Lighting;
import nightmare404.ui.LocationSign;
import nightmare404.ui.QuestTracker;
import nightmare404.ui.WakeUp;
import nightmare404.world.Room;
import nightmare404.world.World;

public class GamePanel extends JPanel implements Runnable {
    // Screen size is derived from the tile grid so the map always fits exactly
    public final int tileSize = 32;
    public final int maxScreenCol = 25;
    public final int maxScreenRow = 19;
    public final int screenWidth = tileSize * maxScreenCol;  // 800
    public final int screenHeight = tileSize * maxScreenRow; // 608
    
    Thread gameThread;
    volatile boolean isRunning = false;
    final int FPS = 60;
    // Created once and reused: making a new Font every frame (60 times a second) is wasted work
    private static final Font NOTICE_FONT = new Font("Arial", Font.BOLD, 16);

    // Movement tracking booleans
    public boolean upPressed, downPressed, leftPressed, rightPressed;
    public boolean sprintPressed; // Shift held down

    // The other keys. Swing hears them on its own thread, so each one is just noted here (volatile) and
    // the game loop acts on it in its next frame (update):
    //   E, Enter, Space  read on in the dialogue box, read a page in the journal, back to the menu at the end.
    //                    Holding one down makes Windows repeat it, which would race through the lines,
    //                    so a press only counts again after the key's been let go (advanceHeld).
    //   Space            outside: focus (see LastStand). Letters typed while focusing go to `typed`.
    //   J, Esc           open and close the journal (Esc also stops focusing). Backspace drops a word.
    //   Esc              otherwise: the main menu, with the game paused until Continue.
    //   1-4              outside: use a First Aid Kit, an Old Textbook, a Red Pen, a Hall Pass.
    private volatile boolean advanceHeld, spaceHeld, backHeld, advanceQueued, spaceQueued, journalQueued, backQueued,
            unlockQueued;
    private volatile int itemQueued;
    private final ConcurrentLinkedQueue<Character> typed = new ConcurrentLinkedQueue<>();
    private boolean wasUp, wasDown;   // W and S a frame ago, so the journal moves once per press

    // Entities (public: the UI and the checks in other packages read the player)
    public Player kai;
    BottomUI bottomUI;

    // The story: the opening scene (Kai waking up at his desk), the dialogue box, the Director that
    // decides when the story happens, the journal (J), the quest tracker (top right), the fight outside,
    // and once Subject Zero is beaten, the walk out through the south gate at dawn, and the ending
    public WakeUp wakeUp;
    public final DialogueBox dialogue = new DialogueBox();
    public Director director;
    public Journal journal;
    public LastStand lastStand;
    public Dawn dawn;
    QuestTracker tracker;
    LocationSign locationSign;
    EndingScreen endingScreen;
    public volatile boolean ended;          // The game is over: the ending screen is showing
    public boolean endedRemembered;         // ...and it's the good ending (every clue found)
    private long endedTicks;
    private boolean talking;                // The dialogue box was open last frame

    // The main menu (see Main and MainMenu). Esc, or Enter after the ending, runs onMenu (null in the
    // checks, which have no menu), and the game waits, paused, until Continue calls resume().
    public Runnable onMenu;
    private volatile boolean paused;
    private boolean journalFromMenu;        // The journal was opened from the menu: closing it goes back there

    // The world is a linked list of rooms; currentRoom points at the node the player is in.
    // Moving to another room just means following one of its links (left, right or door).
    public TileSet tiles;
    public World world;
    public Room currentRoom;

    // Battles. Touching a monster opens its battle in this window (see openBattle); while it's on,
    // the world is paused. After a battle the monsters in the room wait a moment (calm), so another
    // one can't grab the player the instant they're back.
    public volatile boolean inBattle;
    private int calm;
    private Room lastRoom;

    // The camera can be sent somewhere else for a moment (Subject Zero rising, see Director.treeFelled):
    // it slides over to look at (watchX, watchY) and stays there until stopWatching(), then slides back
    // to the player. The world waits while it's away.
    private int watchX, watchY;
    private boolean watching;
    private float look;                     // How far over it is: 0 = on the player, 1 = on the watched point
    private static final float LOOK_STEP = 1 / 45f;   // (It takes 45 frames to get there, 3/4 of a second)
    // What starting a battle does. The checks swap this for something that doesn't open a window.
    public Consumer<Monster> battleStarter = m -> SwingUtilities.invokeLater(() -> openBattle(m));
    public long ticks;                      // Frames since the game began (the monsters bob with it)

    // A short message at the bottom of the screen ("The doors won't open...")
    private volatile String notice = "";
    private volatile int noticeTicks;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);

        tiles = new TileSet();                  // Load every tile image once
        world = new World(tiles, tileSize);     // Build and link all the rooms
        currentRoom = world.start;
        kai = new Player(this);
        bottomUI = new BottomUI(this);
        director = new Director(this);
        journal = new Journal(this);
        lastStand = new LastStand(this);
        dawn = new Dawn(this);
        wakeUp = new WakeUp(this);
        tracker = new QuestTracker(this);
        locationSign = new LocationSign(this);
        endingScreen = new EndingScreen(this);
        setupKeyBindings();
        lastRoom = currentRoom;
        // Read the battle's 170,000-word dictionary now, in the background, so the first battle opens at once
        Thread words = new Thread(BattleScreen::dictionary);
        words.setDaemon(true);
        words.start();
    }

    // Every monster beaten and every floor's key in the player's pocket: for the checks that only test walking
    public void peaceful() {
        if (world.firstFight != null) world.firstFight.defeated = true;   // (No first fight in the hallway either)
        for (Monster m : world.monsters) {
            m.defeated = true;
            if (m.loot != null) {
                m.lootTaken = true;
                kai.keys.add(m.loot.key());
            }
        }
        // ...and every locked room open (the Containment Lab, the Principal's office)
        for (nightmare404.world.Floor f = world.top; f != null; f = f.below) {
            for (Room r = f.head; r != null; r = r.right) {
                for (Room behind : r.linkedRooms()) {
                    if (behind.lockedWith != null) kai.keys.add(behind.lockedWith);
                }
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
        return noticeTicks > 0 ? notice : "";
    }

    private void releaseKeys() {
        upPressed = downPressed = leftPressed = rightPressed = sprintPressed = false;
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
        for (String mod : new String[] {"", "shift "}) {
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
            im.put(KeyStroke.getKeyStroke(mod + "pressed ESCAPE"), "back");
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
        am.put("advancePress", action(() -> { if (!advanceHeld) advanceQueued = true; advanceHeld = true; }));
        am.put("advanceRelease", action(() -> advanceHeld = false));
        am.put("spacePress", action(() -> { if (!spaceHeld) spaceQueued = true; spaceHeld = true; }));
        am.put("spaceRelease", action(() -> spaceHeld = false));
        am.put("journal", action(() -> journalQueued = true));
        am.put("back", action(() -> { if (!backHeld) backQueued = true; backHeld = true; }));
        am.put("backRelease", action(() -> backHeld = false));
        am.put("unlock", action(() -> unlockQueued = true));
        for (int n = 1; n <= 4; n++) {
            int item = n;
            am.put("item" + n, action(() -> itemQueued = item));
        }
        am.put("typed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String c = e.getActionCommand();
                if (c != null && c.length() == 1) typed.add(c.charAt(0));
            }
        });

        // Map Actions to toggle booleans
        am.put("upPress", new MoveAction("up", true));
        am.put("downPress", new MoveAction("down", true));
        am.put("leftPress", new MoveAction("left", true));
        am.put("rightPress", new MoveAction("right", true));
        am.put("sprintPress", new MoveAction("sprint", true));

        am.put("upRelease", new MoveAction("up", false));
        am.put("downRelease", new MoveAction("down", false));
        am.put("leftRelease", new MoveAction("left", false));
        am.put("rightRelease", new MoveAction("right", false));
        am.put("sprintRelease", new MoveAction("sprint", false));
    }

    // A key action that just runs this
    private static AbstractAction action(Runnable run) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                run.run();
            }
        };
    }

    private class MoveAction extends AbstractAction {
        String direction;
        boolean pressed;

        MoveAction(String dir, boolean pressed) {
            this.direction = dir;
            this.pressed = pressed;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            switch (direction) {
                case "up" -> upPressed = pressed;
                case "down" -> downPressed = pressed;
                case "left" -> leftPressed = pressed;
                case "right" -> rightPressed = pressed;
                case "sprint" -> sprintPressed = pressed;
            }
        }
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        isRunning = true;
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000.0 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (isRunning) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                if (!paused) {           // (While the menu's showing, the game waits)
                    update();
                    repaint();
                }
                delta--;
            }

            // Yield the CPU between frames instead of spinning at 100% on one core
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                isRunning = false;
            }
        }
    }

    public void update() {
        if (inBattle) return;            // The world waits while a battle is on
        boolean advance = advanceQueued, space = spaceQueued, toJournal = journalQueued, back = backQueued,
                unlock = unlockQueued;
        int item = itemQueued;
        advanceQueued = spaceQueued = journalQueued = backQueued = unlockQueued = false;
        itemQueued = 0;
        List<Character> letters = new ArrayList<>();
        for (Character c; (c = typed.poll()) != null; ) letters.add(c);

        if (ended) {                     // The ending screen: Enter goes back to the main menu
            endedTicks++;
            if ((advance || space) && endedTicks > 150) leaveForMenu();
            return;
        }
        if (wakeUp.playing()) {          // The opening scene: Kai waking up at his desk (it can't be skipped)
            wakeUp.update();
            return;
        }
        if (dialogue.isOpen()) {         // The world waits while someone's talking
            upPressed = downPressed = leftPressed = rightPressed = false;
            dialogue.update(advance || space);
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
        if (back && !(outside && lastStand.focused()) && onMenu != null) {   // (While focusing, Esc stops it)
            paused = true;
            releaseKeys();
            SwingUtilities.invokeLater(onMenu);
            return;
        }
        director.update();               // The opening scene, or somewhere new for the first time
        if (dialogue.isOpen()) return;
        if (outside && !dawn.on()) {     // The fight outside: focus, type, use items
            if (space || back && lastStand.focused()) lastStand.toggleFocus();
            if (unlock) lastStand.dropTarget();
            if (item > 0) lastStand.useItem(item);
            for (char c : letters) lastStand.type(c);
            if (lastStand.focused()) upPressed = downPressed = leftPressed = rightPressed = false;
        }

        ticks++;
        if (noticeTicks > 0) noticeTicks--;
        FirstBlob blob = world.firstFight;
        if (blob != null && blob.falling()) {   // The first fight: the blob drops on Kai (everything waits)
            blob.update(kai);
            if (!blob.falling()) startBattle(blob);   // Landed: it has him
            return;
        }
        if (watching || look > 0) {      // The camera is away (Subject Zero rising): the world waits
            look = watching ? Math.min(1f, look + LOOK_STEP) : Math.max(0f, look - LOOK_STEP);
            SubjectZero zero = world.finalBoss;
            if (watching && look >= 1 && zero != null && zero.rising()) {   // There: it rises
                zero.updateRise();
                if (!zero.rising()) director.zeroRose();   // Up: its words, then the camera comes back
            }
            return;
        }
        if (dawn.leaving()) {            // Out through the south gate: the screen fades, then the last words
            releaseKeys();
            dawn.update();
            return;
        }
        kai.update();
        if (currentRoom != lastRoom) {   // Just walked into a room: the monsters give you a moment
            for (Monster m : lastRoom.monsters) m.chasing = false;   // The ones left behind stop chasing
            if (lastRoom.outdoors || currentRoom.outdoors) lastStand.reset();   // Going out, or back in
            lastRoom = currentRoom;
            calm = Math.max(calm, 30);
            for (Monster m : currentRoom.monsters) m.playerArrived(kai);   // (The quadrangle's trees start to stir)
        }
        if (dawn.on() && currentRoom.outdoors) {   // After Subject Zero: the sun comes up on the way to the gate
            dawn.update();
            if (dialogue.isOpen() || dawn.leaving()) return;
        }
        for (Clue clue : currentRoom.clues) {   // Walking over a clue picks it up
            if (!kai.clues.contains(clue.id) && clue.area().intersects(kai.solidArea)) {
                director.clueFound(clue);
                return;
            }
        }
        if (currentRoom.haze != null && currentRoom.haze.walkedInto(kai.solidArea)) {   // The haze on the stairs
            releaseKeys();
            director.hazeCalls(currentRoom.floor);
            return;
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
            if (m.lootUnder(kai)) {     // Walking over what a boss dropped picks it up
                m.lootTaken = true;
                kai.collect(m.loot);
                director.lootTaken(m);
                return;
            }
            if (m.defeated) continue;
            m.update(kai);
            if (m.touches(kai)) {
                startBattle(m);
                return;
            }
        }
    }

    // The player touched a monster (or the story started a fight: the first one, see Director): a floor's
    // boss or a monster met for the first time has its say first, then the world pauses and its battle opens
    public void startBattle(Monster m) {
        releaseKeys();                   // Keys let go during the battle would otherwise stay "held"
        director.beforeBattle(m, () -> {
            inBattle = true;
            battleStarter.accept(m);
        });
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
            if (journalFromMenu && onMenu != null) {   // Opened from the menu: back there
                journalFromMenu = false;
                paused = true;
                SwingUtilities.invokeLater(onMenu);
            }
        } else if (advance && journal.chosen() != null) {
            journal.close();
            director.reread(journal.chosen(), journal::open);   // Back to the journal after reading
        }
    }

    // Outside, the player's health ran out (see LastStand): back to the main entrance with the monsters
    // beaten so far still beaten, and at least half health. The trees still standing start over.
    public void loseOutside() {
        kai.respawn();
        kai.health = Math.max(kai.health, LastStand.RESPAWN_HP);
        lastStand.reset();
        for (Monster m : currentRoom.monsters) m.playerArrived(kai);
        lastRoom = currentRoom;
        calm = 60;
        releaseKeys();
        director.wokeUp(currentRoom.outdoors);
    }

    // Subject Zero is beaten and the last words are said: show the ending screen
    public void end(boolean remembered) {
        endedRemembered = remembered;
        endedTicks = 0;
        ended = true;
    }

    // After the ending: this game is over, so its loop stops, and the main menu takes its place
    private void leaveForMenu() {
        isRunning = false;               // This game's loop stops after this frame
        if (onMenu != null) SwingUtilities.invokeLater(onMenu);
    }

    // Stops this game for good (a new game was started from the menu)
    public void stop() {
        isRunning = false;
    }

    // Back from the menu (Continue): the game carries on. Keys pressed or let go while the menu was
    // showing never reached the game, so it starts again with none held down.
    public void resume() {
        releaseKeys();
        advanceHeld = spaceHeld = backHeld = false;
        advanceQueued = spaceQueued = journalQueued = backQueued = unlockQueued = false;
        itemQueued = 0;
        typed.clear();
        paused = false;
    }

    // The menu's Journal: the journal opens, and closing it goes back to the menu (call resume() after)
    public void openJournalFromMenu() {
        journal.open();
        journalFromMenu = true;
    }

    // Shows the battle in this window instead of the school, and puts the school back when it's over.
    // (The quadrangle's monsters are fought out on the map instead: see LastStand. The typing race is
    // kept for any tree that does start a battle.) A floor's boss may start weakened (see Director).
    private void openBattle(Monster m) {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        Consumer<BattleResult> back = result -> {
            frame.setContentPane(this);
            frame.revalidate();
            frame.repaint();
            requestFocusInWindow();
            endBattle(m, result);
        };
        if (typingFight(m)) TypingBattle.fight(frame, m.enemy, kai.health, kai.kits, kai.items, back);
        else BattleScreen.fight(frame, m.enemy, director.foeStartHp(m), kai.health, kai.kits, kai.items, back);
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
            kai.health = Math.max(1, result.heroHp());
            kai.kits = result.kitsLeft();
            director.afterWin(m);        // A boss leaves its loot where it stood
        } else {
            kai.respawn();
            director.wokeUp(currentRoom.outdoors);
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
        dawn.tilt(view, screenWidth, screenHeight - tileSize);   // (The bad ending: the world sways)
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
            if (!kai.clues.contains(clue.id)) clue.draw(view, ticks);
        }
        if (wakeUp.playing()) {
            wakeUp.drawKai(view);                   // Still at his desk (the opening scene)
        } else if (!(currentRoom.outdoors && lastStand.hidePlayer(ticks))) {   // (Blinks just after a hit)
            dawn.drawDouble(view, kai);             // (The bad ending: seeing double)
            kai.draw(view);
            kai.drawStamina(view);                  // (Over his head, after running)
        }
        drawMonsters(view, currentRoom, false);
        if (world.firstFight != null && world.firstFight.room == currentRoom) world.firstFight.draw(view, ticks);
        if (currentRoom.haze != null) currentRoom.haze.draw(view, ticks);
        if (currentRoom.outdoors) lastStand.drawWorld(view, camX, camY, ticks);
        view.dispose();

        // The light: flickering in the hallways, dimmer in the rooms (over the world, under the text and UI)
        Lighting.draw(g2d, currentRoom, kai.x + tileSize / 2 - camX, kai.y + tileSize / 4 - camY, ticks,
                screenWidth, screenHeight - tileSize);
        dawn.drawLight(g2d, screenWidth, screenHeight - tileSize);   // After Subject Zero: the sun coming up

        // The room's name, the quest tracker, the notices and the bottom bar are hidden in the opening
        // scene, while Subject Zero rises, and once Kai's out through the south gate
        boolean hud = !wakeUp.playing() && !watching() && !dawn.leaving();
        if (hud) {
            // Where the player is: a brass plaque, top left (drawn after the camera shift is undone)
            locationSign.draw(g2d);
            if (currentRoom.outdoors && !ended && !dawn.on()) lastStand.drawHud(g2d);
            tracker.draw(g2d);
            if (noticeTicks > 0 && !dialogue.isOpen()) drawNotice(g2d);
        }
        dawn.drawFade(g2d, screenWidth, screenHeight);   // Out through the gate: fading out
        dialogue.draw(g2d, screenWidth, bottomUI.top());   // Above the ID card

        // The bottom UI: the ID card and the backpack (fixed on screen, drawn after the world so nothing covers it)
        if (hud) bottomUI.draw(g2d);
        journal.draw(g2d);
        if (wakeUp.playing()) wakeUp.drawOverlay(g2d, screenWidth, screenHeight);
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
        int camX = lookingAtX() - screenWidth / 2;
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
        int camY = lookingAtY() - viewHeight / 2;
        return Math.max(0, Math.min(camY, currentRoom.map.heightPx() - viewHeight));
    }

    // The point the camera centres on: the player, or part of the way (look) to the watched point.
    // (smooth: slow to start and slow to stop)
    private int lookingAtX() {
        int playerX = kai.x + tileSize / 2;
        return playerX + Math.round((watchX - playerX) * smooth(look));
    }

    private int lookingAtY() {
        int playerY = kai.y + tileSize / 2;
        return playerY + Math.round((watchY - playerY) * smooth(look));
    }

    private static float smooth(float t) {
        return t * t * (3 - 2 * t);
    }

    // Sends the camera to look at (x, y) in the current room, until stopWatching()
    public void watch(int x, int y) {
        watchX = x;
        watchY = y;
        watching = true;
        releaseKeys();
    }

    // The camera goes back to the player
    public void stopWatching() {
        watching = false;
    }

    // True while the camera is away from the player (or on its way back)
    public boolean watching() {
        return watching || look > 0;
    }

    private void drawNeighbour(Graphics2D view, Room neighbour, int offsetX) {
        Graphics2D g = (Graphics2D) view.create();
        g.translate(offsetX, 0);
        neighbour.map.draw(g);
        for (Monster m : neighbour.monsters) m.draw(g, ticks);
        if (neighbour.haze != null) neighbour.haze.draw(g, ticks);
        g.dispose();
    }

    // A room's monsters, in two passes so they overlap the player the right way round:
    // behind = the ones whose feet are higher up the screen than the player's (drawn before the player)
    private void drawMonsters(Graphics2D view, Room room, boolean behind) {
        for (Monster m : room.monsters) {
            if ((m.y <= kai.y) == behind) m.draw(view, ticks);
        }
    }

    // The notice: a dark box just above the bottom UI (long lines wrap, "\n" starts a new one), fading out at the end
    private void drawNotice(Graphics2D g2d) {
        Graphics2D g = (Graphics2D) g2d.create();
        g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, noticeTicks / 30f)));
        g.setFont(NOTICE_FONT);
        FontMetrics fm = g.getFontMetrics();
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (String paragraph : notice.split("\n")) {   // "\n" starts a new line; long lines wrap
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String trial = line.length() == 0 ? word : line + " " + word;
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
        int x = (screenWidth - w) / 2, y = bottomUI.top() - h - 12;
        g.setColor(new Color(8, 6, 6, 220));
        g.fillRect(x, y, w, h);
        g.setColor(new Color(130, 18, 18));
        g.drawRect(x, y, w, h);
        g.setColor(new Color(206, 200, 184));
        for (int i = 0; i < lines.size(); i++) g.drawString(lines.get(i), x + 16, y + 22 + 20 * i);
        g.dispose();
    }
}