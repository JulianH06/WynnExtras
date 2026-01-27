package julianh06.wynnextras.features.misc;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.ClickEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.WEScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@WEModule
public class CustomClassSelection {
    //this was something i tried, idk if ill continue working it but ill keept the code for now

    static String classSelectionTitle = "\uDAFF\uDFD5\uE01F";
    public static Boolean inClassSelection = false;
    static boolean registeredScreenRendering = false;
    int mouseX = 0;
    int mouseY = 0;

    //@SubscribeEvent
    public void onTick(TickEvent event) {
        inClassSelection = false;
        MinecraftClient client = MinecraftClient.getInstance();

        if(client == null) return;

        Screen currentScreen = client.currentScreen;

        if(currentScreen == null) {
            registeredScreenRendering = false;
            return;
        }

        if(currentScreen.getTitle() == null) return;

        if(currentScreen.getTitle().getString().equals(classSelectionTitle)) {
            inClassSelection = true;
//                for(Slot slot : McUtils.containerMenu().slots) {
//                    if(slot.getStack() == null) continue;
//                    if(slot.getStack().getCustomName() == null) continue;
//                    System.out.println(slot.getStack().getCustomName() + " " + slot.getIndex());
//                }
//                System.out.println("----------------");
        } else return;

        if(!registeredScreenRendering) {
            registeredScreenRendering = true;

            ScreenEvents.beforeRender(currentScreen).register((screen, drawContext, mouseX, mouseY, tickDelta) -> {
                if (!(screen instanceof HandledScreen<?> handled)) return;
                if (screen.getTitle() == null) return;

                if (!screen.getTitle().getString().equals(classSelectionTitle)) return;

                int oldMouseX = this.mouseX;
                int oldMouseY = this.mouseY;
                int dx = mouseX - oldMouseX;
                int dy = mouseY - oldMouseY;

                UIUtils ui = CustomClassSelectionOverlay.INSTANCE.getUi();

                // Render the WEScreen overlay
                CustomClassSelectionOverlay.INSTANCE.setDrawContext(drawContext);
                CustomClassSelectionOverlay.INSTANCE.computeScaleAndOffsets();
                CustomClassSelectionOverlay.INSTANCE.setUi(
                        new UIUtils(drawContext,
                                CustomClassSelectionOverlay.INSTANCE.getScaleFactor(),
                                CustomClassSelectionOverlay.INSTANCE.getxStart(),
                                CustomClassSelectionOverlay.INSTANCE.getyStart()));

                CustomClassSelectionOverlay.INSTANCE.render(drawContext, mouseX, mouseY, tickDelta);
                if(!InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), GLFW.GLFW_KEY_SPACE)) return;
                Click click = new Click(mouseX, mouseY, null);
                CustomClassSelectionOverlay.INSTANCE.mouseDragged(click, dx, dy);
            });
        }
    }

    @SubscribeEvent
    public void onClick(ClickEvent event) {
        //Click click = new Click(event.mouseX, event.mouseY, null);
        //CustomClassSelectionOverlay.INSTANCE.mouseClicked(click, false);
    }
}
