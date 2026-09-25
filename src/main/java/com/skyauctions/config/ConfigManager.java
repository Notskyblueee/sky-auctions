package com.skyauctions.config;

import com.skyauctions.SkyAuctions;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final SkyAuctions plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File messagesFile;

    public ConfigManager(SkyAuctions plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);

        // Fill in any new keys shipped in a plugin update without wiping edits
        try (InputStream defStream = plugin.getResource("messages.yml")) {
            if (defStream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defStream, StandardCharsets.UTF_8));
                messages.setDefaults(defaults);
                messages.options().copyDefaults(true);
                messages.save(messagesFile);
            }
        } catch (IOException ignored) {
            // Non-fatal - messages will just use in-memory defaults
        }
    }

    public void reload() {
        load();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getPrefix() {
        return messages.getString("prefix", "&7[&#FFCB6C&lSkyAuctions&7] ");
    }

    /**
     * Fetches a raw message string by dotted path (e.g. "sell.success"),
     * replaces %prefix% and any extra placeholders, and returns it colored.
     */
    public Component msg(String path, Map<String, String> placeholders) {
        String raw = messages.getString(path, path);
        raw = raw.replace("%prefix%", getPrefix());
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                raw = raw.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return com.skyauctions.util.ColorUtil.color(raw);
    }

    public Component msg(String path) {
        return msg(path, new HashMap<>());
    }

    public String rawMsg(String path) {
        return messages.getString(path, path);
    }
}
