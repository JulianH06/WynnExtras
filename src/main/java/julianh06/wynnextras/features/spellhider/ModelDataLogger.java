package julianh06.wynnextras.features.spellhider;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.event.InitEvent;
import julianh06.wynnextras.utils.ChatUtils;
import julianh06.wynnextras.utils.EntityUtils;
import julianh06.wynnextras.utils.ItemUtils;
import julianh06.wynnextras.utils.TimeLimitedSet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Vector3f;

import java.util.concurrent.TimeUnit;

@WEModule
public class ModelDataLogger {
    public enum State {
        OFF,
        CONSOLE_ALL,
        CONSOLE_UNKNOWN,
        CHAT_ALL,
        CHAT_UNKNOWN;

        public static State from(String state) {
            try {
                return valueOf(state.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private static State currentState = State.OFF;

    public static void setState(State state) {
        currentState = state;
    }

    @SubscribeEvent
    public void init(InitEvent empty) {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private static final TimeLimitedSet<Integer> seenEntities = new TimeLimitedSet<>(60, TimeUnit.SECONDS);
    private static final TimeLimitedSet<Float> recentModels = new TimeLimitedSet<>(2, TimeUnit.SECONDS);

    public void onClientTick(MinecraftClient client) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return;
        world.getEntities().forEach(entity -> {
            if (entity instanceof DisplayEntity.ItemDisplayEntity display) {
                if (display.getItemStack().getItem() == Items.OAK_BOAT) {
                    boolean isNew = !seenEntities.contains(entity.getId());
                    if (isNew) {
                        seenEntities.put(entity.getId());
                        // set its scale if a custom one is set
                        SpellModifiers modifiers = SpellHider.getModifiers(display);
                        if (modifiers != null) {
                            try {
                                Vector3f scale = modifiers.get(SpellModifier.SCALE);
                                if (scale != null) {
                                    EntityUtils.setScale(display, scale);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        // log model of new entities
                        if (currentState != State.OFF) {
                            Float model = ItemUtils.getFirsCustomModelDataFloat(display.getItemStack());
                            if (!recentModels.contains(model)) {
                                recentModels.put(model);
                                SpellNamespace name = SpellHider.getNameSpace(display);
                                switch (currentState) {
                                    case CONSOLE_ALL: {
                                        WynnExtras.LOGGER.debug("Entity model: {} group: {}", model, name);
                                        break;
                                    }
                                    case CONSOLE_UNKNOWN: {
                                        if (name == null) {
                                            WynnExtras.LOGGER.debug("Entity model: {}", model);
                                        }
                                        break;
                                    }
                                    case CHAT_ALL: {
                                        ChatUtils.sendMessage("Entity model: " + model + " group: " + name);
                                        break;
                                    }
                                    case CHAT_UNKNOWN: {
                                        if (name == null) {
                                            ChatUtils.sendMessage("Entity model: " + model);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }
}
