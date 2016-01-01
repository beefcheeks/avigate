package com.rabidllamastudios.avigate.helpers;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Ryan on 12/22/15.
 * Manages objects stored via Android's SharedPreferences feature
 */
public class SharedPreferencesManager {
    public static final String KEY_CRAFT_NAME = "CraftName";

    private static final String CRAFT_PROFILES ="CraftProfiles";
    private static final String PREFS_FILE = "AvigatePreferences";

    private SharedPreferences mSharedPreferences;

    public SharedPreferencesManager(Context context) {
        mSharedPreferences = context.getSharedPreferences(PREFS_FILE,
                Context.MODE_PRIVATE);
    }

    //Use hasCraft method before calling this one
    public void addCraft(String craftName) {
        Set<String> craftProfileNames = getCraftList();
        craftProfileNames.add(craftName);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putStringSet(CRAFT_PROFILES, craftProfileNames);
        editor.apply();
    }

    public String getCraftConfiguration(String craftName) {
        if (mSharedPreferences.contains(craftName)) {
            return mSharedPreferences.getString(craftName, null);
        }
        return null;
    }

    public Set<String> getCraftList() {
        if (mSharedPreferences.contains(CRAFT_PROFILES)) {
            return mSharedPreferences.getStringSet(CRAFT_PROFILES, null);
        }
        return new HashSet<>();
    }

    public boolean hasCraft(String craftName) {
        Set<String> craftProfileNames = getCraftList();
        return craftProfileNames != null && craftProfileNames.contains(craftName);
    }

    public void removeCraft(String craftName) {
        Set<String> craftProfileNames = getCraftList();
        SharedPreferences.Editor editor = mSharedPreferences.edit();

        if (craftProfileNames.contains(craftName)) craftProfileNames.remove(craftName);
        editor.remove(CRAFT_PROFILES);
        editor.putStringSet(CRAFT_PROFILES, craftProfileNames);
        editor.remove(craftName);
        editor.apply();
    }

    public void updateCraftName(String oldCraftName, String newCraftName) {
        Set<String> craftProfileNames = getCraftList();
        SharedPreferences.Editor editor = mSharedPreferences.edit();

        if (craftProfileNames.contains(oldCraftName)) craftProfileNames.remove(oldCraftName);
        craftProfileNames.add(newCraftName);
        editor.remove(CRAFT_PROFILES);
        editor.putStringSet(CRAFT_PROFILES, craftProfileNames);

        String configuration = mSharedPreferences.getString(oldCraftName, null);
        editor.remove(oldCraftName);
        editor.putString(newCraftName, configuration);
        editor.apply();
    }

    public void updateCraftConfiguration(String craftName, String craftConfiguration) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(craftName, craftConfiguration);
        editor.apply();
    }

}
