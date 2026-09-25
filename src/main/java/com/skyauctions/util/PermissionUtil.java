package com.skyauctions.util;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

public final class PermissionUtil {

    private static final String PREFIX = "skyauctions.slots.";

    private PermissionUtil() {
    }

    /**
     * Looks through every effective permission the player has (this
     * plays nicely with LuckPerms groups/inheritance) and returns the
     * highest skyauctions.slots.&lt;number&gt; value found, or the
     * given fallback if none apply.
     */
    public static int getMaxListingSlots(Player player, int fallback) {
        int max = fallback;
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) continue;
            String perm = info.getPermission();
            if (perm.startsWith(PREFIX)) {
                String numberPart = perm.substring(PREFIX.length());
                try {
                    int value = Integer.parseInt(numberPart);
                    if (value > max) {
                        max = value;
                    }
                } catch (NumberFormatException ignored) {
                    // not a numeric slots node, skip it
                }
            }
        }
        return max;
    }
}
