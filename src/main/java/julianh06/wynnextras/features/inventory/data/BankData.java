package julianh06.wynnextras.features.inventory.data;

import julianh06.wynnextras.core.WynnExtras;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import julianh06.wynnextras.features.misc.ItemStackDeserializer;
import julianh06.wynnextras.features.misc.ItemStackSerializer;
import julianh06.wynnextras.utils.OptionalTypeAdapter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

public abstract class BankData {
    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor(daemonThreadFactory());
    private int lastPage = 1;
    @SerializedName("BankPages")
    private HashMap<Integer, List<ItemStack>> bankPages = new HashMap<>();
    @SerializedName("BankPageNames")
    private HashMap<Integer, String> bankPageNames = new HashMap<>();
    private String characterNickname = null; // For character banks - stores the character's class name (e.g., "Dark Wizard")
    private int characterLevel = 0; // For character banks - stores the character's combat level
    /** Per-page bag counts keyed by "RAID|TIER" (e.g. "NOG|LEGENDARY" -> 3). Stored as plain
     *  numbers so they survive serialization without depending on Wynntils item annotations. */
    private HashMap<Integer, HashMap<String, Integer>> bagCounts = new HashMap<>();

    public abstract Path getConfigPath();

    public void save() {
        Path path = getConfigPath();
        BankDataSnapshot snapshot = BankDataSnapshot.from(this);
        CompletableFuture.runAsync(() -> saveSnapshot(path, snapshot), SAVE_EXECUTOR).join();
    }

    public CompletableFuture<Void> saveAsync() {
        Path path = getConfigPath();
        BankDataSnapshot snapshot = BankDataSnapshot.from(this);
        return CompletableFuture.runAsync(() -> saveSnapshot(path, snapshot), SAVE_EXECUTOR);
    }

    public CompletableFuture<Void> loadAsync() {
        return loadAsync(getConfigPath(), this.getClass(), () -> true);
    }

    protected CompletableFuture<Void> loadAsync(Path path, Class<? extends BankData> dataClass, BooleanSupplier shouldApply) {
        return CompletableFuture
                .supplyAsync(() -> loadSnapshot(path, dataClass), SAVE_EXECUTOR)
                .thenAccept(result -> MinecraftClient.getInstance().execute(() -> {
                    if (!shouldApply.getAsBoolean()) return;
                    applyLoadResult(result);
                }));
    }

    private static void saveSnapshot(Path path, BankDataSnapshot snapshot) {
        try {
            Files.createDirectories(path.getParent());

            try (Writer writer = Files.newBufferedWriter(path)) {
                getGson().toJson(snapshot, writer);
            }
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Couldn't write bank data:");
            e.printStackTrace();
        }
    }

    public void load() {
        Path path = getConfigPath();
        applyLoadResult(loadSnapshot(path, this.getClass()));
    }

    private static LoadResult loadSnapshot(Path path, Class<? extends BankData> dataClass) {
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Couldn't create config directory:");
            e.printStackTrace();
            return LoadResult.keepExisting();
        }

        if (!Files.exists(path)) {
            return LoadResult.cleared();
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            return LoadResult.loaded(getGson().fromJson(reader, dataClass));
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Couldn't read bank data:");
            e.printStackTrace();
            return LoadResult.keepExisting();
        }
    }

    private void applyLoadResult(LoadResult result) {
        if (result == null) return;
        if (result.loaded() != null) {
            applyLoadedData(result.loaded());
        } else if (result.shouldClear()) {
            clearData();
        }
    }

    protected void applyLoadedData(BankData loaded) {
        this.bankPages = loaded.bankPages != null ? loaded.bankPages : new HashMap<>();
        this.lastPage = loaded.lastPage;
        this.bankPageNames = loaded.bankPageNames != null ? loaded.bankPageNames : new HashMap<>();
        this.characterNickname = loaded.characterNickname;
        this.characterLevel = loaded.characterLevel;
        this.bagCounts = loaded.bagCounts != null ? loaded.bagCounts : new HashMap<>();
    }

    protected void clearData() {
        this.bankPages = new HashMap<>();
        this.lastPage = 1;
        this.bankPageNames = new HashMap<>();
        this.characterNickname = null;
        this.characterLevel = 0;
        this.bagCounts = new HashMap<>();
    }

    public int getLastPage() {
        return lastPage;
    }

    public void setLastPage(int lastPage) {
        this.lastPage = lastPage;
    }

    public void incrementLastPage() {
        lastPage++;
    }

    public HashMap<Integer, List<ItemStack>> getBankPages() {
        return bankPages;
    }

    public HashMap<Integer, String> getBankPageNames() {
        return bankPageNames;
    }

    public String getCharacterNickname() {
        return characterNickname;
    }

    public int getCharacterLevel() {
        return characterLevel;
    }

    public void setCharacterInfo(String characterNickname, int characterLevel) {
        this.characterNickname = characterNickname;
        this.characterLevel = characterLevel;
    }

    public HashMap<Integer, HashMap<String, Integer>> getBagCounts() {
        return bagCounts;
    }

    private static ThreadFactory daemonThreadFactory() {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, "WynnExtras Bank Data Save-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private static class BankDataSnapshot {
        private final int lastPage;
        @SerializedName("BankPages")
        private final HashMap<Integer, List<ItemStack>> bankPages;
        @SerializedName("BankPageNames")
        private final HashMap<Integer, String> bankPageNames;
        private final String characterNickname;
        private final int characterLevel;
        private final HashMap<Integer, HashMap<String, Integer>> bagCounts;

        private BankDataSnapshot(BankData data) {
            this.lastPage = data.lastPage;
            this.bankPages = copyBankPages(data.bankPages);
            this.bankPageNames = new HashMap<>(data.bankPageNames);
            this.characterNickname = data.characterNickname;
            this.characterLevel = data.characterLevel;
            this.bagCounts = copyBagCounts(data.bagCounts);
        }

        private static BankDataSnapshot from(BankData data) {
            return new BankDataSnapshot(data);
        }

        private static HashMap<Integer, List<ItemStack>> copyBankPages(HashMap<Integer, List<ItemStack>> source) {
            HashMap<Integer, List<ItemStack>> copy = new HashMap<>();
            for (Map.Entry<Integer, List<ItemStack>> entry : source.entrySet()) {
                List<ItemStack> items = entry.getValue();
                if (items == null) {
                    copy.put(entry.getKey(), null);
                    continue;
                }

                List<ItemStack> itemCopies = new ArrayList<>(items.size());
                for (ItemStack stack : items) {
                    itemCopies.add(stack == null ? null : stack.copy());
                }
                copy.put(entry.getKey(), itemCopies);
            }
            return copy;
        }

        private static HashMap<Integer, HashMap<String, Integer>> copyBagCounts(HashMap<Integer, HashMap<String, Integer>> source) {
            HashMap<Integer, HashMap<String, Integer>> copy = new HashMap<>();
            for (Map.Entry<Integer, HashMap<String, Integer>> entry : source.entrySet()) {
                HashMap<String, Integer> pageCounts = entry.getValue();
                copy.put(entry.getKey(), pageCounts == null ? null : new HashMap<>(pageCounts));
            }
            return copy;
        }
    }

    private record LoadResult(BankData loaded, boolean shouldClear) {
        private static LoadResult loaded(BankData loaded) {
            return new LoadResult(loaded, false);
        }

        private static LoadResult cleared() {
            return new LoadResult(null, true);
        }

        private static LoadResult keepExisting() {
            return new LoadResult(null, false);
        }
    }

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(new TypeAdapterFactory() {
                @SuppressWarnings("unchecked")
                public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
                    if (!Optional.class.isAssignableFrom(type.getRawType())) {
                        return null;
                    }
                    Type actualType = ((ParameterizedType) type.getType()).getActualTypeArguments()[0];
                    TypeAdapter<?> delegate = gson.getAdapter(TypeToken.get(actualType));
                    return (TypeAdapter<T>) new OptionalTypeAdapter<>(delegate);
                }
            })
            .registerTypeAdapter(ItemStack.class, new ItemStackSerializer())
            .registerTypeAdapter(ItemStack.class, new ItemStackDeserializer())
            .setPrettyPrinting()
            .create();

    public static Gson getGson() {
        return GSON;
    }
}