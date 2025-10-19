package julianh06.wynnextras.features.abilitytree;

import java.util.ArrayList;
import java.util.List;

public class Tree {
    String name;
    String className;
    List<String> input = new ArrayList<>();

    public Tree(String name, List<String> input, String className) {
        this.name = name;
        this.input = input;
        this.className = className;
    }
}
