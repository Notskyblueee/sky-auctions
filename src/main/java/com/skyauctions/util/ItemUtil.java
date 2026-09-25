package com.skyauctions.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Serializes ItemStacks to/from a Base64 string so they can be safely
 * stored inside a YAML file.
 */
public final class ItemUtil {

    private ItemUtil() {
    }

    public static String toBase64(ItemStack item) {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOut = new BukkitObjectOutputStream(byteOut)) {
            dataOut.writeObject(item);
            return Base64.getEncoder().encodeToString(byteOut.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to serialize item stack", e);
        }
    }

    public static ItemStack fromBase64(String data) {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream dataIn = new BukkitObjectInputStream(byteIn)) {
            return (ItemStack) dataIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Unable to deserialize item stack", e);
        }
    }

    /**
     * Human readable name for an item, using its custom display name
     * when set, otherwise a prettified material name.
     */
    public static String niceName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return ColorUtil.plain(item.getItemMeta().displayName());
        }
        String raw = item.getType().name().toLowerCase().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
            }
        }
        return sb.toString().trim();
    }
}
