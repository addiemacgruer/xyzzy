
package uk.addie.xyzzy.gameselection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.addie.xyzzy.MainActivity;
import uk.addie.xyzzy.R;
import uk.addie.xyzzy.preferences.Preferences;
import uk.addie.xyzzy.util.Utility;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SelectionActivity extends Activity implements ListAdapter { // NO_UCD (use default)
    protected static SelectionActivity activity;
    public final static String         STORY_FILE       = "uk.addie.xyzzy.STORY_FILE";
    public final static String         STORY_NAME       = "uk.addie.xyzzy.STORY_NAME";
    private static final int           FILE_SELECT_CODE = 0;
    final static int                   INTERSTITIALS    = 3;
    static int                         selected         = -1;
    //    private static String getPath(final Context context, final Uri uri) {
    //        Log.d("Xyzzy", "getPath:" + context + " uri:" + uri);
    //        Cursor cursor = null;
    //        Log.i("Xyzzy", "Uri path:" + uri.getPath());
    //        try {
    //            cursor = context.getContentResolver().query(uri, null, null, null, null);
    //            final int nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
    //            if (cursor.moveToFirst()) {
    //                final String rval = cursor.getString(nameIndex);
    //                cursor.close();
    //                return rval;
    //            }
    //            cursor.close();
    //        } catch (final Exception e) {
    //            Log.e("Xyzzy", "SelectionActivity.getPath:", e);
    //            if (cursor != null) {
    //                cursor.close();
    //            }
    //        }
    //        return null;
    //    }
    final Map<String, String>          all              = new HashMap<String, String>();
    final List<String>                 games            = new ArrayList<String>();
    private MenuItem[]                 mis;
    final List<DataSetObserver>        observer         = new ArrayList<DataSetObserver>();
    private int                        textSize;

    @Override public boolean areAllItemsEnabled() {
        return true;
    }

    TextView gameNameAtListPosition(final int position) {
        final TextView tv = selectionPageTextView();
        final String name = games.get(position);
        tv.setText(name);
        if (selected == position) {
            tv.setTextColor(0xffffffff);
            tv.setBackgroundColor(0xff000000);
        } else {
            tv.setTextColor(0xff000000);
            tv.setBackgroundColor(0xffffffff);
        }
        tv.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(final View v) {
                //                    startGame(all.get(name)); TODO this is startgame
                selected = position;
                for (final DataSetObserver dso : observer) {
                    dso.onChanged();
                }
            }
        });
        return tv;
    }

    @Override public int getCount() {
        if (selected > games.size()) {
            selected = -1;
        }
        return games.size() + (selected == -1 ? 0 : INTERSTITIALS);
    }

    @Override public Object getItem(final int position) {
        return games.get(position);
    }

    @Override public long getItemId(final int position) {
        return position;
    }

    @Override public int getItemViewType(final int position) {
        return 0;
    }

    @Override public View getView(final int listViewPosition, final View convertView, final ViewGroup parent) {
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
                @Override public void onClick(final View v) {
                    startGame(games.get(selected));
                }
            });
            tv.setTextColor(0xff000000);
            tv.setBackgroundColor(0xffffce4e);
        } else if (position == -3) {
            tv = selectionPageTextView();
            tv.setText("Remove from list");
            tv.setTextColor(0xff000000);
            tv.setBackgroundColor(0xffffce4e);
            tv.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    new AlertDialog.Builder(SelectionActivity.activity)
                            .setMessage("Are you sure you want to remove " + games.get(selected) + " from list?")
                            .setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override public void onClick(final DialogInterface dialog, final int id) {
                                    final SharedPreferences.Editor sp = getSharedPreferences("XyzzyGames", 0).edit();
                                    sp.remove(games.get(selected));
                                    selected = -1;
                                    sp.commit();
                                    regenerateData();
                                    for (final DataSetObserver dso : observer) {
                                        dso.onInvalidated();
                                    }
                                }
                            }).setNegativeButton("No", null).show();
                }
            });
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

    @Override public boolean isEnabled(final int position) {
        return true;
    }

    @Override protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        setupGames();
        setContentView(R.layout.selector);
        final ListView lv = (ListView) findViewById(R.id.selector);
        lv.setAdapter(this);
    }

    @SuppressLint("NewApi") @Override public boolean onCreateOptionsMenu(final Menu menu) {
        mis = new MenuItem[MenuButtons.values().length];
        int i = 0;
        for (final MenuButtons mb : MenuButtons.values()) {
            final MenuItem nextMenu = menu.add(mb.toString());
            if (mb.menuButtonIcon() != -1) {
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
        int selectedMenu = Utility.arrayOffsetOf(item, mis);
        MenuButtons.values()[selectedMenu].invoke();
        return true;
    }

    @Override protected void onResume() {
        super.onResume();
        textSize = (Integer) Preferences.TEXT_SIZE.getValue(this);
        regenerateData();
        for (final DataSetObserver dso : observer) {
            dso.onInvalidated();
        }
    }

    void regenerateData() {
        all.clear();
        games.clear();
        final SharedPreferences sp = getSharedPreferences("XyzzyGames", 0);
        //        all.put("Czech", "@czech.z5");
        final Map<String, ?> stored = sp.getAll();
        for (final String s : stored.keySet()) {
            all.put(s, sp.getString(s, ""));
        }
        for (final String s : all.keySet()) {
            if (s != null) {
                games.add(s);
            }
        }
        Collections.sort(games);
    }

    @Override public void registerDataSetObserver(final DataSetObserver dso) {
        observer.add(dso);
    }

    TextView selectionPageTextView() {
        final TextView tv = new TextView(getApplicationContext());
        tv.setTextSize(textSize * 2);
        tv.setPadding(textSize * 2, textSize * 2, textSize * 2, textSize * 2);
        return tv;
    }

    private void setupGames() {
        regenerateData();
    }

    void showFileChooser() {
        final Intent intent = new Intent(this, FileChooserActivity.class);
        startActivity(intent);
    }

    void startGame(final String name) {
        final Intent intent = new Intent(this, MainActivity.class);
        String path = all.get(name);
        intent.putExtra(STORY_FILE, path);
        intent.putExtra(STORY_NAME, name);
        startActivity(intent);
    }

    @Override public void unregisterDataSetObserver(final DataSetObserver dso) {
        observer.remove(dso);
    }
}
