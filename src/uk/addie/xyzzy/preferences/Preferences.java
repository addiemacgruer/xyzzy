
package uk.addie.xyzzy.preferences;

import static uk.addie.xyzzy.preferences.PreferenceType.*;
import android.app.Activity;

public enum Preferences {
    TEXT_SIZE(INTEGER) {
        @Override Object defaultValue() {
            return 16;
        }

        @Override int max() {
            return 48;
        }

        @Override int min() {
            return 6;
        }

        @Override String prefDescription() {
            return "Display text size (pt)";
        }
    },
    SOUND_ON(BOOLEAN) {
        @Override Object defaultValue() {
            return true;
        }

        @Override String prefDescription() {
            return "Enable sound effects";
        }
    },
    USE_COLOUR(BOOLEAN) {
        @Override Object defaultValue() {
            return true;
        }

        @Override String prefDescription() {
            return "Enable colour";
        }
    },
    UPPER_SCREENS_ARE_MONOSPACED(BOOLEAN) {
        @Override Object defaultValue() {
            return true;
        }

        @Override String prefDescription() {
            return "Upper screens are monospaced";
        }
    },
    REPORT_MINOR(BOOLEAN) {
        @Override Object defaultValue() {
            return false;
        }

        @Override String prefDescription() {
            return "Report minor errors in story files";
        }
    },
    SCROLL_BACK(INTEGER) {
        @Override Object defaultValue() {
            return 50;
        }

        @Override int max() {
            return 100;
        }

        @Override int min() {
            return 5;
        }

        @Override String prefDescription() {
            return "Length of scroll back (screens)";
        }
    },
    AUTOSCROLL_SPEED(INTEGER) {
        @Override Object defaultValue() {
            return 3;
        }

        @Override int max() {
            return 10;
        }

        @Override int min() {
            return 0;
        }

        @Override String prefDescription() {
            return "Speed of automatic scrolling";
        }
    },
    TWENTYFOURHOUR(BOOLEAN) {
        @Override Object defaultValue() {
            return true;
        }

        @Override String prefDescription() {
            return "Use 24-hour clock (in V3 games with time status lines)";
        }
    },
    PIRACY(BOOLEAN) {
        @Override Object defaultValue() {
            return false;
        }

        @Override String prefDescription() {
            return "Run pirated story files (yarr!)";
        }
    },
    MONITOR_PERFORMANCE(BOOLEAN) {
        @Override Object defaultValue() {
            return false;
        }

        @Override String prefDescription() {
            return "Monitor opcounts and performance";
        }
    };
    public final PreferenceType type;

    private Preferences(PreferenceType type) {
        this.type = type;
    }

    abstract Object defaultValue();

    public Object getValue(Activity activity) {
        return type.getValue(activity, toString(), defaultValue());
    }

    @SuppressWarnings("static-method") int max() {
        return 100;
    }

    @SuppressWarnings("static-method") int min() {
        return 0;
    }

    String prefDescription() {
        return toString();
    }

    public void setValue(Activity activity, Object value) {
        type.setValue(activity, toString(), value);
    }
}
