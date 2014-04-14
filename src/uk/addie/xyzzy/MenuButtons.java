
package uk.addie.xyzzy;

import uk.addie.xyzzy.gameselection.SelectionActivity;
import uk.addie.xyzzy.header.ZKeycode;
import uk.addie.xyzzy.htmlview.HtmlViewActivity;
import uk.addie.xyzzy.preferences.PreferencesActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

enum MenuButtons implements MenuButton {
    ABOUT {
        @Override public void invoke() {
            Intent intent = new Intent(MainActivity.activity, HtmlViewActivity.class);
            MainActivity.activity.startActivity(intent);
        }

        @Override public int menuButtonIcon() {
            return R.drawable.ic_action_about;
        }

        @Override public String toString() {
            return "About Xyzzy";
        }
    },
    SHOW_KEYBOARD {
        @Override public void invoke() {
            MainActivity.activity.showKeyboard();
        }

        @Override public int menuButtonIcon() {
            return R.drawable.ic_action_keyboard;
        }

        @Override public String toString() {
            return "Toggle keyboard";
        }
    },
    SETTINGS {
        @Override public void invoke() {
            Intent intent = new Intent(MainActivity.activity, PreferencesActivity.class);
            MainActivity.activity.startActivity(intent);
        }

        @Override public int menuButtonIcon() {
            return R.drawable.ic_action_settings;
        }

        @Override public String toString() {
            return "Settings";
        }
    },
    LEAVE_GAME {
        @Override public void invoke() {
            new AlertDialog.Builder(MainActivity.activity)
                    .setMessage("Are you sure you want to leave?  Progress will not be saved.").setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent(MainActivity.activity, SelectionActivity.class);
                            MainActivity.activity.startActivity(intent);
                            MainActivity.activity.endMe();
                        }
                    }).setNegativeButton("No", null).show();
        }

        @Override public String toString() {
            return "Leave game";
        }
    },
    CURSOR_UP {
        @Override public void invoke() {
            pressZKey(ZKeycode.ARROW_UP);
        }

        @Override public String toString() {
            return "Cursor up";
        }
    },
    CURSOR_DOWN {
        @Override public void invoke() {
            pressZKey(ZKeycode.ARROW_DOWN);
        }

        @Override public String toString() {
            return "Cursor down";
        }
    },
    CURSOR_LEFT {
        @Override public void invoke() {
            pressZKey(ZKeycode.ARROW_LEFT);
        }

        @Override public String toString() {
            return "Cursor left";
        }
    },
    CURSOR_RIGHT {
        @Override public void invoke() {
            pressZKey(ZKeycode.ARROW_RIGHT);
        }

        @Override public String toString() {
            return "Cursor right";
        }
    },
    BACKSPACE {
        @Override public void invoke() {
            pressZKey(ZKeycode.BACKSPACE);
        }

        @Override public String toString() {
            return "Backspace";
        }
    },
    ENTER {
        @Override public void invoke() {
            pressZKey(ZKeycode.RETURN);
        }

        @Override public String toString() {
            return "Enter";
        }
    };
    @Override public int menuButtonIcon() {
        return -1;
    }

    protected void pressZKey(int keyCode) {
        Log.d("Xyzzy", "Synthetic Z Key:" + keyCode);
        MainActivity.activity.onKeyDown(keyCode, null);
    }
}
