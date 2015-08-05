package uk.addie.xyzzy.preferences;

import java.util.HashMap;
import java.util.Map;

import uk.addie.xyzzy.R;
import uk.addie.xyzzy.util.Utility;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

public class PreferencesActivity extends Activity {
  int textSize = 16;

  private MenuItem[] mis;

  private final Map<Preferences, View> selectionMap = new HashMap<Preferences, View>();

  @SuppressLint("NewApi") @Override public boolean onCreateOptionsMenu(final Menu menu) {
    Log.d("Control", "OCOM");
    mis = new MenuItem[MenuButtons.values().length];
    int i = 0;
    for (final MenuButtons mb : MenuButtons.values()) {
      final MenuItem nextMenu = menu.add(mb.toString());
      if (mb.menuButtonIcon() != -1) {
        Log.d("Xyzzy", "Menu icon:" + mb.menuButtonIcon());
        nextMenu.setIcon(mb.menuButtonIcon());
        nextMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
      }
      mis[i++] = nextMenu;
    }
    return true;
  }

  @Override public boolean onOptionsItemSelected(final MenuItem item) {
    if (mis == null) { // then we've not initialised?
      Log.e("Xyzzy", "Android onOptionsItemSelected before onCreateOptionsMenu?");
      return false;
    }
    final int selectedMenu = Utility.arrayOffsetOf(item, mis);
    MenuButtons.values()[selectedMenu].invoke();
    return true;
  }

  @Override protected void onCreate(final Bundle savedInstanceState) {
    activity = this;
    textSize = (Integer) Preferences.TEXT_SIZE.getValue(this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.preferences);
    setTitle("Xyzzy Preferences");
    final LinearLayout ll = (LinearLayout) findViewById(R.id.preferences);
    for (final Preferences p : Preferences.values()) {
      View view = null;
      Log.d("Xyzzy", "Preference:" + p);
      switch (p.type) {
      case BOOLEAN:
        view = new BooleanValueView(this, p);
        break;
      case INTEGER:
        final ScrollValueView svv = new ScrollValueView(this, p);
        view = svv;
        break;
      default:
        continue;
      }
      ll.addView(view);
      selectionMap.put(p, view);
    }
  }

  static PreferencesActivity activity;
}
