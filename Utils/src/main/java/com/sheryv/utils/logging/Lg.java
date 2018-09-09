package com.sheryv.utils.logging;

public class Lg {
    private Lg() {
    }

    public static void console(String text) {
        System.out.println(ConsoleUtils.parseAndReplaceWithColors(text));
    }

    public static void consoleNoColorize(String text) {
        System.out.println(text);
    }
}
