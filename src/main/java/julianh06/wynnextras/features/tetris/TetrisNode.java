package julianh06.wynnextras.features.tetris;

import java.awt.*;
import java.util.ArrayList;

public class TetrisNode {
    public static ArrayList<TetrisNode> nodes = new ArrayList<>();
    private ArrayList<TetrisNode> familyNodes = new ArrayList<>();
    public static final int CELL = 10;
    private int x, y;
    private int color;
    private String shape;
    private int rotation;
    private int downpos;
    public int currCheck = 0;

    public TetrisNode(int x, int y) {
        this.x = x;
        this.y = y;
        this.rotation = 1;
        nodes.add(this);
    }

    public void removeFromList() { nodes.remove(this); }

    public void setColor(int color) {
        for (TetrisNode n : familyNodes) n.color = color;
    }

    public int getColor() { return color; }

    public void setShape(String shape) {
        for (TetrisNode n : familyNodes) n.shape = shape;
    }

    public String getShape() { return shape; }

    public void addToFamily(TetrisNode node) { familyNodes.add(node); }

    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getRotation() { return rotation; }

    public ArrayList<TetrisNode> getFamily() { return familyNodes; }

    public void moveDown() {
        for (int i = 0; i < familyNodes.size(); i++)
            familyNodes.get(i).setY(familyNodes.get(i).getY() + CELL);
    }

    public void moveRight() {
        for (int i = 0; i < familyNodes.size(); i++)
            familyNodes.get(i).setX(familyNodes.get(i).getX() + CELL);
    }

    public void moveLeft() {
        for (int i = 0; i < familyNodes.size(); i++)
            familyNodes.get(i).setX(familyNodes.get(i).getX() - CELL);
    }

    public boolean canMoveRight() {
        for (int i = 0; i < familyNodes.size(); i++) {
            TetrisNode n = getNode(familyNodes.get(i).getX() + CELL, familyNodes.get(i).getY());
            if (n != null && !isInFamily(n)) return false;
            if (familyNodes.get(i).getX() > TetrisScreen.toX - 20) return false;
        }
        return true;
    }

    public boolean canMoveLeft() {
        for (int i = 0; i < familyNodes.size(); i++) {
            TetrisNode n = getNode(familyNodes.get(i).getX() - CELL, familyNodes.get(i).getY());
            if (n != null && !isInFamily(n)) return false;
            if (familyNodes.get(i).getX() < TetrisScreen.fromX + CELL) return false;
        }
        return true;
    }

    public boolean canGoDown() {
        for (int i = 0; i < familyNodes.size(); i++) {
            TetrisNode n = getNode(familyNodes.get(i).getX(), familyNodes.get(i).getY() + CELL);
            if (familyNodes.get(i).getY() > TetrisScreen.toY - CELL) return false;
            if (n != null && !isInFamily(n)) return false;
        }
        return true;
    }

    public void moveCompletelyDown() {
        for (int i = 0; i < 150; i++) {
            if (canGoDown()) moveDown();
        }
        TetrisScreen.instance.doNewPieceStuff();
    }

    public boolean isInFamily(TetrisNode node) { return familyNodes.contains(node); }

    public static TetrisNode getNode(int x, int y) {
        for (int i = 0; i < nodes.size(); i++) {
            TetrisNode n = nodes.get(i);
            if (n.getX() == x && n.getY() == y) return n;
        }
        return null;
    }

    public void clearFamily() {
        for (int i = 0; i < familyNodes.size(); i++) {
            if (!familyNodes.get(i).equals(this)) nodes.remove(familyNodes.get(i));
        }
        familyNodes.clear();
    }

    public void setDownPosition() {
        int y0 = familyNodes.get(0).getY(), y1 = familyNodes.get(1).getY();
        int y2 = familyNodes.get(2).getY(), y3 = familyNodes.get(3).getY();

        for (int i2 = 0; i2 < 100; i2++) {
            for (int i3 = 0; i3 < familyNodes.size(); i3++)
                familyNodes.get(i3).setY(familyNodes.get(i3).getY() + CELL);
            int nodeY = this.y;
            if (!canGoDown()) {
                for (int i = 0; i < familyNodes.size(); i++)
                    familyNodes.get(i).downpos = (familyNodes.get(i).getY() - getY()) + nodeY;
                familyNodes.get(0).setY(y0); familyNodes.get(1).setY(y1);
                familyNodes.get(2).setY(y2); familyNodes.get(3).setY(y3);
                return;
            }
        }
    }

    public int getDownPosition() { return downpos; }

    // SRS rotation - read https://harddrop.com/wiki/SRS
    // Y is inverted: down = positive
    public boolean canRotate(boolean dir) {
        if (!dir) { // COUNTERCLOCKWISE
            String rot = getShape();
            if (rot.equals("O")) return true;
            ArrayList<Point> newPositions = new ArrayList<>();
            int newRotation = rotation - 1;
            if (newRotation < 1) newRotation = 4;
            currCheck = 0;
            boolean checkState = false;
            while (!checkState) {
                int tempX = getX(), tempY = getY();
                if (rot.equals("I")) {
                    if (newRotation == 1) {
                        tempX -= 10;
                        if (currCheck == 1) tempX += 20;
                        else if (currCheck == 2) tempX -= 10;
                        else if (currCheck == 3) { tempX += 20; tempY -= 10; }
                        else if (currCheck == 4) { tempX -= 10; tempY += 20; }
                        newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX + 10, tempY));
                        newPositions.add(new Point(tempX + 20, tempY)); newPositions.add(new Point(tempX, tempY));
                    } else if (newRotation == 2) {
                        tempY -= 10;
                        if (currCheck == 1) tempX += 10;
                        else if (currCheck == 2) tempX -= 20;
                        else if (currCheck == 3) { tempX += 10; tempY += 20; }
                        else if (currCheck == 4) { tempX -= 20; tempY -= 10; }
                        newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX, tempY - 10));
                        newPositions.add(new Point(tempX, tempY - 20)); newPositions.add(new Point(tempX, tempY));
                    } else if (newRotation == 3) {
                        tempX += 10;
                        if (currCheck == 1) tempX -= 20;
                        else if (currCheck == 2) tempX += 10;
                        else if (currCheck == 3) { tempX -= 20; tempY += 10; }
                        else if (currCheck == 4) { tempX += 10; tempY -= 20; }
                        newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX - 10, tempY));
                        newPositions.add(new Point(tempX - 20, tempY)); newPositions.add(new Point(tempX, tempY));
                    } else {
                        tempY += 10;
                        if (currCheck == 1) tempX -= 10;
                        else if (currCheck == 2) tempX += 20;
                        else if (currCheck == 3) { tempX -= 10; tempY -= 20; }
                        else if (currCheck == 4) { tempX += 20; tempY += 10; }
                        newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX, tempY - 20));
                        newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX, tempY));
                    }
                } else {
                    if (newRotation == 1) {
                        if (currCheck == 1) tempX += 10;
                        else if (currCheck == 2) { tempX += 10; tempY += 10; }
                        else if (currCheck == 3) tempY -= 20;
                        else if (currCheck == 4) { tempX += 10; tempY -= 20; }
                    } else if (newRotation == 2) {
                        if (currCheck == 1) tempX -= 10;
                        else if (currCheck == 2) { tempX -= 10; tempY -= 10; }
                        else if (currCheck == 3) tempY += 20;
                        else if (currCheck == 4) { tempX -= 10; tempY += 20; }
                    } else if (newRotation == 3) {
                        if (currCheck == 1) tempX -= 10;
                        else if (currCheck == 2) { tempX -= 10; tempY += 10; }
                        else if (currCheck == 3) tempY -= 20;
                        else if (currCheck == 4) { tempX -= 10; tempY -= 20; }
                    } else {
                        if (currCheck == 1) tempX += 10;
                        else if (currCheck == 2) { tempX += 10; tempY -= 10; }
                        else if (currCheck == 3) tempY += 20;
                        else if (currCheck == 4) { tempX += 10; tempY += 20; }
                    }
                    if (rot.equals("S")) {
                        if (newRotation == 1) { newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX + 10, tempY - 10)); newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX, tempY)); }
                        else if (newRotation == 2) { newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX + 10, tempY + 10)); newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX, tempY)); }
                        else if (newRotation == 3) { newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX - 10, tempY + 10)); newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX, tempY)); }
                        else { newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX - 10, tempY - 10)); newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX, tempY)); }
                    }
                    if (rot.equals("Z")) {
                        if (newRotation == 1) { newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX - 10, tempY - 10)); newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX, tempY)); }
                        else if (newRotation == 2) { newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX + 10, tempY - 10)); newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX, tempY)); }
                        else if (newRotation == 3) { newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX + 10, tempY + 10)); newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX, tempY)); }
                        else { newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX - 10, tempY + 10)); newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX, tempY)); }
                    }
                    if (rot.equals("J")) {
                        if (newRotation == 1) { newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX - 10, tempY - 10)); newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX, tempY)); }
                        else if (newRotation == 2) { newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX + 10, tempY - 10)); newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX, tempY)); }
                        else if (newRotation == 3) { newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX + 10, tempY + 10)); newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX, tempY)); }
                        else { newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX - 10, tempY + 10)); newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX, tempY)); }
                    }
                    if (rot.equals("L")) {
                        if (newRotation == 1) { newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX + 10, tempY - 10)); newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX, tempY)); }
                        else if (newRotation == 2) { newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX + 10, tempY + 10)); newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX, tempY)); }
                        else if (newRotation == 3) { newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX - 10, tempY + 10)); newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX, tempY)); }
                        else { newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX - 10, tempY - 10)); newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX, tempY)); }
                    }
                    if (rot.equals("T")) {
                        if (newRotation == 1) { newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX, tempY)); }
                        else if (newRotation == 2) { newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX, tempY)); }
                        else if (newRotation == 3) { newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX, tempY)); }
                        else { newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX, tempY)); }
                    }
                }
                checkState = true;
                for (Point p : newPositions) {
                    if (p.x < TetrisScreen.fromX || p.x >= TetrisScreen.toX || p.y > TetrisScreen.toY || (getNode(p.x, p.y) != null && !getFamily().contains(getNode(p.x, p.y)))) {
                        checkState = false;
                    }
                }
                if (checkState) return true;
                if (currCheck == 4) return false;
                newPositions.clear();
                currCheck++;
            }
            return true;
        } else { // CLOCKWISE
            String rot = getShape();
            if (rot.equals("O")) return true;
            ArrayList<Point> newPositions = new ArrayList<>();
            int newRotation = rotation + 1;
            if (newRotation > 4) newRotation = 1;
            boolean checkState = false;
            currCheck = 0;
            while (!checkState) {
                int tempX = getX(), tempY = getY();
                if (rot.equals("I")) {
                    if (newRotation == 1) {
                        tempY -= 10;
                        if (currCheck == 1) tempX += 10;
                        else if (currCheck == 2) tempX -= 20;
                        else if (currCheck == 3) { tempX += 10; tempY += 20; }
                        else if (currCheck == 4) { tempX -= 20; tempY -= 10; }
                        newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX + 10, tempY));
                        newPositions.add(new Point(tempX + 20, tempY)); newPositions.add(new Point(tempX, tempY));
                    } else if (newRotation == 2) {
                        tempX += 10;
                        if (currCheck == 1) tempX -= 20;
                        else if (currCheck == 2) tempX += 10;
                        else if (currCheck == 3) { tempX -= 20; tempY += 10; }
                        else if (currCheck == 4) { tempX += 10; tempY -= 20; }
                        newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX, tempY - 10));
                        newPositions.add(new Point(tempX, tempY - 20)); newPositions.add(new Point(tempX, tempY));
                    } else if (newRotation == 3) {
                        tempY += 10;
                        if (currCheck == 1) tempX -= 10;
                        else if (currCheck == 2) tempX += 20;
                        else if (currCheck == 3) { tempX -= 10; tempY -= 20; }
                        else if (currCheck == 4) { tempX += 20; tempY += 10; }
                        newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX - 10, tempY));
                        newPositions.add(new Point(tempX - 20, tempY)); newPositions.add(new Point(tempX, tempY));
                    } else {
                        tempX -= 10;
                        if (currCheck == 1) tempX += 20;
                        else if (currCheck == 2) tempX -= 10;
                        else if (currCheck == 3) { tempX += 20; tempY -= 10; }
                        else if (currCheck == 4) { tempX -= 10; tempY += 20; }
                        newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX, tempY - 20));
                        newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX, tempY));
                    }
                } else {
                    if (newRotation == 1) {
                        if (currCheck == 1) tempX -= 10;
                        else if (currCheck == 2) { tempX -= 10; tempY += 10; }
                        else if (currCheck == 3) tempY -= 20;
                        else if (currCheck == 4) { tempX -= 10; tempY -= 20; }
                    } else if (newRotation == 2) {
                        if (currCheck == 1) tempX -= 10;
                        else if (currCheck == 2) { tempX -= 10; tempY -= 10; }
                        else if (currCheck == 3) tempY += 20;
                        else if (currCheck == 4) { tempX -= 10; tempY += 20; }
                    } else if (newRotation == 3) {
                        if (currCheck == 1) tempX += 10;
                        else if (currCheck == 2) { tempX += 10; tempY += 10; }
                        else if (currCheck == 3) tempY -= 20;
                        else if (currCheck == 4) { tempX += 10; tempY -= 20; }
                    } else {
                        if (currCheck == 1) tempX += 10;
                        else if (currCheck == 2) { tempX += 10; tempY -= 10; }
                        else if (currCheck == 3) tempY += 20;
                        else if (currCheck == 4) { tempX += 10; tempY -= 20; }
                    }
                    if (rot.equals("S")) {
                        if (newRotation == 1) { newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX + 10, tempY - 10)); newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX, tempY)); }
                        else if (newRotation == 2) { newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX + 10, tempY + 10)); newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX, tempY)); }
                        else if (newRotation == 3) { newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX - 10, tempY + 10)); newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX, tempY)); }
                        else { newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX - 10, tempY - 10)); newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX, tempY)); }
                    }
                    if (rot.equals("Z")) {
                        if (newRotation == 1) { newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX - 10, tempY - 10)); newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX, tempY)); }
                        else if (newRotation == 2) { newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX + 10, tempY - 10)); newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX, tempY)); }
                        else if (newRotation == 3) { newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX + 10, tempY + 10)); newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX, tempY)); }
                        else { newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX - 10, tempY + 10)); newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX, tempY)); }
                    }
                    if (rot.equals("J")) {
                        if (newRotation == 1) { newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX - 10, tempY - 10)); newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX, tempY)); }
                        else if (newRotation == 2) { newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX + 10, tempY - 10)); newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX, tempY)); }
                        else if (newRotation == 3) { newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX + 10, tempY + 10)); newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX, tempY)); }
                        else { newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX - 10, tempY + 10)); newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX, tempY)); }
                    }
                    if (rot.equals("L")) {
                        if (newRotation == 1) { newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX + 10, tempY - 10)); newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX, tempY)); }
                        else if (newRotation == 2) { newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX + 10, tempY + 10)); newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX, tempY)); }
                        else if (newRotation == 3) { newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX - 10, tempY + 10)); newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX, tempY)); }
                        else { newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX - 10, tempY - 10)); newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX, tempY)); }
                    }
                    if (rot.equals("T")) {
                        if (newRotation == 1) { newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX, tempY)); }
                        else if (newRotation == 2) { newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX, tempY)); }
                        else if (newRotation == 3) { newPositions.add(new Point(tempX + 10, tempY)); newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX, tempY)); }
                        else { newPositions.add(new Point(tempX, tempY + 10)); newPositions.add(new Point(tempX - 10, tempY)); newPositions.add(new Point(tempX, tempY - 10)); newPositions.add(new Point(tempX, tempY)); }
                    }
                }
                checkState = true;
                for (Point p : newPositions) {
                    if (p.x < TetrisScreen.fromX || p.x >= TetrisScreen.toX || p.y > TetrisScreen.toY || (getNode(p.x, p.y) != null && !getFamily().contains(getNode(p.x, p.y)))) {
                        checkState = false;
                    }
                }
                if (checkState) return true;
                if (currCheck == 4) return false;
                newPositions.clear();
                currCheck++;
            }
            return true;
        }
    }

    public void rotate(boolean dir) {
        if (!dir) { // COUNTERCLOCKWISE
            String rot = getShape();
            if (rot.equals("O")) return;
            rotation--;
            clearFamily();
            familyNodes.add(this);
            if (rotation < 1) rotation = 4;

            if (rot.equals("I")) {
                if (rotation == 1) {
                    x -= 10;
                    if (currCheck == 1) x += 20; else if (currCheck == 2) x -= 10;
                    else if (currCheck == 3) { x += 20; y -= 10; } else if (currCheck == 4) { x -= 10; y += 20; }
                    familyNodes.add(new TetrisNode(x - 10, y)); familyNodes.add(new TetrisNode(x + 10, y)); familyNodes.add(new TetrisNode(x + 20, y));
                } else if (rotation == 2) {
                    y -= 10;
                    if (currCheck == 1) x += 10; else if (currCheck == 2) x -= 20;
                    else if (currCheck == 3) { x += 10; y += 20; } else if (currCheck == 4) { x -= 20; y -= 10; }
                    familyNodes.add(new TetrisNode(x, y + 10)); familyNodes.add(new TetrisNode(x, y - 10)); familyNodes.add(new TetrisNode(x, y - 20));
                } else if (rotation == 3) {
                    x += 10;
                    if (currCheck == 1) x -= 20; else if (currCheck == 2) x += 10;
                    else if (currCheck == 3) { x -= 20; y += 10; } else if (currCheck == 4) { x += 10; y -= 20; }
                    familyNodes.add(new TetrisNode(x + 10, y)); familyNodes.add(new TetrisNode(x - 10, y)); familyNodes.add(new TetrisNode(x - 20, y));
                } else {
                    y += 10;
                    if (currCheck == 1) x -= 10; else if (currCheck == 2) x += 20;
                    else if (currCheck == 3) { x -= 10; y -= 20; } else if (currCheck == 4) { x += 20; y += 10; }
                    familyNodes.add(new TetrisNode(x, y - 10)); familyNodes.add(new TetrisNode(x, y - 20)); familyNodes.add(new TetrisNode(x, y + 10));
                }
                setColor(new Color(15, 155, 217).getRGB());
            } else {
                if (rotation == 1) {
                    if (currCheck == 1) x += 10; else if (currCheck == 2) { x += 10; y += 10; }
                    else if (currCheck == 3) y -= 20; else if (currCheck == 4) { x += 10; y -= 20; }
                } else if (rotation == 2) {
                    if (currCheck == 1) x -= 10; else if (currCheck == 2) { x -= 10; y -= 10; }
                    else if (currCheck == 3) y += 20; else if (currCheck == 4) { x -= 10; y += 20; }
                } else if (rotation == 3) {
                    if (currCheck == 1) x -= 10; else if (currCheck == 2) { x -= 10; y += 10; }
                    else if (currCheck == 3) y -= 20; else if (currCheck == 4) { x -= 10; y -= 20; }
                } else {
                    if (currCheck == 1) x += 10; else if (currCheck == 2) { x += 10; y -= 10; }
                    else if (currCheck == 3) y += 20; else if (currCheck == 4) { x += 10; y += 20; }
                }
                if (rot.equals("S")) {
                    if (rotation == 1) { familyNodes.add(new TetrisNode(x, y - 10)); familyNodes.add(new TetrisNode(x + 10, y - 10)); familyNodes.add(new TetrisNode(x - 10, y)); }
                    else if (rotation == 2) { familyNodes.add(new TetrisNode(x + 10, y)); familyNodes.add(new TetrisNode(x + 10, y + 10)); familyNodes.add(new TetrisNode(x, y - 10)); }
                    else if (rotation == 3) { familyNodes.add(new TetrisNode(x, y + 10)); familyNodes.add(new TetrisNode(x - 10, y + 10)); familyNodes.add(new TetrisNode(x + 10, y)); }
                    else { familyNodes.add(new TetrisNode(x - 10, y)); familyNodes.add(new TetrisNode(x - 10, y - 10)); familyNodes.add(new TetrisNode(x, y + 10)); }
                    setColor(new Color(90, 178, 2).getRGB());
                }
                if (rot.equals("Z")) {
                    if (x - 10 < TetrisScreen.fromX) x += 10;
                    if (rotation == 1) { familyNodes.add(new TetrisNode(x, y - 10)); familyNodes.add(new TetrisNode(x - 10, y - 10)); familyNodes.add(new TetrisNode(x + 10, y)); }
                    else if (rotation == 2) { familyNodes.add(new TetrisNode(x + 10, y)); familyNodes.add(new TetrisNode(x + 10, y - 10)); familyNodes.add(new TetrisNode(x, y + 10)); }
                    else if (rotation == 3) { familyNodes.add(new TetrisNode(x, y + 10)); familyNodes.add(new TetrisNode(x + 10, y + 10)); familyNodes.add(new TetrisNode(x - 10, y)); }
                    else { familyNodes.add(new TetrisNode(x - 10, y)); familyNodes.add(new TetrisNode(x - 10, y + 10)); familyNodes.add(new TetrisNode(x, y - 10)); }
                    setColor(new Color(216, 16, 54).getRGB());
                }
                if (rot.equals("J")) {
                    if (rotation == 1) { familyNodes.add(new TetrisNode(x - 10, y)); familyNodes.add(new TetrisNode(x - 10, y - 10)); familyNodes.add(new TetrisNode(x + 10, y)); }
                    else if (rotation == 2) { familyNodes.add(new TetrisNode(x, y - 10)); familyNodes.add(new TetrisNode(x + 10, y - 10)); familyNodes.add(new TetrisNode(x, y + 10)); }
                    else if (rotation == 3) { familyNodes.add(new TetrisNode(x + 10, y)); familyNodes.add(new TetrisNode(x + 10, y + 10)); familyNodes.add(new TetrisNode(x - 10, y)); }
                    else { familyNodes.add(new TetrisNode(x, y + 10)); familyNodes.add(new TetrisNode(x - 10, y + 10)); familyNodes.add(new TetrisNode(x, y - 10)); }
                    setColor(new Color(33, 66, 198).getRGB());
                }
                if (rot.equals("L")) {
                    if (rotation == 1) { familyNodes.add(new TetrisNode(x + 10, y)); familyNodes.add(new TetrisNode(x + 10, y - 10)); familyNodes.add(new TetrisNode(x - 10, y)); }
                    else if (rotation == 2) { familyNodes.add(new TetrisNode(x, y + 10)); familyNodes.add(new TetrisNode(x + 10, y + 10)); familyNodes.add(new TetrisNode(x, y - 10)); }
                    else if (rotation == 3) { familyNodes.add(new TetrisNode(x - 10, y)); familyNodes.add(new TetrisNode(x - 10, y + 10)); familyNodes.add(new TetrisNode(x + 10, y)); }
                    else { familyNodes.add(new TetrisNode(x, y - 10)); familyNodes.add(new TetrisNode(x - 10, y - 10)); familyNodes.add(new TetrisNode(x, y + 10)); }
                    setColor(new Color(228, 92, 3).getRGB());
                }
                if (rot.equals("T")) {
                    if (rotation == 1) { familyNodes.add(new TetrisNode(x - 10, y)); familyNodes.add(new TetrisNode(x, y - 10)); familyNodes.add(new TetrisNode(x + 10, y)); }
                    else if (rotation == 2) { familyNodes.add(new TetrisNode(x, y - 10)); familyNodes.add(new TetrisNode(x + 10, y)); familyNodes.add(new TetrisNode(x, y + 10)); }
                    else if (rotation == 3) { familyNodes.add(new TetrisNode(x + 10, y)); familyNodes.add(new TetrisNode(x, y + 10)); familyNodes.add(new TetrisNode(x - 10, y)); }
                    else { familyNodes.add(new TetrisNode(x, y + 10)); familyNodes.add(new TetrisNode(x - 10, y)); familyNodes.add(new TetrisNode(x, y - 10)); }
                    setColor(new Color(175, 41, 138).getRGB());
                }
            }
        } else { // CLOCKWISE
            String rot = getShape();
            if (rot.equals("O")) return;
            rotation++;
            clearFamily();
            familyNodes.add(this);
            if (rotation > 4) rotation = 1;

            if (rot.equals("I")) {
                if (rotation == 1) {
                    y -= 10;
                    if (currCheck == 1) x += 10; else if (currCheck == 2) x -= 20;
                    else if (currCheck == 3) { x += 10; y += 20; } else if (currCheck == 4) { x -= 20; y -= 10; }
                    familyNodes.add(new TetrisNode(x - 10, y)); familyNodes.add(new TetrisNode(x + 10, y)); familyNodes.add(new TetrisNode(x + 20, y));
                } else if (rotation == 2) {
                    x += 10;
                    if (currCheck == 1) x -= 20; else if (currCheck == 2) x += 10;
                    else if (currCheck == 3) { x -= 20; y += 10; } else if (currCheck == 4) { x += 10; y -= 20; }
                    familyNodes.add(new TetrisNode(x, y + 10)); familyNodes.add(new TetrisNode(x, y - 10)); familyNodes.add(new TetrisNode(x, y - 20));
                } else if (rotation == 3) {
                    y += 10;
                    if (currCheck == 1) x -= 10; else if (currCheck == 2) x += 20;
                    else if (currCheck == 3) { x -= 10; y -= 20; } else if (currCheck == 4) { x += 20; y += 10; }
                    familyNodes.add(new TetrisNode(x + 10, y)); familyNodes.add(new TetrisNode(x - 10, y)); familyNodes.add(new TetrisNode(x - 20, y));
                } else {
                    x -= 10;
                    if (currCheck == 1) x += 20; else if (currCheck == 2) x -= 10;
                    else if (currCheck == 3) { x += 20; y -= 10; } else if (currCheck == 4) { x -= 10; y += 20; }
                    familyNodes.add(new TetrisNode(x, y - 10)); familyNodes.add(new TetrisNode(x, y - 20)); familyNodes.add(new TetrisNode(x, y + 10));
                }
                setColor(new Color(15, 155, 217).getRGB());
            } else {
                if (rotation == 1) {
                    if (currCheck == 1) x -= 10; else if (currCheck == 2) { x -= 10; y += 10; }
                    else if (currCheck == 3) y -= 20; else if (currCheck == 4) { x -= 10; y -= 20; }
                } else if (rotation == 2) {
                    if (currCheck == 1) x -= 10; else if (currCheck == 2) { x -= 10; y -= 10; }
                    else if (currCheck == 3) y += 20; else if (currCheck == 4) { x -= 10; y += 20; }
                } else if (rotation == 3) {
                    if (currCheck == 1) x += 10; else if (currCheck == 2) { x += 10; y += 10; }
                    else if (currCheck == 3) y -= 20; else if (currCheck == 4) { x += 10; y -= 20; }
                } else {
                    if (currCheck == 1) x += 10; else if (currCheck == 2) { x += 10; y -= 10; }
                    else if (currCheck == 3) y += 20; else if (currCheck == 4) { x += 10; y -= 20; }
                }
                if (rot.equals("S")) {
                    if (rotation == 1) { familyNodes.add(new TetrisNode(x, y - 10)); familyNodes.add(new TetrisNode(x + 10, y - 10)); familyNodes.add(new TetrisNode(x - 10, y)); }
                    else if (rotation == 2) { familyNodes.add(new TetrisNode(x + 10, y)); familyNodes.add(new TetrisNode(x + 10, y + 10)); familyNodes.add(new TetrisNode(x, y - 10)); }
                    else if (rotation == 3) { familyNodes.add(new TetrisNode(x, y + 10)); familyNodes.add(new TetrisNode(x - 10, y + 10)); familyNodes.add(new TetrisNode(x + 10, y)); }
                    else { familyNodes.add(new TetrisNode(x - 10, y)); familyNodes.add(new TetrisNode(x - 10, y - 10)); familyNodes.add(new TetrisNode(x, y + 10)); }
                    setColor(new Color(90, 178, 2).getRGB());
                }
                if (rot.equals("Z")) {
                    if (rotation == 1) { familyNodes.add(new TetrisNode(x, y - 10)); familyNodes.add(new TetrisNode(x - 10, y - 10)); familyNodes.add(new TetrisNode(x + 10, y)); }
                    else if (rotation == 2) { familyNodes.add(new TetrisNode(x + 10, y)); familyNodes.add(new TetrisNode(x + 10, y - 10)); familyNodes.add(new TetrisNode(x, y + 10)); }
                    else if (rotation == 3) { familyNodes.add(new TetrisNode(x, y + 10)); familyNodes.add(new TetrisNode(x + 10, y + 10)); familyNodes.add(new TetrisNode(x - 10, y)); }
                    else { familyNodes.add(new TetrisNode(x - 10, y)); familyNodes.add(new TetrisNode(x - 10, y + 10)); familyNodes.add(new TetrisNode(x, y - 10)); }
                    setColor(new Color(216, 16, 54).getRGB());
                }
                if (rot.equals("J")) {
                    if (rotation == 1) { familyNodes.add(new TetrisNode(x - 10, y)); familyNodes.add(new TetrisNode(x - 10, y - 10)); familyNodes.add(new TetrisNode(x + 10, y)); }
                    else if (rotation == 2) { familyNodes.add(new TetrisNode(x, y - 10)); familyNodes.add(new TetrisNode(x + 10, y - 10)); familyNodes.add(new TetrisNode(x, y + 10)); }
                    else if (rotation == 3) { familyNodes.add(new TetrisNode(x + 10, y)); familyNodes.add(new TetrisNode(x + 10, y + 10)); familyNodes.add(new TetrisNode(x - 10, y)); }
                    else { familyNodes.add(new TetrisNode(x, y + 10)); familyNodes.add(new TetrisNode(x - 10, y + 10)); familyNodes.add(new TetrisNode(x, y - 10)); }
                    setColor(new Color(33, 66, 198).getRGB());
                }
                if (rot.equals("L")) {
                    if (rotation == 1) { familyNodes.add(new TetrisNode(x + 10, y)); familyNodes.add(new TetrisNode(x + 10, y - 10)); familyNodes.add(new TetrisNode(x - 10, y)); }
                    else if (rotation == 2) { familyNodes.add(new TetrisNode(x, y + 10)); familyNodes.add(new TetrisNode(x + 10, y + 10)); familyNodes.add(new TetrisNode(x, y - 10)); }
                    else if (rotation == 3) { familyNodes.add(new TetrisNode(x - 10, y)); familyNodes.add(new TetrisNode(x - 10, y + 10)); familyNodes.add(new TetrisNode(x + 10, y)); }
                    else { familyNodes.add(new TetrisNode(x, y - 10)); familyNodes.add(new TetrisNode(x - 10, y - 10)); familyNodes.add(new TetrisNode(x, y + 10)); }
                    setColor(new Color(228, 92, 3).getRGB());
                }
                if (rot.equals("T")) {
                    if (rotation == 1) { familyNodes.add(new TetrisNode(x - 10, y)); familyNodes.add(new TetrisNode(x, y - 10)); familyNodes.add(new TetrisNode(x + 10, y)); }
                    else if (rotation == 2) { familyNodes.add(new TetrisNode(x, y - 10)); familyNodes.add(new TetrisNode(x + 10, y)); familyNodes.add(new TetrisNode(x, y + 10)); }
                    else if (rotation == 3) { familyNodes.add(new TetrisNode(x + 10, y)); familyNodes.add(new TetrisNode(x, y + 10)); familyNodes.add(new TetrisNode(x - 10, y)); }
                    else { familyNodes.add(new TetrisNode(x, y + 10)); familyNodes.add(new TetrisNode(x - 10, y)); familyNodes.add(new TetrisNode(x, y - 10)); }
                    setColor(new Color(175, 41, 138).getRGB());
                }
            }
        }
    }
}
