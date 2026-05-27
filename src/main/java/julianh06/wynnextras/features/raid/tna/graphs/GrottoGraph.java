package julianh06.wynnextras.features.raid.tna.graphs;

import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.raid.tna.Door;
import julianh06.wynnextras.features.raid.tna.Grotto;
import julianh06.wynnextras.utils.path.Graph;
import julianh06.wynnextras.utils.path.Node;
import julianh06.wynnextras.utils.path.Path;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.Arrays;
import java.util.List;

public class GrottoGraph extends Graph {
    private final Door[] doors;
    private final Grotto whoAmI;

    public GrottoGraph(Grotto self, List<Node> nodes, Door... doors) {
        super(nodes);
        this.doors = doors;
        this.whoAmI = self;
    }

    private Door getDoorTo(Grotto target) {
        if (target == Grotto.Entrance && whoAmI == Grotto.Entrance) return Arrays.stream(doors).filter(d -> d.to() == Grotto.Outside).findFirst().orElse(null);
        Grotto fakeTarget = getFakeTarget(target);
        for (Door door : doors) {
            if (door.to() == fakeTarget) return door;
        }
        WynnExtras.LOGGER.error("Failed to get door from {} to {}", whoAmI, target);
        return null;
    }

    public Path pathTo(Grotto targetGrotto) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return null;
        Door door = getDoorTo(targetGrotto);
        if (door != null) {
            return findPath(player.getEntityPos(), door.pos());
        }
        return null;
    }

    private Grotto getFakeTarget(Grotto target) {
        return switch (whoAmI) {
            case Entrance -> switch (target) {
                case Orange, Black, White -> Grotto.Gray;
                default -> target;
            };
            case Gray -> switch (target) {
                case Orange -> Grotto.Blue;
                case White -> Grotto.Black;
                default -> target;
            };
            case Blue -> switch (target) {
                case White -> Grotto.Orange;
                default -> target;
            };
            case Black -> switch (target) {
                case Orange -> Grotto.White;
                case Entrance -> Grotto.Gray;
                default -> target;
            };
            case Orange -> switch (target) {
                case Black -> Grotto.White;
                case Gray, Entrance -> Grotto.Blue;
                default -> target;
            };
            case White -> switch (target) {
                case Blue -> Grotto.Orange;
                case Gray -> Grotto.Black;
                case Entrance -> Grotto.Black;
                default -> target;
            };
            default -> target;
        };
    }

    private static final Node entToOutside = new Node(24182, 188, -22263);
    private static final Node entToBlue = new Node(24230, 201, -22233);
    private static final Node entToGray = new Node(24198, 187, -22280);
    public static final GrottoGraph EntranceGraph;
    static {
        // Force enum loading
        Grotto[] values = Grotto.values();

        EntranceGraph = new GrottoGraph(
                Grotto.Entrance,
                new Graph.GraphBuilder(entToOutside)
                        .next(new Node(24186, 188, -22263))
                        .next(new Node(24198, 187, -22276))
                        .next(entToGray)
                        .split(new Node(24193, 186, -22262), 1)
                        .next(new Node(24199, 189, -22256))
                        .next(new Node(24208, 195, -22246))
                        .next(new Node(24214, 199, -22240))
                        .next(new Node(24222, 201, -22234))
                        .next(entToBlue)
                        .connect(2, 5)
                        .nodes(),
                new Door(entToOutside.getPos().toCenterPos(), Grotto.Outside),
                new Door(entToBlue.getPos().toCenterPos(), Grotto.Blue),
                new Door(entToGray.getPos().toCenterPos(), Grotto.Gray)
        );
    }

    private static final Node blackToGray = new Node(24195, 157, -22262);
    private static final Node blackToWhite = new Node(24183, 160, -22249);
    private static final Node blackToBlue = new Node(24206, 163, -22242);
    public static final GrottoGraph BlackGraph = new GrottoGraph(
            Grotto.Black,
            new Graph.GraphBuilder(blackToWhite)
                    .next(new Node(24188, 160, -22250)) // index 1
                    .next(new Node(24190, 160, -22253))
                    .next(blackToGray)
                    .next(new Node(24198, 156, -22255))
                    .next(new Node(24205, 158, -22250))
                    .next(blackToBlue)
                    .next(new Node(24204, 162, -22247))
                    .next(new Node(24192, 160, -22246)) // index 8
                    .connect(1, 8)
                    .connect(2, 4)
                    .connect(5, 7)
                    .nodes(),
            new Door(blackToGray.getPos().toCenterPos(), Grotto.Gray),
            new Door(blackToWhite.getPos().toCenterPos(), Grotto.White),
            new Door(blackToBlue.getPos().toCenterPos(), Grotto.Blue)
    );

    private static final Node grayToEnt = new Node(24186, 123, -22242);
    private static final Node grayToBlack = new Node(24183, 125, -22269);
    private static final Node grayToBlue = new Node(24168, 125, -22257);
    public static final GrottoGraph GrayGraph = new GrottoGraph(
            Grotto.Gray,
            new Graph.GraphBuilder(grayToEnt)
                    .next(new Node(24189, 122, -22250))
                    .next(new Node(24183, 121, -22247))
                    .next(new Node(24178, 122, -22248))
                    .next(new Node(24175, 122, -22252)) // index 4
                    .next(grayToBlue)
                    .next(new Node(24177, 123, -22261)) // index 6
                    .next(grayToBlack)
                    .next(new Node(24187, 123, -22261))
                    .next(new Node(24190, 123, -22258)) // index 9
                    .connect(0, 2)
                    .connect(4, 6)
                    .connect(6, 8)
                    .connect(1, 9)
                    .nodes(),
            new Door(grayToEnt.getPos().toCenterPos(), Grotto.Entrance),
            new Door(grayToBlack.getPos().toCenterPos(), Grotto.Black),
            new Door(grayToBlue.getPos().toCenterPos(), Grotto.Blue)
    );

    private static final Node whiteToBlack = new Node(24205, 101, -22242);
    private static final Node whiteToOrange = new Node(24177, 97, -22231);
    public static final GrottoGraph WhiteGraph = new GrottoGraph(
            Grotto.White,
            new Graph.GraphBuilder(whiteToBlack)
                    .next(new Node(24192, 98, -22241))
                    .next(new Node(24182, 97, -22238))
                    .next(whiteToOrange)
                    .nodes(),
            new Door(whiteToBlack.getPos().toCenterPos(), Grotto.Black),
            new Door(whiteToOrange.getPos().toCenterPos(), Grotto.Orange)
    );

    // not 100% sure this is the right white door
    private static final Node orangeToWhite = new Node(24196, 62, -22276);
    private static final Node orangeToBlue = new Node(24171, 63, -22264);
    public static final GrottoGraph OrangeGraph = new GrottoGraph(
            Grotto.Orange,
            new Graph.GraphBuilder(orangeToBlue)
                    .next(new Node(24179, 61, -22266))
                    .next(new Node(24193, 61, -22271))
                    .next(orangeToWhite)
                    .nodes(),
            new Door(orangeToWhite.getPos().toCenterPos(), Grotto.White),
            new Door(orangeToBlue.getPos().toCenterPos(), Grotto.Blue)
    );

    private static final Node blueToEnt = new Node(24198, 33, -22275);
    private static final Node blueToOrange = new Node(24193, 29, -22243);
    private static final Node blueToBlack = new Node(24176, 35, -22248);
    private static final Node blueToGray = new Node(24199, 40, -22255);
    public static final GrottoGraph BlueGraph = new GrottoGraph(
            Grotto.Blue,
            new Graph.GraphBuilder(blueToOrange)
                    .next(new Node(24184, 31, -22245))
                    .next(blueToBlack)
                    .next(new Node(24184, 32, -22251))
                    .next(new Node(24186, 34, -22256)) // index 4
                    .next(new Node(24193, 36, -22255))
                    .next(blueToGray)
                    .split(new Node(24191, 30, -22262), 4)
                    .next(new Node(24196, 30, -22268))
                    .next(blueToEnt)
                    .connect(1, 3)
                    .connect(5, 7)
                    .nodes(),
            new Door(blueToEnt.getPos().toCenterPos(), Grotto.Entrance),
            new Door(blueToOrange.getPos().toCenterPos(), Grotto.Orange),
            new Door(blueToBlack.getPos().toCenterPos(), Grotto.Black),
            new Door(blueToGray.getPos().toCenterPos(), Grotto.Gray)
    );
}
