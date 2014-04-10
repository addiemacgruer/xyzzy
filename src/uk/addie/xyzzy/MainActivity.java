
package uk.addie.xyzzy;

import java.util.ArrayList;
import java.util.List;

import uk.addie.xyzzy.zmachine.Decoder;
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
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class MainActivity extends Activity {
    public static MainActivity      activity;
    private static final List<View> textBoxes       = new ArrayList<View>();
    private static Thread           logicThread     = null;
    public static int               lastKey         = 0;
    public final static Object      inputSyncObject = new Object();
    public static int               width, height;

    static void focusTextView(final View tv) {
        //        if (tv instanceof EditText) {
        //            showKeyboard();
        //        }
        tv.setFocusableInTouchMode(true);
        tv.requestFocus();
    }

    private MenuItem[] mis;

    public void addView(final View tv, final int viewId) {
        //        snooze();
        runOnUiThread(new Runnable() {
            @Override public void run() {
                LinearLayout ll = (LinearLayout) MainActivity.activity.findViewById(viewId);
                ll.addView(tv);
                focusTextView(tv);
            }
        });
        tv.setTag(viewId);
        textBoxes.add(tv);
    }

    public void endMe() {
        Log.i("Xyzzy", "Shutting down...");
        activity = null;
        textBoxes.clear();
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
                //                et.setEnabled(false);
                et.setOnKeyListener(null);
            }
        });
    }

    @SuppressWarnings("deprecation") private void getScreenSize() {
        Display display = getWindowManager().getDefaultDisplay();
        width = display.getWidth();
        height = display.getHeight();
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        activity = this;
        getScreenSize();
        Intent intent = getIntent();
        String message = intent.getStringExtra(SelectionActivity.EXTRA_MESSAGE);
        Xyzzy.story = message;
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        boolean layout = true;
        synchronized (this) {
            if (logicThread == null) {
                Log.i("Xyzzy", "Starting new thread");
                logicThread = new Thread(new Xyzzy(), "XyzzyInterpreter");
                logicThread.start();
                layout = false;
            }
        }
        if (layout) {
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

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("Control", "OCOM");
        mis = new MenuItem[MenuButtons.values().length];
        int i = 0;
        for (MenuButtons mb : MenuButtons.values()) {
            mis[i++] = menu.add(mb.toString());
        }
        return true;
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        synchronized (inputSyncObject) {
            switch (keyCode) {
            // TODO other keycodes (up, down, ...)
            case KeyEvent.KEYCODE_MENU:
                return false;
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

    public void removeChildren(final int viewId) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                LinearLayout ll = (LinearLayout) MainActivity.activity.findViewById(viewId);
                ll.removeAllViews();
            }
        });
    }

    public void removeView(final View et, final int viewId) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                LinearLayout ll = (LinearLayout) MainActivity.activity.findViewById(viewId);
                ll.removeView(et);
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

    public int waitOnKey() {
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
}
