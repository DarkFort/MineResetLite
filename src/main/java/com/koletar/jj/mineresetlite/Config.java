package com.koletar.jj.mineresetlite;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author jjkoletar
 */
public class Config {
    private Config() {}
    private static boolean broadcastInWorldOnly = false;
    private static boolean broadcastNearbyOnly = false;
    private static boolean checkForUpdates = true;
    private static String locale = "en";

    static boolean getBroadcastInWorldOnly() {
        return broadcastInWorldOnly;
    }

    static boolean getBroadcastNearbyOnly() {
        return broadcastNearbyOnly;
    }

    private static void setBroadcastInWorldOnly(boolean broadcastInWorldOnly) {
        Config.broadcastInWorldOnly = broadcastInWorldOnly;
    }

    private static void setBroadcastNearbyOnly(boolean broadcastNearbyOnly) {
        Config.broadcastNearbyOnly = broadcastNearbyOnly;
    }

    private static void writeBroadcastInWorldOnly(BufferedWriter out) throws IOException {
        out.write("# Если у вас есть несколько миров и желаете, чтобы предупреждения об автоматической перезагрузке");
        out.newLine();
        out.write("# были видны только в том мире, где находится шахта, тогда установите это значение true.");
        out.newLine();
        out.write("broadcast-in-world-only: false");
        out.newLine();
    }

    private static void writeBroadcastNearbyOnly(BufferedWriter out) throws IOException {
        out.write("# Если вы хотите чтобы только игроки, которые находятся возле шахты получали,");
        out.newLine();
        out.write("# сообщения об перезагрузке шахты, тогда установите это значение true. Заметка: В настоящее время сообщения видно только игрокам в шахте");
        out.newLine();
        out.write("broadcast-nearby-only: false");
        out.newLine();
    }

    public static boolean getCheckForUpdates() {
        return checkForUpdates;
    }

    private static void setCheckForUpdates(boolean checkForUpdates) {
        Config.checkForUpdates = checkForUpdates;
    }

    private static void writeCheckForUpdates(BufferedWriter out) throws IOException {
        out.write("# Когда true, эта опция конфигурации включает оповещения об обновлениях. Я не посылаю никакой дополнительной информации, если ");
        out.newLine();
        out.write("# файл размещен на Dropbox. ");
        out.newLine();
        out.write("check-for-updates: false");
        out.newLine();
    }

    static String getLocale() {
        return locale;
    }

    private static void setLocale(String locale) {
        Config.locale = locale;
    }

    private static void writeLocale(BufferedWriter out) throws IOException {
        out.write("# Поддрежка разных языков MineResetLite. Укажите язык, используемый здесь.");
        out.newLine();
        out.write("# Доступные языки: Danish (спасибо Beijiru), Spanish (спасибо enetocs), Portuguese (спасибо FelipeMarques14), Italian (спасибо JoLong))");
        out.newLine();
        out.write("# Используйте следующие значения для этих языков: English: 'en', Danish: 'da', Spanish: 'es', Portuguese: 'pt', Italian: 'it', French: 'fr', Dutch: 'nl', Polish: 'pl'");
        out.newLine();
        out.write("# Список языков можно найти здесь http://dev.bukkit.org/server-mods/mineresetlite/pages/internationalization/");
        out.newLine();
        out.write("locale: en");
        out.newLine();
    }

    static void initConfig(File dataFolder) throws IOException {
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }
        File configFile = new File(dataFolder, "config.yml");
        if (!configFile.exists()) {
            configFile.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(configFile));
            out.write("# Конфигурационный файл MineResetLite");
            out.newLine();
            Config.writeBroadcastInWorldOnly(out);
            Config.writeBroadcastNearbyOnly(out);
            Config.writeCheckForUpdates(out);
            Config.writeLocale(out);
            out.close();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        BufferedWriter out = new BufferedWriter(new FileWriter(configFile, true));
        if (config.contains("broadcast-in-world-only")) {
            Config.setBroadcastInWorldOnly(config.getBoolean("broadcast-in-world-only"));
        } else {
            Config.writeBroadcastInWorldOnly(out);
        }
        if (config.contains("broadcast-nearby-only")) {
            Config.setBroadcastNearbyOnly(config.getBoolean("broadcast-nearby-only"));
        } else {
            Config.writeBroadcastNearbyOnly(out);
        }
        if (config.contains("check-for-updates")) {
            Config.setCheckForUpdates(config.getBoolean("check-for-updates"));
        } else {
            Config.writeCheckForUpdates(out);
        }
        if (config.contains("locale")) {
            Config.setLocale(config.getString("locale"));
        } else {
            Config.writeLocale(out);
        }
        out.close();
    }
}
