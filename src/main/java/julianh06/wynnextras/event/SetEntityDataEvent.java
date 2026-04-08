package julianh06.wynnextras.event;

import julianh06.wynnextras.event.api.WEEvent;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SetEntityDataEvent extends WEEvent {
    private final int id;
    private final List<DataTracker.SerializedEntry<?>> packedItems;

    public SetEntityDataEvent(EntityTrackerUpdateS2CPacket packet) {
        this.id = packet.id();
        this.packedItems = new ArrayList<>(packet.trackedValues());
    }

    public int getId() {
        return id;
    }

    public List<DataTracker.SerializedEntry<?>> getPackedItems() {
        return Collections.unmodifiableList(packedItems);
    }

    public void addPackedItem(DataTracker.SerializedEntry<?> packedItem) {
        packedItems.add(packedItem);
    }

    public void removePackedItem(DataTracker.SerializedEntry<?> packedItem) {
        packedItems.remove(packedItem);
    }
}
