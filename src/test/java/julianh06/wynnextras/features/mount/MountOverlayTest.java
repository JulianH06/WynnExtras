package julianh06.wynnextras.features.mount;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class MountOverlayTest {
    
    @Disabled
    @Test
    void testHello() {
        System.out.println("hello world");
    }

    @Test
    @Disabled
    void testOptomizev1() {
        Map<MountStat, Integer> needed = new HashMap<>();

        needed.put(MountStat.SPEED, 5);
        needed.put(MountStat.ALTITUDE, 7);
        needed.put(MountStat.HANDLING, 7);
        needed.put(MountStat.TOUGHNESS, 3);

        Map<MaterialType, Integer> result = new HashMap<>();
        MountOverlay.optimizeNeeded(result, 47, needed);

        int num = 0;
        for (Map.Entry<MaterialType, Integer> item : result.entrySet()) {
            System.out.println(item.getKey());
            System.out.println(item.getValue());
            num += item.getValue();
        }

        assert num == 3;
        
    }

    @Test
    void testOptomizev2() {
        Map<MountStat, Integer> needed = new HashMap<>();

        needed.put(MountStat.SPEED, 5);
        needed.put(MountStat.ALTITUDE, 7);
        needed.put(MountStat.HANDLING, 7);
        needed.put(MountStat.TOUGHNESS, 3);

        Map<MaterialType, Integer> result = new HashMap<>();
        MountOverlay.optimizeNeededv2(result, 47, needed);

        int num = 0;
        for (Map.Entry<MaterialType, Integer> item : result.entrySet()) {
            System.out.println(item.getKey());
            System.out.println(item.getValue());
            num += item.getValue();
        }

        assert num == 3;
        
    }
}
