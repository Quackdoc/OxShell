package com.OxGames.OxShell.Data;

import android.graphics.Typeface;
import android.view.KeyEvent;

import com.OxGames.OxShell.Helpers.AndroidHelpers;
import com.OxGames.OxShell.Helpers.Serialaver;

import java.util.HashMap;

public class SettingsKeeper {
    public static final String TIMES_LOADED = "times_loaded";

    public static final String SUPER_PRIMARY_INPUT = "super_primary_input";
    public static final String PRIMARY_INPUT = "primary_input";
    public static final String SECONDARY_INPUT = "secondary_input";
    public static final String CANCEL_INPUT = "cancel_input";
    public static final String HOME_COMBOS = "home_combos";
    public static final String RECENTS_COMBOS = "recents_combos";
    public static final String EXPLORER_HIGHLIGHT_INPUT = "explorer_highlight_input";
    public static final String EXPLORER_GO_UP_INPUT = "explorer_go_up_input";
    public static final String EXPLORER_GO_BACK_INPUT = "explorer_go_back_input";
    public static final String EXPLORER_EXIT_INPUT = "explorer_exit_input";
    public static final String NAVIGATE_UP = "navigate_up";
    public static final String NAVIGATE_DOWN = "navigate_down";
    public static final String NAVIGATE_LEFT = "navigate_left";
    public static final String NAVIGATE_RIGHT = "navigate_right";

    public static final String HOME_ITEM_SCALE = "home_item_scale";
    public static final String HOME_SELECTION_ALPHA = "home_selection_alpha";
    public static final String HOME_NON_SELECTION_ALPHA = "home_non_selection_alpha";
    public static final String HOME_BEHIND_INNER_ALPHA = "home_behind_inner_alpha";
    public static final String FONT_REF = "font_ref";
    public static final String VERSION_CODE = "version_code";

    private static boolean fileDidExist;
    private static HashMap<String, Object> settingsCache;

    public static boolean fileDidNotExist() {
        return !fileDidExist;
    }
    public static void loadOrCreateSettings() {
        load();
        if (settingsCache == null) {
            settingsCache = new HashMap<>();
            SettingsKeeper.setValueAndSave(TIMES_LOADED, 1);
        } else {
            fileDidExist = true;
            if (SettingsKeeper.hasValue(TIMES_LOADED)) {
                int timesLoaded = (Integer)SettingsKeeper.getValue(TIMES_LOADED); // even if saved as Integer for some reason it comes back as Double
                SettingsKeeper.setValueAndSave(TIMES_LOADED, timesLoaded + 1);
            }
        }
    }
    public static void load() {
        if (AndroidHelpers.fileExists(Paths.SETTINGS_INTERNAL_PATH))
            settingsCache = (HashMap<String, Object>)Serialaver.loadFromFSTJSON(Paths.SETTINGS_INTERNAL_PATH);
    }
    public static void save() {
        if (!AndroidHelpers.fileExists(Paths.SETTINGS_INTERNAL_PATH))
            AndroidHelpers.makeFile(Paths.SETTINGS_INTERNAL_PATH);
        Serialaver.saveAsFSTJSON(settingsCache, Paths.SETTINGS_INTERNAL_PATH);
    }
    public static void setValueAndSave(String key, Object value) {
        setValue(key, value);
        save();
    }
    public static void setValue(String key, Object value) {
        if (settingsCache == null)
            loadOrCreateSettings();
        settingsCache.put(key, value);
    }
    public static Object getValue(String key) {
        if (settingsCache == null)
            loadOrCreateSettings();
        return settingsCache.get(key);
    }
    public static boolean hasValue(String key) {
        if (settingsCache == null)
            loadOrCreateSettings();
        return settingsCache.containsKey(key);
    }

    public static Typeface getFont() {
        if (hasValue(FONT_REF))
            return ((FontRef)getValue(FONT_REF)).getFont();
        return null;
    }
    public static KeyCombo[] getSuperPrimaryInput() {
        // create default if not existing
        if (!hasValue(SUPER_PRIMARY_INPUT))
            setValue(SUPER_PRIMARY_INPUT, new KeyCombo[] { KeyCombo.createUpCombo(KeyEvent.KEYCODE_BUTTON_START), KeyCombo.createUpCombo(true, KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_ENTER), KeyCombo.createUpCombo(true, KeyEvent.KEYCODE_CTRL_RIGHT, KeyEvent.KEYCODE_ENTER), KeyCombo.createUpCombo(true, KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_NUMPAD_ENTER), KeyCombo.createUpCombo(true, KeyEvent.KEYCODE_CTRL_RIGHT, KeyEvent.KEYCODE_NUMPAD_ENTER) });

        return ((KeyCombo[])getValue(SUPER_PRIMARY_INPUT));
    }
    public static KeyCombo[] getPrimaryInput() {
        // create default if not existing
        if (!hasValue(PRIMARY_INPUT))
            setValue(PRIMARY_INPUT, new KeyCombo[] { KeyCombo.createUpCombo(KeyEvent.KEYCODE_BUTTON_A), KeyCombo.createUpCombo(KeyEvent.KEYCODE_ENTER), KeyCombo.createUpCombo(KeyEvent.KEYCODE_NUMPAD_ENTER) });

        return ((KeyCombo[])getValue(PRIMARY_INPUT));
    }
    public static KeyCombo[] getSecondaryInput() {
        // create default if not existing
        if (!hasValue(SECONDARY_INPUT))
            setValue(SECONDARY_INPUT, new KeyCombo[] { KeyCombo.createUpCombo(KeyEvent.KEYCODE_BUTTON_Y), KeyCombo.createUpCombo(KeyEvent.KEYCODE_MENU), KeyCombo.createUpCombo(KeyEvent.KEYCODE_SPACE) });

        return ((KeyCombo[])getValue(SECONDARY_INPUT));
    }
    public static KeyCombo[] getExplorerHighlightInput() {
        // create default if not existing
        if (!hasValue(EXPLORER_HIGHLIGHT_INPUT))
            setValue(EXPLORER_HIGHLIGHT_INPUT, new KeyCombo[] { KeyCombo.createDownCombo(0, KeyCombo.defaultRepeatStartDelay, KeyCombo.defaultRepeatTime, KeyEvent.KEYCODE_BUTTON_X) });

        return ((KeyCombo[])getValue(EXPLORER_HIGHLIGHT_INPUT));
    }
    public static KeyCombo[] getExplorerGoUpInput() {
        // create default if not existing
        if (!hasValue(EXPLORER_GO_UP_INPUT))
            setValue(EXPLORER_GO_UP_INPUT, new KeyCombo[] { KeyCombo.createUpCombo(KeyEvent.KEYCODE_BUTTON_B) });

        return ((KeyCombo[])getValue(EXPLORER_GO_UP_INPUT));
    }
    public static KeyCombo[] getExplorerGoBackInput() {
        // create default if not existing
        if (!hasValue(EXPLORER_GO_BACK_INPUT))
            setValue(EXPLORER_GO_BACK_INPUT, new KeyCombo[] { KeyCombo.createUpCombo(true, KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_B) });

        return ((KeyCombo[])getValue(EXPLORER_GO_BACK_INPUT));
    }
    public static KeyCombo[] getExplorerExitInput() {
        // create default if not existing
        if (!hasValue(EXPLORER_EXIT_INPUT))
            setValue(EXPLORER_EXIT_INPUT, new KeyCombo[] { KeyCombo.createUpCombo(false, KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1), KeyCombo.createUpCombo(KeyEvent.KEYCODE_BACK) });

        return ((KeyCombo[])getValue(EXPLORER_EXIT_INPUT));
    }
    public static KeyCombo[] getCancelInput() {
        // create default if not existing
        if (!hasValue(CANCEL_INPUT))
            setValue(CANCEL_INPUT, new KeyCombo[] { KeyCombo.createUpCombo(KeyEvent.KEYCODE_BUTTON_B), KeyCombo.createUpCombo(KeyEvent.KEYCODE_BACK), KeyCombo.createUpCombo(KeyEvent.KEYCODE_ESCAPE) });

        return ((KeyCombo[])getValue(CANCEL_INPUT));
    }
    public static KeyCombo[] getHomeCombos() {
        // create default if not existing
        if (!hasValue(HOME_COMBOS))
            setValue(HOME_COMBOS, new KeyCombo[] { KeyCombo.createUpCombo(true, KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_B) });

        return ((KeyCombo[])getValue(HOME_COMBOS));
    }
    public static KeyCombo[] getRecentsCombos() {
        // create default if not existing
        if (!hasValue(RECENTS_COMBOS))
            setValue(RECENTS_COMBOS, new KeyCombo[] { KeyCombo.createUpCombo(true, KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_X) });

        return ((KeyCombo[])getValue(RECENTS_COMBOS));
    }
    public static KeyCombo[] getNavigateUp() {
        // create default if not existing
        if (!hasValue(NAVIGATE_UP))
            setValue(NAVIGATE_UP, new KeyCombo[] { KeyCombo.createDownCombo(0, KeyCombo.defaultRepeatStartDelay, KeyCombo.defaultRepeatTime, KeyEvent.KEYCODE_DPAD_UP) });

        return ((KeyCombo[])getValue(NAVIGATE_UP));
    }
    public static KeyCombo[] getNavigateDown() {
        // create default if not existing
        if (!hasValue(NAVIGATE_DOWN))
            setValue(NAVIGATE_DOWN, new KeyCombo[] { KeyCombo.createDownCombo(0, KeyCombo.defaultRepeatStartDelay, KeyCombo.defaultRepeatTime, KeyEvent.KEYCODE_DPAD_DOWN) });

        return ((KeyCombo[])getValue(NAVIGATE_DOWN));
    }
    public static KeyCombo[] getNavigateLeft() {
        // create default if not existing
        if (!hasValue(NAVIGATE_LEFT))
            setValue(NAVIGATE_LEFT, new KeyCombo[] { KeyCombo.createDownCombo(0, KeyCombo.defaultRepeatStartDelay, KeyCombo.defaultRepeatTime, KeyEvent.KEYCODE_DPAD_LEFT) });

        return ((KeyCombo[])getValue(NAVIGATE_LEFT));
    }
    public static KeyCombo[] getNavigateRight() {
        // create default if not existing
        if (!hasValue(NAVIGATE_RIGHT))
            setValue(NAVIGATE_RIGHT, new KeyCombo[] { KeyCombo.createDownCombo(0, KeyCombo.defaultRepeatStartDelay, KeyCombo.defaultRepeatTime, KeyEvent.KEYCODE_DPAD_RIGHT) });

        return ((KeyCombo[])getValue(NAVIGATE_RIGHT));
    }
}
