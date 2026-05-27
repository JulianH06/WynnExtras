package julianh06.wynnextras.utils.path;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class Node {
    private final BlockPos pos;
    private final List<Node> neighbors = new ArrayList<>();

    public Node(int x, int y, int z) {
        this.pos = new BlockPos(x, y, z);
    }

    public BlockPos getPos() {
        return pos;
    }

    public List<Node> getNeighbors() {
        return neighbors;
    }

    public void addNeighbor(Node neighbor) {
        this.neighbors.add(neighbor);
    }
}
