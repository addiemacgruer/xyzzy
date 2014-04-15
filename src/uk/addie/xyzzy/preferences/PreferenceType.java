
package uk.addie.xyzzy.preferences;

import android.app.Activity;
import android.content.SharedPreferences;

public enum PreferenceType {
    BOOLEAN {
        @Override Object getValue(Activity activity, String setting, Object defaultValue) {
            final SharedPreferences xyzzyPrefs = activity.getSharedPreferences("Xyzzy", 0);
            return xyzzyPrefs.getBoolean(setting, (Boolean) defaultValue);
        }

        @Override void setValue(Activity activity, String setting, Object value) {
            final SharedPreferences.Editor sp = activity.getSharedPreferences("Xyzzy", 0).edit();
            sp.putBoolean(setting, (Boolean) value);
            sp.commit();
        }
    },
    INTEGER {
        @Override Object getValue(Activity activity, String setting, Object defaultValue) {
            final SharedPreferences xyzzyPrefs = activity.getSharedPreferences("Xyzzy", 0);
            return xyzzyPrefs.getInt(setting, (Integer) defaultValue);
        }

        @Override void setValue(Activity activity, String setting, Object value) {
            final SharedPreferences.Editor sp = activity.getSharedPreferences("Xyzzy", 0).edit();
            sp.putInt(setting, (Integer) value);
            sp.commit();
        }
    };
    abstract Object getValue(final Activity activity, String setting, Object defaultValue);

    abstract void setValue(final Activity activity, String setting, Object value);
}
