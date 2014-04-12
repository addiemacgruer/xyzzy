
package uk.addie.xyzzy.gameselection;

import uk.addie.xyzzy.MenuButton;
import uk.addie.xyzzy.R;
import uk.addie.xyzzy.preferences.PreferencesActivity;
import android.content.Intent;

enum MenuButtons implements MenuButton {
    ABOUT {
        @Override public void invoke() {
            // TODO about this app
        }

        @Override public int menuButtonIcon() {
            return R.drawable.ic_action_about;
        }

        @Override public String toString() {
            return "About Xyzzy";
        }
    },
    HELP {
        @Override public void invoke() {
            // TODO help with this app
        }

        @Override public int menuButtonIcon() {
            return R.drawable.ic_action_help;
        }

        @Override public String toString() {
            return "Help";
        }
    },
    SETTINGS {
        @Override public void invoke() {
            Intent intent = new Intent(SelectionActivity.activity, PreferencesActivity.class);
            SelectionActivity.activity.startActivity(intent);
        }

        @Override public int menuButtonIcon() {
            return R.drawable.ic_action_settings;
        }

        @Override public String toString() {
            return "Settings";
        }
    };
}
