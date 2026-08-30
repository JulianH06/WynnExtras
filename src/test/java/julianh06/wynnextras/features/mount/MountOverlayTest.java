package julianh06.wynnextras.features.mount;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import oshi.driver.unix.freebsd.disk.Mount;

public class MountOverlayTest {

    static int statCount = MountStat.values().length;
    static int materialCount = MaterialType.values().length;
    
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

        Map<MaterialType, Integer> result = MountOverlay.optimizeNeeded(47, needed);

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

        Map<MaterialType, Integer> result = MountOverlay.optimizeNeededv2(47, needed);

        int num = 0;
        for (Map.Entry<MaterialType, Integer> item : result.entrySet()) {
            System.out.println(item.getKey());
            System.out.println(item.getValue());
            num += item.getValue();

            assert item.getValue() != 0;
        }

        assert num == 3;
        
    }

    @Test
    void testMixLevel() {
        Map<MountStat, Integer> needed = new HashMap<>();
        int[][] materialStatsTable = MountOverlay.makeMaterialStatsTable(10);

        int[] materials = new int[materialCount];
        int[] stats = new int[statCount];

        for (MountStat s : MountStat.values())
            needed.put(s, 20 - );
        

         new HashMap<>();
        Map<MaterialType, Integer> result = MountOverlay.optimizeNeededv2(10, needed);

    }
}
