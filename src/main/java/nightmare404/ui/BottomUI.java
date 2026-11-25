package nightmare404.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import nightmare404.entity.Player;
import nightmare404.main.GamePanel;

// The bottom UI bar. It fills the last tile row of the screen (row 18, y 576-607):
// every map is 18 rows tall, so this row is never covered by the world.
// It shows the player's health (the heart bar, see HealthBar), what they carry and the floor's key.
// (Only the health bar has its final look so far; the rest is still TEMPORARY.)
public class BottomUI {
    private static final Color BACKGROUND = new Color(20, 22, 30);
    private static final Color BORDER = new Color(70, 74, 90);
    private static final Font LABEL_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Font SMALL_FONT = new Font("Arial", Font.BOLD, 12);

    private static final int BAR_X = 12;         // Left edge of the health bar
    private static final int BAR_SCALE = 2;      // The bar's pixel art drawn twice its size (112 x 20)

    GamePanel gp;

    public BottomUI(GamePanel gp) {
        this.gp = gp;
    }

    public void draw(Graphics2D g2d) {
        int top = gp.screenHeight - gp.tileSize;   // 608 - 32 = 576
        int height = gp.tileSize;

        // Background, with a 1 px line separating it from the world
        g2d.setColor(BACKGROUND);
        g2d.fillRect(0, top, gp.screenWidth, height);
        g2d.setColor(BORDER);
        g2d.drawLine(0, top, gp.screenWidth, top);

        // The heart bar, then the numbers, vertically centred in the 32 px row
        Player player = gp.flor;
        int barY = top + (height - HealthBar.PLAYER.height(BAR_SCALE)) / 2 + 1;
        HealthBar.PLAYER.draw(g2d, player.health, player.maxHealth, BAR_X, barY, BAR_SCALE);

        g2d.setFont(LABEL_FONT);
        g2d.setColor(Color.WHITE);
        int textY = top + height / 2 + 5;
        int numbersX = BAR_X + HealthBar.PLAYER.width(BAR_SCALE) + 8;
        g2d.drawString(player.health + " / " + player.maxHealth, numbersX, textY);

        // What the player carries into battle
        g2d.setFont(SMALL_FONT);
        g2d.drawString("Kits " + player.kits + "   Textbooks " + player.items.textbooks + "   Red Pens "
                + player.items.redPens + "   Hall Passes " + player.items.hallPasses, numbersX + 92, textY);

        // The key to this floor's way out, once the player has it
        String key = gp.currentRoom.floor.exitKey;
        if (key != null && player.keys.contains(key)) {
            String text = "Key: " + key;
            g2d.setColor(new Color(232, 184, 70));
            g2d.drawString(text, gp.screenWidth - 16 - g2d.getFontMetrics().stringWidth(text), textY);
        }
    }
}
