
package uk.addie.xyzzy.gameselection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.addie.xyzzy.MainActivity;
import uk.addie.xyzzy.R;
import uk.addie.xyzzy.preferences.Preferences;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SelectionActivity extends Activity implements ListAdapter { // NO_UCD (use default)
    static int                         selected      = -1;
    protected static SelectionActivity activity;
    final static int                   INTERSTITIALS = 3;

    private static String getPath(Context context, Uri uri) {
        Log.d("Xyzzy", "getPath:" + context + " uri:" + uri);
        Cursor cursor = null;
        Log.i("Xyzzy", "Uri path:" + uri.getPath());
        try {
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            int nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
            if (cursor.moveToFirst()) {
                String rval = cursor.getString(nameIndex);
                cursor.close();
                return rval;
            }
            cursor.close();
        } catch (Exception e) {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private final List<String>  games            = new ArrayList<String>();
    final Map<String, String>   all              = new HashMap<String, String>();
    private int                 textSize;
    final List<DataSetObserver> observer         = new ArrayList<DataSetObserver>();
    public final static String  EXTRA_MESSAGE    = "uk.addie.xyzzy.MESSAGE";
    private static final int    FILE_SELECT_CODE = 0;
    private MenuItem[]          mis;

    private void addPathToGamesList(String path) {
        Log.d("Xyzzy", "Adding path:" + path);
        getGameNameDialogue(path);
    }

    @Override public boolean areAllItemsEnabled() {
        return true;
    }

    TextView gameNameAtListPosition(final int position) {
        TextView tv = selectionPageTextView();
        final String name = games.get(position);
        tv.setText(name);
        if (selected == position) {
            tv.setTextColor(0xffffffff);
            tv.setBackgroundColor(0xff000000);
        } else {
            tv.setTextColor(0xff000000);
            tv.setBackgroundColor(0xffffffff);
        }
        if (!name.startsWith("+")) {
            tv.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    //                    startGame(all.get(name)); TODO this is startgame
                    selected = position;
                    for (DataSetObserver dso : observer) {
                        dso.onChanged();
                    }
                }
            });
        } else {
            tv.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    showFileChooser();
                }
            });
        }
        return tv;
    }

    @Override public int getCount() {
        return games.size() + (selected == -1 ? 0 : INTERSTITIALS);
    }

    private void getGameNameDialogue(final String pathToGame) {
        final EditText input = new EditText(getApplicationContext());
        input.setText("New game");
        input.setTextColor(0xff000000);
        new AlertDialog.Builder(this).setTitle("Please name this file").setView(input)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int whichButton) {
                        SharedPreferences.Editor sp = getSharedPreferences("XyzzyGames", 0).edit();
                        sp.putString(input.getText().toString(), pathToGame);
                        sp.commit();
                        regenerateData();
                        for (DataSetObserver dso : observer) {
                            dso.onChanged();
                        }
                    }
                }).show();
    }

    @Override public Object getItem(int position) {
        return games.get(position);
    }

    @Override public long getItemId(int position) {
        return position;
    }

    @Override public int getItemViewType(int position) {
        return 0;
    }

    @Override public View getView(final int listViewPosition, View convertView, ViewGroup parent) {
        final int position;
        if (selected == -1) {
            position = listViewPosition;
        } else if (listViewPosition <= selected) {
            position = listViewPosition;
        } else if (listViewPosition > selected + INTERSTITIALS) {
            position = listViewPosition - INTERSTITIALS;
        } else {
            position = selected - listViewPosition;
        }
        final TextView tv;
        if (position >= 0) {
            tv = gameNameAtListPosition(position);
        } else if (position == -1) {
            tv = selectionPageTextView();
            tv.setText(all.get(games.get(selected)));
            tv.setTextSize(textSize);
            tv.setTextColor(0x88000000);
            tv.setBackgroundColor(0xffffce4e);
        } else if (position == -2) {
            tv = selectionPageTextView();
            tv.setText("Play " + games.get(selected));
            tv.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startGame(all.get(games.get(selected)));
                }
            });
            tv.setTextColor(0xff000000);
            tv.setBackgroundColor(0xffffce4e);
        } else if (position == -3) {
            tv = selectionPageTextView();
            tv.setText("Remove from list");
            tv.setTextColor(0xff000000);
            tv.setBackgroundColor(0xffffce4e);
        } else {
            tv = selectionPageTextView();
            tv.setText("????");
        }
        return tv;
    }

    @Override public int getViewTypeCount() {
        return 1;
    }

    @Override public boolean hasStableIds() {
        return true;
    }

    @Override public boolean isEmpty() {
        return getCount() == 0;
    }

    @Override public boolean isEnabled(int position) {
        return true;
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("Xyzzy", "onActivityResult requestCode:" + requestCode + " resultCode:" + resultCode + " data:" + data);
        switch (requestCode) {
        case FILE_SELECT_CODE:
        default:
            if (resultCode == RESULT_OK) {
                // Get the Uri of the selected file 
                Uri uri = data.getData();
                Log.d("Xyzzy", "File Uri: " + uri.toString());
                // Get the path
                String path = uri.toString();
                //                path = getPath(this, uri);
                Log.d("Xyzzy", "File Path: " + path);
                if (path != null) {
                    addPathToGamesList(path);
                }
            }
            break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        Log.d("Xyzzy", "SelectionActivity onCreate");
        super.onCreate(savedInstanceState);
        activity = this;
        setupGames();
        setContentView(R.layout.selector);
        ListView lv = (ListView) findViewById(R.id.selector);
        lv.setAdapter(this);
    }

    @SuppressLint("NewApi") @Override public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("Control", "OCOM");
        mis = new MenuItem[MenuButtons.values().length];
        int i = 0;
        for (MenuButtons mb : MenuButtons.values()) {
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
        this.textSize = (Integer) Preferences.TEXT_SIZE.getValue(this);
        Log.d("Xyzzy", "SelectionActivity onResume");
        for (DataSetObserver dso : observer) {
            dso.onChanged();
        }
    }

    void regenerateData() {
        Log.d("Xyzzy", "Regenerating data");
        all.clear();
        games.clear();
        SharedPreferences sp = getSharedPreferences("XyzzyGames", 0);
        all.put("Czech", "@czech.z5");
        Map<String, ?> stored = sp.getAll();
        for (String s : stored.keySet()) {
            all.put(s, sp.getString(s, ""));
        }
        for (String s : all.keySet()) {
            if (s != null) {
                games.add(s);
            }
        }
        Collections.sort(games);
        games.add("+ Add another...");
    }

    @Override public void registerDataSetObserver(DataSetObserver dso) {
        Log.d("Xyzzy", "Registering observer:" + dso);
        this.observer.add(dso);
    }

    TextView selectionPageTextView() {
        TextView tv = new TextView(getApplicationContext());
        tv.setTextSize(textSize * 2);
        tv.setPadding(textSize * 2, textSize * 2, textSize * 2, textSize * 2);
        return tv;
    }

    private void setupGames() {
        regenerateData();
    }

    void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    void startGame(String name) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_MESSAGE, name);
        startActivity(intent);
    }

    @Override public void unregisterDataSetObserver(DataSetObserver dso) {
        this.observer.remove(dso);
    }
}
