package julianh06.wynnextras.features.raid;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.ChatEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@WEModule
public class RaidMemberDetector {
    @SubscribeEvent
    public void onChat(ChatEvent event) {
        Text message = event.message;
        if (message == null || message.getString().isEmpty()) return;

        if (message.getString().contains("Party members")) {
            List<String> names = new ArrayList<>();
            message.visit((style, string) -> {
                String cleaned = string.replaceAll("§[0-9a-fk-or]", "").trim();
                if (cleaned.matches(".*[\\uE000-\\uF8FF].*") || cleaned.isEmpty()) return Optional.empty();

                cleaned = cleaned.replace("Party members:", "").replace("and", "").trim();
                for (String part : cleaned.split(",")) {
                    part = part.trim();
                    if (!part.isEmpty()) names.add(part);
                }
                return Optional.empty();
            }, Style.EMPTY);
            RaidListScreen.currentPlayers = names;
        }
    }
}
