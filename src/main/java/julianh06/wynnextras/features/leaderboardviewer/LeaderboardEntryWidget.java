package julianh06.wynnextras.features.leaderboardviewer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import julianh06.wynnextras.features.guildviewer.GVScreen;
import julianh06.wynnextras.features.guildviewer.GV;
import julianh06.wynnextras.features.guildviewer.BannerGuiElementState;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.utils.render.HorizontalAlignment;
import julianh06.wynnextras.utils.render.RenderUtils;
import julianh06.wynnextras.utils.render.VerticalAlignment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.block.entity.model.BannerFlagBlockModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.util.DyeColor;
import net.minecraft.text.Text;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LeaderboardEntryWidget extends Widget {
    private static final CustomColor GOLD = CustomColor.fromHexString("FFD966");
    private static final Identifier REDACTED_SKIN = Identifier.of(
            "wynnextras", "textures/gui/leaderboardviewer/redacted_steve.png");

    private final LeaderboardEntry entry;
    private final LVScreen.Type type;
    private final LeaderboardDefinition leaderboard;
    private final LeaderboardEntry nextEntry;
    private final GuildSeason guildSeason;
    private final boolean ownEntry;
    private final boolean redacted;
    private final GuildBanner guildBanner;
    private final BannerFlagBlockModel guildBannerModel;
    private Identifier skinTexture = DefaultSkinHelper.getSteve().body().texturePath();

    public LeaderboardEntryWidget(LeaderboardEntry entry, LVScreen.Type type, LeaderboardDefinition leaderboard,
                                  GuildSeason guildSeason, boolean ownEntry, LeaderboardEntry nextEntry) {
        this.entry = entry;
        this.type = type;
        this.leaderboard = leaderboard;
        this.nextEntry = nextEntry;
        this.guildSeason = guildSeason;
        this.ownEntry = ownEntry;
        redacted = isRedacted(entry);
        guildBanner = type == LVScreen.Type.Guild ? createGuildBanner(entry.rawData()) : null;
        guildBannerModel = guildBanner == null ? null : new BannerFlagBlockModel(
                MinecraftClient.getInstance().getLoadedEntityModels()
                        .getModelPart(EntityModelLayers.STANDING_BANNER_FLAG));
        if (type != LVScreen.Type.Guild && !redacted) {
            LeaderboardPlayerSkinService.fetchSkin(entry.uuid(), entry.name())
                    .thenAccept(texture -> skinTexture = texture);
        }
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        ui.drawFixedVanillaPanelButtonFade(x, y, width, height, 10, 2, hovered);
        ui.drawCenteredText("#" + entry.rank(), x + 36, y + height / 2f, GOLD, 2.5f);

        if (type == LVScreen.Type.Guild) {
            drawGuildBanner(ctx);
        } else {
            if (redacted) {
                ui.drawImage(REDACTED_SKIN, x + 72, y + 8, 36, 36);
            } else {
                drawPlayerHead(ctx, x + 72, y + 8, 36);
            }
        }

        String name = redacted ? "§4🔒 §8HIDDEN"
                : entry.name() == null || entry.name().isBlank() ? "Unknown" : entry.name();
        if (entry.prefix() != null && !entry.prefix().isBlank()) name += " [" + entry.prefix() + "]";
        CustomColor nameColor = type == LVScreen.Type.Guild && ownEntry ? GOLD : getPlayerRankColor();
        Text nameText = type != LVScreen.Type.Guild && ownEntry
                ? Text.literal(name).styled(style -> style.withBold(true))
                : Text.literal(name);
        ui.drawText(nameText, x + 125, y + height / 2f, nameColor,
                HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 2.5f);

        ui.drawText(LeaderboardValueFormatter.format(leaderboard, entry, nextEntry),
                x + width - 20, y + height / 2f, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.RIGHT, VerticalAlignment.MIDDLE, 2.15f);
    }

    @Override
    protected boolean onClick(int button) {
        if (redacted) return false;
        MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
        if (type == LVScreen.Type.Guild) {
            String guild = entry.prefix() == null || entry.prefix().isBlank() ? entry.name() : entry.prefix();
            if (guild == null || guild.isBlank()) return false;
            GV.open(guild);
        } else {
            if (entry.name() == null || entry.name().isBlank()) return false;
            PV.open(entry.name());
        }
        return true;
    }

    private void drawPlayerHead(DrawContext ctx, int headX, int headY, int size) {
        RenderUtils.drawTexturedRect(ctx, skinTexture, ui.sx(headX), ui.sy(headY), ui.sw(size), ui.sh(size),
                8, 8, 8, 8, 64, 64);
        RenderUtils.drawTexturedRect(ctx, skinTexture, ui.sx(headX), ui.sy(headY), ui.sw(size), ui.sh(size),
                40, 8, 8, 8, 64, 64);
    }

    private void drawGuildBanner(DrawContext ctx) {
        if (guildBanner == null || guildBannerModel == null
                || !(MinecraftClient.getInstance().currentScreen instanceof LVScreen screen)) return;
        float specialScale = screen.getSpecialElementScale();
        int x1 = Math.round((x + 68) * specialScale);
        int y1 = Math.round((y - 2) * specialScale);
        int x2 = Math.round((x + 116) * specialScale);
        int y2 = Math.round((y + height - 2) * specialScale);
        BannerGuiElementState state = new BannerGuiElementState(
                guildBannerModel, guildBanner.baseColor(), guildBanner.patterns(), x1, y1, x2, y2,
                ctx.scissorStack.peekLast(), 16 * specialScale, 0f, false);
        ctx.state.addSpecialElement(state);
    }

    private static GuildBanner createGuildBanner(JsonObject rawData) {
        if (rawData == null || !rawData.has("banner") || !rawData.get("banner").isJsonObject()) {
            return null;
        }

        JsonObject banner = rawData.getAsJsonObject("banner");
        String base = getString(banner, "base");
        DyeColor baseColor = GVScreen.dyeColorFromName(base);
        if (!banner.has("layers") || !banner.get("layers").isJsonArray()) {
            return new GuildBanner(baseColor, BannerPatternsComponent.DEFAULT);
        }

        BannerPatternsComponent.Builder patterns = new BannerPatternsComponent.Builder();
        JsonArray layers = banner.getAsJsonArray("layers");
        for (JsonElement layerElement : layers) {
            if (!layerElement.isJsonObject()) continue;
            JsonObject layer = layerElement.getAsJsonObject();
            RegistryEntry<BannerPattern> pattern = GVScreen.resolvePatternEntry(getString(layer, "pattern"));
            if (pattern != null) patterns.add(pattern, GVScreen.dyeColorFromName(getString(layer, "colour")));
        }
        return new GuildBanner(baseColor, patterns.build());
    }

    private static String getString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static boolean isRedacted(LeaderboardEntry entry) {
        if ("redacted".equalsIgnoreCase(entry.uuid()) || "redacted".equalsIgnoreCase(entry.name())) return true;
        JsonObject rawData = entry.rawData();
        return rawData != null && rawData.has("restricted") && rawData.get("restricted").getAsBoolean();
    }

    private CustomColor getPlayerRankColor() {
        if (type == LVScreen.Type.Guild || redacted || entry.rawData() == null) {
            return CustomColor.fromHexString("FFFFFF");
        }
        JsonObject rawData = entry.rawData();
        if (!rawData.has("legacyRankColour") || !rawData.get("legacyRankColour").isJsonObject()) {
            return CustomColor.fromHexString("FFFFFF");
        }
        JsonElement mainColor = rawData.getAsJsonObject("legacyRankColour").get("main");
        if (mainColor == null || mainColor.isJsonNull()) return CustomColor.fromHexString("FFFFFF");
        try {
            return CustomColor.fromHexString(mainColor.getAsString());
        } catch (IllegalArgumentException ignored) {
            return CustomColor.fromHexString("FFFFFF");
        }
    }

    public List<Text> getSeasonTooltip() {
        if (guildSeason == null || entry.score() == null) return List.of();
        double score = entry.score();
        NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.getDefault());
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.literal("§6Season Rating: §f" + numberFormat.format(score) + " SR"));

        GuildSeason.Reward reached = guildSeason.lastReachedRatingReward(score);
        if (reached != null) {
            tooltip.add(Text.literal("§7Last reward: §f" + GuildSeasonRewardsWidget.formatCondition(reached)
                    + " §8- §f" + GuildSeasonRewardsWidget.formatReward(reached)));
        }

        GuildSeason.Reward next = guildSeason.nextRatingReward(score);
        if (next == null) {
            tooltip.add(Text.literal("§aAll SR rewards reached"));
        } else {
            long remaining = Math.max(0, Math.round(next.threshold() - score));
            tooltip.add(Text.literal("§7Next reward: §f" + GuildSeasonRewardsWidget.formatCondition(next)
                    + " §8- §f" + GuildSeasonRewardsWidget.formatReward(next)));
            tooltip.add(Text.literal("§7Remaining: §e" + numberFormat.format(remaining) + " SR"));
        }
        return tooltip;
    }

    private record GuildBanner(DyeColor baseColor, BannerPatternsComponent patterns) {
    }
}
