
package uk.addie.xyzzy;

import java.net.URISyntaxException;
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
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SelectionActivity extends Activity implements ListAdapter {
    public static String getPath(Context context, Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { MediaStore.Images.Media.DATA };
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    List<String>                        games            = new ArrayList<String>();
    Map<String, String>                 all              = new HashMap<String, String>();    ;
    private int                         textSize;
    private final List<DataSetObserver> observer         = new ArrayList<DataSetObserver>();
    public final static String          EXTRA_MESSAGE    = "uk.addie.xyzzy.MESSAGE";
    private static final int            FILE_SELECT_CODE = 0;
    private String                      rval;

    @Override public boolean areAllItemsEnabled() {
        return true;
    }

    @Override public int getCount() {
        return games.size();
    }

    private String getDialogueName() {
        final EditText input = new EditText(getApplicationContext());
        input.setText("New game");
        input.setTextColor(0xff000000);
        new AlertDialog.Builder(this).setTitle("Please name this file").setView(input)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int whichButton) {
                        rval = input.getText().toString();
                    }
                }).show();
        return rval;
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
        switch (requestCode) {
        case FILE_SELECT_CODE:
            if (resultCode == RESULT_OK) {
                // Get the Uri of the selected file 
                Uri uri = data.getData();
                Log.d("Xyzzy", "File Uri: " + uri.toString());
                // Get the path
                String path;
                try {
                    path = getPath(this, uri);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                Log.d("Xyzzy", "File Path: " + path);
                if (path != null) {
                    String name = getDialogueName();
                    SharedPreferences.Editor sp = getSharedPreferences("XyzzyGames", 0).edit();
                    sp.putString(name, path);
                    sp.commit();
                    regenerateData();
                    for (DataSetObserver dso : observer) {
                        dso.onChanged();
                    }
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

    private void regenerateData() {
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

    @Override public void registerDataSetObserver(DataSetObserver observer) {
        this.observer.add(observer);
    }

    private void setupGames() {
        SharedPreferences xyzzyPrefs = getSharedPreferences("Xyzzy", 0);
        this.textSize = xyzzyPrefs.getInt("textSize", 16);
        regenerateData();
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    protected void startGame(String name) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_MESSAGE, name);
        startActivity(intent);
    }

    @Override public void unregisterDataSetObserver(DataSetObserver observer) {
        this.observer.remove(observer);
    }
}
