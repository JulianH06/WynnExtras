package julianh06.wynnextras.features.misc;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.functions.Function;
import com.wynntils.core.consumers.functions.arguments.FunctionArguments;
import com.wynntils.utils.type.CappedValue;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.event.WorldChangeEvent;
import julianh06.wynnextras.utils.CircularBuffer;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.List;
import java.util.Optional;

@WEModule
public class HealthRegenTick {
    private static final int VALID_COUNT = 3;
    private static final int TARGET_TICKS = 79;

    private static int lastHealth = -1;
    private static final CircularBuffer<Integer> recentHealths = new CircularBuffer<>(250);
    private static long ticksSinceLastConfirmedHprTick = -1;

    public static double getProjectedSeconds() {
        return (TARGET_TICKS - (ticksSinceLastConfirmedHprTick % TARGET_TICKS)) / 20.0;
    }

    public static class NextHealthRegenTickFunction extends Function<Double> {
        @Override
        public Double getValue(FunctionArguments arguments) {
            return getProjectedSeconds();
        }

        @Override
        protected List<String> getAliases() {
            return List.of("next_hpr_tick");
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (ticksSinceLastConfirmedHprTick != -1) ticksSinceLastConfirmedHprTick++;
        Optional<CappedValue> healthOpt = Models.CharacterStats.getHealth();
        if (healthOpt.isEmpty()) {
            return;
        }
        int health = healthOpt.get().current();
        recentHealths.insert(health);

        if (lastHealth != -1 && lastHealth != health) {
            if (isValid()) {
                ticksSinceLastConfirmedHprTick = 0;
            }
        }

        lastHealth = health;
    }

    private static boolean isValid() {
        if (!recentHealths.isFull()) return false;
        int lastValue = -1;
        int validCounter = 0;
        for (int b = 0; b < VALID_COUNT; b++) {
            int base = b * TARGET_TICKS;
            for (int i = base + TARGET_TICKS - 3; i <= base + TARGET_TICKS + 3; i++) {
                if (lastValue != -1 && recentHealths.get(i) != lastValue) {
                    validCounter++;
                    if (validCounter >= VALID_COUNT) return true;
                    break;
                }
                lastValue = recentHealths.get(i);
            }
            lastValue = -1;
        }
        return false;
    }

    @SubscribeEvent
    public void onWorldChange(WorldChangeEvent event) {
        recentHealths.reset();
        ticksSinceLastConfirmedHprTick = -1;
    }

}
