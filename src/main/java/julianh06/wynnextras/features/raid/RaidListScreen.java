package julianh06.wynnextras.features.raid;

import com.wynntils.models.raid.raids.RaidKind;
import com.wynntils.models.raid.type.RaidInfo;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.utils.render.RenderLayers;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class RaidListScreen extends Screen {
    private static int scrollOffset = 0;
    private static long lastScrollTime = 0;
    private static final long scrollCooldown = 0; // in ms

    Identifier ButtonTexture = Identifier.of("wynnextras", "textures/gui/bankoverlay/button.png");
    Identifier NOTGTexture = Identifier.of("wynnextras", "textures/gui/raidicons/nestofthegrootslangs-small.png");
    Identifier NOLTexture = Identifier.of("wynnextras", "textures/gui/raidicons/orphionsnexusoflight-small.png");
    Identifier TCCTexture = Identifier.of("wynnextras", "textures/gui/raidicons/thecanyoncolossus-small.png");
    Identifier TNATexture = Identifier.of("wynnextras", "textures/gui/raidicons/thenamelessanomaly-small.png");

    public RaidListScreen() {
        super(Text.of("Raid List Test"));
        scrollOffset = 0;
    }

    @Override
    protected void init() {
        ScreenMouseEvents.afterMouseScroll(this).register((
                screen,
                mX,
                mY,
                horizontalAmount,
                verticalAmount
        ) -> {
            long now = System.currentTimeMillis();
            if (now - lastScrollTime < scrollCooldown) {
                return;
            }
            lastScrollTime = now;

            if (verticalAmount > 0) {
                scrollOffset -= 10; //Scroll up
            } else {
                scrollOffset += 10; //Scroll down
            }
            if (scrollOffset < 0) {
                scrollOffset = 0;
            }
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        //super.render(context, mouseX, mouseY, delta);
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        int width = (int) (screenWidth * 0.5);
        int xStart = screenWidth / 2 - width / 2;
        int yStart = screenWidth / 2;

        RenderUtils.drawRect(context.getMatrices(), CustomColor.fromInt(-804253680), 0, 0, 0, MinecraftClient.getInstance().currentScreen.width, MinecraftClient.getInstance().currentScreen.height);
        int i = 0;
        for(RaidData raid : RaidListData.INSTANCE.raids) {
            int yPos = 10 + 50 * i - scrollOffset;

            if (yPos + 40 >= 0 && yPos <= screenHeight) {
                Identifier raidTexture = getTexture(raid.raidInfo.getRaidKind());
                RenderUtils.drawTexturedRect(context.getMatrices(), ButtonTexture, xStart, yPos, 0, width, 40, width, 40);
                RenderUtils.drawTexturedRect(context.getMatrices(), raidTexture, xStart + 1, yPos + 2.5f, 0, 35, 35, 35, 35);

                context.drawText(MinecraftClient.getInstance().textRenderer, raid.raidInfo.getRaidKind().getRaidName(), xStart + 42, yPos + 6, CustomColor.fromHexString("FFFFFF").asInt(), true);
                context.drawText(MinecraftClient.getInstance().textRenderer, convertTime(raid.raidEndTime), xStart + 42, yPos + 16, CustomColor.fromHexString("FFFFFF").asInt(), true);
                if(raid.completed) {
                    context.drawText(MinecraftClient.getInstance().textRenderer, "Completed", xStart + 42, yPos + 26, CustomColor.fromHexString("FFFFFF").asInt(), true);
                } else {
                    context.drawText(MinecraftClient.getInstance().textRenderer, "FAILED", xStart + 42, yPos + 26, CustomColor.fromHexString("FF0000").asInt(), true);
                }
            }
            i++;
        }
    }

    @Override
    public void close() {
        super.close();
        System.out.println("closed");
    }

    public Identifier getTexture(RaidKind raidKind) {
        return switch (raidKind.getAbbreviation()) {
            case "NOG" -> NOTGTexture;
            case "TNA" -> TNATexture;
            case "NOL" -> NOLTexture;
            case "TCC" -> TCCTexture;
            default -> null;
        };
    }

    public static String convertTime(long time) {

        ZonedDateTime dateTime = Instant.ofEpochMilli(time)
                .atZone(ZoneId.systemDefault());

        String formatted = dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        return formatted;
        //System.out.println("Time: " + formatted);
    }

}
