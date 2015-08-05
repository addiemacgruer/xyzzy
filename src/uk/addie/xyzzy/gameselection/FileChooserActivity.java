package uk.addie.xyzzy.gameselection;

import java.io.File;
import java.io.IOException;
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
  private List<String> pathContents;

  private final List<DataSetObserver> observers = new ArrayList<DataSetObserver>();

  private int textSize;

  @Override public boolean areAllItemsEnabled() {
    return true;
  }

  @Override public int getCount() {
    return pathContents.size();
  }

  @Override public Object getItem(final int position) {
    return null;
  }

  @Override public long getItemId(final int position) {
    return position;
  }

  @Override public int getItemViewType(final int position) {
    return 0;
  }

  @Override public View getView(final int position, final View convertView, final ViewGroup parent) {
    final TextView tv = selectionPageTextView();
    final String itemName = pathContents.get(position);
    if (itemName.equals("..")) {
      tv.setText(".. (up to previous directory)");
    } else {
      tv.setText(itemName);
    }
    final File subfile = new File(path, itemName);
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

  @Override public boolean isEnabled(final int position) {
    return true;
  }

  @Override public void onClick(final View v) {
    File file = (File) v.getTag();
    Log.d("Xyzzy", "Clicked on:" + file.toString());
    try {
      file = new File(file.getCanonicalPath());
    } catch (final IOException e) {
      Log.e("Xyzzy", "FileChooserActivity.onClick", e);
    } // remove ".."
    if (file.toString().length() == 0) { // ROOT
      file = new File("/");
    }
    Log.d("Xyzzy", "Canonical:" + file);
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

  @Override public void registerDataSetObserver(final DataSetObserver observer) {
    observers.add(observer);
  }

  @Override public void unregisterDataSetObserver(final DataSetObserver observer) {
    observers.remove(observer);
  }

  @Override protected void onCreate(final Bundle savedInstanceState) {
    if (pathContents == null) {
      updatePathContents();
    }
    super.onCreate(savedInstanceState);
    setContentView(R.layout.filechooser);
    final ListView lv = (ListView) findViewById(R.id.filechooser);
    setTitle(path.toString());
    lv.setAdapter(this);
  }

  @Override protected void onResume() {
    super.onResume();
    textSize = (Integer) Preferences.TEXT_SIZE.getValue(this);
  }

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

  private TextView selectionPageTextView() {
    final TextView tv = new TextView(this);
    tv.setTextSize(textSize * 2);
    tv.setPadding(textSize * 2, textSize * 2, textSize * 2, textSize * 2);
    return tv;
  }

  private void updateObservers() {
    for (final DataSetObserver dso : observers) {
      dso.onChanged();
    }
  }

  private void updatePathContents() {
    if (pathContents == null) {
      pathContents = new ArrayList<String>();
    }
    pathContents.clear();
    if (path.list() != null) {
      for (final String s : path.list()) {
        if (s.charAt(0) != '.') {
          final File f = new File(path, s);
          if (f.isDirectory()) {
            pathContents.add(s);
            continue;
          }
          for (final String suffix : SUFFIXES) {
            if (s.endsWith(suffix)) {
              pathContents.add(s);
              break;
            }
          }
        }
      }
    }
    Collections.sort(pathContents);
    Log.d("Xyzzy", "Path ='" + path.toString() + "'");
    if (!path.toString().equals("/")) {
      pathContents.add(0, "..");
    }
  }

  private static File path = Environment.getExternalStorageDirectory();

  private static final String[] SUFFIXES = { ".dat", ".z1", ".z2", ".z3", ".z4", ".z5", ".z6",
      ".z8", ".zblorb" };
}
