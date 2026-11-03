package com.ieltsmate;

import com.ieltsmate.util.UITheme;
import org.junit.jupiter.api.Test;

import javax.swing.UIManager;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UIThemeTest {
    @Test
    void installsLightAndDarkFlatLafThemes() {
        String originalTheme = System.getProperty("ieltsmate.theme");
        try {
            System.setProperty("ieltsmate.theme", "dark");
            UITheme.install();
            assertTrue(UIManager.getLookAndFeel().getClass().getName().contains("FlatDarkLaf"));

            System.setProperty("ieltsmate.theme", "light");
            UITheme.install();
            assertTrue(UIManager.getLookAndFeel().getClass().getName().contains("FlatLightLaf"));
        } finally {
            if (originalTheme == null) {
                System.clearProperty("ieltsmate.theme");
            } else {
                System.setProperty("ieltsmate.theme", originalTheme);
            }
            UITheme.install();
        }
    }
}
