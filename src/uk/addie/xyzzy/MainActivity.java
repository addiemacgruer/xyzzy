
package uk.addie.xyzzy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import uk.addie.xyzzy.gameselection.SelectionActivity;
import uk.addie.xyzzy.preferences.Preferences;
import uk.addie.xyzzy.util.Utility;
import uk.addie.xyzzy.zmachine.Decoder;
import uk.addie.xyzzy.zobjects.ZWindow;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
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

public class MainActivity extends Activity {
    public static MainActivity      activity;
    public final static InputSync   inputSyncObject = new InputSync();
    private static Thread           logicThread     = null;
    final static View.OnKeyListener okl;
    static final List<View>         textBoxes       = new ArrayList<View>();
    public static int               width, height;
    static {
        okl = new View.OnKeyListener() {
            private void delayDisable(final EditText et) {
                new Thread(new Runnable() {
                    @Override public void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (final InterruptedException e) {
                            Log.e("Xyzzy", "MainActivity.okl.delayDisable interrupted", e);
                        }
                        MainActivity.activity.runOnUiThread(new Runnable() {
                            @Override public void run() {
                                et.setEnabled(false);
                            }
                        });
                    }
                }, "Disable old EditText").start();
            }

            private void giveDisabledAppearance(final EditText et) {
                et.setGravity(Gravity.RIGHT);
                et.setTextColor(ZWindow.foreground);
                et.setBackgroundColor(ZWindow.background);
                delayDisable(et);
            }

            @Override public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
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
        };
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
        return et;
    }

    private static void startBackgroundLogicThread(final String message) {
        if (logicThread == null) {
            Log.i("Xyzzy", "Starting new thread");
            logicThread = new Thread(new Xyzzy(message), "XyzzyInterpreter");
            logicThread.start();
        }
    }

    static TextView textView(final int foreground, final int background) {
        final TextView ett = new TextView(MainActivity.activity.getApplicationContext());
        ett.setTextColor(foreground);
        ett.setBackgroundColor(background);
        ett.setPadding(0, 0, 0, 0);
        return ett;
    }

    public static int waitOnKey() { // returns 0 if interrupted or shutting down.
        synchronized (inputSyncObject) {
            inputSyncObject.setCharacter(0);
            try {
                inputSyncObject.wait();
            } catch (final InterruptedException e) {
                Log.e("Xyzzy", "Wait on key interrupted:", e);
            }
            return inputSyncObject.character();
        }
    }

    private MenuItem[] mis;
    int                textSize;

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

    public void addTextView(final SpannableStringBuilder ssb, final int foreground, final int background,
            final int windowId) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                final TextView tv = textView(foreground, background);
                tv.setText(ssb);
                tv.setTag(windowId);
                MainActivity.activity.addView(tv, windowId);
            }
        });
    }

    public void addView(final View tv, final int viewId) {
        final int maximumScroll = (Integer) Preferences.SCROLL_BACK.getValue(this);
        runOnUiThread(new Runnable() {
            private void removeSurplusScrollback(final LinearLayout ll) {
                while (ll.getChildCount() > maximumScroll) {
                    final View view = ll.getChildAt(0);
                    ll.removeViewAt(0);
                    synchronized (textBoxes) {
                        textBoxes.remove(view);
                    }
                }
            }

            @Override public void run() {
                if (tv instanceof TextView) {
                    ((TextView) tv).setTextSize(textSize);
                }
                final LinearLayout ll = (LinearLayout) MainActivity.activity.findViewById(viewId);
                removeSurplusScrollback(ll);
                ll.addView(tv);
                focusTextView(tv);
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
        this.finish();
    }

    public void finishEditing(final EditText et, final int foreground, final int background) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                et.setGravity(Gravity.RIGHT);
                et.setBackgroundColor(background);
                et.setTextColor(foreground);
                et.setOnKeyListener(null);
            }
        });
    }

    @SuppressWarnings("deprecation") private void getScreenSize() {
        final Display display = getWindowManager().getDefaultDisplay();
        width = display.getWidth();
        height = display.getHeight();
    }

    private String getStoryName() {
        return getIntent().getStringExtra(SelectionActivity.STORY_NAME);
    }

    private String getStorySelection() {
        return getIntent().getStringExtra(SelectionActivity.STORY_FILE);
    }

    @Override protected void onCreate(final Bundle savedInstanceState) {
        Log.d("Xyzzy", "MainActivity onCreate");
        getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        activity = this;
        getScreenSize();
        final String story = getStorySelection();
        super.onCreate(savedInstanceState);
        //        setTitle(story);
        setContentView(R.layout.activity_main);
        setTitle("Xyzzy: " + getStoryName());
        synchronized (this) {
            startBackgroundLogicThread(story);
        }
        redisplayScreen();
    }

    @SuppressLint("NewApi") @Override public boolean onCreateOptionsMenu(final Menu menu) {
        Log.d("Control", "OCOM");
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
        int selected = Utility.arrayOffsetOf(item, mis);
        MenuButtons.values()[selected].invoke();
        return true;
    }

    @Override protected void onResume() {
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

    public void removeChild(final View view) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                final int viewId = (Integer) view.getTag();
                final LinearLayout ll = (LinearLayout) MainActivity.activity.findViewById(viewId);
                ll.removeView(view);
                synchronized (textBoxes) {
                    textBoxes.remove(view);
                }
            }
        });
    }

    public void removeChildren(final int viewId) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                final LinearLayout ll = (LinearLayout) MainActivity.activity.findViewById(viewId);
                ll.removeAllViews();
                synchronized (textBoxes) {
                    for (Iterator<View> it = textBoxes.iterator(); it.hasNext();) {
                        View v = it.next();
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
}
