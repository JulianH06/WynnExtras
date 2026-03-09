package julianh06.wynnextras.features.spellhider;

public class SpellData {
    private final String filePath;
    private final int hash;
    private String FQName = null;
    private Integer customModelData = null;

    public SpellData(String filePath, int hash) {
        this.filePath = filePath;
        this.hash = hash;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getHash() {
        return hash;
    }

    public String getFQName() {
        return FQName;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public SpellNamespace getNamespace() {
        if (FQName == null || FQName.isEmpty()) return null;
        return SpellNamespace.from(FQName);
    }

    public void setFQName(String FQName) {
        this.FQName = FQName;
    }

    public void setCustomModelData(Integer customModelData) {
        this.customModelData = customModelData;
    }
}
