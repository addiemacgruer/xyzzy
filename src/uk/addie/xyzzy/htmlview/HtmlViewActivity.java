package uk.addie.xyzzy.htmlview;

import java.io.IOException;
import java.util.Scanner;

import uk.addie.xyzzy.R;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class HtmlViewActivity extends Activity {
  private WebView mWebView;

  @Override protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.web);
    mWebView = (WebView) findViewById(R.id.webview);
    fillContent();
  }

  private void fillContent() {
    Scanner ins;
    final StringBuilder output = new StringBuilder();
    try {
      ins = new Scanner(getAssets().open("help.html"), "utf-8");
      while (ins.hasNext()) {
        output.append(ins.nextLine() + " ");
      }
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    mWebView.loadDataWithBaseURL(null, output.toString(), "text/html", "utf-8", null);
  }
}
