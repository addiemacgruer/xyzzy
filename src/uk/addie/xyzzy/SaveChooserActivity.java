
package uk.addie.xyzzy;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.addie.xyzzy.preferences.Preferences;
import android.app.Activity;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SaveChooserActivity extends Activity implements ListAdapter, OnClickListener {
    public static final StringBuilder   syncObject   = new StringBuilder();
    public static String                gameName     = "";
    private final List<String>          pathContents = new ArrayList<String>();
    private final List<DataSetObserver> observers    = new ArrayList<DataSetObserver>();
    private int                         textSize;

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
        tv.setText(itemName.substring(gameName.length()));
        tv.setTag(itemName);
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
        String file = (String) v.getTag();
        synchronized (syncObject) {
            syncObject.append(file);
            syncObject.notifyAll();
        }
        finish();
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        updatePathContents();
        synchronized (syncObject) {
            syncObject.setLength(0);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filechooser);
        ListView lv = (ListView) findViewById(R.id.filechooser);
        setTitle("Xyzzy: select save");
        lv.setAdapter(this);
    }

    @Override protected void onResume() {
        super.onResume();
        this.textSize = (Integer) Preferences.TEXT_SIZE.getValue(this);
    }

    @Override protected void onStop() {
        super.onStop();
        synchronized (syncObject) {
            syncObject.notifyAll();
        }
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

    private void updatePathContents() {
        File privateArea = getFilesDir();
        pathContents.clear();
        for (String s : privateArea.list()) {
            if (s.startsWith(gameName)) {
                pathContents.add(s);
            }
        }
        Collections.sort(pathContents);
    }
}
