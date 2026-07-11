// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — reimplementation of ErrorOr sum type.
 * Either holds a value or an error message.
 */
package julianh06.wynnextras.wtshim.utils.type;

public final class ErrorOr<T> {
    private final T value;
    private final String error;

    private ErrorOr(T value, String error) {
        this.value = value;
        this.error = error;
    }

    public static <T> ErrorOr<T> of(T value) {
        return new ErrorOr<>(value, null);
    }

    public static <T> ErrorOr<T> error(String msg) {
        return new ErrorOr<>(null, msg == null ? "" : msg);
    }

    public boolean hasError() {
        return error != null;
    }

    public T getValue() {
        return value;
    }

    public String getError() {
        return error;
    }

    /** Logs the error (if any) and returns this, mirroring Wynntils' fluent ErrorOr.logged(). */
    public ErrorOr<T> logged() {
        if (error != null) {
            System.err.println("[wtshim] ErrorOr: " + error);
        }
        return this;
    }
}
