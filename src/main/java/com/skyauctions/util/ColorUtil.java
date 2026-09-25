package com.skyauctions.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles both legacy (&a, &l ...) and hex (&#RRGGBB) color codes,
 * producing Adventure Components for anything shown to a player
 * (chat, item names, lore, inventory titles, action bar).
 */
public final class ColorUtil {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private ColorUtil() {
    }

    /**
     * Translate a raw string (may contain %placeholders% already replaced)
     * into a colored Component.
     */
    public static Component color(String text) {
        if (text == null) {
            return Component.empty();
        }
        return LEGACY.deserialize(text);
    }

    public static List<Component> colorList(List<String> lines) {
        List<Component> result = new ArrayList<>();
        if (lines == null) {
            return result;
        }
        for (String line : lines) {
            result.add(color(line));
        }
        return result;
    }

    /**
     * Strip all color codes - useful for plain text placeholders.
     */
    public static String stripColor(String text) {
        if (text == null) {
            return "";
        }
        return PlainTextComponentSerializer.plainText().serialize(color(text));
    }

    /** Plain text (no color codes at all) from an already-built Component. */
    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
