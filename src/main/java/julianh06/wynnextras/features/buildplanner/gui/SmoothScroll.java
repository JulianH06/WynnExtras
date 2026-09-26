package julianh06.wynnextras.features.buildplanner.gui;

final class SmoothScroll {
    private final float speed;
    private float position;
    private float target;
    private long lastFrameNanos = System.nanoTime();

    SmoothScroll(float speed) {
        this.speed = speed;
    }

    float update() {
        long now = System.nanoTime();
        float elapsed = (now - lastFrameNanos) / 1_000_000_000.0F;
        lastFrameNanos = now;
        position = UiTheme.approach(position, target, speed, elapsed);
        if (Math.abs(position - target) < 0.02F) {
            position = target;
        }
        return position;
    }

    float position() {
        return position;
    }

    void move(float amount, float minimum, float maximum) {
        target = clamp(target + amount, minimum, maximum);
    }

    void clamp(float minimum, float maximum) {
        target = clamp(target, minimum, maximum);
        position = clamp(position, minimum, maximum);
    }

    void jump(float value) {
        position = value;
        target = value;
        lastFrameNanos = System.nanoTime();
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
