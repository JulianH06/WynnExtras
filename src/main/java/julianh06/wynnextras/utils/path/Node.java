package julianh06.wynnextras.utils.path;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

public class Node {
    private final BlockPos pos;
    private final Map<Node, Double> neighbors = new HashMap<>();
    private final Group group;

    public Node(int x, int y, int z, Group group) {
        this.pos = new BlockPos(x, y, z);
        this.group = group;
    }

    public Group getGroup() {
        return group;
    }

    public BlockPos getPos() {
        return pos;
    }

    public Vec3d getCenterPos() {
        return pos.toCenterPos();
    }

    public Map<Node, Double> getNeighbors() {
        return neighbors;
    }

    public Double getDistance(Node node) {
        return neighbors.getOrDefault(node, -1.);
    }

    public void addNeighbor(Node neighbor) {
        double dist = getCenterPos().distanceTo(neighbor.getCenterPos());
        this.neighbors.put(neighbor, dist);
    }

    public void addTpPoint(Node neighbor) {
        neighbors.put(neighbor, 0.);
    }

    public interface Group {
        String name();
    }

    @Override
    public String toString() {
        return "Node{" +
                "pos=" + pos +
                ", conns=" + neighbors.size() +
                ", group=" + group +
                '}';
    }
}
