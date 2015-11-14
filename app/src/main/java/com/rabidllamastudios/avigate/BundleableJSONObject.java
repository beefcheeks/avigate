package com.rabidllamastudios.avigate;

import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Set;

/**
 * BundleableJSONObject is identical to a JSONObject, except it can convert between JSON and bundle
 * Created by Ryan on 11/12/15.
 */
public class BundleableJSONObject extends JSONObject {

    public BundleableJSONObject(String json) throws JSONException {
        super(json);
    }

    public BundleableJSONObject(Bundle bundle) {
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            try {
                this.put(key, JSONObject.wrap(bundle.get(key)));
            } catch (JSONException e) {
                //TODO Handle exception here
            }
        }
    }

    //TODO find less awful method if there is one
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        Iterator<String> keys = this.keys();
        while (keys.hasNext()) {
            try {
                String key = keys.next();
                Object value = this.get(key);
                if (value instanceof String)
                    bundle.putString(key, (String) value);
                else if (value instanceof Boolean)
                    bundle.putBoolean(key, (Boolean) value);
                else if (value instanceof Integer)
                    bundle.putInt(key, (Integer) value);
                else if (value instanceof Long)
                    bundle.putLong(key, (Long) value);
                else if (value instanceof Float)
                    bundle.putFloat(key, (Float) value);
                else if (value instanceof Double)
                    bundle.putDouble(key, (Double) value);
            } catch (JSONException e) {
                //TODO Handle exception here
            }
        }
        return bundle;
    }
}
