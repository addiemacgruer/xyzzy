
package uk.addie.xyzzy.preferences;

import android.app.Activity;
import android.content.SharedPreferences;

public enum Preferences {
    TEXT_SIZE {
        @Override Object defaultValue() {
            return 16;
        }

        @Override Object getValue(Activity activity) {
            return getIntType(activity);
        }

        @Override void setValue(Activity activity, Object value) {
            setIntType(activity, value);
        }
    },
    SOUND_ON {
        @Override Object defaultValue() {
            return true;
        }

        @Override Object getValue(Activity activity) {
            return getBooleanType(activity);
        }

        @Override void setValue(Activity activity, Object value) {
            setBooleanType(activity, value);
        }
    };
    abstract Object defaultValue();

    Object getBooleanType(Activity activity) {
        SharedPreferences xyzzyPrefs = activity.getSharedPreferences("Xyzzy", 0);
        return xyzzyPrefs.getBoolean(toString(), (Boolean) defaultValue());
    }

    Object getIntType(Activity activity) {
        SharedPreferences xyzzyPrefs = activity.getSharedPreferences("Xyzzy", 0);
        return xyzzyPrefs.getInt(toString(), (Integer) defaultValue());
    }

    abstract Object getValue(Activity activity);

    void setBooleanType(Activity activity, Object value) {
        SharedPreferences.Editor sp = activity.getSharedPreferences("Xyzzy", 0).edit();
        sp.putBoolean(toString(), (Boolean) value);
        sp.commit();
    }

    void setIntType(Activity activity, Object value) {
        SharedPreferences.Editor sp = activity.getSharedPreferences("Xyzzy", 0).edit();
        sp.putInt(toString(), (Integer) value);
        sp.commit();
    }

    abstract void setValue(Activity activity, Object value);
}
