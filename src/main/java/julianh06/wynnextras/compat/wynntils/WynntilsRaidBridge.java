package julianh06.wynnextras.compat.wynntils;

import java.lang.reflect.Method;

public final class WynntilsRaidBridge {
    private record Binding(Object model, Method currentRaid) {}

    private static final WynntilsCapability<Binding> RAID = new WynntilsCapability<>(
            "raid-state",
            () -> {
                Class<?> models = WynntilsCompat.requireClass("com.wynntils.core.components.Models");
                Object model = models.getField("Raid").get(null);
                return new Binding(model, model.getClass().getMethod("getCurrentRaid"));
            }
    );

    private WynntilsRaidBridge() {}

    public static long currentRoomTime() {
        return RAID.invoke(binding -> {
            Object raid = binding.currentRaid.invoke(binding.model);
            if (raid == null) return 0L;
            Object room = raid.getClass().getMethod("getCurrentRoom").invoke(raid);
            if (room == null) return 0L;
            Object time = room.getClass().getMethod("getRoomTotalTime").invoke(room);
            return time instanceof Number number ? number.longValue() : 0L;
        }).orElse(0L);
    }
}
