package julianh06.wynnextras.features.wci.service;

public record WciHaveCount(
        int inventory,
        int accountBank,
        int characterBank,
        int miscBucket,
        boolean bankCacheAvailable,
        boolean bankCachePossiblyIncomplete) {
    public WciHaveCount {
        inventory = Math.max(0, inventory);
        accountBank = Math.max(0, accountBank);
        characterBank = Math.max(0, characterBank);
        miscBucket = Math.max(0, miscBucket);
    }

    public WciHaveCount(int inventory,
                        int accountBank,
                        int characterBank,
                        boolean bankCacheAvailable,
                        boolean bankCachePossiblyIncomplete) {
        this(inventory, accountBank, characterBank, 0, bankCacheAvailable, bankCachePossiblyIncomplete);
    }

    public int total() {
        return inventory + accountBank + characterBank + miscBucket;
    }
}
