package julianh06.wynnextras.features.mount;

import java.util.*;

public class Solver {
    public static Map<MaterialType, Integer> solve(Map<MountStat, StatEntry> mountStats, Map<MaterialType, MaterialStats> materialData) {
        ArrayDeque<Attempt> queue = new ArrayDeque<>();
        queue.add(new Attempt());
        Set<Attempt> visited = new HashSet<>();
        int counter = 0;
        while (!queue.isEmpty() && counter < 50000) {
            counter++;
            Attempt p = queue.pop();
            if (p.isSolved(mountStats, materialData)) {
                System.out.println("found solution: " + p);
                return p.result;
            } else {
                if (counter % 1000 == 0) {
                    System.out.println("non correct: " + p);
                }
                p.progress(queue, visited);
            }
        }
        System.out.println("exiting mount solve, failure after " + counter + " attempts");
        return null;
    }

    public static class Attempt {
        private Map<MaterialType, Integer> result = new HashMap<>();

        public Attempt(Attempt other) {
            this.result = new HashMap<>(other.result);
        }

        public Attempt() {
            Arrays.stream(MaterialType.values()).forEach(v -> result.put(v, 0));
        }

        public Attempt iterate(MaterialType type) {
            result.compute(type, (k, data) -> data + 1);
            return this;
        }

        public boolean isSolved(Map<MountStat, StatEntry> goal, Map<MaterialType, MaterialStats> materialData) {
            Map<MountStat, Integer> cumulative = new HashMap<>();
            for (Map.Entry<MaterialType, Integer> entry : result.entrySet()) {
                materialData.get(entry.getKey()).getStats().forEach((k, v) ->
                        cumulative.merge(k, v * entry.getValue(), Integer::sum));
            }
            return cumulative.entrySet().stream().allMatch((e) -> {
                StatEntry statEntry = goal.get(e.getKey());
                return statEntry.limit() + e.getValue() >= statEntry.max();
            });
        }

        public void progress(ArrayDeque<Attempt> queue, Set<Attempt> visited) {
            for (int i = 0; i < 8; i++) {
                Attempt p = new Attempt(this).iterate(MaterialType.values()[i]);
                if (!visited.add(p)) continue;
                queue.add(p);
            }
        }

        @Override
        public String toString() {
            return result.toString();
        }

        @Override
        public int hashCode() {
            return result.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Attempt other) {
                return this.result.equals(other.result);
            }
            return false;
        }
    }
}
