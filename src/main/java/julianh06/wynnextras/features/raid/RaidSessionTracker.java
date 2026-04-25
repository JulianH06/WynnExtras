package julianh06.wynnextras.features.raid;

import com.wynntils.core.components.Models;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.event.RaidEndedEvent;
import julianh06.wynnextras.event.api.WEEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class RaidSessionTracker {

    private static final List<Session> sessions = new ArrayList<>();
    private static boolean wasInRaid = false;
    private static boolean lastRaidCompleted = false;
    private static long hudTickCounter = 0;
    private static boolean wasClicking = false;
    private static boolean dragging = false;
    private static double dragOffX = 0, dragOffY = 0;

    private static final long PAUSE_THRESHOLD_MS = 15 * 60 * 1000; // 15 minutes
    private static final int LINE_HEIGHT = 11;

    private static class Session {
        long startTime;
        int raidCount = 0;
        int failCount = 0;
        long lastRaidTime;
        long pausedTime = 0;
        boolean manuallyPaused = false;
        long manualPauseStart = 0;
        long totalRunTimeMs = 0;
        int timedRunCount = 0;
        String cachedStatsLine = "";

        Session() {
            startTime = System.currentTimeMillis();
            lastRaidTime = startTime;
        }

        long getElapsedMs() {
            long elapsed = System.currentTimeMillis() - startTime - pausedTime;
            if (manuallyPaused) {
                elapsed -= (System.currentTimeMillis() - manualPauseStart);
            }
            return Math.max(0, elapsed);
        }

        boolean isAutoPaused() {
            if (lastRaidTime == 0) return false;
            return System.currentTimeMillis() - lastRaidTime > PAUSE_THRESHOLD_MS;
        }

        void unpause() {
            if (isAutoPaused() && !manuallyPaused) {
                long pauseStart = lastRaidTime + PAUSE_THRESHOLD_MS;
                pausedTime += System.currentTimeMillis() - pauseStart;
            }
        }

        void togglePause() {
            if (manuallyPaused) {
                pausedTime += System.currentTimeMillis() - manualPauseStart;
                manuallyPaused = false;
                manualPauseStart = 0;
            } else {
                manuallyPaused = true;
                manualPauseStart = System.currentTimeMillis();
            }
        }

        String buildStatsLine(int index) {
            julianh06.wynnextras.config.WynnExtrasConfig c = julianh06.wynnextras.config.WynnExtrasConfig.INSTANCE;
            long elapsed = getElapsedMs();
            double hours = elapsed / 3_600_000.0;
            double runsPerHour = hours > 0.001 ? raidCount / hours : 0;
            long totalMin = elapsed / 60_000;
            long h = totalMin / 60;
            long m = totalMin % 60;
            String time = h > 0 ? h + "h " + m + "m" : m + "m";

            StringBuilder sb = new StringBuilder();
            if (sessions.size() > 1) sb.append("#").append(index + 1).append(" ");

            if (c.raidSessionShowRuns) {
                sb.append("Runs: ").append(raidCount);
                if (c.raidSessionShowFails && failCount > 0) {
                    sb.append(" (").append(failCount).append(" F)");
                }
            }
            if (c.raidSessionShowRate) {
                if (sb.length() > 0 && !sb.toString().endsWith(" ")) sb.append(" | ");
                sb.append(String.format("%.1f/hr", runsPerHour));
            }
            if (c.raidSessionShowTime) {
                if (sb.length() > 0 && !sb.toString().endsWith(" ")) sb.append(" | ");
                sb.append(time);
            }
            if (c.raidSessionShowAvgTime && timedRunCount > 0) {
                long avgMs = totalRunTimeMs / timedRunCount;
                long avgMin = avgMs / 60_000;
                long avgSec = (avgMs % 60_000) / 1000;
                if (sb.length() > 0 && !sb.toString().endsWith(" ")) sb.append(" | ");
                sb.append(String.format("avg %d:%02d", avgMin, avgSec));
            }
            if (manuallyPaused) sb.append(" (Paused)");
            return sb.toString();
        }
    }

    private static Session primarySession() {
        return sessions.isEmpty() ? null : sessions.get(0);
    }

    public static void reset() {
        sessions.clear();
        if (McUtils.player() != null) {
            McUtils.sendMessageToClient(Text.literal("§e[Session] §fAll sessions cleared"));
        }
    }

    public static void startNewSession() {
        sessions.add(new Session());
        if (McUtils.player() != null) {
            McUtils.sendMessageToClient(Text.literal(
                    "§e[Session] §fNew session started" + (sessions.size() > 1 ? " (#" + sessions.size() + ")" : "")));
        }
    }

    private static void removeSession(int index) {
        if (index >= 0 && index < sessions.size()) {
            sessions.remove(index);
            if (McUtils.player() != null) {
                McUtils.sendMessageToClient(Text.literal("§e[Session] §fRemoved session"));
            }
        }
    }

    @SubscribeEvent
    public void onRaidEnded(RaidEndedEvent event) {
        if (!WynnExtrasConfig.INSTANCE.raidSessionEnabled) return;
        if (!(event instanceof RaidEndedEvent.Completed)) return; // fails don't count toward avg
        if (event.getRaid() == null) return;
        long runMs = event.getRaid().getTimeInRaid();
        if (runMs <= 0) return;
        for (Session s : sessions) {
            if (!s.manuallyPaused) {
                s.totalRunTimeMs += runMs;
                s.timedRunCount++;
            }
        }
    }

    public static String getStatsString() {
        Session s = primarySession();
        if (s == null) return null;
        long elapsed = s.getElapsedMs();
        double hours = elapsed / 3_600_000.0;
        double runsPerHour = hours > 0.001 ? s.raidCount / hours : 0;
        String base = String.format("Raids: %d | %.1f/hr | Completed: %d | Failed: %d",
                s.raidCount + s.failCount, runsPerHour, s.raidCount, s.failCount);
        if (s.timedRunCount > 0) {
            long avgMs = s.totalRunTimeMs / s.timedRunCount;
            long avgMin = avgMs / 60_000;
            long avgSec = (avgMs % 60_000) / 1000;
            base += String.format(" | Avg: %d:%02d", avgMin, avgSec);
        }
        return base;
    }

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!WynnExtrasConfig.INSTANCE.raidSessionEnabled) return;
            String raw = Formatting.strip(message.getString());
            if (raw == null) return;
            if (raw.contains("Raid Completed!") && !raw.contains(":")) {
                lastRaidCompleted = true;
                for (Session s : sessions) {
                    if (!s.manuallyPaused) s.unpause();
                    s.raidCount++;
                    s.lastRaidTime = System.currentTimeMillis();
                }
            }
            if (raw.contains("Raid Failed!") && !raw.contains(":")) {
                lastRaidCompleted = false;
                for (Session s : sessions) {
                    if (!s.manuallyPaused) s.unpause();
                    s.failCount++;
                    s.lastRaidTime = System.currentTimeMillis();
                }
            }
        });

        // Completion-time tracking via the raid-end event — only fires for actually
        // completed raids, so fails will never contribute to the average.
        WEEventBus.registerEventListener(new RaidSessionTracker());

        // Detect raid start + auto-start first session + refresh cached stats
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!WynnExtrasConfig.INSTANCE.raidSessionEnabled) return;
            if (client.player == null) return;
            hudTickCounter++;
            if (hudTickCounter % 20 == 0) {
                for (int i = 0; i < sessions.size(); i++) {
                    sessions.get(i).cachedStatsLine = sessions.get(i).buildStatsLine(i);
                }
            }
            try {
                boolean inRaid = Models.Raid.getCurrentRaid() != null;
                if (inRaid && !wasInRaid) {
                    for (Session s : sessions) {
                        if (!s.manuallyPaused) s.unpause();
                    }
                    if (sessions.isEmpty()) {
                        startNewSession();
                    }
                }
                wasInRaid = inRaid;
            } catch (Exception ignored) {}
        });

        // Detect clicks/drags on HUD while inventory is open
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!WynnExtrasConfig.INSTANCE.raidSessionEnabled) return;
            if (client.player == null || !(client.currentScreen instanceof InventoryScreen)) {
                wasClicking = false;
                dragging = false;
                return;
            }
            long window = client.getWindow().getHandle();
            boolean clicking = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            double[] mx = new double[1], my = new double[1];
            GLFW.glfwGetCursorPos(window, mx, my);
            double scale = client.getWindow().getScaleFactor();
            double mouseX = mx[0] / scale, mouseY = my[0] / scale;

            if (clicking && !wasClicking) {
                if (isOnHud(mouseX, mouseY) && !isOnButtons(mouseX, mouseY)) {
                    dragging = true;
                    dragOffX = mouseX - WynnExtrasConfig.INSTANCE.raidSessionHudX;
                    dragOffY = mouseY - WynnExtrasConfig.INSTANCE.raidSessionHudY;
                } else {
                    handleClick(mouseX, mouseY);
                }
            } else if (clicking && dragging) {
                WynnExtrasConfig.INSTANCE.raidSessionHudX = (int)(mouseX - dragOffX);
                WynnExtrasConfig.INSTANCE.raidSessionHudY = (int)(mouseY - dragOffY);
            } else if (!clicking && dragging) {
                dragging = false;
                WynnExtrasConfig.save();
            }
            wasClicking = clicking;
        });

        // HUD render — only when NO screen is open
        HudRenderCallback.EVENT.register((ctx, tickDelta) -> {
            if (!WynnExtrasConfig.INSTANCE.raidSessionEnabled) return;
            if (WynnExtrasConfig.INSTANCE.raidSessionOnlyInInventory) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.options.hudHidden) return;
            if (mc.currentScreen != null) return;
            if (WynnExtrasConfig.INSTANCE.raidSessionOnlyInRaid && wasInRaid == false) {
                try { if (Models.Raid.getCurrentRaid() == null) return; } catch (Exception ignored) { return; }
            }
            renderHud(ctx, mc, false);
        });

        // Render on top of own inventory only
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof InventoryScreen)) return;
            ScreenEvents.afterRender(screen).register((s, ctx, mouseX, mouseY, tickDelta) -> {
                if (!WynnExtrasConfig.INSTANCE.raidSessionEnabled) return;
                if (client.player == null) return;
                if (WynnExtrasConfig.INSTANCE.raidSessionOnlyInRaid) {
                    try { if (Models.Raid.getCurrentRaid() == null) return; } catch (Exception ignored) { return; }
                }
                renderHud(ctx, client, true);
            });
        });
    }

    private static int totalRows() {
        return sessions.isEmpty() ? 1 : sessions.size() + 1;
    }

    private static void renderHud(DrawContext ctx, MinecraftClient mc, boolean showButtons) {
        float ts = WynnExtrasConfig.INSTANCE.raidSessionHudScale;
        int baseX = WynnExtrasConfig.INSTANCE.raidSessionHudX;
        int baseY = WynnExtrasConfig.INSTANCE.raidSessionHudY;

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(baseX, baseY);
        ctx.getMatrices().scale(ts, ts);

        if (sessions.isEmpty()) {
            if (showButtons) {
                ctx.drawText(mc.textRenderer, "Session: --", 0, 0, 0xFF888888, true);
                int addX = mc.textRenderer.getWidth("Session: --  ");
                ctx.drawText(mc.textRenderer, "[ADD]", addX, 0, 0xFF66FF66, true);
            }
            ctx.getMatrices().popMatrix();
            return;
        }

        for (int i = 0; i < sessions.size(); i++) {
            Session s = sessions.get(i);
            String line = s.cachedStatsLine.isEmpty() ? s.buildStatsLine(i) : s.cachedStatsLine;
            int lineColor = s.manuallyPaused ? 0xFFFFFF66 : 0xFFAAFFAA;
            int y = i * LINE_HEIGHT;

            ctx.drawText(mc.textRenderer, line, 0, y, lineColor, true);
            if (showButtons) {
                int btnX = mc.textRenderer.getWidth(line + "  ");
                ctx.drawText(mc.textRenderer, "[X]", btnX, y, 0xFFFF6666, true);
                String pauseLabel = s.manuallyPaused ? "[>]" : "[||]";
                int pauseX = btnX + mc.textRenderer.getWidth("[X] ");
                ctx.drawText(mc.textRenderer, pauseLabel, pauseX, y, 0xFFFFFF66, true);
            }
        }

        if (showButtons) {
            int addY = sessions.size() * LINE_HEIGHT;
            ctx.drawText(mc.textRenderer, "[ADD]", 0, addY, 0xFF66FF66, true);
        }

        ctx.getMatrices().popMatrix();
    }

    private static boolean isOnHud(double mouseX, double mouseY) {
        float ts = WynnExtrasConfig.INSTANCE.raidSessionHudScale;
        int baseX = WynnExtrasConfig.INSTANCE.raidSessionHudX;
        int baseY = WynnExtrasConfig.INSTANCE.raidSessionHudY;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.textRenderer == null) return false;

        int totalH = (int)(totalRows() * LINE_HEIGHT * ts);
        if (mouseY < baseY || mouseY > baseY + totalH) return false;

        int maxWidth = 0;
        if (sessions.isEmpty()) {
            maxWidth = mc.textRenderer.getWidth("Session: --  [ADD]");
        } else {
            for (int i = 0; i < sessions.size(); i++) {
                Session s = sessions.get(i);
                String line = s.buildStatsLine(i);
                String pauseLabel = s.manuallyPaused ? "[>]" : "[||]";
                int w = mc.textRenderer.getWidth(line + "  [X] " + pauseLabel);
                if (w > maxWidth) maxWidth = w;
            }
            int addW = mc.textRenderer.getWidth("[ADD]");
            if (addW > maxWidth) maxWidth = addW;
        }
        return mouseX >= baseX && mouseX <= baseX + (int)(maxWidth * ts);
    }

    private static boolean isOnButtons(double mouseX, double mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.textRenderer == null) return false;
        float ts = WynnExtrasConfig.INSTANCE.raidSessionHudScale;
        int baseX = WynnExtrasConfig.INSTANCE.raidSessionHudX;
        int baseY = WynnExtrasConfig.INSTANCE.raidSessionHudY;

        if (sessions.isEmpty()) {
            int h = (int)(LINE_HEIGHT * ts);
            if (mouseY < baseY || mouseY > baseY + h) return false;
            int addX = baseX + (int)(mc.textRenderer.getWidth("Session: --  ") * ts);
            int addEnd = addX + (int)(mc.textRenderer.getWidth("[ADD]") * ts);
            return mouseX >= addX && mouseX <= addEnd;
        }

        for (int i = 0; i < sessions.size(); i++) {
            int rowY = baseY + (int)(i * LINE_HEIGHT * ts);
            int rowH = (int)(LINE_HEIGHT * ts);
            if (mouseY >= rowY && mouseY < rowY + rowH) {
                Session s = sessions.get(i);
                String line = s.buildStatsLine(i);
                String pauseLabel = s.manuallyPaused ? "[>]" : "[||]";
                int btnStart = baseX + (int)(mc.textRenderer.getWidth(line + "  ") * ts);
                int btnEnd = btnStart + (int)(mc.textRenderer.getWidth("[X] " + pauseLabel) * ts);
                return mouseX >= btnStart && mouseX <= btnEnd;
            }
        }

        int addRowY = baseY + (int)(sessions.size() * LINE_HEIGHT * ts);
        int addRowH = (int)(LINE_HEIGHT * ts);
        if (mouseY >= addRowY && mouseY < addRowY + addRowH) {
            int addEnd = baseX + (int)(mc.textRenderer.getWidth("[ADD]") * ts);
            return mouseX >= baseX && mouseX <= addEnd;
        }

        return false;
    }

    private static void handleClick(double mouseX, double mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.textRenderer == null) return;

        float ts = WynnExtrasConfig.INSTANCE.raidSessionHudScale;
        int baseX = WynnExtrasConfig.INSTANCE.raidSessionHudX;
        int baseY = WynnExtrasConfig.INSTANCE.raidSessionHudY;

        if (sessions.isEmpty()) {
            int h = (int)(LINE_HEIGHT * ts);
            if (mouseY < baseY || mouseY > baseY + h) return;
            int addX = baseX + (int)(mc.textRenderer.getWidth("Session: --  ") * ts);
            int addW = (int)(mc.textRenderer.getWidth("[ADD]") * ts);
            if (mouseX >= addX && mouseX <= addX + addW) {
                startNewSession();
            }
            return;
        }

        for (int i = 0; i < sessions.size(); i++) {
            int rowY = baseY + (int)(i * LINE_HEIGHT * ts);
            int rowH = (int)(LINE_HEIGHT * ts);
            if (mouseY < rowY || mouseY >= rowY + rowH) continue;

            Session s = sessions.get(i);
            String line = s.buildStatsLine(i);
            String pauseLabel = s.manuallyPaused ? "[>]" : "[||]";

            int xBtnX = baseX + (int)(mc.textRenderer.getWidth(line + "  ") * ts);
            int xBtnW = (int)(mc.textRenderer.getWidth("[X]") * ts);
            if (mouseX >= xBtnX && mouseX <= xBtnX + xBtnW) {
                removeSession(i);
                return;
            }

            int pauseX = xBtnX + (int)(mc.textRenderer.getWidth("[X] ") * ts);
            int pauseW = (int)(mc.textRenderer.getWidth(pauseLabel) * ts);
            if (mouseX >= pauseX && mouseX <= pauseX + pauseW) {
                s.togglePause();
                String state = s.manuallyPaused ? "Paused" : "Resumed";
                if (McUtils.player() != null) {
                    String label = sessions.size() > 1 ? " (#" + (i + 1) + ")" : "";
                    McUtils.sendMessageToClient(Text.literal("§e[Session] §f" + state + label));
                }
                return;
            }
            return;
        }

        int addRowY = baseY + (int)(sessions.size() * LINE_HEIGHT * ts);
        int addRowH = (int)(LINE_HEIGHT * ts);
        if (mouseY >= addRowY && mouseY < addRowY + addRowH) {
            int addW = (int)(mc.textRenderer.getWidth("[ADD]") * ts);
            if (mouseX >= baseX && mouseX <= baseX + addW) {
                startNewSession();
            }
        }
    }
}
