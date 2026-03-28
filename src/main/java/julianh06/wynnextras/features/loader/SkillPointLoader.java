package julianh06.wynnextras.features.loader;

import com.wynntils.core.components.Models;
import com.wynntils.models.containers.containers.CharacterInfoContainer;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.Time;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.misc.CompassMenuOverlay;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

public class SkillPointLoader {
    private static SkillPointLoader INSTANCE;

    private int strength, dexterity, intelligence, defence, agility;
    private boolean loading = false;
    private long startTime = 0;

    private static final long OPEN_MENU_WAIT_MS = 600;
    private static final int SLOT_OFFSET = 11; // slots 11–15 = Str/Dex/Int/Def/Agi

    private SkillPointLoader() {}

    public static SkillPointLoader getInstance() {
        if (INSTANCE == null) INSTANCE = new SkillPointLoader();
        return INSTANCE;
    }

    public void load(int strength, int dexterity, int intelligence, int defence, int agility) {
        if (loading) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    Text.of("Skill point assignment is already running.")));
            return;
        }

        this.strength = strength;
        this.dexterity = dexterity;
        this.intelligence = intelligence;
        this.defence = defence;
        this.agility = agility;
        this.loading = true;
        this.startTime = Time.now().timestamp();

        if(Models.Container.getCurrentContainer() != null && Models.Container.getCurrentContainer() instanceof CharacterInfoContainer) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) { reset(); return; }

        int prevSlot = client.player.getInventory().getSelectedSlot();
        client.player.getInventory().setSelectedSlot(7);
        client.interactionManager.interactItem(client.player, net.minecraft.util.Hand.MAIN_HAND);
        client.player.getInventory().setSelectedSlot(prevSlot);
    }

    public void reset() {
        strength = dexterity = intelligence = defence = agility = 0;
        loading = false;
        startTime = 0;
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(tick -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;


            if (!(client.currentScreen instanceof HandledScreen<?> screen)) {
                CompassMenuOverlay.setSelectingWeapon(false);
                return;
            }

            SkillPointLoader spl = getInstance();
            if (!spl.loading) return;

            if (Time.now().timestamp() - spl.startTime < OPEN_MENU_WAIT_MS) {
                client.interactionManager.clickSlot(
                        screen.getScreenHandler().syncId,
                        4, 0, SlotActionType.QUICK_MOVE, client.player);
                return;
            }

            int[] points = {
                    spl.strength,
                    spl.dexterity,
                    spl.intelligence,
                    spl.defence,
                    spl.agility
            };

            int finishedCount = 0;

            for (int i = 0; i < 5; i++) {
                int remaining = points[i];
                if (remaining <= 0) { finishedCount++; continue; }

                int slot = SLOT_OFFSET + i;

                if (remaining % 5 == 0) {
                    client.interactionManager.clickSlot(
                            screen.getScreenHandler().syncId,
                            slot, 0, SlotActionType.QUICK_MOVE, client.player);
                    points[i] -= 5;
                } else {
                    client.interactionManager.clickSlot(
                            screen.getScreenHandler().syncId,
                            slot, 0, SlotActionType.PICKUP, client.player);
                    points[i]--;
                }

                break;
            }

            spl.strength     = points[0];
            spl.dexterity    = points[1];
            spl.intelligence = points[2];
            spl.defence      = points[3];
            spl.agility      = points[4];

            if (finishedCount == 5) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                        Text.of("Finished assigning skill points.")));
                spl.reset();
            }
        });
    }
}