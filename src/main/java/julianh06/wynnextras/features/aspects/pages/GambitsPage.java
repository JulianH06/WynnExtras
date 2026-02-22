package julianh06.wynnextras.features.aspects.pages;

import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.aspects.*;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class GambitsPage extends PageWidget{
    Identifier ltop = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/ltop.png");
    Identifier rtop = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/rtop.png");
    Identifier ttop = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/ttop.png");
    Identifier btop = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/btop.png");
    Identifier tltop = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/tltop.png");
    Identifier trtop = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/trtop.png");
    Identifier bltop = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/bltop.png");
    Identifier brtop = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/brtop.png");

    Identifier l = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/l.png");
    Identifier r = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/r.png");
    Identifier t = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/t.png");
    Identifier b = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/b.png");
    Identifier tl = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/tl.png");
    Identifier tr = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/tr.png");
    Identifier bl = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/bl.png");
    Identifier br = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/br.png");

    Identifier ltopd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/ltop.png");
    Identifier rtopd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/rtop.png");
    Identifier ttopd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/ttop.png");
    Identifier btopd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/btop.png");
    Identifier tltopd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/tltop.png");
    Identifier trtopd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/trtop.png");
    Identifier bltopd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/bltop.png");
    Identifier brtopd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/brtop.png");

    Identifier ld = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/l.png");
    Identifier rd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/r.png");
    Identifier td = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/t.png");
    Identifier bd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/b.png");
    Identifier tld = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/tl.png");
    Identifier trd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/tr.png");
    Identifier bld = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/bl.png");
    Identifier brd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/br.png");

    private boolean fetchedCrowdsourcedGambits = false;
    private List<GambitData.GambitEntry> crowdsourcedGambits = null;
    private static ZonedDateTime lastCrowdsourceFetch = null;
    private static Boolean fetchRunning = false;
    private static Boolean hasOldData = false;

    private final OpenPartyFinderWidget openPartyFinderWidget;

    public GambitsPage(AspectScreen parent) {
        super(parent);

        openPartyFinderWidget = new OpenPartyFinderWidget();
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        float centerX = (x + width / 2f) * ui.getScaleFactorF();

        ui.drawCenteredText("§6§lToday's Gambits", centerX, 60);

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("CET"));
        ZonedDateTime nextReset = now.withHour(19).withMinute(0).withSecond(0).withNano(0);
        if (nextReset.isBefore(now) || nextReset.isEqual(now)) {
            nextReset = nextReset.plusWeeks(1);
        }

        // Calculate time difference
        Duration duration = Duration.between(now, nextReset);
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        String hourString = hours == 1 ? "hour" : "hours";
        String minuteString = minutes == 1 ? "minute" : "minutes";

        String countdown = "§7Resets in";
        if(hours > 0) countdown += " §e" + hours + " §7" + hourString;
        if(minutes > 0) countdown += " §e" + minutes + " §7" + minuteString;

        ui.drawCenteredText(countdown, centerX, 100);

        if (shouldFetchGambits() && !fetchRunning) {
            fetchRunning = true;

            lastCrowdsourceFetch = now;
            WynncraftApiHandler.fetchCrowdsourcedGambits().thenAccept(result -> {
                fetchRunning = false;
                if (result != null && !result.isEmpty()) {
                    List<GambitData.GambitEntry> oldGambits = crowdsourcedGambits;

                    lastCrowdsourceFetch = now;
                    if(isSamePool(oldGambits, result)) {
                        System.out.println("still old pool, retry in 30s");
                        hasOldData = true;
                    } else {
                        hasOldData = false;
                        crowdsourcedGambits = result;
                        GambitData.INSTANCE.saveGambits(result);
                    }
                }
            });
        }

        int panelWidth = Math.min(800, ((int) (width * ui.getScaleFactorF()) - 200) / 2);
        int panelHeight = 180;
        int spacingH = 50;
        int spacingV = 40;
        int startY = (int) (height * ui.getScaleFactorF() / 2f - panelHeight - spacingH / 2f);

        List<GambitData.GambitEntry> gambitsToShow;

        if (crowdsourcedGambits != null && !crowdsourcedGambits.isEmpty()) {
            gambitsToShow = crowdsourcedGambits;
        } else {
            GambitData data = GambitData.INSTANCE;
            if (data.hasToday() && !data.gambits.isEmpty()) {
                gambitsToShow = data.gambits;
            } else {
                gambitsToShow = null;
            }
        }

        if (gambitsToShow == null) {
            if (!fetchedCrowdsourcedGambits) {
                ui.drawCenteredText("§7Loading crowdsourced data...", centerX, startY + 150);
            } else {
                ui.drawCenteredText("§7No crowdsourced gambit data available yet", centerX, startY + 150);
                int textWidth = (int) (MinecraftClient.getInstance().textRenderer.getWidth("§e§nClick to open Party Finder to scan") * ui.getScaleFactorF());
                openPartyFinderWidget.setBounds((int) (centerX - textWidth / 2f), startY + 200, textWidth, 30);
                openPartyFinderWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
                //ui.drawCenteredText( "§e§nClick to open Party Finder to scan", centerX, startY + 200);
            }
        } else {
            int totalWidth = (panelWidth * 2) + spacingH;
            int startX = ((int) (width * ui.getScaleFactorF()) - totalWidth) / 2;

            for (int i = 0; i < Math.min(gambitsToShow.size(), 4); i++) {
                GambitData.GambitEntry gambit = gambitsToShow.get(i);
                int col = i % 2;
                int row = i / 2;
                int x = startX + (col * (panelWidth + spacingH));
                int y = startY + (row * (panelHeight + spacingV));
                drawGambitPanel(x, y, panelWidth, panelHeight, gambit);
            }
        }
    }

    private static boolean isSamePool(List<GambitData.GambitEntry> oldGambits, List<GambitData.GambitEntry> newGambits) {
        if (oldGambits == null || newGambits == null) return false;
        if (oldGambits.size() != newGambits.size()) return false;

        Set<String> oldNames = oldGambits.stream().map(i -> i.name).collect(Collectors.toSet());

        for (GambitData.GambitEntry gambitEntry : newGambits) {
            if (!oldNames.contains(gambitEntry.name)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return openPartyFinderWidget.mouseClicked(mx, my, button);
    }

    private void drawGambitPanel(int x, int y, int panelWidth, int panelHeight, GambitData.GambitEntry gambit) {
        int topHeight = 60;

        if(WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode) {
            ui.drawNineSlice(x, y, panelWidth,
                    topHeight, 33, ltopd, rtopd, ttopd, btopd, tltopd, trtopd, bltopd, brtopd, CustomColor.fromHexString("2c2d2f"));

            ui.drawNineSlice(x, y + topHeight, panelWidth,
                    panelHeight - topHeight, 33, ld, rd, td, bd, tld, trd, bld, brd, CustomColor.fromHexString("444448"));
        } else {
            ui.drawNineSlice(x, y, panelWidth,
                    topHeight, 33, ltop, rtop, ttop, btop, tltop, trtop, bltop, brtop, CustomColor.fromHexString("81644b"));

            ui.drawNineSlice(x, y + topHeight, panelWidth,
                    panelHeight - topHeight, 33, l, r, t, b, tl, tr, bl, br, CustomColor.fromHexString("cca76f"));
        }

        // Name
        String truncatedName = gambit.name;
        int maxNameChars = (panelWidth - 60) / 12;
        if (truncatedName.length() > maxNameChars) {
            truncatedName = truncatedName.substring(0, Math.max(3, maxNameChars - 3)) + "...";
        }
        ui.drawCenteredText("§6§l" + truncatedName, x + panelWidth / 2f, y + 30);

        // Description
        String desc = gambit.description;
        int charsPerLine = (panelWidth - 80) / 20;
        List<String> lines = wrapTextByChars(desc, charsPerLine);

        int textY = y + 65;
        int lineSpacing = 30;
        int maxLines = (panelHeight - 90) / lineSpacing;
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            if (textY < y + panelHeight - 25) {
                ui.drawText("§7" + lines.get(i), x + 30, textY);
                textY += lineSpacing;
            }
        }
    }

    private List<String> wrapTextByChars(String text, int maxChars) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (word.length() > maxChars) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
                lines.add(word.substring(0, Math.max(1, maxChars - 3)) + "...");
                continue;
            }

            int neededSpace = currentLine.length() > 0 ? word.length() + 1 : word.length();
            if (currentLine.length() + neededSpace <= maxChars) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0) {
            String finalLine = currentLine.toString();
            if (finalLine.length() > maxChars) {
                finalLine = finalLine.substring(0, Math.max(1, maxChars - 3)) + "...";
            }
            lines.add(finalLine);
        }
        return lines;
    }

    private static class OpenPartyFinderWidget extends Widget {
        public OpenPartyFinderWidget() {
            super();
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawText((hovered ? "§e§n" : "§e") + "Click to open Party Finder to scan", x, y);
        }

        @Override
        protected boolean onClick(int button) {
            AspectUtils.joinRaidPartyFinder("NOTG");
            return true;
        }
    }

    private static boolean shouldFetchGambits() {
        ZonedDateTime currentReset = GambitData.getLastResetTime();
        ZonedDateTime lastFetch = lastCrowdsourceFetch;
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("CET"));

        if(hasOldData) return lastFetch.plusSeconds(30).isBefore(now);

        if (lastFetch != null && lastFetch.plusSeconds(30).isAfter(now)) return false;

        return lastFetch == null || currentReset.isAfter(lastFetch);
    }
}