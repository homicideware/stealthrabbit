package org.homicideware.stealthrabbit.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import org.homicideware.stealthrabbit.utils.contract.JSON;

public class SRRepo {

    private static JSONObject mRepo = null;

    public static boolean setRepo(JSONObject repo) {
        if (isValid(repo)) {
            mRepo = repo;
            return true;
        } else {
            return false;
        }
    }

    public static boolean isValid(JSONObject repo) {
        ArrayList<String> keys = new JSON().getKeys(repo);
        for (int i = 0; i < keys.size(); i++) {
            try {
                String obj = repo.getString(keys.get(i));
                JSONObject chroot = new JSONObject(obj);
                if (chroot.has("name") && (chroot.has("url") || chroot.has("file")) && chroot.has("author")) {
                } else
                    return false;
            } catch (JSONException e) {
                return false;
            }
        }
        return true;
    }

    public static ArrayList<String> getMainKeys() {
        ArrayList<String> mainKeys = new ArrayList<String>();
        try {
            for (String key : new JSON().getKeys(mRepo)) {
                String jsonData = mRepo.getString(key);
                JSONObject jsonObj = new JSONObject(jsonData);
                String name = jsonObj.getString("name");
                mainKeys.add(name);
            }
        } catch (JSONException ignored) {
        }
        return mainKeys;
    }

    public static String getKeyData(String key) throws JSONException {
        return mRepo.getString(key);
    }
}
