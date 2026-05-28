package julianh06.wynnextras.utils.path;

import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.utils.WEVec;
import julianh06.wynnextras.utils.render.WorldRenderUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;

public class Graph {
    private final List<Node> nodes;

    public Graph(List<Node> nodes) {
        this.nodes = nodes;
    }

    public Path findPath(Vec3d start, Vec3d target) {
        Node startNode = findClosestNode(start);
        Node targetNode = findClosestNode(target);

        if (startNode == null || targetNode == null) {
            return null;
        }

        return findPath(startNode, targetNode);
    }

    public Path findPath(Node startNode, Node targetNode) {
        if (startNode == null || targetNode == null) {
            return null;
        }
        return findPath(startNode, node -> node.equals(targetNode));
    }

    public Path findPath(Vec3d startPos, Node.Group targetGroup) {
        if (startPos == null || targetGroup == null) {
            return null;
        }
        return findPath(findClosestNode(startPos), node -> node.getGroup().equals(targetGroup));
    }

    private Path findPath(Node startNode, Function<Node, Boolean> arrived) {
        if (startNode == null || arrived == null) {
            return null;
        }

        // Dijkstra's algorithm for weighted shortest path
        Map<Node, Double> distances = new HashMap<>();
        Map<Node, Node> parentMap = new HashMap<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingDouble(distances::get));
        Set<Node> visited = new HashSet<>();

        // Initialize
        distances.put(startNode, 0.0);
        queue.add(startNode);
        parentMap.put(startNode, null);

        while (!queue.isEmpty()) {
            Node current = queue.poll();

            if (visited.contains(current)) {
                continue;
            }

            if (arrived.apply(current)) {
                // Reconstruct path
                List<Node> pathPositions = new ArrayList<>();
                Node node = current;
                while (node != null) {
                    pathPositions.addFirst(node);
                    node = parentMap.get(node);
                }
                return new Path(pathPositions);
            }

            visited.add(current);

            // Explore neighbors using the precalculated distances
            for (Map.Entry<Node, Double> entry : current.getNeighbors().entrySet()) {
                Node neighbor = entry.getKey();
                double edgeWeight = entry.getValue();

                if (visited.contains(neighbor)) {
                    continue;
                }

                double newDistance = distances.get(current) + edgeWeight;

                if (!distances.containsKey(neighbor) || newDistance < distances.get(neighbor)) {
                    distances.put(neighbor, newDistance);
                    parentMap.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        return null; // No node with target group found
    }

    private Node findClosestNode(Vec3d pos) {
        Node closestNode = null;
        double closestDist = Double.MAX_VALUE;
        for (Node node : nodes) {
            double dist = node.getPos().toCenterPos().squaredDistanceTo(pos);
            if (dist < closestDist) {
                closestNode = node;
                closestDist = dist;
            }
        }
        return closestNode;
    }

    public void drawFullGraph(RenderWorldEvent event) {
        if (nodes == null || nodes.isEmpty()) return;
        Set<BlockPos> seen = new HashSet<>();

        for (Node node : nodes) {
            BlockPos from = node.getPos();
            seen.add(from);

            for (Map.Entry<Node, Double> entry : node.getNeighbors().entrySet()) {
                BlockPos to = entry.getKey().getPos();
                if (seen.contains(to)) continue;

                WorldRenderUtils.draw3DLine(
                        event,
                        new WEVec(from.toCenterPos()),
                        new WEVec(to.toCenterPos()),
                        entry.getValue() == 0 ? Color.RED : Color.GREEN,
                        2,
                        false
                );
            }
        }
    }

    public static class GraphBuilder {
        private final List<Node> nodes = new ArrayList<>();

        public GraphBuilder(Node start) {
            nodes.add(start);
        }

        public GraphBuilder next(Node node) {
            Node prev = nodes.getLast();
            prev.addNeighbor(node);
            node.addNeighbor(prev);
            nodes.add(node);
            return this;
        }

        public GraphBuilder nextTp(Node node) {
            Node prev = nodes.getLast();
            prev.addTpPoint(node);
            node.addTpPoint(prev);
            nodes.add(node);
            return this;
        }

        public GraphBuilder split(Node node, int index) {
            Node prev = nodes.get(nodes.size() - (index + 1));
            prev.addNeighbor(node);
            node.addNeighbor(prev);
            nodes.add(node);
            return this;
        }

        public GraphBuilder splitTp(Node node, int index) {
            Node prev = nodes.get(nodes.size() - (index + 1));
            prev.addTpPoint(node);
            node.addTpPoint(prev);
            nodes.add(node);
            return this;
        }

        public GraphBuilder connect(int index1, int index2) {
            Node a = nodes.get(nodes.size() - (index1 + 1));
            Node b = nodes.get(nodes.size() - (index2 + 1));
            a.addNeighbor(b);
            b.addNeighbor(a);
            return this;
        }

        public GraphBuilder connect(int index) {
            return connect(0, index);
        }

        public List<Node> nodes() {
            return nodes;
        }

        public Graph build() {
            return new Graph(nodes);
        }
    }
}
