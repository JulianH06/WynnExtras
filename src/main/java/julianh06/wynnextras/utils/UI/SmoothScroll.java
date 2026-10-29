package julianh06.wynnextras.utils.UI;

/** Shared scroll state using the same interpolation as the waypoint UI. */
public final class SmoothScroll {
    private static final float SPEED = 0.3F;
    private static final float SNAP_DISTANCE = 0.5F;

    private float position;
    private float target;

    public float update(float tickDelta) {
        position = interpolate(position, target, tickDelta);
        return position;
    }

    public float position() {
        return position;
    }

    public void move(float amount, float minimum, float maximum) {
        target = Math.clamp(target + amount, minimum, maximum);
    }

    public void clamp(float minimum, float maximum) {
        target = Math.clamp(target, minimum, maximum);
        position = Math.clamp(position, minimum, maximum);
    }

    public void jump(float value) {
        position = value;
        target = value;
    }

    public static float interpolate(float position, float target, float tickDelta) {
        return interpolate(position, target, tickDelta, SNAP_DISTANCE);
    }

    public static float interpolate(float position, float target, float tickDelta, float snapDistance) {
        float difference = target - position;
        if (Math.abs(difference) < snapDistance) {
            return target;
        }
        return position + difference * SPEED * tickDelta;
    }
}
