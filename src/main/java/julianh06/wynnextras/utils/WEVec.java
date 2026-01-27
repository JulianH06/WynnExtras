package julianh06.wynnextras.utils;

import julianh06.wynnextras.utils.render.WorldRenderUtils;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;
import java.util.Set;

public record WEVec(double x, double y, double z) {
    public WEVec(Vec3d vec) {
        this(vec.x, vec.y, vec.z);
    }

    public WEVec() {
        this(0.0, 0.0, 0.0);
    }

    public WEVec(float x, float y, float z) {
        this((double) x, (double) y, (double) z);
    }

    public Vec3d toVec3d() {
        return new Vec3d(x, y, z);
    }

    // Arithmetic operations
    public WEVec add(WEVec other) {
        return new WEVec(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    public WEVec add(double x, double y, double z) {
        return new WEVec(this.x + x, this.y + y, this.z + z);
    }

    public WEVec subtract(WEVec other) {
        return new WEVec(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public WEVec multiply(WEVec other) {
        return new WEVec(this.x * other.x, this.y * other.y, this.z * other.z);
    }

    public WEVec multiply(double scalar) {
        return new WEVec(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    public WEVec multiply(int scalar) {
        return new WEVec(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    public WEVec divide(WEVec other) {
        return new WEVec(this.x / other.x, this.y / other.y, this.z / other.z);
    }

    public WEVec divide(double scalar) {
        return new WEVec(this.x / scalar, this.y / scalar, this.z / scalar);
    }

    public double dot(WEVec other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    public WEVec cross(WEVec other) {
        return new WEVec(
                this.y * other.z - this.z * other.y,
                this.z * other.x - this.x * other.z,
                this.x * other.y - this.y * other.x
        );
    }

    // Trigonometrical operations
    public double angleAsCos(WEVec other) {
        return normalize().dot(other.normalize());
    }

    public double angleInRad(WEVec other) {
        return Math.acos(angleAsCos(other));
    }

    public double angleInDeg(WEVec other) {
        return Math.toDegrees(angleInRad(other));
    }

    // General utility methods
    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public double distanceTo(WEVec other) {
        return Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2) + Math.pow(z - other.z, 2));
    }

    public WEVec normalize() {
        double scalar = org.joml.Math.invsqrt(Math.fma(x, x, Math.fma(y, y, z * z)));
        return new WEVec(x * scalar, y * scalar, z * scalar);
    }

    public WEVec inverse() {
        return new WEVec(1.0 / x, 1.0 / y, 1.0 / z);
    }

    public WEVec negate() {
        return new WEVec(-x, -y, -z);
    }

    public double min() {
        return Math.min(Math.min(x, y), z);
    }

    public double max() {
        return Math.max(Math.max(x, y), z);
    }

    public boolean equals(WEVec other) {
        if (this == other) return true;
        if (other == null) return false;

        return Objects.equals(x, other.x) &&
                Objects.equals(y, other.y) &&
                Objects.equals(z, other.z);
    }

    public WEVec down() {
        return new WEVec(x, y - 1, z);
    }

    public WEVec up() {
        return new WEVec(x, y + 1, z);
    }

    // Blocks

    public WEVec toBlockPos() {
        return new WEVec(Math.floor(x), Math.floor(y), Math.floor(z));
    }

    public WEVec blockCenter() {
        return toBlockPos().add(0.5, 0.5, 0.5);
    }

    public Box boundingToOffset(double offX, double offY, double offZ) {
        return new Box(
                Math.min(x, x + offX), Math.min(y, y + offY), Math.min(z, z + offZ),
                Math.max(x, x + offX), Math.max(y, y + offY), Math.max(z, z + offZ)
        );
    }

    public Box boundingTo(WEVec other) {
        return new Box(
                Math.min(x, other.x), Math.min(y, other.y), Math.min(z, other.z),
                Math.max(x, other.x), Math.max(y, other.y), Math.max(z, other.z)
        );
    }

    public Set<Pair<WEVec, WEVec>> edges() {
        return WorldRenderUtils.calculateEdges(boundingToOffset(1.0, 1.0, 1.0));
    }
}
