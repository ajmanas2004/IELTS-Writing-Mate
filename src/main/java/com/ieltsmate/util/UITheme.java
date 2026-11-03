package com.ieltsmate.util;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.UIManager;

public final class UITheme {
    private UITheme() {
    }

    public static void install() {
        String theme = System.getProperty("ieltsmate.theme", "light");
        boolean dark = "dark".equalsIgnoreCase(theme);
        boolean installed = dark
                ? FlatDarkLaf.setup() : FlatLightLaf.setup();
        if (!installed) {
            throw new IllegalStateException("Could not install FlatLaf theme");
        }
        UIManager.put("Button.arc", 14);
        UIManager.put("Component.arc", 12);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("Table.rowHeight", 34);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.alternateRowColor", dark
                ? new java.awt.Color(0x262B33) : new java.awt.Color(0xF3F6FA));
        UIManager.put("TableHeader.height", 38);
        UIManager.put("Button.margin", new java.awt.Insets(8, 14, 8, 14));
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("TabbedPane.tabHeight", 38);
    }
}
