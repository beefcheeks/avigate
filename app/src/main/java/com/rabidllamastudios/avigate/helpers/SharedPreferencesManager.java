package com.rabidllamastudios.avigate.helpers;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages objects stored via Android's SharedPreferences feature
 * Created by Ryan Staatz on 12/22/15.
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

    /** Adds a new a craft with no configuration. Will replace an existing craft with the same name
     * Use hasCraft() method to determine if there is already an existing craft under the input name
     * @param craftName the unique name of the craft (e.g. Wilga 2000)
     */
    public void addCraft(String craftName) {
        Set<String> craftProfileNames = getCraftList();
        craftProfileNames.add(craftName);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putStringSet(CRAFT_PROFILES, craftProfileNames);
        editor.apply();
    }

    /** Returns the craft configuration for the input craft name
     * @param craftName the unique name of the craft (e.g. Wilga 2000)
     * @return the configuration as a JSON String. Returns null if no configuration found.
     */
    public String getCraftConfiguration(String craftName) {
        if (mSharedPreferences.contains(craftName)) {
            return mSharedPreferences.getString(craftName, null);
        }
        return null;
    }

    /** Returns the list of stored craft profile names */
    public Set<String> getCraftList() {
        //TODO use consistent fallback logic
        if (mSharedPreferences.contains(CRAFT_PROFILES)) {
            return mSharedPreferences.getStringSet(CRAFT_PROFILES, null);
        }
        return new HashSet<>();
    }

    /** Returns true if the input craft name exists in the list of craft profile names */
    public boolean hasCraft(String craftName) {
        Set<String> craftProfileNames = getCraftList();
        return craftProfileNames != null && craftProfileNames.contains(craftName);
    }

    /** Removes the craft name and associated configuration matching the input craft name */
    public void removeCraft(String craftName) {
        Set<String> craftProfileNames = getCraftList();
        SharedPreferences.Editor editor = mSharedPreferences.edit();

        if (craftProfileNames.contains(craftName)) craftProfileNames.remove(craftName);
        editor.remove(CRAFT_PROFILES);
        editor.putStringSet(CRAFT_PROFILES, craftProfileNames);
        editor.remove(craftName);
        editor.apply();
    }

    /** Updates the name of a craft profile
     * @param oldCraftName the original (current) name of the craft
     * @param newCraftName the new (prospective) name of the craft
     */
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

    /** Updates the craft configuration based on the input name and new configuration
     * @param craftName the name of the craft to update the configuration for
     * @param craftConfiguration the JSON String containing the new craft configuration
     */
    public void updateCraftConfiguration(String craftName, String craftConfiguration) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(craftName, craftConfiguration);
        editor.apply();
    }

}
