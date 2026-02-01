package julianh06.wynnextras.features.showcase;

import java.util.function.Supplier;

public class FeatureInfo {
    private final String name;
    private final String description;
    private final String category;
    private final String keybind;
    private final Supplier<Boolean> enabledSupplier;

    public FeatureInfo(String name, String description, String category, String keybind, Supplier<Boolean> enabledSupplier) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.keybind = keybind;
        this.enabledSupplier = enabledSupplier;
    }

    public FeatureInfo(String name, String description, String category, String keybind) {
        this(name, description, category, keybind, () -> true);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getKeybind() {
        return keybind;
    }

    public boolean isEnabled() {
        return enabledSupplier.get();
    }
}
