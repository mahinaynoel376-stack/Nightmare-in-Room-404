package nightmare404.battle;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import nightmare404.battle.BattleScreen.Attack;
import nightmare404.battle.BattleScreen.EnemyDef;
import nightmare404.entity.Hunter;
import nightmare404.entity.Monster;
import nightmare404.entity.Player;
import nightmare404.entity.Sapling;
import nightmare404.entity.SubjectZero;
import nightmare404.entity.TreeMonster;
import nightmare404.main.GamePanel;
import nightmare404.story.Story;
import nightmare404.ui.HealthBar;
import nightmare404.world.Room;

// THE LAST STAND: the quadrangle's fight, out on the map, with no battle screen.
//
//   The trees wake and hunt the player (TreeMonster), all at once. Each hunting monster has a WORD
//   over its head. Press SPACE to FOCUS: the world slows right down, the player stands still, and the
//   keyboard types. Type a monster's word (its first letter picks it) and a STRIKE flies at it: the
//   letters themselves. Longer words hit harder, and so does a STREAK of words without a mistake.
//   Focus runs out (the meter at the top), and comes back while you move, so it's a rhythm: run to
//   somewhere safe, focus, strike, run again.
//   Strikes fly in straight lines: walls, the bus and trees stop them, but fences, benches and bushes
//   don't (TileSet.isShootThrough), and anything right next to you is cleared, so you can strike out
//   from behind the book barricade, the bus or the court's fence. The quadrangle's strongholds matter.
//   A monster that reaches you hits you, knocks you back and breaks your focus. It reels for a moment.
//   Keys 1-4 use what you carry: a First Aid Kit, an Old Textbook (halves the next hit), a Red Pen
//   (doubles the next strike), a Hall Pass (everything out here stands still for 3 seconds).
//
//   When the last tree falls, SUBJECT ZERO wakes by the south gate (story.Director) and comes for you.
//   Its words are longer. Every few seconds the ground cracks round you (a warning), then roots burst
//   up: get out of the way. Hurt, it calls up SAPLINGS through the gates, twice.
//
//   Every monster felled out here saves your progress; going down brings you back to the main entrance
//   with the trees beaten so far still beaten (see GamePanel.loseOutside).
//
// GamePanel calls update() every frame while the player is outside, and passes on the keys.
// The game loop changes the lists here (strikes, words, roots...) while Swing draws them on its own
// thread, so the public methods are synchronized: one thread at a time, or a list could change while
// it's being drawn.
public class LastStand {
    // ---- Focus ----
    public static final int FOCUS_MAX = 300;           // A full meter: 5 seconds of focus...
    public static final int FOCUS_MIN = 60;            // ...and you need at least 1 second's worth to start
    static final double FOCUS_REFILL = 0.75;           // It comes back while you're not focusing (empty to full in ~7 s)
    static final int FOCUS_PER_WORD = 40;              // Each word typed gives some back
    static final int MISTAKE_COST = 25;                // A wrong key costs some
    public static final double SLOW = 0.3;             // The world's speed while you focus

    // ---- Strikes ----
    public static final double DAMAGE_PER_LETTER = 3.5;
    static final double STREAK_BONUS = 0.10;           // +10% for each word in a row without a mistake...
    static final int STREAK_MAX = 5;                   // ...up to +50%
    static final double RED_PEN = 2.0;
    static final double TEXTBOOK = 0.5;
    static final int STRIKE_SPEED = 12;                // Pixels per tick
    public static final int CLEAR_DISTANCE = 52;       // Strikes fly over anything this close to the player (the tile beside you)

    // ---- Being hit ----
    static final int HURT_TICKS = 70;                  // Can't be hit again for this long
    static final int KNOCKBACK = 44;                   // Pixels
    static final int REEL = 50;                        // Ticks a monster stands reeling after it hits you
    public static final int TREE_HEAL = 10;            // Health back for every tree felled
    public static final int RESPAWN_HP = 50;           // Going down out here, you come back with at least this
    static final int HALL_PASS = 180;                  // Ticks everything stands still

    // ---- Subject Zero ----
    static final int ROOT_EVERY = 200;                 // Ticks between its root attacks (faster as it weakens)
    public static final int ROOT_WARN = 75;            // The ground cracks this long before the roots burst...
    static final int ROOT_BURST = 16;                  // ...and they stay up this long
    static final int ROOT_RADIUS = 24;
    public static final int ROOT_DAMAGE = 12;
    static final Point[] GATES = {new Point(1, 17), new Point(48, 10), new Point(27, 34)};   // Just inside each

    private static final Font WORD_FONT = new Font(Font.MONOSPACED, Font.BOLD, 15);
    private static final Font STRIKE_FONT = new Font(Font.MONOSPACED, Font.BOLD, 14);
    private static final Font FLOAT_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    private static final Font HUD_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    private static final Color PALE = new Color(206, 200, 184);
    private static final Color TYPED = new Color(130, 226, 120);
    private static final Color GOLD = new Color(232, 184, 70);
    private static final Color BLOOD = new Color(170, 18, 22);
    private static final Color FOCUS_BLUE = new Color(110, 160, 255);

    // A strike in flight: the word, flying at its target
    private static final class Strike {
        double x, y, travelled;
        final Hunter target;
        final int damage;
        final String word, note;

        Strike(double x, double y, Hunter target, int damage, String word, String note) {
            this.x = x; this.y = y; this.target = target; this.damage = damage; this.word = word; this.note = note;
        }
    }

    // Roots about to burst out of the ground (warn counting down), or bursting (burst counting down)
    private static final class Eruption {
        final int x, y;
        int warn = ROOT_WARN, burst;

        Eruption(int x, int y) { this.x = x; this.y = y; }
    }

    private record FloatText(double x, double y, String text, Color color, int born) {}

    private final GamePanel gp;
    private final Random random = new Random(404);
    private boolean focused;
    private double focus = FOCUS_MAX;
    private Hunter target;                  // The monster whose word is being typed (null: none yet)
    private int typed;                      // How many letters of its word are typed
    public int streak;
    public boolean redPenReady, textbookReady;
    private int hurt, freeze, mistakeFlash, rootTimer = ROOT_EVERY, saplingWaves;
    private double clock;                   // Slow motion: the world moves one step each time this reaches 1
    private long now;                       // Frames since the player came out (for the floating text)
    public Monster lastHitBy;               // The last monster to hit the player (for the checks)
    private final Map<Hunter, String> words = new HashMap<>();
    private final Map<Hunter, Integer> health = new HashMap<>();
    private final List<Sapling> saplings = new ArrayList<>();
    private final List<Strike> strikes = new ArrayList<>();
    private final List<Eruption> eruptions = new ArrayList<>();
    private final List<FloatText> floats = new ArrayList<>();

    public LastStand(GamePanel gp) {
        this.gp = gp;
    }

    // ------------------------------------------------------------------------------------ the keys

    public synchronized boolean focused() {
        return focused;
    }

    public synchronized double focus() {
        return focus;
    }

    // SPACE: start focusing (if there's enough focus), or stop
    public synchronized void toggleFocus() {
        setFocus(!focused);
    }

    private void setFocus(boolean on) {
        if (on && focus < FOCUS_MIN) {
            gp.showNotice("Not enough focus yet: keep moving.", 90);
            return;
        }
        focused = on;
        if (!on) dropTarget();
    }

    // BACKSPACE: give up on the word being typed (to pick another monster)
    public synchronized void dropTarget() {
        target = null;
        typed = 0;
    }

    // A letter typed while focusing. The first letter picks the monster (the nearest, if two words start
    // the same); the rest must follow its word. A wrong key costs focus and breaks the streak.
    public synchronized void type(char c) {
        if (!focused) return;
        c = Character.toLowerCase(c);
        if (c < 'a' || c > 'z') return;
        if (target != null && (!target.hunting() || !words.containsKey(target))) dropTarget();
        if (target == null) {
            Hunter best = null;
            for (Hunter h : foesInView()) {
                String w = words.get(h);
                if (w != null && w.charAt(0) == c && (best == null || distance(h) < distance(best))) best = h;
            }
            if (best == null) {
                mistake();
                return;
            }
            target = best;
            typed = 1;
        } else if (words.get(target).charAt(typed) == c) {
            typed++;
        } else {
            mistake();
            return;
        }
        if (typed == words.get(target).length()) launch();
    }

    private void mistake() {
        streak = 0;
        focus = Math.max(0, focus - MISTAKE_COST);
        mistakeFlash = 12;
    }

    // The word's typed: it flies at the monster, which gets a new word straight away
    private void launch() {
        Player p = gp.flor;
        String word = words.get(target);
        double damage = word.length() * DAMAGE_PER_LETTER * (1 + STREAK_BONUS * Math.min(streak, STREAK_MAX))
                * (redPenReady ? RED_PEN : 1);
        String note = redPenReady ? "RED PEN!" : streak >= 2 ? "x" + (streak + 1) : "";
        redPenReady = false;
        streak++;
        strikes.add(new Strike(p.x + 16, p.y, target, (int) Math.round(damage), word, note));
        focus = Math.min(FOCUS_MAX, focus + FOCUS_PER_WORD);
        words.put(target, newWord(target));
        dropTarget();
    }

    // Keys 1-4: a First Aid Kit, an Old Textbook, a Red Pen, a Hall Pass
    public synchronized void useItem(int n) {
        Player p = gp.flor;
        switch (n) {
            case 1 -> {
                if (p.kits <= 0 || p.health >= p.maxHealth) return;
                p.kits--;
                int before = p.health;
                p.health = Math.min(p.maxHealth, p.health + BattleScreen.KIT_HEAL);
                say(p.x + 16, p.y - 40, "+" + (p.health - before) + " HP", TYPED);
            }
            case 2 -> {
                if (p.items.textbooks <= 0 || textbookReady) return;
                p.items.textbooks--;
                textbookReady = true;
                gp.showNotice("Old Textbook: the next hit is halved.", 120);
            }
            case 3 -> {
                if (p.items.redPens <= 0 || redPenReady) return;
                p.items.redPens--;
                redPenReady = true;
                gp.showNotice("Red Pen: your next strike hits twice as hard.", 120);
            }
            case 4 -> {
                if (p.items.hallPasses <= 0 || freeze > 0) return;
                p.items.hallPasses--;
                freeze = HALL_PASS;
                gp.showNotice("Hall Pass: everything out here stands still for a moment.", 150);
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------------------------ the fight

    // Everything back to how it is when the player comes outside (or goes down): no strikes, no roots,
    // no saplings, every monster still standing at full health with no word yet
    public synchronized void reset() {
        focused = false;
        focus = FOCUS_MAX;
        dropTarget();
        streak = 0;
        redPenReady = textbookReady = false;
        hurt = freeze = mistakeFlash = saplingWaves = 0;
        rootTimer = ROOT_EVERY;
        clock = 0;
        strikes.clear();
        eruptions.clear();
        floats.clear();
        saplings.clear();
        words.clear();
        health.clear();
    }

    // One frame outside. While focusing the world moves at SLOW speed: a step every few frames.
    public synchronized void update() {
        Room quad = gp.currentRoom;
        now++;
        if (hurt > 0) hurt--;
        if (mistakeFlash > 0) mistakeFlash--;
        if (focused) {
            focus -= 1;
            if (focus <= 0) {
                focus = 0;
                setFocus(false);
                gp.showNotice(Story.OUT_OF_FOCUS, 150);
            }
        } else {
            focus = Math.min(FOCUS_MAX, focus + FOCUS_REFILL);
        }
        floats.removeIf(f -> now - f.born() > 50);
        clock += focused ? SLOW : 1;
        while (clock >= 1) {
            clock -= 1;
            step(quad);
            if (gp.currentRoom != quad || gp.dialogue.isOpen() || gp.ended) {
                clock = 0;
                return;
            }
        }
    }

    // One step of the world out here
    private void step(Room quad) {
        Player p = gp.flor;
        if (freeze > 0) freeze--;
        List<Hunter> all = new ArrayList<>();
        for (Monster m : quad.monsters) {
            if (m instanceof Hunter h && !h.defeated) all.add(h);
        }
        all.addAll(saplings);
        for (Hunter h : all) {
            if (h.defeated) continue;
            if (freeze == 0 || !h.hunting()) h.update(p);
            if (!h.hunting()) continue;
            health.putIfAbsent(h, h.enemy.maxHp());
            if (!words.containsKey(h)) words.put(h, newWord(h));
            if (hurt == 0 && h.touches(p)) {
                grabbed(h);
                if (p.health <= 0) {
                    gp.loseOutside();
                    return;
                }
            }
        }
        moveStrikes(quad);
        SubjectZero zero = gp.world.finalBoss;
        if (zero != null && zero.hunting()) {
            roots(zero);
            if (p.health <= 0) {
                gp.loseOutside();
                return;
            }
            callSaplings(zero, quad);
        }
    }

    // A monster has hold of the player: it hits them, knocks them back and breaks their focus
    private void grabbed(Hunter h) {
        Attack attack = pickAttack(h.enemy);
        hit(attack.damage(), h.centreX(), h.y + 24);
        h.stunned = REEL;
        lastHitBy = h;
    }

    private void hit(int damage, double fromX, double fromY) {
        Player p = gp.flor;
        if (textbookReady) {
            damage = (int) Math.round(damage * TEXTBOOK);
            textbookReady = false;
        }
        p.health = Math.max(0, p.health - damage);
        hurt = HURT_TICKS;
        streak = 0;
        setFocus(false);
        say(p.x + 16, p.y - 40, "-" + damage, new Color(235, 50, 40));
        double dx = p.solidArea.getCenterX() - fromX, dy = p.solidArea.getCenterY() - fromY;
        double d = Math.max(1, Math.hypot(dx, dy));
        p.push((int) Math.round(dx / d * KNOCKBACK), (int) Math.round(dy / d * KNOCKBACK));
    }

    private void moveStrikes(Room quad) {
        for (Iterator<Strike> it = strikes.iterator(); it.hasNext(); ) {
            Strike s = it.next();
            if (s.target.defeated || !s.target.hunting()) {
                it.remove();
                continue;
            }
            double dx = s.target.centreX() - s.x, dy = s.target.centreY() - s.y, d = Math.hypot(dx, dy);
            if (d <= STRIKE_SPEED) {
                it.remove();
                impact(s);
                continue;
            }
            s.x += dx / d * STRIKE_SPEED;
            s.y += dy / d * STRIKE_SPEED;
            s.travelled += STRIKE_SPEED;
            boolean nearTarget = d < 30;
            if (s.travelled > CLEAR_DISTANCE && !nearTarget
                    && quad.map.stopsShots((int) Math.floor(s.x / 32), (int) Math.floor(s.y / 32))) {
                it.remove();
                say(s.x, s.y - 10, "BLOCKED", PALE);
            }
        }
    }

    // A strike lands
    private void impact(Strike s) {
        Hunter h = s.target;
        int left = health.getOrDefault(h, h.enemy.maxHp()) - s.damage;
        say(h.centreX(), h.centreY() - 30, "-" + s.damage + (s.note.isEmpty() ? "" : "  " + s.note), GOLD);
        h.stunned = Math.max(h.stunned, 8);   // It flinches
        if (h instanceof SubjectZero z) z.hurtPose = 14;
        if (left <= 0) fell(h);
        else health.put(h, left);
    }

    // A monster is brought down: a tree leaves a stump and the player catches their breath (and their
    // progress is saved); the last tree wakes Subject Zero; Subject Zero ends the game
    public synchronized void fell(Hunter h) {
        h.defeated = true;
        words.remove(h);
        health.remove(h);
        if (target == h) dropTarget();
        if (h instanceof Sapling s) {
            saplings.remove(s);
            say(h.centreX(), h.centreY(), "snap", PALE);
            return;
        }
        Player p = gp.flor;
        if (h instanceof TreeMonster) {
            p.health = Math.min(p.maxHealth, p.health + TREE_HEAL);
            p.saveProgress();
            gp.director.treeFelled(TREE_HEAL, gp.world.treesLeft());
        } else if (h instanceof SubjectZero) {
            focused = false;
            saplings.clear();
            eruptions.clear();
            strikes.clear();
            gp.director.zeroBeaten();
        }
    }

    // Subject Zero's roots: every few seconds the ground cracks under and around the player; a moment
    // later roots burst up there
    private void roots(SubjectZero zero) {
        Player p = gp.flor;
        if (freeze == 0 && --rootTimer <= 0) {
            double weakness = 1 - (double) health.getOrDefault(zero, zero.enemy.maxHp()) / zero.enemy.maxHp();
            rootTimer = (int) Math.round(ROOT_EVERY * (1 - 0.4 * weakness));
            int px = (int) p.solidArea.getCenterX(), py = (int) p.solidArea.getCenterY();
            eruptions.add(new Eruption(px, py));                 // Right where the player stands...
            int more = 2 + (int) Math.round(weakness * 2);       // ...and round about
            for (int i = 0; i < more; i++) {
                double angle = random.nextDouble() * Math.PI * 2, r = 44 + random.nextInt(44);
                eruptions.add(new Eruption(px + (int) (Math.cos(angle) * r), py + (int) (Math.sin(angle) * r)));
            }
            zero.attackPose = 30;
            gp.director.groundCracks();                         // (The first time, Flor shouts)
        }
        for (Iterator<Eruption> it = eruptions.iterator(); it.hasNext(); ) {
            Eruption e = it.next();
            if (e.warn > 0) {
                if (--e.warn == 0) {
                    e.burst = ROOT_BURST;
                    double d = Math.hypot(p.solidArea.getCenterX() - e.x, p.solidArea.getCenterY() - e.y);
                    if (hurt == 0 && d <= ROOT_RADIUS + 6) {
                        hit(ROOT_DAMAGE, e.x, e.y + 8);
                        lastHitBy = zero;
                    }
                }
            } else if (--e.burst <= 0) {
                it.remove();
            }
        }
    }

    // Hurt to two thirds, then to a third, Subject Zero calls up two saplings through the gates
    private void callSaplings(SubjectZero zero, Room quad) {
        double left = (double) health.getOrDefault(zero, zero.enemy.maxHp()) / zero.enemy.maxHp();
        if (!(saplingWaves == 0 && left <= 2 / 3.0 || saplingWaves == 1 && left <= 1 / 3.0)) return;
        saplingWaves++;
        for (int i = 0; i < 2; i++) {
            Point gate = GATES[(saplingWaves + i) % GATES.length];
            Sapling s = new Sapling(QuadrangleEnemies.SAPLING, quad, 32, gate.x, gate.y);
            saplings.add(s);
        }
        gp.director.saplingsCalled();
    }

    // ------------------------------------------------------------------------------------ the words

    // A new word for a monster: short for the trees (longer as they weaken), long for Subject Zero, very
    // short for the saplings. No two monsters' words start with the same letter, so the first letter
    // typed always picks one.
    private String newWord(Hunter h) {
        double weakness = 1 - (double) health.getOrDefault(h, h.enemy.maxHp()) / h.enemy.maxHp();
        int shortest, longest;
        if (h instanceof SubjectZero) {
            shortest = 5 + (int) Math.round(weakness * 2);
            longest = 7 + (int) Math.round(weakness * 4);
        } else if (h instanceof Sapling) {
            shortest = 3;
            longest = 4;
        } else {
            shortest = 3 + (int) Math.round(weakness * 2);
            longest = 5 + (int) Math.round(weakness * 3);
        }
        Set<Character> taken = new HashSet<>();
        for (Map.Entry<Hunter, String> e : words.entrySet()) {
            if (e.getKey() != h) taken.add(e.getValue().charAt(0));
        }
        String old = words.get(h);
        List<String> fit = new ArrayList<>(), anyLength = new ArrayList<>();
        for (String w : TypingBattle.words()) {
            if (taken.contains(w.charAt(0)) || w.equals(old)) continue;
            anyLength.add(w);
            if (w.length() >= shortest && w.length() <= longest) fit.add(w);
        }
        List<String> from = !fit.isEmpty() ? fit : !anyLength.isEmpty() ? anyLength : TypingBattle.words();
        return from.get(random.nextInt(from.size()));
    }

    // The monsters that can be struck right now: hunting, and on screen
    public synchronized List<Hunter> foesInView() {
        List<Hunter> foes = new ArrayList<>();
        int camX = gp.cameraX(), camY = gp.cameraY();
        Rectangle view = new Rectangle(camX - 16, camY - 16, gp.screenWidth + 32, gp.screenHeight - gp.tileSize + 32);
        for (Hunter h : words.keySet()) {
            if (h.hunting() && !h.defeated && view.contains(h.centreX(), h.centreY())) foes.add(h);
        }
        return foes;
    }

    // For the checks and the drawing
    public synchronized String wordOf(Hunter h) {
        return words.get(h);
    }

    public synchronized int healthOf(Hunter h) {
        return health.getOrDefault(h, h.enemy.maxHp());
    }

    public synchronized Hunter target() {
        return target;
    }

    public synchronized int strikesInFlight() {
        return strikes.size();
    }

    public synchronized int eruptions() {
        return eruptions.size();
    }

    public synchronized List<Sapling> saplings() {
        return List.copyOf(saplings);
    }

    public synchronized boolean frozen() {
        return freeze > 0;
    }

    // True on the frames the player should blink (just hit: can't be hit again yet)
    public synchronized boolean hidePlayer(long ticks) {
        return hurt > 0 && (ticks / 4) % 2 == 0;
    }

    private double distance(Hunter h) {
        return Math.hypot(h.centreX() - gp.flor.solidArea.getCenterX(), h.centreY() - gp.flor.solidArea.getCenterY());
    }

    private static Attack pickAttack(EnemyDef enemy) {
        List<Attack> choices = new ArrayList<>();
        for (Attack a : enemy.attacks()) {
            if (a.damage() <= 0) continue;
            for (int i = 0; i < Math.max(1, a.weight()); i++) choices.add(a);
        }
        if (choices.isEmpty()) return new Attack("Strike", 8, 0, 0, 1, "It strikes.");
        return choices.get((int) (Math.random() * choices.size()));
    }

    private void say(double x, double y, String text, Color color) {
        floats.add(new FloatText(x, y, text, color, (int) now));
    }

    // ------------------------------------------------------------------------------------ drawing

    // In the world (the camera's shift already applied): the roots, the saplings, the strikes, the
    // focus shade, then every monster's word on top
    public synchronized void drawWorld(Graphics2D g2d, int camX, int camY, long ticks) {
        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        for (Eruption e : eruptions) drawEruption(g, e, ticks);
        for (Sapling s : saplings) s.draw(g, ticks);
        g.setFont(STRIKE_FONT);
        for (Strike s : strikes) {
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(s.word);
            g.setColor(new Color(110, 160, 255, 70));
            g.fillOval((int) s.x - w / 2 - 6, (int) s.y - 14, w + 12, 20);
            g.setColor(new Color(220, 235, 255));
            g.drawString(s.word, (int) s.x - w / 2, (int) s.y);
        }
        if (focused) {
            g.setColor(new Color(8, 12, 40, 95));
            g.fillRect(camX, camY, gp.screenWidth, gp.screenHeight - gp.tileSize);
        }
        g.setFont(WORD_FONT);
        FontMetrics fm = g.getFontMetrics();
        for (Hunter h : foesInView()) {
            String word = words.get(h);
            boolean locked = h == target;
            int w = fm.stringWidth(word) + 12, cx = h.centreX();
            int top = h.y + 32 - h.drawnHeight() - 34;   // The word, with room for its health bar under it
            int bx = cx - w / 2;
            int alpha = focused ? 225 : 150;
            g.setColor(new Color(12, 10, 10, alpha));
            g.fillRoundRect(bx, top, w, 20, 6, 6);
            g.setColor(locked ? GOLD : new Color(130, 18, 18, alpha));
            g.drawRoundRect(bx, top, w, 20, 6, 6);
            int typedHere = locked ? typed : 0;
            g.setColor(TYPED);
            g.drawString(word.substring(0, typedHere), bx + 6, top + 15);
            g.setColor(new Color(240, 236, 226, focused ? 255 : 170));
            g.drawString(word.substring(typedHere), bx + 6 + fm.stringWidth(word.substring(0, typedHere)), top + 15);
            int max = h.enemy.maxHp();
            if (max > 1) {                              // Its health, under the word (see HealthBar)
                int hp = health.getOrDefault(h, max);
                int barW = HealthBar.width(HealthBar.Style.ENEMY, 1);
                HealthBar.draw(g, HealthBar.Style.ENEMY, hp, max, cx - barW / 2, top + 22, 1, alpha / 255f);
            }
        }
        g.setFont(FLOAT_FONT);
        for (FloatText f : floats) {
            int age = (int) (now - f.born());
            int alpha = Math.max(0, 255 - age * 5);
            FontMetrics ffm = g.getFontMetrics();
            g.setColor(new Color(0, 0, 0, alpha / 2));
            g.drawString(f.text(), (int) f.x() - ffm.stringWidth(f.text()) / 2 + 1, (int) f.y() - age / 2 + 1);
            g.setColor(new Color(f.color().getRed(), f.color().getGreen(), f.color().getBlue(), alpha));
            g.drawString(f.text(), (int) f.x() - ffm.stringWidth(f.text()) / 2, (int) f.y() - age / 2);
        }
        g.dispose();
    }

    private void drawEruption(Graphics2D g, Eruption e, long ticks) {
        if (e.warn > 0) {                               // The warning: cracks spreading, glowing red
            double k = 1 - (double) e.warn / ROOT_WARN;
            int r = (int) Math.round(ROOT_RADIUS * (0.4 + 0.6 * k));
            g.setColor(new Color(30, 14, 8, 150));
            g.fillOval(e.x - r, e.y - r / 2, r * 2, r);
            boolean pulse = (ticks / 6) % 2 == 0;
            g.setColor(new Color(220, 40, 20, pulse ? 200 : 120));
            g.setStroke(new BasicStroke(2));
            g.drawOval(e.x - ROOT_RADIUS, e.y - ROOT_RADIUS / 2, ROOT_RADIUS * 2, ROOT_RADIUS);
            g.setColor(new Color(20, 8, 4));
            for (int i = 0; i < 5; i++) {
                double a = i * 1.3 + e.x;
                g.drawLine(e.x, e.y, e.x + (int) (Math.cos(a) * r), e.y + (int) (Math.sin(a) * r / 2));
            }
        } else {                                        // The roots, bursting up
            int h = (int) Math.round(30 * Math.sin(Math.PI * e.burst / ROOT_BURST)) + 6;
            g.setColor(new Color(60, 30, 12));
            g.fillOval(e.x - ROOT_RADIUS, e.y - ROOT_RADIUS / 2, ROOT_RADIUS * 2, ROOT_RADIUS);
            for (int i = -2; i <= 2; i++) {
                int bx = e.x + i * 9;
                g.setColor(Color.BLACK);
                g.fillPolygon(new int[] {bx - 5, bx + 5, bx + i}, new int[] {e.y + 2, e.y + 2, e.y - h + Math.abs(i) * 4 - 1}, 3);
                g.setColor(new Color(110, 66, 30));
                g.fillPolygon(new int[] {bx - 3, bx + 3, bx + i}, new int[] {e.y + 1, e.y + 1, e.y - h + Math.abs(i) * 4 + 2}, 3);
            }
        }
    }

    // On the screen: the focus meter (top middle), what's ready, and a red flash for a wrong key
    public synchronized void drawHud(Graphics2D g2d) {
        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(HUD_FONT);
        String label = focused ? "FOCUSED: type a word" + (streak >= 2 ? "   streak x" + streak : "")
                : "SPACE focus   1-4 items";
        int labelW = g.getFontMetrics().stringWidth(label);
        int middle = 440;                               // A little right of centre, clear of the room's name
        int w = 220, x = middle - w / 2, y = 16;
        int boxW = Math.max(w, labelW) + 16, boxX = middle - boxW / 2;
        g.setColor(new Color(8, 6, 6, 190));
        g.fillRect(boxX, y - 6, boxW, 40);
        g.setColor(focused ? FOCUS_BLUE : new Color(70, 74, 90));
        g.drawRect(boxX, y - 6, boxW, 40);
        g.setColor(new Color(30, 30, 46));
        g.fillRect(x, y, w, 9);
        g.setColor(focus >= FOCUS_MIN || focused ? FOCUS_BLUE : new Color(90, 90, 110));
        g.fillRect(x, y, (int) Math.round(w * focus / FOCUS_MAX), 9);
        g.setColor(new Color(160, 160, 190));
        g.drawLine(x + w * FOCUS_MIN / FOCUS_MAX, y - 2, x + w * FOCUS_MIN / FOCUS_MAX, y + 10);
        g.setColor(focused ? new Color(190, 210, 255) : PALE);
        g.drawString(label, middle - labelW / 2, y + 26);
        StringBuilder ready = new StringBuilder();
        if (redPenReady) ready.append("RED PEN READY   ");
        if (textbookReady) ready.append("TEXTBOOK UP   ");
        if (freeze > 0) ready.append("HALL PASS");
        if (ready.length() > 0) {
            g.setColor(GOLD);
            String r = ready.toString().trim();
            g.drawString(r, middle - g.getFontMetrics().stringWidth(r) / 2, y + 48);
        }
        if (mistakeFlash > 0) {
            g.setColor(new Color(200, 20, 20, mistakeFlash * 12));
            g.setStroke(new BasicStroke(6));
            g.drawRect(3, 3, gp.screenWidth - 6, gp.screenHeight - gp.tileSize - 6);
        }
        g.dispose();
    }
}
