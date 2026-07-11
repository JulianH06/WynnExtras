// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
package julianh06.wynnextras.wtshim.core.mod;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import julianh06.wynnextras.wtshim.core.components.Manager;
import julianh06.wynnextras.wtshim.mc.event.TickAlwaysEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public final class TickSchedulerManager extends Manager {
    private final Map<ScheduledTask, Integer> tasks = new ConcurrentHashMap<>();

    public ScheduledTask scheduleLater(Runnable runnable, int ticksDelay) {
        ScheduledTask task = new ScheduledTask(runnable);
        tasks.put(task, ticksDelay);
        return task;
    }

    public ScheduledTask scheduleNextTick(Runnable runnable) {
        return scheduleLater(runnable, 0);
    }

    public void cancel(ScheduledTask task) {
        tasks.remove(task);
    }

    // The priority is set to HIGHEST to ensure that the tasks are run
    // before any other tick event listeners could schedule new tasks
    // making it run in the same tick
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onTick(TickAlwaysEvent e) {
        Iterator<Map.Entry<ScheduledTask, Integer>> it = tasks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ScheduledTask, Integer> entry = it.next();
            int ticksLeft = entry.getValue();
            if (ticksLeft == 0) {
                entry.getKey().task.run();
                it.remove();
            } else {
                entry.setValue(ticksLeft - 1);
            }
        }
    }

    public record ScheduledTask(Runnable task) {}
}
