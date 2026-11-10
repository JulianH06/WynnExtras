package julianh06.wynnextras.features.buildplanner.config;

public record SavedCraft(String name, String code, long savedAt) {
    public SavedCraft {
        name = name == null ? "" : name.trim();
        if (name.isEmpty() || name.length() > 64) throw new IllegalArgumentException("Invalid craft name");
        code = code == null ? "" : code.trim();
        if (!code.startsWith("CR-") || code.length() > 2048) throw new IllegalArgumentException("Invalid craft code");
    }
}
