package uk.addie.xyzzy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import uk.addie.xyzzy.gameselection.SelectionActivity;
import uk.addie.xyzzy.preferences.Preferences;
import uk.addie.xyzzy.state.Memory;
import uk.addie.xyzzy.util.Utility;
import uk.addie.xyzzy.zmachine.Decoder;
import uk.addie.xyzzy.zobjects.ZWindow;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

/** Main android activity.
 *
 * @author addie */
public class MainActivity extends Activity {
  private MenuItem[] mis;

  int textSize;

  public void addEditView(final int foreground, final int background, final int windowId) {
    runOnUiThread(new Runnable() {
      @Override public void run() {
        final TextView tv = formattedEditText(background, foreground);
        tv.setOnKeyListener(okl);
        tv.setTag(windowId);
        MainActivity.activity.addView(tv, windowId);
      }
    });
  }

  public void addTextView(final SpannableStringBuilder ssb, final int foreground,
      final int background, final int windowId) {
    runOnUiThread(new Runnable() {
      @Override public void run() {
        final TextView tv = textView(foreground, background);
        tv.setText(ssb);
        tv.setTag(windowId);
        MainActivity.activity.addView(tv, windowId);
      }
    });
  }

  @SuppressLint("NewApi") @Override public boolean onCreateOptionsMenu(final Menu menu) {
    mis = new MenuItem[MenuButtons.values().length];
    int i = 0;
    for (final MenuButtons mb : MenuButtons.values()) {
      final MenuItem nextMenu = menu.add(mb.toString());
      if (mb.menuButtonIcon() != -1) {
        nextMenu.setIcon(mb.menuButtonIcon());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
          nextMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
      }
      mis[i++] = nextMenu;
    }
    return true;
  }

  @Override public boolean onKeyDown(final int keyCode, final KeyEvent event) {
    Log.d("Xyzzy", "onKeyDown:" + keyCode + " :" + event);
    if (event == null) { // ie. it's synthetic
      synchronized (inputSyncObject) {
        inputSyncObject.setCharacter(keyCode);
        inputSyncObject.notifyAll();
      }
      return true;
    }
    switch (keyCode) {
    case KeyEvent.KEYCODE_BACK:
    case KeyEvent.KEYCODE_MENU:
    case KeyEvent.KEYCODE_DEL:
    case KeyEvent.KEYCODE_ENTER:
      return false;
    default:
      synchronized (inputSyncObject) {
        inputSyncObject.setCharacter(event.getUnicodeChar());
        inputSyncObject.notifyAll();
      }
      return true;
    }
  }

  @Override public boolean onOptionsItemSelected(final MenuItem item) {
    if (mis == null) { // then we've not initialised?
      Log.e("Xyzzy", "Android onOptionsItemSelected before onCreateOptionsMenu?");
      return false;
    }
    final int selected = Utility.arrayOffsetOf(item, mis);
    MenuButtons.values()[selected].invoke();
    return true;
  }

  public void removeChildren(final int viewId) {
    runOnUiThread(new Runnable() {
      @Override public void run() {
        final LinearLayout ll = (LinearLayout) MainActivity.activity.findViewById(viewId);
        ll.removeAllViews();
        synchronized (textBoxes) {
          for (final Iterator<View> it = textBoxes.iterator(); it.hasNext();) {
            final View v = it.next();
            if ((Integer) v.getTag() == viewId) {
              it.remove();
            }
          }
        }
      }
    });
  }

  public void setBackgroundColour(final int colour) {
    runOnUiThread(new Runnable() {
      @Override public void run() {
        final RelativeLayout rl = (RelativeLayout) MainActivity.activity.findViewById(R.id.screen);
        rl.setBackgroundColor(colour);
      }
    });
  }

  @Override protected void onCreate(final Bundle savedInstanceState) {
    Log.d("Xyzzy", "MainActivity onCreate");
    getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    activity = this;
    getScreenSize();
    final String story = getStorySelection();
    super.onCreate(savedInstanceState);
    // setTitle(story);
    setContentView(R.layout.activity_main);
    setTitle("Xyzzy: " + getStoryName());
    startBackgroundLogicThread(story);
    redisplayScreen();
  }

  @Override protected void onRestart() {
    Log.d("Xyzzy", "MainActivity.onRestart");
    super.onRestart();
  }

  @Override protected void onResume() {
    Log.d("Xyzzy", "MainActivity.onRestart");
    super.onResume();
    ZScrollView.updateScrollingSpeed();
    textSize = (Integer) Preferences.TEXT_SIZE.getValue(this);
    synchronized (textBoxes) {
      for (final View v : textBoxes) {
        if (v instanceof TextView) {
          ((TextView) v).setTextSize(textSize);
        }
      }
    }
    synchronized (logicThread) {
      if (logicThread != null && Memory.current() != null && Memory.current().buffer != null) {
        Memory.setScreenColumns();
      }
    }
  }

  void addView(final View tv, final int viewId) {
    final int maximumScroll = (Integer) Preferences.SCROLL_BACK.getValue(this);
    runOnUiThread(new Runnable() {
      @Override public void run() {
        if (tv instanceof TextView) {
          ((TextView) tv).setTextSize(textSize);
          if ((Boolean) Preferences.UPPER_SCREENS_ARE_MONOSPACED.getValue(MainActivity.this)) {
            if (viewId != R.id.screen0) {
              ((TextView) tv).setTypeface(Typeface.MONOSPACE);
            }
          }
        }
        final LinearLayout ll = (LinearLayout) MainActivity.activity.findViewById(viewId);
        removeSurplusScrollback(ll);
        ll.addView(tv);
        focusTextView(tv);
      }

      private void removeSurplusScrollback(final LinearLayout ll) {
        while (ll.getChildCount() > maximumScroll) {
          final View view = ll.getChildAt(0);
          ll.removeViewAt(0);
          synchronized (textBoxes) {
            textBoxes.remove(view);
          }
        }
      }
    });
    tv.setTag(viewId);
    synchronized (textBoxes) {
      textBoxes.add(tv);
    }
  }

  void endMe() {
    Log.i("Xyzzy", "Shutting down...");
    activity = null;
    synchronized (textBoxes) {
      textBoxes.clear();
    }
    Decoder.terminate();
    logicThread = null;
    synchronized (inputSyncObject) { // release background thread if it's waiting on input.
      inputSyncObject.notifyAll();
    }
    finish();
  }

  void showKeyboard() {
    runOnUiThread(new Runnable() {
      @Override public void run() {
        Log.i("Xyzzy", "Showing keyboard");
        final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        final RelativeLayout ll = (RelativeLayout) MainActivity.activity.findViewById(R.id.screen);
        inputMethodManager.toggleSoftInputFromWindow(ll.getApplicationWindowToken(),
            InputMethodManager.SHOW_FORCED, 0);
      }
    });
  }

  @SuppressWarnings("deprecation") private void getScreenSize() {
    final Display display = getWindowManager().getDefaultDisplay();
    width = display.getWidth();
  }

  private String getStoryName() {
    return getIntent().getStringExtra(SelectionActivity.STORY_NAME);
  }

  private String getStorySelection() {
    return getIntent().getStringExtra(SelectionActivity.STORY_FILE);
  }

  private void redisplayScreen() {
    synchronized (textBoxes) {
      for (final View v : textBoxes) {
        final LinearLayout oldLinearLayout = (LinearLayout) v.getParent();
        if (oldLinearLayout != null) {
          oldLinearLayout.removeView(v);
        }
        final LinearLayout newLinearLayout = (LinearLayout) findViewById((Integer) v.getTag());
        newLinearLayout.addView(v);
      }
    }
  }

  public static MainActivity activity;

  public final static InputSync inputSyncObject = new InputSync();

  private static Thread logicThread = null;

  final static View.OnKeyListener okl;

  final static OnEditorActionListener oeal;

  static final List<View> textBoxes = new ArrayList<View>();
  static {
    okl = new View.OnKeyListener() {
      @Override public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
        Log.d("Xyzzy", "MainActivity.onKey:" + v + "," + keyCode + "," + event);
        final EditText et = (EditText) v;
        if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER) {
          synchronized (MainActivity.inputSyncObject) {
            MainActivity.inputSyncObject.setString(et.getText().toString());
            MainActivity.inputSyncObject.notifyAll();
          }
          giveDisabledAppearance(et);
          return true;
        }
        return false;
      }

      private void delayDisable(final EditText et) {
        new Thread(new Runnable() {
          @Override public void run() {
            try {
              Thread.sleep(5000);
            } catch (final InterruptedException e) {
              Log.e("Xyzzy", "MainActivity.okl.delayDisable interrupted", e);
            }
            try {
              synchronized (MainActivity.activity) {
                if (MainActivity.activity != null) {
                  MainActivity.activity.runOnUiThread(new Runnable() {
                    @Override public void run() {
                      et.setEnabled(false);
                    }
                  });
                }
              }
            } catch (final NullPointerException npe) {
              Log.e("Xyzzy", "MainActivity.delayDisable", npe);
            }
          }
        }, "Disable old EditText").start();
      }

      private void giveDisabledAppearance(final EditText et) {
        et.setGravity(Gravity.RIGHT);
        et.setTextColor(ZWindow.foreground);
        et.setBackgroundColor(ZWindow.background);
        delayDisable(et);
      }
    };
    oeal = new OnEditorActionListener() {
      @Override public boolean onEditorAction(final TextView v, final int actionId,
          final KeyEvent event) {
        Log.d("Xyzzy", "MainActivity.onEditorAction:" + v + "," + actionId + "," + event);
        final EditText et = (EditText) v;
        synchronized (MainActivity.inputSyncObject) {
          MainActivity.inputSyncObject.setString(et.getText().toString());
          MainActivity.inputSyncObject.notifyAll();
        }
        giveDisabledAppearance(et);
        return true;
      }

      private void delayDisable(final EditText et) {
        new Thread(new Runnable() {
          @Override public void run() {
            try {
              Thread.sleep(5000);
            } catch (final InterruptedException e) {
              Log.e("Xyzzy", "MainActivity.okl.delayDisable interrupted", e);
            }
            try {
              synchronized (MainActivity.activity) {
                if (MainActivity.activity != null) {
                  MainActivity.activity.runOnUiThread(new Runnable() {
                    @Override public void run() {
                      et.setEnabled(false);
                    }
                  });
                }
              }
            } catch (final NullPointerException npe) {
              Log.e("Xyzzy", "MainActivity.delayDisable", npe);
            }
          }
        }, "Disable old EditText").start();
      }

      private void giveDisabledAppearance(final EditText et) {
        et.setGravity(Gravity.RIGHT);
        et.setTextColor(ZWindow.foreground);
        et.setBackgroundColor(ZWindow.background);
        delayDisable(et);
      }
    };
  }

  public static int width;

  public static int waitOnKey() { // returns 0 if interrupted or shutting down.
    final long timeBefore = System.currentTimeMillis();
    synchronized (inputSyncObject) {
      inputSyncObject.setCharacter(0);
      try {
        inputSyncObject.wait();
      } catch (final InterruptedException e) {
        Log.e("Xyzzy", "Wait on key interrupted:", e);
      }
      final long timeAfter = System.currentTimeMillis();
      ZWindow.keyWaitLag(timeAfter - timeBefore);
      return inputSyncObject.character();
    }
  }

  static void focusTextView(final View tv) {
    tv.setFocusableInTouchMode(true);
    tv.requestFocus();
  }

  static EditText formattedEditText(final int foreground, final int background) {
    final EditText et = new EditText(MainActivity.activity.getApplicationContext());
    et.setLayoutParams(new LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
    et.setTextColor(foreground);
    et.setPadding(0, 0, 0, 0);
    et.setBackgroundColor(background);
    et.setImeActionLabel(">", 0);
    et.setHorizontallyScrolling(true);
    et.setInputType(InputType.TYPE_CLASS_TEXT);
    et.setOnKeyListener(okl);
    et.setOnEditorActionListener(oeal);
    return et;
  }

  static TextView textView(final int foreground, final int background) {
    final TextView ett = new TextView(MainActivity.activity.getApplicationContext());
    ett.setTextColor(foreground);
    ett.setBackgroundColor(background);
    ett.setPadding(0, 0, 0, 0);
    return ett;
  }

  private synchronized static void startBackgroundLogicThread(final String message) {
    if (logicThread == null) {
      logicThread = new Thread(new Xyzzy(message), "XyzzyInterpreter");
      logicThread.start();
    }
  }
}
