package julianh06.wynnextras.features.tetris;

import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.utils.UI.UIUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.random.Random;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TetrisScreen extends Screen {
    public static TetrisScreen instance;

    public static final int fromX = 0, toX = 100, fromY = 0, toY = 200;
    static float boardScale = 1.0f;
    static float boardOffsetX = 0, boardOffsetY = 0;

    static TetrisNode currentPiece;
    static boolean gameOver = true;
    static int score = 0;
    static long lastTick = 0;
    static int linesCleared = 0;
    static int totalLines = 0;
    static boolean holdCooldown = false;
    static String holdShape = " ";
    static TetrominoGenerator generator = new TetrominoGenerator(7);

    private static final int TSPIN_NONE = 0, TSPIN_MINI = 1, TSPIN_FULL = 2;

    private boolean lastActionWasRotation = false;
    static boolean b2bActive = false;
    static int comboCount = 0;
    static boolean sprintMode = false;
    static long sprintStartTime = 0;
    static long sprintEndTime = 0;
    private long flashUntil = 0;
    private String flashMessage = "";

    private long lockStart = 0;
    private long lockStartMax = 0;
    boolean pieceTouchedGround = false;
    private int lockResets = 0;
    private static final int MAX_LOCK_RESETS = 15;

    boolean rightPressed, leftPressed, downPressed;
    private long rightPressTime, leftPressTime, downPressTime;

    private long das()      { return WynnExtrasConfig.INSTANCE.tetrisDAS; }
    private long arr()      { return WynnExtrasConfig.INSTANCE.tetrisARR; }
    private long sdfDelay() { return WynnExtrasConfig.INSTANCE.tetrisSDFDelay; }
    private long sdf()      { return WynnExtrasConfig.INSTANCE.tetrisSDF; }

    private UIUtils ui;

    public TetrisScreen() {
        super(Text.of("Tetris"));
        instance = this;
    }

    @Override
    protected void init() {
        super.init();
        rightPressed = leftPressed = downPressed = false;
        pieceTouchedGround = false;
    }

    private void updateBounds() {
        boardScale = Math.min(this.height / 250f, (this.width - 160) / 100f);
        boardScale = Math.max(boardScale, 0.5f);
        boardOffsetX = this.width / 2f - boardScale * 50;
        boardOffsetY = this.height / 2f - boardScale * 100;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        try { if (client != null) super.applyBlur(context); } catch (Exception ignored) {}

        if (ui == null) ui = new UIUtils(context, 1.0, 0, 0);
        else ui.updateContext(context, 1.0, 0, 0);

        updateBounds();

        if (!gameOver) runGameLogic();

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(boardOffsetX, boardOffsetY);
        context.getMatrices().scale(boardScale, boardScale);

        drawBoard(context);

        if (!gameOver) {
            drawGrid(context);
            drawGhost(context);
        }

        drawBlocks(context);

        if (!gameOver) {
            drawHud(context);
        } else {
            drawGameOver(context);
        }

        context.getMatrices().popMatrix();
    }

    private void runGameLogic() {
        if (TetrisNode.nodes.isEmpty()) {
            doNewPieceStuff();
            return;
        }

        long now = System.currentTimeMillis();

        int level = currentLevel();
        if (is20G(level)) {
            dropToGround();
        } else {
            int divided = Math.max(15, 600 - level * 50);
            long tick = now / divided;
            if (tick != lastTick) {
                if (currentPiece.canGoDown()) currentPiece.moveDown();
                lastTick = tick;
            }
        }

        if (!currentPiece.canGoDown()) {
            if (!pieceTouchedGround) {
                lockStart = now;
                if (lockStartMax == 0) lockStartMax = now;
                pieceTouchedGround = true;
            }
            if (now - lockStart > 500 || now - lockStartMax > 5000) {
                doNewPieceStuff();
                return;
            }
        } else {
            pieceTouchedGround = false;
        }

        if (rightPressed && now - rightPressTime >= das()) {
            if (currentPiece.canMoveRight()) {
                if (pieceTouchedGround && lockResets < MAX_LOCK_RESETS) { lockStart = now; lockResets++; }
                currentPiece.moveRight();
                rightPressTime = now + arr() - das();
            }
        }
        if (leftPressed && now - leftPressTime >= das()) {
            if (currentPiece.canMoveLeft()) {
                if (pieceTouchedGround && lockResets < MAX_LOCK_RESETS) { lockStart = now; lockResets++; }
                currentPiece.moveLeft();
                leftPressTime = now + arr() - das();
            }
        }
        if (downPressed && now - downPressTime >= sdfDelay()) {
            if (currentPiece.canGoDown()) {
                currentPiece.moveDown();
                score += 1;
                downPressTime = now + sdf() - sdfDelay();
            }
        }
    }

    private int currentLevel() {
        return 1 + totalLines / 10;
    }

    private boolean is20G(int level) {
        return WynnExtrasConfig.INSTANCE.tetris20GEnabled && level >= WynnExtrasConfig.INSTANCE.tetris20GLevel;
    }

    private void dropToGround() {
        while (currentPiece.canGoDown()) currentPiece.moveDown();
    }

    public void doNewPieceStuff() {
        pieceTouchedGround = false;
        lockResets = 0;
        lockStartMax = 0;
        MinecraftUtils.playSoundUI(SoundEvents.BLOCK_GLASS_PLACE);
        int tSpinType = getTSpinType();
        lastActionWasRotation = false;
        removeLayer();
        if (linesCleared > 0) {
            float pitch = Math.min(2.0f, 1.0f + comboCount * 0.1f);
            MinecraftClient.getInstance().getSoundManager().play(new PositionedSoundInstance(
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP.id(), SoundCategory.MASTER,
                1.0f, pitch, Random.create(), false, 0,
                SoundInstance.AttenuationType.NONE, 0, 0, 0, true
            ));
        }
        int level = currentLevel();
        totalLines += linesCleared;

        boolean isDifficult = false;
        int baseScore = 0;
        String msg = null;

        if (tSpinType == TSPIN_FULL) {
            isDifficult = linesCleared >= 1;
            switch (linesCleared) {
                case 0 -> { baseScore = 400; msg = "T-Spin!"; }
                case 1 -> { baseScore = b2bActive ? 1200 : 800; msg = b2bActive ? "B2B T-Spin Single!" : "T-Spin Single!"; }
                case 2 -> { baseScore = b2bActive ? 1800 : 1200; msg = b2bActive ? "B2B T-Spin Double!" : "T-Spin Double!"; }
                case 3 -> { baseScore = b2bActive ? 2400 : 1600; msg = b2bActive ? "B2B T-Spin Triple!" : "T-Spin Triple!"; }
            }
        } else if (tSpinType == TSPIN_MINI) {
            switch (linesCleared) {
                case 0 -> { baseScore = 100; msg = "Mini T-Spin!"; }
                case 1 -> { baseScore = 200; msg = "Mini T-Spin Single!"; }
                case 2 -> { isDifficult = true; baseScore = b2bActive ? 600 : 400; msg = b2bActive ? "B2B Mini T-Spin Double!" : "Mini T-Spin Double!"; }
            }
        } else {
            switch (linesCleared) {
                case 1 -> { baseScore = 100; msg = "Single"; }
                case 2 -> { baseScore = 300; msg = "Double!"; }
                case 3 -> { baseScore = 500; msg = "Triple!"; }
                case 4 -> { isDifficult = true; baseScore = b2bActive ? 1200 : 800; msg = b2bActive ? "B2B Tetris!" : "Tetris!"; }
            }
        }

        score += baseScore * level;
        score += level;

        if (linesCleared > 0 && TetrisNode.nodes.isEmpty()) {
            score += 1000 * level;
            msg = "Full Clear!";
        }

        if (linesCleared > 0) {
            if (comboCount > 0) score += 50 * comboCount * level;
            if (comboCount > 1) msg = (msg != null ? msg + " " : "") + comboCount + "x Combo!";
            comboCount++;
            b2bActive = isDifficult;
        } else {
            comboCount = 0;
        }

        if (msg != null) { flashMessage = msg; flashUntil = System.currentTimeMillis() + 1500; }

        linesCleared = 0;

        if (sprintMode && totalLines >= 40) {
            sprintEndTime = System.currentTimeMillis();
            int elapsed = (int)(sprintEndTime - sprintStartTime);
            if (WynnExtrasConfig.INSTANCE.tetrisBest40LinesMs == 0 || elapsed < WynnExtrasConfig.INSTANCE.tetrisBest40LinesMs) {
                WynnExtrasConfig.INSTANCE.tetrisBest40LinesMs = elapsed;
                WynnExtrasConfig.save();
            }
            gameOver = true;
            TetrisNode.nodes.clear();
            return;
        }

        setShapes();
        holdCooldown = false;
        if (!currentPiece.canGoDown()) {
            onGameOver();
        }
    }

    private int getTSpinType() {
        if (currentPiece == null || !"T".equals(currentPiece.getShape())) return TSPIN_NONE;
        if (!lastActionWasRotation) return TSPIN_NONE;
        int cx = currentPiece.getX(), cy = currentPiece.getY();
        boolean[] cf = new boolean[4]; // TL, TR, BL, BR
        int[][] corners = {{cx - 10, cy - 10}, {cx + 10, cy - 10}, {cx - 10, cy + 10}, {cx + 10, cy + 10}};
        int totalFilled = 0;
        for (int i = 0; i < 4; i++) {
            int[] c = corners[i];
            if (c[0] < fromX || c[0] >= toX || c[1] <= fromY || c[1] > toY) cf[i] = true;
            else { TetrisNode n = TetrisNode.getNode(c[0], c[1]); if (n != null && !currentPiece.isInFamily(n)) cf[i] = true; }
            if (cf[i]) totalFilled++;
        }
        if (totalFilled < 3) return TSPIN_NONE;
        boolean frontFilled = switch (currentPiece.getRotation()) {
            case 1 -> cf[0] && cf[1]; // bump up  → front = TL, TR
            case 2 -> cf[1] && cf[3]; // bump right → front = TR, BR
            case 3 -> cf[2] && cf[3]; // bump down → front = BL, BR
            case 4 -> cf[0] && cf[2]; // bump left → front = TL, BL
            default -> false;
        };
        return frontFilled ? TSPIN_FULL : TSPIN_MINI;
    }

    private static void removeLayer() {
        if (currentPiece == null) return;
        for (int i = 0; i < currentPiece.getFamily().size(); i++) {
            int y = currentPiece.getFamily().get(i).getY();
            int x = fromX - 10;
            for (int i2 = 0; i2 < 100; i2++) {
                x += 10;
                if (x > toX - 10) {
                    x = fromX - 10;
                    for (int i3 = 0; i3 < 100; i3++) {
                        x += 10;
                        TetrisNode.nodes.remove(TetrisNode.getNode(x, y));
                    }
                    for (int i4 = 0; i4 < TetrisNode.nodes.size(); i4++) {
                        if (TetrisNode.nodes.get(i4).getY() <= y)
                            TetrisNode.nodes.get(i4).setY(TetrisNode.nodes.get(i4).getY() + 10);
                    }
                    linesCleared++;
                    break;
                }
                if (TetrisNode.getNode(x, y) == null) break;
            }
        }
    }

    private static void setShapes() {
        TetrisNode n = new TetrisNode(fromX + 40, fromY - 10);
        n.addToFamily(n);
        int x = n.getX(), y = n.getY();
        char next = generator.nextTetromino();
        switch (next) {
            case 'O' -> { n.addToFamily(new TetrisNode(x + 10, y)); n.addToFamily(new TetrisNode(x, y + 10)); n.addToFamily(new TetrisNode(x + 10, y + 10)); n.setShape("O"); n.setColor(pieceColor('O')); }
            case 'I' -> { n.addToFamily(new TetrisNode(x - 10, y)); n.addToFamily(new TetrisNode(x + 10, y)); n.addToFamily(new TetrisNode(x + 20, y)); n.setShape("I"); n.setColor(pieceColor('I')); }
            case 'S' -> { n.addToFamily(new TetrisNode(x, y - 10)); n.addToFamily(new TetrisNode(x + 10, y - 10)); n.addToFamily(new TetrisNode(x - 10, y)); n.setShape("S"); n.setColor(pieceColor('S')); }
            case 'Z' -> { n.addToFamily(new TetrisNode(x, y - 10)); n.addToFamily(new TetrisNode(x - 10, y - 10)); n.addToFamily(new TetrisNode(x + 10, y)); n.setShape("Z"); n.setColor(pieceColor('Z')); }
            case 'L' -> { n.addToFamily(new TetrisNode(x + 10, y)); n.addToFamily(new TetrisNode(x + 10, y - 10)); n.addToFamily(new TetrisNode(x - 10, y)); n.setShape("L"); n.setColor(pieceColor('L')); }
            case 'J' -> { n.addToFamily(new TetrisNode(x - 10, y)); n.addToFamily(new TetrisNode(x - 10, y - 10)); n.addToFamily(new TetrisNode(x + 10, y)); n.setShape("J"); n.setColor(pieceColor('J')); }
            case 'T' -> { n.addToFamily(new TetrisNode(x - 10, y)); n.addToFamily(new TetrisNode(x, y - 10)); n.addToFamily(new TetrisNode(x + 10, y)); n.setShape("T"); n.setColor(pieceColor('T')); }
        }
        currentPiece = n;
    }

    private static void holdPiece() {
        if (holdCooldown) return;
        if (holdShape.equals(" ")) {
            holdShape = currentPiece.getShape();
            for (int i = 0; i < currentPiece.getFamily().size(); i++) currentPiece.getFamily().get(i).removeFromList();
            setShapes();
        } else {
            List<Character> temp = new ArrayList<>();
            temp.add(holdShape.charAt(0));
            holdShape = currentPiece.getShape();
            for (int i = 0; i < currentPiece.getFamily().size(); i++) currentPiece.getFamily().get(i).removeFromList();
            for (Character c : temp) generator.bag.addFirst(c);
            setShapes();
        }
        holdCooldown = true;
    }

    private void onGameOver() {
        rightPressed = leftPressed = downPressed = false;
        pieceTouchedGround = false;
        if (score > WynnExtrasConfig.INSTANCE.tetrisBestScore) {
            WynnExtrasConfig.INSTANCE.tetrisBestScore = score;
            WynnExtrasConfig.save();
        }
        gameOver = true;
        TetrisNode.nodes.clear();
    }

    private void startGame() {
        score = 0;
        linesCleared = 0;
        totalLines = 0;
        holdShape = " ";
        holdCooldown = false;
        TetrisNode.nodes.clear();
        currentPiece = null;
        generator = new TetrominoGenerator(7);
        rightPressed = leftPressed = downPressed = false;
        pieceTouchedGround = false;
        lockResets = 0;
        lockStartMax = 0;
        lastActionWasRotation = false;
        b2bActive = false;
        comboCount = 0;
        flashUntil = 0;
        flashMessage = "";
        lastTick = 0;
        sprintStartTime = System.currentTimeMillis();
        sprintEndTime = 0;
        gameOver = false;
    }

    // --- Rendering ---

    private void drawPanel(DrawContext ctx, int x, int y, int w, int h, int accent) {
        ctx.fill(x, y, x + w, y + h, 0xFF4d3c2d);
        ctx.fill(x, y, x + 3, y + h, accent);
        ctx.fill(x + 3, y, x + w, y + 1, 0xFF876141);
        ctx.fill(x, y + h - 1, x + w, y + h, 0xFF1a1410);
    }

    private void drawBoard(DrawContext ctx) {
        ctx.fill(fromX, fromY, toX, toY, 0xFF0e0b08);
        ctx.fill(fromX - 2, fromY - 1, fromX, toY + 1, 0xFF0872bc); // left accent
        ctx.fill(fromX, fromY - 1, toX + 1, fromY, 0xFF876141);      // top highlight
        ctx.fill(toX, fromY - 1, toX + 1, toY + 1, 0xFF4d3c2d);      // right border
        ctx.fill(fromX - 2, toY, toX + 1, toY + 1, 0xFF1a1410);      // bottom shadow
    }

    private void drawBlocks(DrawContext ctx) {
        for (TetrisNode node : TetrisNode.nodes) {
            ctx.fill(node.getX(), node.getY(), node.getX() + 10, node.getY() - 10, node.getColor());
        }
    }

    private void drawGrid(DrawContext ctx) {
        int gridColor = new Color(58, 45, 36, 90).getRGB();
        for (int i = 1; i < 10; i++) ctx.fill(fromX + i * 10, fromY, fromX + i * 10 + 1, toY, gridColor);
        for (int i = 1; i < 20; i++) ctx.fill(fromX, fromY + i * 10, toX, fromY + i * 10 + 1, gridColor);
    }

    private void drawGhost(DrawContext ctx) {
        if (currentPiece == null || !currentPiece.canGoDown()) return;
        currentPiece.setDownPosition();
        for (TetrisNode node : currentPiece.getFamily()) {
            int x = node.getX(), y = node.getDownPosition();
            ctx.fill(x, y - 10, x + 10, y, 0x446c4f36);
        }
    }

    private static String formatTime(long ms) {
        if (ms >= 60000) {
            long min = ms / 60000, sec = (ms % 60000) / 1000, millis = ms % 1000;
            return min + ":" + String.format("%02d", sec) + "." + String.format("%03d", millis);
        }
        return (ms / 1000) + "." + String.format("%03d", ms % 1000) + "s";
    }

    private void drawHud(DrawContext ctx) {
        int level = currentLevel();
        long elapsed = System.currentTimeMillis() - sprintStartTime;

        int holdX = fromX - 72, holdY = fromY, holdW = 62, holdH = 70;
        drawPanel(ctx, holdX, holdY, holdW, holdH, 0xFF0f9bd7); // cyan — I piece
        ctx.drawText(textRenderer, "Hold", holdX + 6, holdY + 5, 0xFFe8dcc8, false);
        if (!holdShape.equals(" ")) drawPiecePreview(ctx, holdShape.charAt(0), holdX + 4, holdY + 18);

        int scoreX = fromX - 72, scoreY = fromY + 78, scoreW = 62, scoreH = 95;
        drawPanel(ctx, scoreX, scoreY, scoreW, scoreH, 0xFFcca76f); // gold
        if (sprintMode) {
            ctx.drawText(textRenderer, "Lines", scoreX + 6, scoreY + 5, 0xFFe8dcc8, false);
            ctx.drawText(textRenderer, Math.max(0, 40 - totalLines) + " left", scoreX + 6, scoreY + 18, 0xFFcca76f, false);
            ctx.drawText(textRenderer, "Time", scoreX + 6, scoreY + 36, 0xFFe8dcc8, false);
            ctx.drawText(textRenderer, formatTime(elapsed), scoreX + 6, scoreY + 49, 0xFFcca76f, false);
            int best = WynnExtrasConfig.INSTANCE.tetrisBest40LinesMs;
            ctx.drawText(textRenderer, "Best", scoreX + 6, scoreY + 67, 0xFF9a8b70, false);
            ctx.drawText(textRenderer, best > 0 ? formatTime(best) : "-", scoreX + 6, scoreY + 80, 0xFF9a8b70, false);
        } else {
            ctx.drawText(textRenderer, "Score", scoreX + 6, scoreY + 5, 0xFFe8dcc8, false);
            ctx.drawText(textRenderer, String.valueOf(score), scoreX + 6, scoreY + 18, 0xFFcca76f, false);
            ctx.drawText(textRenderer, "Best", scoreX + 6, scoreY + 36, 0xFFe8dcc8, false);
            ctx.drawText(textRenderer, String.valueOf(WynnExtrasConfig.INSTANCE.tetrisBestScore), scoreX + 6, scoreY + 49, 0xFFcca76f, false);
            ctx.drawText(textRenderer, "Lvl " + level, scoreX + 6, scoreY + 67, 0xFF9a8b70, false);
            ctx.drawText(textRenderer, totalLines + " lines", scoreX + 6, scoreY + 80, 0xFF9a8b70, false);
        }

        int nextX = toX + 10, nextY = fromY, nextW = 62, nextH = 200;
        drawPanel(ctx, nextX, nextY, nextW, nextH, 0xFF4a8c3a); // green — S piece
        ctx.drawText(textRenderer, "Next", nextX + 6, nextY + 5, 0xFFe8dcc8, false);

        List<Character> nextPieces = generator.bag.subList(0, Math.min(5, generator.bag.size()));
        int previewY = nextY + 20;
        for (char c : nextPieces) {
            drawPiecePreview(ctx, c, nextX + 4, previewY);
            previewY += 36;
        }

        if (System.currentTimeMillis() < flashUntil) {
            int textW = textRenderer.getWidth(flashMessage);
            ctx.drawText(textRenderer, flashMessage, fromX + (toX - fromX) / 2 - textW / 2, toY + 8, 0xFFcca76f, true);
        }

        String hint = keyName(WynnExtrasConfig.INSTANCE.tetrisQuitKey) + ": quit";
        int hintW = textRenderer.getWidth(hint);
        ctx.drawText(textRenderer, hint, fromX + (toX - fromX) / 2 - hintW / 2, fromY - 14, 0x559a8b70, false);
    }

    private void drawGameOver(DrawContext ctx) {
        ctx.fill(fromX, fromY, toX, toY, 0x99000000);

        int panelW = 120, panelH = 105;
        int panelX = 50 - panelW / 2;
        int panelY = 100 - panelH / 2 - 10;
        int gameOverAccent = sprintMode ? 0xFF4a8c3a : 0xFFa83232;
        drawPanel(ctx, panelX, panelY, panelW, panelH, gameOverAccent);

        int tx = panelX + 7;
        if (sprintMode && sprintEndTime > 0) {
            long elapsed = sprintEndTime - sprintStartTime;
            int best = WynnExtrasConfig.INSTANCE.tetrisBest40LinesMs;
            ctx.drawText(textRenderer, "40 Lines!", tx, panelY + 8, 0xFFe8dcc8, true);
            ctx.drawText(textRenderer, "Time:  " + formatTime(elapsed), tx, panelY + 24, 0xFFcca76f, false);
            ctx.drawText(textRenderer, "Best:  " + (best > 0 ? formatTime(best) : "-"), tx, panelY + 38, 0xFFcca76f, false);
            ctx.drawText(textRenderer, keyName(WynnExtrasConfig.INSTANCE.tetrisToggleModeKey) + ": Classic", tx, panelY + 55, 0xFF9a8b70, false);
        } else if (sprintMode) {
            ctx.drawText(textRenderer, "40 Lines", tx, panelY + 8, 0xFFe8dcc8, true);
            int best = WynnExtrasConfig.INSTANCE.tetrisBest40LinesMs;
            ctx.drawText(textRenderer, "Best:  " + (best > 0 ? formatTime(best) : "-"), tx, panelY + 24, 0xFFcca76f, false);
            ctx.drawText(textRenderer, keyName(WynnExtrasConfig.INSTANCE.tetrisToggleModeKey) + ": Classic", tx, panelY + 55, 0xFF9a8b70, false);
        } else {
            ctx.drawText(textRenderer, "Game Over!", tx, panelY + 8, 0xFFe8dcc8, true);
            ctx.drawText(textRenderer, "Score: " + score, tx, panelY + 24, 0xFFcca76f, false);
            ctx.drawText(textRenderer, "Best:  " + WynnExtrasConfig.INSTANCE.tetrisBestScore, tx, panelY + 38, 0xFFcca76f, false);
            ctx.drawText(textRenderer, keyName(WynnExtrasConfig.INSTANCE.tetrisToggleModeKey) + ": Sprint", tx, panelY + 55, 0xFF9a8b70, false);
        }
        ctx.drawText(textRenderer, keyName(WynnExtrasConfig.INSTANCE.tetrisStartKey) + " / " + keyName(WynnExtrasConfig.INSTANCE.tetrisRestartKey), tx, panelY + 70, 0xFF9a8b70, false);
    }

    private void fillBlock(DrawContext ctx, int x, int y, int color) {
        ctx.fill(x, y, x + 10, y + 10, color | 0xFF000000);
    }

    private static int pieceColor(char shape) {
        return switch (shape) {
            case 'O' -> new Color(226, 158, 1).getRGB();
            case 'I' -> new Color(15, 155, 217).getRGB();
            case 'S' -> new Color(90, 178, 2).getRGB();
            case 'Z' -> new Color(216, 16, 54).getRGB();
            case 'L' -> new Color(228, 92, 3).getRGB();
            case 'J' -> new Color(33, 66, 198).getRGB();
            case 'T' -> new Color(175, 41, 138).getRGB();
            default -> 0xFF888888;
        };
    }

    private void drawPiecePreview(DrawContext ctx, char shape, int ox, int oy) {
        int c = pieceColor(shape);
        switch (shape) {
            case 'O' -> { fillBlock(ctx, ox + 11, oy + 5, c); fillBlock(ctx, ox + 21, oy + 5, c); fillBlock(ctx, ox + 11, oy + 15, c); fillBlock(ctx, ox + 21, oy + 15, c); }
            case 'I' -> { fillBlock(ctx, ox + 1, oy + 10, c); fillBlock(ctx, ox + 11, oy + 10, c); fillBlock(ctx, ox + 21, oy + 10, c); fillBlock(ctx, ox + 31, oy + 10, c); }
            case 'S' -> { fillBlock(ctx, ox + 11, oy + 5, c); fillBlock(ctx, ox + 21, oy + 5, c); fillBlock(ctx, ox + 1, oy + 15, c); fillBlock(ctx, ox + 11, oy + 15, c); }
            case 'Z' -> { fillBlock(ctx, ox + 1, oy + 5, c); fillBlock(ctx, ox + 11, oy + 5, c); fillBlock(ctx, ox + 11, oy + 15, c); fillBlock(ctx, ox + 21, oy + 15, c); }
            case 'L' -> { fillBlock(ctx, ox + 1, oy + 10, c); fillBlock(ctx, ox + 11, oy + 10, c); fillBlock(ctx, ox + 21, oy + 10, c); fillBlock(ctx, ox + 21, oy, c); }
            case 'J' -> { fillBlock(ctx, ox + 1, oy + 10, c); fillBlock(ctx, ox + 11, oy + 10, c); fillBlock(ctx, ox + 21, oy + 10, c); fillBlock(ctx, ox + 1, oy, c); }
            case 'T' -> { fillBlock(ctx, ox + 1, oy + 10, c); fillBlock(ctx, ox + 11, oy + 10, c); fillBlock(ctx, ox + 21, oy + 10, c); fillBlock(ctx, ox + 11, oy, c); }
        }
    }

    // --- Input ---

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        if (matchesKey(keyCode, WynnExtrasConfig.INSTANCE.tetrisStartKey) && gameOver) {
            startGame();
            return true;
        }
        if (matchesKey(keyCode, WynnExtrasConfig.INSTANCE.tetrisRestartKey)) {
            startGame();
            return true;
        }
        if (matchesKey(keyCode, WynnExtrasConfig.INSTANCE.tetrisToggleModeKey) && gameOver) {
            sprintMode = !sprintMode;
            return true;
        }
        if (matchesKey(keyCode, WynnExtrasConfig.INSTANCE.tetrisQuitKey) && !gameOver) {
            onGameOver();
            return true;
        }
        if (!gameOver && currentPiece != null) {
            long now = System.currentTimeMillis();
            if (matchesKey(keyCode, WynnExtrasConfig.INSTANCE.tetrisMoveRightKey, WynnExtrasConfig.INSTANCE.tetrisMoveRightAltKey)) {
                rightPressed = true;
                rightPressTime = now;
                if (currentPiece.canMoveRight()) {
                    resetLockIfNeeded(now);
                    currentPiece.moveRight();
                    lastActionWasRotation = false;
                }
                return true;
            }
            if (matchesKey(keyCode, WynnExtrasConfig.INSTANCE.tetrisMoveLeftKey, WynnExtrasConfig.INSTANCE.tetrisMoveLeftAltKey)) {
                leftPressed = true;
                leftPressTime = now;
                if (currentPiece.canMoveLeft()) {
                    resetLockIfNeeded(now);
                    currentPiece.moveLeft();
                    lastActionWasRotation = false;
                }
                return true;
            }
            if (matchesKey(keyCode, WynnExtrasConfig.INSTANCE.tetrisSoftDropKey, WynnExtrasConfig.INSTANCE.tetrisSoftDropAltKey)) {
                downPressed = true;
                downPressTime = now;
                lastActionWasRotation = false;
                return true;
            }
            if (matchesKey(keyCode, WynnExtrasConfig.INSTANCE.tetrisRotateClockwiseKey, WynnExtrasConfig.INSTANCE.tetrisRotateClockwiseAltKey)) {
                if (currentPiece.canRotate(true)) {
                    resetLockIfNeeded(now);
                    currentPiece.rotate(true);
                    lastActionWasRotation = true;
                }
                return true;
            }
            if (matchesKey(keyCode, WynnExtrasConfig.INSTANCE.tetrisRotateCounterClockwiseKey, WynnExtrasConfig.INSTANCE.tetrisRotateCounterClockwiseAltKey)) {
                if (currentPiece.canRotate(false)) {
                    resetLockIfNeeded(now);
                    currentPiece.rotate(false);
                    lastActionWasRotation = true;
                }
                return true;
            }
            if (matchesKey(keyCode, WynnExtrasConfig.INSTANCE.tetrisHardDropKey)) {
                if (!currentPiece.getFamily().isEmpty()) {
                    currentPiece.setDownPosition();
                    int cells = Math.max(0, (currentPiece.getFamily().get(0).getDownPosition() - currentPiece.getFamily().get(0).getY()) / TetrisNode.CELL);
                    score += 2 * cells;
                }
                pieceTouchedGround = false;
                lastActionWasRotation = false;
                currentPiece.moveCompletelyDown();
                return true;
            }
            if (matchesKey(keyCode, WynnExtrasConfig.INSTANCE.tetrisHoldKey, WynnExtrasConfig.INSTANCE.tetrisHoldAltKey)) {
                holdPiece();
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        int keyCode = input.key();
        if (matchesKey(keyCode, WynnExtrasConfig.INSTANCE.tetrisMoveRightKey, WynnExtrasConfig.INSTANCE.tetrisMoveRightAltKey)) {
            rightPressed = false;
            return true;
        }
        if (matchesKey(keyCode, WynnExtrasConfig.INSTANCE.tetrisMoveLeftKey, WynnExtrasConfig.INSTANCE.tetrisMoveLeftAltKey)) {
            leftPressed = false;
            return true;
        }
        if (matchesKey(keyCode, WynnExtrasConfig.INSTANCE.tetrisSoftDropKey, WynnExtrasConfig.INSTANCE.tetrisSoftDropAltKey)) {
            downPressed = false;
            return true;
        }
        return false;
    }

    private void resetLockIfNeeded(long now) {
        if (pieceTouchedGround && lockResets < MAX_LOCK_RESETS) {
            lockStart = now;
            lockResets++;
        }
    }

    private static boolean matchesKey(int keyCode, int... configuredKeys) {
        for (int configuredKey : configuredKeys) {
            if (configuredKey != GLFW.GLFW_KEY_UNKNOWN && keyCode == configuredKey) return true;
        }
        return false;
    }

    private static String keyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return "UNBOUND";
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null) return name.toUpperCase();
        return switch (key) {
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_UP -> "UP";
            case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            default -> "KEY_" + key;
        };
    }

    @Override
    public boolean shouldPause() { return false; }

    public static void open() {
        MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new TetrisScreen()));
    }
}
