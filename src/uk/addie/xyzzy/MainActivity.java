
package uk.addie.xyzzy;

import java.util.ArrayList;
import java.util.List;

import uk.addie.xyzzy.gameselection.SelectionActivity;
import uk.addie.xyzzy.header.ZKeycode;
import uk.addie.xyzzy.preferences.Preferences;
import uk.addie.xyzzy.zmachine.Decoder;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
    public static MainActivity activity;
    static final List<View>    textBoxes       = new ArrayList<View>();
    private static Thread      logicThread     = null;
    private static int         lastKey         = 0;
    public final static Object inputSyncObject = new Object();
    public static int          width, height;

    static void focusTextView(final View tv) {
        tv.setFocusableInTouchMode(true);
        tv.requestFocus();
    }

    public static int waitOnKey() {
        lastKey = 0;
        //        showKeyboard();
        while (lastKey == 0) {
            synchronized (inputSyncObject) {
                try {
                    inputSyncObject.wait();
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
        }
        return lastKey;
    }

    private MenuItem[] mis;
    EditText           readyToDisable = null;
    public int         textSize;

    public void addView(final View tv, final int viewId) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                LinearLayout ll = (LinearLayout) MainActivity.activity.findViewById(viewId);
                if (tv instanceof TextView) {
                    ((TextView) tv).setTextSize(textSize);
                }
                ll.addView(tv);
                focusTextView(tv);
            }
        });
        tv.setTag(viewId);
        int maximumScroll = (Integer) Preferences.SCROLL_BACK.getValue(this);
        synchronized (textBoxes) {
            textBoxes.add(tv);
            while (textBoxes.size() > maximumScroll) {
                textBoxes.remove(0);
            }
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
        synchronized (inputSyncObject) {
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
                readyToDisable = et;
            }
        });
    }

    @SuppressWarnings("deprecation") private void getScreenSize() {
        Display display = getWindowManager().getDefaultDisplay();
        width = display.getWidth();
        height = display.getHeight();
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        Log.d("Xyzzy", "MainActivity onCreate");
        getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        activity = this;
        getScreenSize();
        Intent intent = getIntent();
        String message = intent.getStringExtra(SelectionActivity.EXTRA_MESSAGE);
        Xyzzy.story = message;
        super.onCreate(savedInstanceState);
        //        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        synchronized (this) {
            if (logicThread == null) {
                Log.i("Xyzzy", "Starting new thread");
                logicThread = new Thread(new Xyzzy(), "XyzzyInterpreter");
                logicThread.start();
            }
        }
        synchronized (textBoxes) {
            for (View v : textBoxes) {
                final LinearLayout oldLinearLayout = (LinearLayout) v.getParent();
                if (oldLinearLayout != null) {
                    oldLinearLayout.removeView(v);
                }
                final LinearLayout newLinearLayout = (LinearLayout) findViewById((Integer) v.getTag());
                newLinearLayout.addView(v);
            }
        }
    }

    @SuppressLint("NewApi") @Override public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("Control", "OCOM");
        mis = new MenuItem[MenuButtons.values().length];
        int i = 0;
        for (MenuButtons mb : MenuButtons.values()) {
            final MenuItem nextMenu = menu.add(mb.toString());
            if (mb.menuButtonIcon() != -1) {
                nextMenu.setIcon(mb.menuButtonIcon());
                nextMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
            mis[i++] = nextMenu;
        }
        return true;
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d("Xyzzy", "onKeyDown:" + keyCode + " :" + event);
        if (event == null) { // ie. it's synthetic
            lastKey = keyCode;
            synchronized (inputSyncObject) {
                inputSyncObject.notifyAll();
            }
            return true;
        }
        synchronized (inputSyncObject) {
            switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_MENU:
                return false;
                //            case KeyEvent.KEYCODE_DEL: // TODO bad idea while editing text
                //                lastKey = ZKeycode.BACKSPACE;
                //                break;
            case KeyEvent.KEYCODE_ENTER:
                lastKey = ZKeycode.RETURN;
                break;
            default:
                lastKey = event.getUnicodeChar();
            }
            inputSyncObject.notifyAll();
        }
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int selected = -1;
        if (mis == null) { // then we've not initialised?
            Log.e("Xyzzy", "Android onOptionsItemSelected before onCreateOptionsMenu?");
            return false;
        }
        for (int i = 0; i < mis.length; ++i) {
            if (item == mis[i]) {
                selected = i;
            }
        }
        MenuButtons.values()[selected].invoke();
        return true;
    }

    @Override protected void onResume() {
        super.onResume();
        textSize = (Integer) Preferences.TEXT_SIZE.getValue(this);
        synchronized (textBoxes) {
            for (View v : textBoxes) {
                if (v instanceof TextView) {
                    ((TextView) v).setTextSize(textSize);
                }
            }
        }
    }

    public void removeChildren(final int viewId) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                LinearLayout ll = (LinearLayout) MainActivity.activity.findViewById(viewId);
                ll.removeAllViews();
                synchronized (textBoxes) {
                    List<View> tbCopy = new ArrayList<View>(textBoxes);
                    for (View v : tbCopy) {
                        if ((Integer) v.getTag() == viewId) {
                            textBoxes.remove(v);
                        }
                    }
                }
            }
        });
    }

    public void setBackgroundColour(final int colour) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                RelativeLayout ll = (RelativeLayout) MainActivity.activity.findViewById(R.id.screen);
                ll.setBackgroundColor(colour);
            }
        });
    }

    void showKeyboard() {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Log.i("Xyzzy", "Showing keyboard");
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                RelativeLayout ll = (RelativeLayout) MainActivity.activity.findViewById(R.id.screen);
                inputMethodManager.toggleSoftInputFromWindow(ll.getApplicationWindowToken(),
                        InputMethodManager.SHOW_FORCED, 0);
            }
        });
    }
}
