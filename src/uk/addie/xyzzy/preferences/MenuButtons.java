package uk.addie.xyzzy.preferences;

import uk.addie.xyzzy.R;
import uk.addie.xyzzy.htmlview.HtmlViewActivity;
import uk.addie.xyzzy.interfaces.IMenuButton;
import android.content.Intent;

enum MenuButtons implements IMenuButton {
  ABOUT {
    @Override public void invoke() {
      final Intent intent = new Intent(PreferencesActivity.activity, HtmlViewActivity.class);
      PreferencesActivity.activity.startActivity(intent);
    }

    @Override public int menuButtonIcon() {
      return R.drawable.ic_action_about;
    }

    @Override public String toString() {
      return "About Xyzzy";
    }
  }
}
