
package uk.addie.xyzzy;

import uk.addie.xyzzy.gameselection.SelectionActivity;
import uk.addie.xyzzy.header.ZKeycode;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

enum MenuButtons implements Invokeable {
    ABOUT {
        @Override public void invoke() {
            // TODO about this app
        }

        @Override public String toString() {
            return "About Xyzzy";
        }
    },
    HELP {
        @Override public void invoke() {
            // TODO help with this app
        }

        @Override public String toString() {
            return "Help";
        }
    },
    SHOW_KEYBOARD {
        @Override public void invoke() {
            MainActivity.activity.showKeyboard();
        }

        @Override public String toString() {
            return "Show keyboard";
        }
    },
    SETTINGS {
        @Override public void invoke() {
            // TODO settings
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
    },
    CURSOR_DOWN {
        @Override public void invoke() {
            pressZKey(ZKeycode.ARROW_DOWN);
        }
    },
    CURSOR_LEFT {
        @Override public void invoke() {
            pressZKey(ZKeycode.ARROW_LEFT);
        }
    },
    CURSOR_RIGHT {
        @Override public void invoke() {
            pressZKey(ZKeycode.ARROW_RIGHT);
        }
    },
    BACKSPACE {
        @Override public void invoke() {
            pressZKey(ZKeycode.BACKSPACE);
        }
    },
    ENTER {
        @Override public void invoke() {
            pressZKey(ZKeycode.RETURN);
        }
    };
    protected void pressZKey(int keyCode) {
        Log.d("Xyzzy", "Synthetic Z Key:" + keyCode);
        MainActivity.activity.onKeyDown(keyCode, null);
    }
}
