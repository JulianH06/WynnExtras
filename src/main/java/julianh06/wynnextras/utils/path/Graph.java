package julianh06.wynnextras.utils.path;

import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.utils.WEVec;
import julianh06.wynnextras.utils.render.WorldRenderUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Graph {
    private final List<Node> nodes;

    public Graph(List<Node> nodes) {
        this.nodes = nodes;
    }

    // finds the closest nodes nodes to start and finosh and returns the path from node to node (not ideal)
    public Path findPath(Vec3d start, Vec3d target) {
        // Find the starting node that contains the start position
        Node startNode = findClosestNode(start);
        Node targetNode = findClosestNode(target);

        if (startNode == null || targetNode == null) {
            return null;
        }

        // BFS to find path
        Queue<Node> queue = new LinkedList<>();
        Map<Node, Node> parentMap = new HashMap<>();
        Set<Node> visited = new HashSet<>();

        queue.add(startNode);
        visited.add(startNode);
        parentMap.put(startNode, null);

        while (!queue.isEmpty()) {
            Node current = queue.poll();

            if (current.equals(targetNode)) {
                // Reconstruct path
                List<BlockPos> pathPositions = new ArrayList<>();
                Node node = current;
                while (node != null) {
                    pathPositions.addFirst(node.getPos()); // Add to beginning to maintain order
                    node = parentMap.get(node);
                }
                return new Path(pathPositions);
            }

            // Explore neighbors
            for (Node neighbor : current.getNeighbors()) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parentMap.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        return null;
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

    public void drawFullGraph(RenderWorldEvent event, Color color) {
        if (nodes == null || nodes.isEmpty()) return;
        Set<BlockPos> seen = new HashSet<>();

        for (Node node : nodes) {
            BlockPos from = node.getPos();
            seen.add(from);

            for (Node neighbor : node.getNeighbors()) {
                BlockPos to = neighbor.getPos();
                if (seen.contains(to)) continue;

                WorldRenderUtils.draw3DLine(
                        event,
                        new WEVec(from.toCenterPos()),
                        new WEVec(to.toCenterPos()),
                        color,
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

        public GraphBuilder split(Node node, int index) {
            Node prev = nodes.get(index);
            prev.addNeighbor(node);
            node.addNeighbor(prev);
            nodes.add(node);
            return this;
        }

        public GraphBuilder connect(int index1, int index2) {
            Node a = nodes.get(index1);
            Node b = nodes.get(index2);
            a.addNeighbor(b);
            b.addNeighbor(a);
            return this;
        }

        public List<Node> nodes() {
            return nodes;
        }

        public Graph build() {
            return new Graph(nodes);
        }
    }
}
