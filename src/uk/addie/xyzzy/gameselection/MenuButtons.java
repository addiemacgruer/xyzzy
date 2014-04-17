
package uk.addie.xyzzy.gameselection;

import uk.addie.xyzzy.R;
import uk.addie.xyzzy.htmlview.HtmlViewActivity;
import uk.addie.xyzzy.interfaces.IMenuButton;
import uk.addie.xyzzy.preferences.PreferencesActivity;
import android.content.Intent;

public enum MenuButtons implements IMenuButton {
    ABOUT {
        @Override public void invoke() {
            final Intent intent = new Intent(SelectionActivity.activity, HtmlViewActivity.class);
            SelectionActivity.activity.startActivity(intent);
        }

        @Override public int menuButtonIcon() {
            return R.drawable.ic_action_about;
        }

        @Override public String toString() {
            return "About Xyzzy";
        }
    },
    SETTINGS {
        @Override public void invoke() {
            final Intent intent = new Intent(SelectionActivity.activity, PreferencesActivity.class);
            SelectionActivity.activity.startActivity(intent);
        }

        @Override public int menuButtonIcon() {
            return R.drawable.ic_action_settings;
        }

        @Override public String toString() {
            return "Settings";
        }
    },
    ADD_ANOTHER {
        @Override public void invoke() {
            SelectionActivity.activity.showFileChooser();
        }

        @Override public int menuButtonIcon() {
            return 0;
        }

        @Override public String toString() {
            return "Add another";
        }
    }
}
