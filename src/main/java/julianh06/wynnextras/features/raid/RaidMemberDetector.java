package julianh06.wynnextras.features.raid;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.event.TickEvent;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

@WEModule
public class RaidMemberDetector {
    public static List<String> members = new ArrayList<>();
    public static List<Boolean> ready = new ArrayList<>();
    public static boolean inRaidSelectionScreen = false;
    public static Raid currentSelectedRaid;

    private static Command membersCmd = new Command(
            "printMembers",
            "",
            context -> {
                int i = 0;
                for(String member : members) {
                    if(member.isEmpty()) return 1;
                    if(ready.get(i)) {
                        System.out.println(member + " is ready");
                    } else {
                        System.out.println(member + " is not ready");
                    }
                    i++;
                }
                return 1;
            },
            null,
            null
    );

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if(members.isEmpty()) {
            members.add("");
            members.add("");
            members.add("");
            members.add("");
        }

        if(ready.isEmpty()) {
            ready.add(false);
            ready.add(false);
            ready.add(false);
            ready.add(false);
        }

        inRaidSelectionScreen = false;
        Screen screen = McUtils.mc().currentScreen;
        if(screen != null) {
            if(screen.getTitle().getString().contains("Party Finder")) {
                if(McUtils.containerMenu().getSlot(10).getStack() != null) {
                    if(McUtils.containerMenu().getSlot(10).getStack().getName().getString().contains("Back")) {
                        inRaidSelectionScreen = true;
                    }
                }
            }
            if(screen.getTitle().getString().contains("󏿡")) {
                for (int i = 0; i < 4; i++) {
                    ItemStack player = McUtils.containerMenu().getSlot(i + 18).getStack();
                    if(player == null) return;
                    if(player.getItem() == Items.SNOW) return;

                    Text rawName = player.getName();

                    Text firstLevel = getLastOrSelf(rawName);
                    Text secondLevel = getLastOrSelf(firstLevel);
                    Style style = secondLevel.getStyle();
                    HoverEvent hover = style.getHoverEvent();

                    String outputName;
                    if(hover != null && hover.getAction() == HoverEvent.Action.SHOW_TEXT) {
                        outputName = extractRealName(hover);
                    } else {
                        String[] parts = rawName.getString().split("\\s+", 2);
                        outputName = parts.length > 1
                                ? parts[1]
                                : rawName.getString().trim();
                    }

                    members.set(i, outputName);

                    ItemStack readyStack = McUtils.containerMenu().getSlot(i + 27).getStack();
                    if(readyStack == null) return;
                    if(readyStack.getName().getString().contains("Not")) {
                        ready.set(i, false);
                    } else {
                        ready.set(i, true);
                    }
                }
            }
        }
    }

    private static Text getLastOrSelf(Text text) {
        List<? extends Text> siblings = text.getSiblings();
        if (siblings.isEmpty()) {
            return text;
        }
        return siblings.get(siblings.size() - 1);
    }

    public String extractRealName(HoverEvent ev) {
        if (ev == null || ev.getAction() != HoverEvent.Action.SHOW_TEXT) {
            return null; // kein Hover-Text vorhanden
        }

        Text hoverTexts = ev.getValue(HoverEvent.Action.SHOW_TEXT);
        if (hoverTexts == null) {
            return null;
        }

        Text realNameHolder = hoverTexts
                .getSiblings().getFirst()
                .getSiblings().getFirst();

        return realNameHolder.getString();
    }

    @SubscribeEvent
    public void onChat(ChatEvent event) {
        String message = event.message.getString();
        if(message.contains("would like to start")) {
            if(message.contains("Nest of")) {
                RaidMemberDetector.notifyRaidSelection(Raid.NOTG);
                return;
            }

            if(message.contains("Nexus of")) {
                RaidMemberDetector.notifyRaidSelection(Raid.NOL);
                return;
            }

            if(message.contains("The Canyon")) {
                RaidMemberDetector.notifyRaidSelection(Raid.TCC);
                return;
            }

            if(message.contains("The Nameless")) {
                RaidMemberDetector.notifyRaidSelection(Raid.TNA);
                return;
            }
        }
        if(message.contains("ready")) {
            boolean isReady;
            if(message.contains("no longer")) {
                isReady = false;
            } else {
                isReady = true;
            }

            Text raw = event.message;
            String realName = getName(raw);

            boolean isNicked = false;
            for (Text comp : raw.getSiblings()) {
                Style style = comp.getStyle();

                HoverEvent hover = style.getHoverEvent();
                if (hover == null) continue;

                if (hover.getAction() == HoverEvent.Action.SHOW_TEXT) {
                    Text hoverText = hover.getValue(HoverEvent.Action.SHOW_TEXT);
                    String full = hoverText.getString();
                    realName = full.substring(full.lastIndexOf(' ') + 1);
                    isNicked = true;
                    break;
                }
            }
            if(!isNicked) {
                realName = realName.split("\\s+", 2)[1];
            }

            System.out.println("Entzifferter Name: " + realName);

            if(isReady) {
                if(members.contains(realName)) {
                    int i = 0;
                    for(String member : members) {
                        if(member.equals(realName)) {
                            ready.set(i, true);
                            return;
                        }
                        i++;
                    }
                }
                int i = 0;
                for (boolean isMemberReady : ready) {
                    if (isMemberReady) {
                        i++;
                        continue;
                    }
                    members.set(i, realName);
                    ready.set(i, true);
                    return;
                }
            } else {
                int j = 0;
                for (String member : members) {
                    if(realName.equals(member)) {
                        ready.set(j, false);
                        return;
                    }
                    j++;
                }
            }
        }
    }

    private static String getName(Text raw) {
        String rawString = raw.getString();
        String suffix1 = " is ready!";
        String suffix2 = " is no longer ready!";
        String baseName;
        if (rawString.endsWith(suffix1)) {
            baseName = rawString.substring(0, rawString.length() - suffix1.length());
        } else if (rawString.endsWith(suffix2)) {
            baseName = rawString.substring(0, rawString.length() - suffix2.length());
        } else {
            baseName = rawString.split("\\s+")[0];
        }

        return baseName;
    }

    public static void notifyRaidSelection(Raid raid) {
        currentSelectedRaid = raid;
    }
}
