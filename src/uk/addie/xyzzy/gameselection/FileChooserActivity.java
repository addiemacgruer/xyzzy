
package uk.addie.xyzzy.gameselection;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.addie.xyzzy.R;
import uk.addie.xyzzy.preferences.Preferences;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class FileChooserActivity extends Activity implements ListAdapter, OnClickListener {
    private static File                 path      = Environment.getExternalStorageDirectory();
    private List<String>                pathContents;
    private final List<DataSetObserver> observers = new ArrayList<DataSetObserver>();
    private int                         textSize;

    private void addPathToGamesList(final File f) {
        final EditText input = new EditText(this);
        String name = f.getName();
        Log.d("Xyzzy", "Name:" + name);
        if (name.indexOf('.') != -1) {
            name = name.substring(0, name.indexOf('.'));
        }
        Log.d("Xyzzy", "Shortened:" + name);
        input.setText(name);
        input.setTextColor(0xff000000);
        new AlertDialog.Builder(this).setTitle("Please name this story").setView(input)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override public void onClick(final DialogInterface dialog, final int whichButton) {
                        final SharedPreferences.Editor sp = getSharedPreferences("XyzzyGames", 0).edit();
                        sp.putString(input.getText().toString(), f.getAbsolutePath());
                        sp.commit();
                        FileChooserActivity.this.finish();
                    }
                }).show();
    }

    @Override public boolean areAllItemsEnabled() {
        return true;
    }

    @Override public int getCount() {
        return pathContents.size();
    }

    @Override public Object getItem(int position) {
        return null;
    }

    @Override public long getItemId(int position) {
        return position;
    }

    @Override public int getItemViewType(int position) {
        return 0;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        TextView tv = selectionPageTextView();
        final String itemName = pathContents.get(position);
        tv.setText(itemName);
        File subfile = new File(path, itemName);
        if (subfile.isDirectory()) {
            tv.setBackgroundColor(0xff9999ff);
        }
        tv.setTag(subfile);
        tv.setOnClickListener(this);
        return tv;
    }

    @Override public int getViewTypeCount() {
        return 1;
    }

    @Override public boolean hasStableIds() {
        return true;
    }

    @Override public boolean isEmpty() {
        return false;
    }

    @Override public boolean isEnabled(int position) {
        return true;
    }

    @Override public void onClick(View v) {
        File file = (File) v.getTag();
        file.getAbsoluteFile(); // remove ".."
        if (file.isDirectory()) {
            path = file;
            setTitle(path.toString());
            Log.v("Xyzzy", "Path:" + path);
            updatePathContents();
            updateObservers();
        } else {
            addPathToGamesList(file);
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        if (pathContents == null) {
            updatePathContents();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filechooser);
        ListView lv = (ListView) findViewById(R.id.filechooser);
        setTitle(path.toString());
        lv.setAdapter(this);
    }

    @Override protected void onResume() {
        super.onResume();
        this.textSize = (Integer) Preferences.TEXT_SIZE.getValue(this);
    }

    @Override public void registerDataSetObserver(DataSetObserver observer) {
        observers.add(observer);
    }

    private TextView selectionPageTextView() {
        final TextView tv = new TextView(this);
        tv.setTextSize(textSize * 2);
        tv.setPadding(textSize * 2, textSize * 2, textSize * 2, textSize * 2);
        return tv;
    }

    @Override public void unregisterDataSetObserver(DataSetObserver observer) {
        observers.remove(observer);
    }

    private void updateObservers() {
        for (DataSetObserver dso : observers) {
            dso.onChanged();
        }
    }

    private void updatePathContents() {
        if (pathContents == null) {
            pathContents = new ArrayList<String>();
        }
        pathContents.clear();
        if (path.list() != null) {
            for (String s : path.list()) {
                if (s.charAt(0) != '.') {
                    pathContents.add(s);
                }
            }
        }
        Collections.sort(pathContents);
        pathContents.add(0, "..");
    }
}
