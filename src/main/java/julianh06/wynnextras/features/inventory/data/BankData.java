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
import net.minecraft.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public abstract class BankData {
    private static final long ASYNC_SAVE_DELAY_MS = 250;
    private static final ScheduledExecutorService SAVE_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "WynnExtras BankData Save");
        thread.setDaemon(true);
        return thread;
    });

    private int lastPage = 1;
    @SerializedName("BankPages")
    private HashMap<Integer, List<ItemStack>> bankPages = new HashMap<>();
    @SerializedName("BankPageNames")
    private HashMap<Integer, String> bankPageNames = new HashMap<>();
    private String characterNickname = null; // For character banks - stores the character's class name (e.g., "Dark Wizard")
    private int characterLevel = 0; // For character banks - stores the character's combat level
    private List<ItemStack> playerInventory = List.of();
    private List<ItemStack> playerArmor = List.of();
    /** Per-page bag counts keyed by "RAID|TIER" (e.g. "NOG|LEGENDARY" -> 3). Stored as plain
     *  numbers so they survive serialization without depending on Wynntils item annotations. */
    private HashMap<Integer, HashMap<String, Integer>> bagCounts = new HashMap<>();
    private final AtomicLong asyncSaveGeneration = new AtomicLong();

    public abstract Path getConfigPath();

    public void save() {
        asyncSaveGeneration.incrementAndGet();
        writeToPath(getConfigPath(), this);
    }

    public void saveAsyncDebounced() {
        Path path = getConfigPath();
        BankDataSaveSnapshot snapshot = createSaveSnapshot();
        long generation = asyncSaveGeneration.incrementAndGet();
        SAVE_EXECUTOR.schedule(() -> {
            if (asyncSaveGeneration.get() != generation) return;
            writeToPath(path, snapshot);
        }, ASYNC_SAVE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private static void writeToPath(Path path, Object data) {
        try {
            Files.createDirectories(path.getParent());

            try (Writer writer = Files.newBufferedWriter(path)) {
                getGson().toJson(data, writer);
            }
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Couldn't write bank data:");
            e.printStackTrace();
        }
    }

    public void load() {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Couldn't create config directory:");
            e.printStackTrace();
        }

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                BankData loaded = getGson().fromJson(reader, this.getClass());
                if (loaded != null) {
                    this.bankPages = loaded.bankPages != null ? loaded.bankPages : new HashMap<>();
                    this.lastPage = loaded.lastPage;
                    this.bankPageNames = loaded.bankPageNames != null ? loaded.bankPageNames : new HashMap<>();
                    this.characterNickname = loaded.characterNickname;
                    this.characterLevel = loaded.characterLevel;
                    this.playerInventory = loaded.playerInventory != null ? loaded.playerInventory : List.of();
                    this.playerArmor = loaded.playerArmor != null ? loaded.playerArmor : List.of();
                    this.bagCounts = loaded.bagCounts != null ? loaded.bagCounts : new HashMap<>();
                }
            } catch (IOException e) {
                WynnExtras.LOGGER.error("[WynnExtras] Couldn't read bank data:");
                e.printStackTrace();
            }
        } else {
            // No file for this UUID/character yet — clear EVERYTHING so we don't leak the
            // previous character's pages/bag counts into the new in-memory INSTANCE.
            this.bankPages = new HashMap<>();
            this.lastPage = 1;
            this.bankPageNames = new HashMap<>();
            this.characterNickname = null;
            this.characterLevel = 0;
            this.playerInventory = List.of();
            this.playerArmor = List.of();
            this.bagCounts = new HashMap<>();
        }
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

    public List<ItemStack> getPlayerInventory() {
        return playerInventory;
    }

    public List<ItemStack> getPlayerArmor() {
        return playerArmor;
    }

    public void setPlayerInventorySnapshot(List<ItemStack> playerInventory, List<ItemStack> playerArmor) {
        this.playerInventory = copyItemList(playerInventory);
        this.playerArmor = copyItemList(playerArmor);
    }

    public HashMap<Integer, HashMap<String, Integer>> getBagCounts() {
        return bagCounts;
    }

    private static List<ItemStack> copyItemList(List<ItemStack> items) {
        if (items == null) return List.of();
        return items.stream()
                .map(stack -> stack == null ? null : stack.copy())
                .toList();
    }

    private BankDataSaveSnapshot createSaveSnapshot() {
        BankDataSaveSnapshot snapshot = new BankDataSaveSnapshot();
        snapshot.lastPage = lastPage;
        snapshot.bankPages = copyBankPages(bankPages);
        snapshot.bankPageNames = bankPageNames == null ? new HashMap<>() : new HashMap<>(bankPageNames);
        snapshot.characterNickname = characterNickname;
        snapshot.characterLevel = characterLevel;
        snapshot.playerInventory = copyItemList(playerInventory);
        snapshot.playerArmor = copyItemList(playerArmor);
        snapshot.bagCounts = copyBagCounts(bagCounts);
        return snapshot;
    }

    private static HashMap<Integer, List<ItemStack>> copyBankPages(HashMap<Integer, List<ItemStack>> pages) {
        HashMap<Integer, List<ItemStack>> copy = new HashMap<>();
        if (pages == null) return copy;
        pages.forEach((page, items) -> copy.put(page, copyItemList(items)));
        return copy;
    }

    private static HashMap<Integer, HashMap<String, Integer>> copyBagCounts(HashMap<Integer, HashMap<String, Integer>> counts) {
        HashMap<Integer, HashMap<String, Integer>> copy = new HashMap<>();
        if (counts == null) return copy;
        counts.forEach((page, pageCounts) -> copy.put(page, pageCounts == null ? new HashMap<>() : new HashMap<>(pageCounts)));
        return copy;
    }

    private static class BankDataSaveSnapshot {
        private int lastPage = 1;
        @SerializedName("BankPages")
        private HashMap<Integer, List<ItemStack>> bankPages = new HashMap<>();
        @SerializedName("BankPageNames")
        private HashMap<Integer, String> bankPageNames = new HashMap<>();
        private String characterNickname = null;
        private int characterLevel = 0;
        private List<ItemStack> playerInventory = List.of();
        private List<ItemStack> playerArmor = List.of();
        private HashMap<Integer, HashMap<String, Integer>> bagCounts = new HashMap<>();
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
