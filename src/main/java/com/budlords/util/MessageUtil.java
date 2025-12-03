package com.budlords.util;

import org.bukkit.ChatColor;

public class MessageUtil {

    private MessageUtil() {
        // Utility class
    }

    public static String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String stripColor(String message) {
        if (message == null) return "";
        return ChatColor.stripColor(message);
    }

    public static String formatMoney(double amount, String currencySymbol) {
        return currencySymbol + String.format("%.2f", amount);
    }

    public static String createProgressBar(double progress, int length, String filledChar, String emptyChar) {
        int filled = (int) (progress * length);
        StringBuilder bar = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            if (i < filled) {
                bar.append(filledChar);
            } else {
                bar.append(emptyChar);
            }
        }
        
        return bar.toString();
    }
}
