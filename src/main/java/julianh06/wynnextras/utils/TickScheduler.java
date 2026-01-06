package julianh06.wynnextras.utils;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

public final class TickScheduler {
    private static final Map<Integer, Task> TASKS = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    private static boolean initialized = false;

    private TickScheduler() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientTickEvents.END_CLIENT_TICK.register(TickScheduler::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client == null || client.world == null) return;

        for (Task task : new ArrayList<>(TASKS.values())) {
            if (task.tick()) {
                TASKS.remove(task.id);
            }
        }
    }

    public static int runWhen(BooleanSupplier condition, Runnable action) {
        return schedule(new RunWhenTask(condition, action));
    }

    public static int runAfterTicks(int ticks, Runnable action) {
        return schedule(new DelayTask(ticks, action));
    }

    public static int runEveryTicks(int interval, Runnable action) {
        return schedule(new IntervalTask(interval, action));
    }

    public static int runUntil(BooleanSupplier condition, Runnable action) {
        return schedule(new RunUntilTask(condition, action));
    }

    public static void cancel(int taskId) {
        TASKS.remove(taskId);
    }

    public static void cancelAll() {
        TASKS.clear();
    }

    private static int schedule(Task task) {
        int id = NEXT_ID.incrementAndGet();
        task.id = id;
        TASKS.put(id, task);
        return id;
    }

    private static abstract class Task {
        int id;
        abstract boolean tick(); // return true = finished
    }

    private static final class RunWhenTask extends Task {
        private final BooleanSupplier condition;
        private final Runnable action;

        RunWhenTask(BooleanSupplier condition, Runnable action) {
            this.condition = condition;
            this.action = action;
        }

        @Override
        boolean tick() {
            if (condition.getAsBoolean()) {
                action.run();
                return true;
            }
            return false;
        }
    }

    private static final class DelayTask extends Task {
        private int ticks;
        private final Runnable action;

        DelayTask(int ticks, Runnable action) {
            this.ticks = Math.max(0, ticks);
            this.action = action;
        }

        @Override
        boolean tick() {
            if (ticks-- <= 0) {
                action.run();
                return true;
            }
            return false;
        }
    }

    private static final class IntervalTask extends Task {
        private final int interval;
        private int counter = 0;
        private final Runnable action;

        IntervalTask(int interval, Runnable action) {
            this.interval = Math.max(1, interval);
            this.action = action;
        }

        @Override
        boolean tick() {
            if (++counter >= interval) {
                counter = 0;
                action.run();
            }
            return false;
        }
    }

    private static final class RunUntilTask extends Task {
        private final BooleanSupplier condition;
        private final Runnable action;

        RunUntilTask(BooleanSupplier condition, Runnable action) {
            this.condition = condition;
            this.action = action;
        }

        @Override
        boolean tick() {
            if (condition.getAsBoolean()) {
                return true;
            }
            action.run();
            return false;
        }
    }
}
