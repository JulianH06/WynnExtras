package julianh06.wynnextras.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class TimeLimitedSet<T> {
    private final Cache<@NotNull T, @NotNull Boolean> cache;

    public TimeLimitedSet(long duration, TimeUnit unit) {
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, unit)  // Entries expire after this time
                .build();
    }

    public void put(T element) {
        cache.put(element, Boolean.TRUE);
    }

    public boolean contains(T element) {
        return cache.getIfPresent(element) != null;
    }
}
