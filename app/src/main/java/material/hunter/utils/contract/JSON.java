package material.hunter.utils.contract;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class JSON {

    public JSON() {

    }

    public JSONObject getFromWeb(String link) throws JSONException, IOException {
        JSONObject result;
        String content = new Web().getContent(link);
        result = new JSONObject(content);
        return result;
    }

    public ArrayList<String> getKeys(@NonNull JSONObject json) {
        ArrayList<String> result = new ArrayList<String>();
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            result.add(key);
        }
        return result;
    }
}