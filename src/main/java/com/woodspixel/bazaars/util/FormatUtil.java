package com.woodspixel.bazaars.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.text.DecimalFormat;

public final class FormatUtil {

    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");

    private FormatUtil() {
    }

    public static Component color(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public static String number(double value) {
        return PRICE_FORMAT.format(value);
    }

    public static Component info(String text) {
        return Component.text(text, NamedTextColor.GRAY);
    }

    public static Component success(String text) {
        return Component.text(text, NamedTextColor.GREEN);
    }

    public static Component error(String text) {
        return Component.text(text, NamedTextColor.RED);
    }
}
