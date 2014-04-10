
package uk.addie.xyzzy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SelectionActivity extends Activity implements ListAdapter { // NO_UCD (use default)
    private static String getPath(Context context, Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { MediaColumns.DATA };
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
                if (cursor.moveToFirst()) {
                    String rval = cursor.getString(column_index);
                    cursor.close();
                    return rval;
                }
                cursor.close();
            } catch (Exception e) {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    private final List<String>  games            = new ArrayList<String>();
    final Map<String, String>   all              = new HashMap<String, String>();
    private int                 textSize;
    final List<DataSetObserver> observer         = new ArrayList<DataSetObserver>();
    final static String         EXTRA_MESSAGE    = "uk.addie.xyzzy.MESSAGE";
    private static final int    FILE_SELECT_CODE = 0;

    private void addPathToGamesList(String path) {
        Log.d("Xyzzy", "Adding path:" + path);
        getGameNameDialogue(path);
    }

    @Override public boolean areAllItemsEnabled() {
        return true;
    }

    @Override public int getCount() {
        return games.size();
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

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        TextView tv = new TextView(getApplicationContext());
        final String name = games.get(position);
        tv.setText(name);
        tv.setTextSize(textSize * 2);
        tv.setPadding(textSize * 2, textSize * 2, textSize * 2, textSize * 2);
        tv.setTextColor(0xff000000);
        if (!name.startsWith("+")) {
            tv.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startGame(all.get(name));
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
                String path;
                path = getPath(this, uri);
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
        super.onCreate(savedInstanceState);
        setupGames();
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.selector);
        ListView lv = (ListView) findViewById(R.id.selector);
        lv.setAdapter(this);
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

    private void setupGames() {
        SharedPreferences xyzzyPrefs = getSharedPreferences("Xyzzy", 0);
        this.textSize = xyzzyPrefs.getInt("textSize", 16);
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
