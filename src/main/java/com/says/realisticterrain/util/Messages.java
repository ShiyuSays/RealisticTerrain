package com.says.realisticterrain.util;

/** 统一的聊天前缀。 */
public final class Messages {

    public static final String PREFIX = "§8[§6真实地形§8] §7";

    private Messages() {
    }

    public static String prefix(String message) {
        return PREFIX + message;
    }
}
