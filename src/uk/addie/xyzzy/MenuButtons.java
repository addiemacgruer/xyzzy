
package uk.addie.xyzzy;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

public enum MenuButtons implements Invokeable {
    ABOUT {
        @Override public void invoke() {
        }

        @Override public String toString() {
            return "About Xyzzy";
        }
    },
    HELP {
        @Override public void invoke() {
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
    };
}
