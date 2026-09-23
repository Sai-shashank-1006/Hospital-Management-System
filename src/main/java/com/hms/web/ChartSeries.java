package com.hms.web;

import com.hms.dao.ReportDAO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * View model for one chart on the reports page.
 *
 * <p>Bar lengths are a proportion of the series maximum, and working that out needs
 * the whole series. Computing it here keeps the arithmetic out of the JSP, which can
 * then simply read a percentage off each point.
 *
 * <p>Bar <em>length</em> is what encodes magnitude, so every bar is drawn in the same
 * hue; varying colour across the bars would imply a second dimension that does not
 * exist.
 */
public final class ChartSeries {

    /**
     * One bar: its label, a preformatted value for the direct label, and its length
     * as a percentage of the series maximum.
     *
     * <p>A class with getters rather than a record, because Expression Language in
     * Tomcat 10.1 (EL 5.0) does not read record accessors as properties -
     * {@code ${point.display}} resolves through {@code getDisplay()} only.
     */
    public static final class Point {

        private final String label;
        private final String display;
        private final double percent;
        private final long raw;

        Point(String label, String display, double percent, long raw) {
            this.label = label;
            this.display = display;
            this.percent = percent;
            this.raw = raw;
        }

        public String getLabel() {
            return label;
        }

        public String getDisplay() {
            return display;
        }

        public double getPercent() {
            return percent;
        }

        public long getRaw() {
            return raw;
        }
    }

    private final List<Point> points;
    private final boolean allZero;

    private ChartSeries(List<Point> points, boolean allZero) {
        this.points = points;
        this.allZero = allZero;
    }

    public List<Point> getPoints() {
        return points;
    }

    /**
     * True when every value is zero, so the view can say "no data" instead of
     * drawing a row of flat bars that look like a rendering fault.
     *
     * <p>Deliberately not named {@code isEmpty()}: {@code empty} is a reserved
     * operator in Expression Language, and {@code ${series.empty}} fails to parse.
     */
    public boolean isAllZero() {
        return allZero;
    }

    public static ChartSeries ofCounts(List<ReportDAO.Count> counts) {
        long max = counts.stream().mapToLong(ReportDAO.Count::value).max().orElse(0);

        NumberFormat fmt = NumberFormat.getIntegerInstance(Locale.UK);

        List<Point> points = counts.stream()
                .map(c -> new Point(
                        c.label(),
                        fmt.format(c.value()),
                        percent(c.value(), max),
                        c.value()))
                .toList();

        return new ChartSeries(points, max == 0);
    }

    public static ChartSeries ofMoney(List<ReportDAO.Money> amounts) {
        BigDecimal max = amounts.stream()
                .map(ReportDAO.Money::value)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        List<Point> points = amounts.stream()
                .map(m -> new Point(
                        m.label(),
                        formatMoney(m.value()),
                        percent(m.value(), max),
                        m.value().longValue()))
                .toList();

        return new ChartSeries(points, max.signum() == 0);
    }

    private static double percent(long value, long max) {
        if (max <= 0) {
            return 0;
        }
        return Math.round(value * 1000.0 / max) / 10.0;
    }

    private static double percent(BigDecimal value, BigDecimal max) {
        if (max.signum() <= 0) {
            return 0;
        }
        return value.multiply(BigDecimal.valueOf(100))
                .divide(max, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /** Compact money for a direct label: 1,240 / 12.4K / 1.2M. */
    static String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        long value = amount.setScale(0, RoundingMode.HALF_UP).longValue();

        if (value >= 1_000_000) {
            return trim(value / 1_000_000.0) + "M";
        }
        if (value >= 10_000) {
            return trim(value / 1_000.0) + "K";
        }
        return NumberFormat.getIntegerInstance(Locale.UK).format(value);
    }

    private static String trim(double value) {
        return (value == Math.floor(value))
                ? String.valueOf((long) value)
                : String.format(Locale.UK, "%.1f", value);
    }
}
