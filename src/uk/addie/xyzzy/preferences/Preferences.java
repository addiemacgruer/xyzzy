
package uk.addie.xyzzy.preferences;

import android.app.Activity;
import android.content.SharedPreferences;

public enum Preferences {
    REPORT_MINOR {
        @Override Object defaultValue() {
            return false;
        }

        @Override public Object getValue(final Activity activity) {
            return getBooleanType(activity);
        }

        @Override public void setValue(final Activity activity, final Object value) {
            setBooleanType(activity, value);
        }
    },
    SCROLL_BACK {
        @Override Object defaultValue() {
            return 50;
        }

        @Override public Object getValue(final Activity activity) {
            return getIntType(activity);
        }

        @Override public void setValue(final Activity activity, final Object value) {
            setIntType(activity, value);
        }
    },
    SOUND_ON {
        @Override Object defaultValue() {
            return true;
        }

        @Override public Object getValue(final Activity activity) {
            return getBooleanType(activity);
        }

        @Override public void setValue(final Activity activity, final Object value) {
            setBooleanType(activity, value);
        }
    },
    TEXT_SIZE {
        @Override Object defaultValue() {
            return 16;
        }

        @Override public Object getValue(final Activity activity) {
            return getIntType(activity);
        }

        @Override public void setValue(final Activity activity, final Object value) {
            setIntType(activity, value);
        }
    },
    USE_COLOUR {
        @Override Object defaultValue() {
            return true;
        }

        @Override public Object getValue(final Activity activity) {
            return getBooleanType(activity);
        }

        @Override public void setValue(final Activity activity, final Object value) {
            setBooleanType(activity, value);
        }
    };
    abstract Object defaultValue();

    Object getBooleanType(final Activity activity) {
        final SharedPreferences xyzzyPrefs = activity.getSharedPreferences("Xyzzy", 0);
        return xyzzyPrefs.getBoolean(toString(), (Boolean) defaultValue());
    }

    Object getIntType(final Activity activity) {
        final SharedPreferences xyzzyPrefs = activity.getSharedPreferences("Xyzzy", 0);
        return xyzzyPrefs.getInt(toString(), (Integer) defaultValue());
    }

    public abstract Object getValue(Activity activity);

    void setBooleanType(final Activity activity, final Object value) {
        final SharedPreferences.Editor sp = activity.getSharedPreferences("Xyzzy", 0).edit();
        sp.putBoolean(toString(), (Boolean) value);
        sp.commit();
    }

    void setIntType(final Activity activity, final Object value) {
        final SharedPreferences.Editor sp = activity.getSharedPreferences("Xyzzy", 0).edit();
        sp.putInt(toString(), (Integer) value);
        sp.commit();
    }

    public abstract void setValue(Activity activity, Object value);
}
