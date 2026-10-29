package Nightmareon404;

import javax.swing.*;
import tile.TileSet;
import world.Room;
import world.World;

import java.awt.*;
import java.awt.event.ActionEvent;

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
    private static final Font ROOM_NAME_FONT = new Font("Arial", Font.BOLD, 24);

    // Movement tracking booleans
    public boolean upPressed, downPressed, leftPressed, rightPressed;

    // Entities
    Player flor;
    BottomUI bottomUI;

    // The world is a linked list of rooms; currentRoom points at the node the player is in.
    // Moving to another room just means following one of its links (left, right or door).
    TileSet tiles;
    public World world;
    public Room currentRoom;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);

        tiles = new TileSet();                  // Load every tile image once
        world = new World(tiles, tileSize);     // Build and link all the rooms
        currentRoom = world.start;
        flor = new Player(this);
        bottomUI = new BottomUI(this);
        setupKeyBindings();
    }

    public void enterRoom(Room room) {
        currentRoom = room;
    }

    private void setupKeyBindings() {
        InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        // Bind W, A, S, D Presses ("pressed" modifier is required)
        im.put(KeyStroke.getKeyStroke("pressed W"), "upPress");
        im.put(KeyStroke.getKeyStroke("pressed S"), "downPress");
        im.put(KeyStroke.getKeyStroke("pressed A"), "leftPress");
        im.put(KeyStroke.getKeyStroke("pressed D"), "rightPress");

        // Bind W, A, S, D Releases
        im.put(KeyStroke.getKeyStroke("released W"), "upRelease");
        im.put(KeyStroke.getKeyStroke("released S"), "downRelease");
        im.put(KeyStroke.getKeyStroke("released A"), "leftRelease");
        im.put(KeyStroke.getKeyStroke("released D"), "rightRelease");

        // Map Actions to toggle booleans
        am.put("upPress", new MoveAction("up", true));
        am.put("downPress", new MoveAction("down", true));
        am.put("leftPress", new MoveAction("left", true));
        am.put("rightPress", new MoveAction("right", true));

        am.put("upRelease", new MoveAction("up", false));
        am.put("downRelease", new MoveAction("down", false));
        am.put("leftRelease", new MoveAction("left", false));
        am.put("rightRelease", new MoveAction("right", false));
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
                update();
                repaint();
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

    private void update() {
        flor.update();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw the world shifted by the camera: the current room, plus the neighbouring
        // room on whichever side has scrolled into view
        int camX = cameraX();
        Graphics2D view = (Graphics2D) g2d.create();
        view.translate(-camX, 0);
        currentRoom.map.draw(view);
        if (camX < 0) {
            drawNeighbour(view, currentRoom.left, -screenWidth);
        } else if (camX > 0) {
            drawNeighbour(view, currentRoom.right, screenWidth);
        }
        flor.draw(view);
        view.dispose();

        // The room's name stays fixed on screen (drawn after the camera shift is undone)
        g2d.setColor(Color.WHITE);
        g2d.setFont(ROOM_NAME_FONT);
        g2d.drawString(currentRoom.name + "  |  Floor " + currentRoom.floor.number, 20, 40);

        // UI in the bottom row (fixed on screen, drawn last so nothing covers it)
        bottomUI.draw(g2d);

        // Flushes graphic pipeline (prevents stutter on non-Windows OS)
        Toolkit.getDefaultToolkit().sync();
        // Do NOT call g2d.dispose() here
    }

    // The camera: how far the view is scrolled right, in pixels, from the current room's left edge.
    // It keeps the player centred, but never scrolls toward a side with no neighbouring room.
    // So classrooms (no left/right links) never scroll, and the hallway stops at the stairwells.
    // The player is always inside the current room, so the camera moves at most half a screen
    // either way, and only the neighbours next to the current room ever need drawing.
    public int cameraX() {
        int camX = flor.x + tileSize / 2 - screenWidth / 2;
        if (currentRoom.left == null) camX = Math.max(camX, 0);
        if (currentRoom.right == null) camX = Math.min(camX, 0);
        return camX;
    }

    private void drawNeighbour(Graphics2D view, Room neighbour, int offsetX) {
        Graphics2D g = (Graphics2D) view.create();
        g.translate(offsetX, 0);
        neighbour.map.draw(g);
        g.dispose();
    }
}