package julianh06.wynnextras.utils;

/**
 * Tiny recursive-descent calculator for the {@code /calc} command. Handles
 * +, -, *, /, parentheses, and decimal/negative numbers. Throws on parse error.
 */
public final class Calc {
    private final String s;
    private int i;

    private Calc(String input) { this.s = input; }

    public static double eval(String input) {
        Calc c = new Calc(input);
        double v = c.expr();
        c.skipWs();
        if (c.i < c.s.length()) throw new IllegalArgumentException("unexpected '" + c.s.charAt(c.i) + "' at " + c.i);
        return v;
    }

    private double expr() {
        double v = term();
        while (true) {
            skipWs();
            if (peek('+')) { i++; v += term(); }
            else if (peek('-')) { i++; v -= term(); }
            else return v;
        }
    }

    private double term() {
        double v = factor();
        while (true) {
            skipWs();
            if (peek('*')) { i++; v *= factor(); }
            else if (peek('/')) { i++; double r = factor(); if (r == 0) throw new ArithmeticException("divide by zero"); v /= r; }
            else return v;
        }
    }

    private double factor() {
        skipWs();
        if (peek('(')) { i++; double v = expr(); skipWs(); if (!peek(')')) throw new IllegalArgumentException("expected ')'"); i++; return v; }
        if (peek('-')) { i++; return -factor(); }
        if (peek('+')) { i++; return factor(); }
        int start = i;
        while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
        if (start == i) throw new IllegalArgumentException("expected number at " + i);
        return Double.parseDouble(s.substring(start, i));
    }

    private boolean peek(char c) { return i < s.length() && s.charAt(i) == c; }
    private void skipWs() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
}
