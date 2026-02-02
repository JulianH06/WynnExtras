package julianh06.wynnextras.features.crafting.data;

import julianh06.wynnextras.utils.Pair;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Constants {
    public record RecipeRange(Vector2i level,
                              Vector2i durability,
                              Vector2i consuDuration,
                              Vector2i cookingDuration,
                              Vector2i health) {

        public boolean containsLevel(int lvl) {
            return lvl >= level.x && lvl < level.y;
        }
    }

    private static final RecipeRange[] RANGES = {
            new RecipeRange(new Vector2i(1, 3), new Vector2i(175, 182), new Vector2i(360, 372), new Vector2i(1080, 1116), new Vector2i(11, 14)),
            new RecipeRange(new Vector2i(3, 5), new Vector2i(182, 189), new Vector2i(372, 384), new Vector2i(1116, 1152), new Vector2i(14, 21)),
            new RecipeRange(new Vector2i(5, 7), new Vector2i(189, 196), new Vector2i(384, 396), new Vector2i(1152, 1188), new Vector2i(21, 27)),
            new RecipeRange(new Vector2i(7, 9), new Vector2i(196, 203), new Vector2i(396, 408), new Vector2i(1188, 1224), new Vector2i(26, 38)),

            new RecipeRange(new Vector2i(10, 13), new Vector2i(207, 217), new Vector2i(414, 432), new Vector2i(1242, 1296), new Vector2i(35, 47)),
            new RecipeRange(new Vector2i(13, 15), new Vector2i(217, 224), new Vector2i(432, 444), new Vector2i(1296, 1332), new Vector2i(47, 67)),
            new RecipeRange(new Vector2i(15, 17), new Vector2i(224, 231), new Vector2i(444, 456), new Vector2i(1332, 1368), new Vector2i(67, 79)),
            new RecipeRange(new Vector2i(17, 19), new Vector2i(231, 238), new Vector2i(456, 468), new Vector2i(1368, 1404), new Vector2i(79, 92)),

            new RecipeRange(new Vector2i(20, 23), new Vector2i(242, 252), new Vector2i(474, 492), new Vector2i(1422, 1476), new Vector2i(87, 104)),
            new RecipeRange(new Vector2i(23, 25), new Vector2i(252, 259), new Vector2i(492, 504), new Vector2i(1476, 1512), new Vector2i(99, 121)),
            new RecipeRange(new Vector2i(25, 27), new Vector2i(259, 266), new Vector2i(504, 516), new Vector2i(1512, 1548), new Vector2i(121, 148)),
            new RecipeRange(new Vector2i(27, 29), new Vector2i(266, 273), new Vector2i(516, 528), new Vector2i(1548, 1584), new Vector2i(148, 176)),

            new RecipeRange(new Vector2i(30, 33), new Vector2i(277, 287), new Vector2i(534, 552), new Vector2i(1602, 1656), new Vector2i(160, 190)),
            new RecipeRange(new Vector2i(33, 35), new Vector2i(287, 294), new Vector2i(552, 564), new Vector2i(1656, 1692), new Vector2i(190, 225)),
            new RecipeRange(new Vector2i(35, 37), new Vector2i(294, 301), new Vector2i(564, 576), new Vector2i(1692, 1728), new Vector2i(225, 260)),
            new RecipeRange(new Vector2i(37, 39), new Vector2i(301, 308), new Vector2i(576, 588), new Vector2i(1728, 1764), new Vector2i(260, 300)),

            new RecipeRange(new Vector2i(40, 43), new Vector2i(312, 322), new Vector2i(594, 612), new Vector2i(1782, 1836), new Vector2i(310, 365)),
            new RecipeRange(new Vector2i(43, 45), new Vector2i(322, 329), new Vector2i(612, 624), new Vector2i(1836, 1872), new Vector2i(365, 420)),
            new RecipeRange(new Vector2i(45, 47), new Vector2i(329, 336), new Vector2i(624, 636), new Vector2i(1872, 1908), new Vector2i(420, 480)),
            new RecipeRange(new Vector2i(47, 49), new Vector2i(336, 343), new Vector2i(636, 648), new Vector2i(1908, 1944), new Vector2i(480, 540)),

            new RecipeRange(new Vector2i(50, 53), new Vector2i(347, 357), new Vector2i(654, 672), new Vector2i(1962, 2016), new Vector2i(550, 630)),
            new RecipeRange(new Vector2i(53, 55), new Vector2i(357, 364), new Vector2i(672, 684), new Vector2i(2022, 2052), new Vector2i(630, 740)),
            new RecipeRange(new Vector2i(55, 57), new Vector2i(364, 371), new Vector2i(684, 696), new Vector2i(2052, 2088), new Vector2i(740, 850)),
            new RecipeRange(new Vector2i(57, 59), new Vector2i(371, 378), new Vector2i(696, 708), new Vector2i(2088, 2124), new Vector2i(850, 960)),

            new RecipeRange(new Vector2i(60, 63), new Vector2i(382, 392), new Vector2i(714, 732), new Vector2i(2142, 2196), new Vector2i(970, 1070)),
            new RecipeRange(new Vector2i(63, 65), new Vector2i(392, 399), new Vector2i(732, 744), new Vector2i(2196, 2232), new Vector2i(1070, 1160)),
            new RecipeRange(new Vector2i(65, 67), new Vector2i(399, 406), new Vector2i(744, 756), new Vector2i(2232, 2268), new Vector2i(1160, 1260)),
            new RecipeRange(new Vector2i(67, 69), new Vector2i(406, 413), new Vector2i(756, 768), new Vector2i(2268, 2304), new Vector2i(1260, 1350)),

            new RecipeRange(new Vector2i(70, 73), new Vector2i(417, 427), new Vector2i(774, 792), new Vector2i(2322, 2376), new Vector2i(1360, 1480)),
            new RecipeRange(new Vector2i(73, 75), new Vector2i(427, 434), new Vector2i(792, 804), new Vector2i(2376, 2412), new Vector2i(1380, 1600)),
            new RecipeRange(new Vector2i(75, 77), new Vector2i(434, 441), new Vector2i(804, 816), new Vector2i(2412, 2448), new Vector2i(1600, 1730)),
            new RecipeRange(new Vector2i(77, 79), new Vector2i(441, 448), new Vector2i(816, 828), new Vector2i(2448, 2484), new Vector2i(1730, 1850)),

            new RecipeRange(new Vector2i(80, 83), new Vector2i(452, 462), new Vector2i(834, 852), new Vector2i(2502, 2556), new Vector2i(1870, 2010)),
            new RecipeRange(new Vector2i(83, 85), new Vector2i(462, 469), new Vector2i(852, 864), new Vector2i(2556, 2592), new Vector2i(2001, 2160)),
            new RecipeRange(new Vector2i(85, 87), new Vector2i(469, 476), new Vector2i(864, 876), new Vector2i(2593, 2628), new Vector2i(2160, 2320)),
            new RecipeRange(new Vector2i(87, 89), new Vector2i(476, 483), new Vector2i(876, 888), new Vector2i(2628, 2664), new Vector2i(2320, 2460)),

            new RecipeRange(new Vector2i(90, 93), new Vector2i(487, 497), new Vector2i(894, 912), new Vector2i(2682, 2736), new Vector2i(2480, 2640)),
            new RecipeRange(new Vector2i(93, 95), new Vector2i(497, 504), new Vector2i(912, 924), new Vector2i(2736, 2772), new Vector2i(2640, 2840)),
            new RecipeRange(new Vector2i(95, 97), new Vector2i(504, 511), new Vector2i(924, 936), new Vector2i(2772, 2808), new Vector2i(2840, 3020)),
            new RecipeRange(new Vector2i(97, 99), new Vector2i(511, 518), new Vector2i(936, 948), new Vector2i(2808, 2844), new Vector2i(3020, 3200)),

            new RecipeRange(new Vector2i(100, 103), new Vector2i(522, 525), new Vector2i(954, 960), new Vector2i(2862, 2880), new Vector2i(3250, 3300)),
            new RecipeRange(new Vector2i(103, 105), new Vector2i(525, 527), new Vector2i(960, 964), new Vector2i(2880, 2900), new Vector2i(3300, 3350)),
    };

    private static final Map<Pair<CraftableType, Vector2i>, Pair<Vector2i, Vector2i>> DAMAGES = new HashMap<>();

    static {
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(1, 3)), new Pair<>(new Vector2i(3, 4), new Vector2i(5, 6)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(3, 5)), new Pair<>(new Vector2i(6, 7), new Vector2i(8, 9)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(5, 7)), new Pair<>(new Vector2i(9, 11), new Vector2i(11, 14)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(7, 9)), new Pair<>(new Vector2i(11, 14), new Vector2i(13, 16)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(10, 13)), new Pair<>(new Vector2i(12, 15), new Vector2i(15, 18)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(13, 15)), new Pair<>(new Vector2i(15, 18), new Vector2i(18, 22)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(15, 17)), new Pair<>(new Vector2i(18, 22), new Vector2i(21, 26)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(17, 19)), new Pair<>(new Vector2i(21, 26), new Vector2i(23, 28)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(20, 23)), new Pair<>(new Vector2i(23, 28), new Vector2i(25, 30)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(23, 25)), new Pair<>(new Vector2i(24, 29), new Vector2i(26, 31)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(25, 27)), new Pair<>(new Vector2i(25, 30), new Vector2i(27, 33)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(27, 29)), new Pair<>(new Vector2i(26, 31), new Vector2i(28, 35)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(30, 33)), new Pair<>(new Vector2i(28, 35), new Vector2i(33, 40)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(33, 35)), new Pair<>(new Vector2i(33, 40), new Vector2i(36, 45)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(35, 37)), new Pair<>(new Vector2i(36, 45), new Vector2i(39, 48)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(37, 39)), new Pair<>(new Vector2i(39, 48), new Vector2i(43, 52)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(40, 43)), new Pair<>(new Vector2i(44, 53), new Vector2i(48, 59)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(43, 45)), new Pair<>(new Vector2i(48, 59), new Vector2i(53, 64)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(45, 47)), new Pair<>(new Vector2i(53, 64), new Vector2i(58, 71)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(47, 49)), new Pair<>(new Vector2i(58, 71), new Vector2i(63, 77)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(50, 53)), new Pair<>(new Vector2i(63, 78), new Vector2i(70, 85)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(53, 55)), new Pair<>(new Vector2i(70, 85), new Vector2i(76, 93)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(55, 57)), new Pair<>(new Vector2i(76, 93), new Vector2i(82, 101)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(57, 59)), new Pair<>(new Vector2i(82, 101), new Vector2i(89, 108)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(60, 63)), new Pair<>(new Vector2i(90, 110), new Vector2i(97, 118)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(63, 65)), new Pair<>(new Vector2i(97, 118), new Vector2i(104, 127)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(65, 67)), new Pair<>(new Vector2i(104, 127), new Vector2i(111, 136)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(67, 69)), new Pair<>(new Vector2i(111, 136), new Vector2i(118, 145)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(70, 73)), new Pair<>(new Vector2i(124, 151), new Vector2i(129, 158)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(73, 75)), new Pair<>(new Vector2i(129, 158), new Vector2i(132, 161)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(75, 77)), new Pair<>(new Vector2i(132, 161), new Vector2i(135, 166)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(77, 79)), new Pair<>(new Vector2i(135, 166), new Vector2i(136, 167)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(80, 83)), new Pair<>(new Vector2i(138, 169), new Vector2i(143, 174)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(83, 85)), new Pair<>(new Vector2i(143, 174), new Vector2i(145, 178)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(85, 87)), new Pair<>(new Vector2i(145, 178), new Vector2i(148, 181)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(87, 89)), new Pair<>(new Vector2i(148, 181), new Vector2i(149, 182)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(90, 93)), new Pair<>(new Vector2i(150, 183), new Vector2i(153, 187)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(93, 95)), new Pair<>(new Vector2i(153, 187), new Vector2i(154, 189)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(95, 97)), new Pair<>(new Vector2i(154, 189), new Vector2i(155, 190)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(97, 99)), new Pair<>(new Vector2i(155, 190), new Vector2i(157, 192)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(100, 103)), new Pair<>(new Vector2i(157, 192), new Vector2i(158, 193)));
        DAMAGES.put(new Pair<>(CraftableType.SPEAR, new Vector2i(103, 105)), new Pair<>(new Vector2i(158, 193), new Vector2i(159, 194)));

        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(1, 3)), new Pair<>(new Vector2i(5, 6), new Vector2i(7, 8)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(3, 5)), new Pair<>(new Vector2i(7, 8), new Vector2i(10, 13)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(5, 7)), new Pair<>(new Vector2i(10, 13), new Vector2i(13, 16)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(7, 9)), new Pair<>(new Vector2i(13, 16), new Vector2i(15, 18)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(10, 13)), new Pair<>(new Vector2i(16, 19), new Vector2i(19, 24)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(13, 15)), new Pair<>(new Vector2i(19, 24), new Vector2i(21, 26)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(15, 17)), new Pair<>(new Vector2i(21, 26), new Vector2i(24, 29)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(17, 19)), new Pair<>(new Vector2i(24, 29), new Vector2i(27, 33)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(20, 23)), new Pair<>(new Vector2i(27, 33), new Vector2i(29, 36)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(23, 25)), new Pair<>(new Vector2i(29, 36), new Vector2i(32, 39)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(25, 27)), new Pair<>(new Vector2i(29, 36), new Vector2i(33, 40)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(27, 29)), new Pair<>(new Vector2i(32, 39), new Vector2i(36, 44)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(30, 33)), new Pair<>(new Vector2i(36, 45), new Vector2i(41, 50)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(33, 35)), new Pair<>(new Vector2i(41, 50), new Vector2i(45, 56)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(35, 37)), new Pair<>(new Vector2i(45, 56), new Vector2i(49, 60)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(37, 39)), new Pair<>(new Vector2i(49, 60), new Vector2i(54, 66)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(40, 43)), new Pair<>(new Vector2i(54, 67), new Vector2i(60, 73)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(43, 45)), new Pair<>(new Vector2i(60, 73), new Vector2i(66, 81)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(45, 47)), new Pair<>(new Vector2i(66, 81), new Vector2i(72, 89)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(47, 49)), new Pair<>(new Vector2i(72, 89), new Vector2i(79, 96)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(50, 53)), new Pair<>(new Vector2i(80, 97), new Vector2i(87, 106)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(53, 55)), new Pair<>(new Vector2i(87, 106), new Vector2i(95, 116)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(55, 57)), new Pair<>(new Vector2i(95, 116), new Vector2i(103, 126)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(57, 59)), new Pair<>(new Vector2i(103, 126), new Vector2i(111, 136)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(60, 63)), new Pair<>(new Vector2i(112, 137), new Vector2i(121, 148)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(63, 65)), new Pair<>(new Vector2i(121, 148), new Vector2i(130, 159)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(65, 67)), new Pair<>(new Vector2i(130, 159), new Vector2i(139, 170)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(67, 69)), new Pair<>(new Vector2i(139, 170), new Vector2i(148, 181)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(70, 73)), new Pair<>(new Vector2i(154, 189), new Vector2i(162, 198)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(73, 75)), new Pair<>(new Vector2i(162, 198), new Vector2i(165, 202)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(75, 77)), new Pair<>(new Vector2i(165, 202), new Vector2i(170, 207)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(77, 79)), new Pair<>(new Vector2i(170, 207), new Vector2i(171, 209)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(80, 83)), new Pair<>(new Vector2i(172, 211), new Vector2i(178, 217)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(83, 85)), new Pair<>(new Vector2i(178, 217), new Vector2i(181, 222)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(85, 87)), new Pair<>(new Vector2i(181, 222), new Vector2i(185, 226)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(87, 89)), new Pair<>(new Vector2i(185, 226), new Vector2i(186, 227)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(90, 93)), new Pair<>(new Vector2i(187, 228), new Vector2i(190, 233)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(93, 95)), new Pair<>(new Vector2i(190, 233), new Vector2i(193, 236)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(95, 97)), new Pair<>(new Vector2i(193, 236), new Vector2i(195, 238)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(97, 99)), new Pair<>(new Vector2i(195, 238), new Vector2i(196, 239)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(100, 103)), new Pair<>(new Vector2i(197, 240), new Vector2i(198, 242)));
        DAMAGES.put(new Pair<>(CraftableType.DAGGER, new Vector2i(103, 105)), new Pair<>(new Vector2i(198, 242), new Vector2i(198, 243)));

        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(1, 3)), new Pair<>(new Vector2i(6, 7), new Vector2i(9, 11)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(3, 5)), new Pair<>(new Vector2i(10, 13), new Vector2i(13, 16)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(5, 7)), new Pair<>(new Vector2i(13, 16), new Vector2i(15, 18)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(7, 9)), new Pair<>(new Vector2i(15, 18), new Vector2i(19, 24)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(10, 13)), new Pair<>(new Vector2i(20, 25), new Vector2i(22, 27)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(13, 15)), new Pair<>(new Vector2i(22, 27), new Vector2i(27, 33)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(15, 17)), new Pair<>(new Vector2i(27, 33), new Vector2i(30, 37)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(17, 19)), new Pair<>(new Vector2i(30, 37), new Vector2i(33, 40)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(20, 23)), new Pair<>(new Vector2i(32, 39), new Vector2i(35, 42)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(23, 25)), new Pair<>(new Vector2i(35, 42), new Vector2i(37, 46)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(25, 27)), new Pair<>(new Vector2i(36, 45), new Vector2i(38, 47)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(27, 29)), new Pair<>(new Vector2i(38, 47), new Vector2i(43, 52)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(30, 33)), new Pair<>(new Vector2i(44, 53), new Vector2i(49, 60)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(33, 35)), new Pair<>(new Vector2i(49, 60), new Vector2i(54, 67)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(35, 37)), new Pair<>(new Vector2i(54, 67), new Vector2i(59, 72)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(37, 39)), new Pair<>(new Vector2i(59, 72), new Vector2i(64, 79)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(40, 43)), new Pair<>(new Vector2i(65, 80), new Vector2i(72, 88)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(43, 45)), new Pair<>(new Vector2i(72, 88), new Vector2i(80, 97)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(45, 47)), new Pair<>(new Vector2i(80, 97), new Vector2i(87, 106)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(47, 49)), new Pair<>(new Vector2i(87, 106), new Vector2i(95, 116)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(50, 53)), new Pair<>(new Vector2i(96, 117), new Vector2i(104, 127)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(53, 55)), new Pair<>(new Vector2i(104, 127), new Vector2i(114, 139)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(55, 57)), new Pair<>(new Vector2i(114, 139), new Vector2i(124, 151)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(57, 59)), new Pair<>(new Vector2i(124, 151), new Vector2i(134, 163)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(60, 63)), new Pair<>(new Vector2i(135, 165), new Vector2i(145, 178)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(63, 65)), new Pair<>(new Vector2i(145, 178), new Vector2i(156, 191)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(65, 67)), new Pair<>(new Vector2i(156, 191), new Vector2i(167, 204)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(67, 69)), new Pair<>(new Vector2i(167, 204), new Vector2i(178, 217)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(70, 73)), new Pair<>(new Vector2i(186, 227), new Vector2i(193, 236)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(73, 75)), new Pair<>(new Vector2i(193, 236), new Vector2i(198, 243)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(75, 77)), new Pair<>(new Vector2i(198, 243), new Vector2i(204, 249)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(77, 79)), new Pair<>(new Vector2i(204, 249), new Vector2i(205, 250)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(80, 83)), new Pair<>(new Vector2i(207, 254), new Vector2i(214, 261)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(83, 85)), new Pair<>(new Vector2i(214, 261), new Vector2i(217, 266)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(85, 87)), new Pair<>(new Vector2i(217, 266), new Vector2i(223, 272)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(87, 89)), new Pair<>(new Vector2i(223, 272), new Vector2i(224, 273)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(90, 93)), new Pair<>(new Vector2i(225, 275), new Vector2i(228, 279)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(93, 95)), new Pair<>(new Vector2i(228, 279), new Vector2i(231, 282)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(95, 97)), new Pair<>(new Vector2i(231, 282), new Vector2i(234, 286)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(97, 99)), new Pair<>(new Vector2i(234, 286), new Vector2i(235, 288)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(100, 103)), new Pair<>(new Vector2i(236, 289), new Vector2i(237, 290)));
        DAMAGES.put(new Pair<>(CraftableType.BOW, new Vector2i(103, 105)), new Pair<>(new Vector2i(237, 290), new Vector2i(238, 291)));

        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(1, 3)), new Pair<>(new Vector2i(2, 3), new Vector2i(4, 5)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(3, 5)), new Pair<>(new Vector2i(4, 5), new Vector2i(6, 7)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(5, 7)), new Pair<>(new Vector2i(6, 7), new Vector2i(9, 11)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(7, 9)), new Pair<>(new Vector2i(9, 11), new Vector2i(10, 13)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(10, 13)), new Pair<>(new Vector2i(9, 12), new Vector2i(11, 14)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(13, 15)), new Pair<>(new Vector2i(11, 14), new Vector2i(13, 16)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(15, 17)), new Pair<>(new Vector2i(13, 16), new Vector2i(15, 18)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(17, 19)), new Pair<>(new Vector2i(14, 17), new Vector2i(17, 20)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(20, 23)), new Pair<>(new Vector2i(17, 20), new Vector2i(18, 23)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(23, 25)), new Pair<>(new Vector2i(18, 22), new Vector2i(20, 25)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(25, 27)), new Pair<>(new Vector2i(19, 24), new Vector2i(21, 26)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(27, 29)), new Pair<>(new Vector2i(19, 24), new Vector2i(22, 27)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(30, 33)), new Pair<>(new Vector2i(22, 27), new Vector2i(25, 30)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(33, 35)), new Pair<>(new Vector2i(25, 30), new Vector2i(27, 34)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(35, 37)), new Pair<>(new Vector2i(27, 34), new Vector2i(29, 36)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(37, 39)), new Pair<>(new Vector2i(29, 36), new Vector2i(32, 39)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(40, 43)), new Pair<>(new Vector2i(33, 40), new Vector2i(36, 44)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(43, 45)), new Pair<>(new Vector2i(36, 44), new Vector2i(39, 48)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(45, 47)), new Pair<>(new Vector2i(29, 48), new Vector2i(44, 53)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(47, 49)), new Pair<>(new Vector2i(44, 53), new Vector2i(47, 58)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(50, 53)), new Pair<>(new Vector2i(47, 58), new Vector2i(52, 63)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(53, 55)), new Pair<>(new Vector2i(52, 63), new Vector2i(57, 70)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(55, 57)), new Pair<>(new Vector2i(57, 70), new Vector2i(62, 75)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(57, 59)), new Pair<>(new Vector2i(62, 75), new Vector2i(66, 81)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(60, 63)), new Pair<>(new Vector2i(67, 82), new Vector2i(72, 89)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(63, 65)), new Pair<>(new Vector2i(72, 89), new Vector2i(78, 95)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(65, 67)), new Pair<>(new Vector2i(78, 95), new Vector2i(83, 102)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(67, 69)), new Pair<>(new Vector2i(83, 102), new Vector2i(89, 108)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(70, 73)), new Pair<>(new Vector2i(92, 113), new Vector2i(97, 118)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(73, 75)), new Pair<>(new Vector2i(97, 118), new Vector2i(99, 121)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(75, 77)), new Pair<>(new Vector2i(99, 121), new Vector2i(101, 124)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(77, 79)), new Pair<>(new Vector2i(101, 124), new Vector2i(102, 125)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(80, 83)), new Pair<>(new Vector2i(103, 126), new Vector2i(107, 130)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(83, 85)), new Pair<>(new Vector2i(107, 130), new Vector2i(108, 133)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(85, 87)), new Pair<>(new Vector2i(108, 133), new Vector2i(111, 136)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(87, 89)), new Pair<>(new Vector2i(111, 136), new Vector2i(112, 137)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(90, 93)), new Pair<>(new Vector2i(113, 138), new Vector2i(114, 139)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(93, 95)), new Pair<>(new Vector2i(114, 139), new Vector2i(116, 141)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(95, 97)), new Pair<>(new Vector2i(116, 141), new Vector2i(117, 143)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(97, 99)), new Pair<>(new Vector2i(117, 143), new Vector2i(117, 144)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(100, 103)), new Pair<>(new Vector2i(117, 144), new Vector2i(118, 145)));
        DAMAGES.put(new Pair<>(CraftableType.WAND, new Vector2i(103, 105)), new Pair<>(new Vector2i(118, 145), new Vector2i(119, 146)));
    }

    public static List<RecipeRange> getByLevel(int level) {
        List<RecipeRange> result = new ArrayList<>();
        for (RecipeRange range : RANGES) {
            if (range.containsLevel(level)) result.add(range);
        }
        return result;
    }

    public static RecipeRange getByLevel(Vector2i level) {
        for (RecipeRange range : RANGES) {
            if (range.level.equals(level)) return range;
        }
        return null;
    }

    public static Pair<Vector2i, Vector2i> getDamage(CraftableType type, Vector2i level) {
        CraftableType t = type == CraftableType.RELIK ? CraftableType.BOW : type;
        return DAMAGES.get(new Pair<>(t, level));
    }

}
