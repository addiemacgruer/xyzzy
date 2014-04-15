
package uk.addie.xyzzy.htmlview;

import java.io.IOException;
import java.util.Scanner;

import uk.addie.xyzzy.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * displays the html file from assets/*.html as specified in {@link Statics#htmlPage} as a new Android activity. Used
 * for the help file and the license page
 */
public class HtmlViewActivity extends Activity { // NO_UCD (instantiated by
                                                 // Android Runtime)
    class HelpClient extends WebViewClient {
        @Override public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
            fillContent();
            return true;
        }
    }

    private WebView mWebView;

    void fillContent() {
        Scanner ins;
        final StringBuilder output = new StringBuilder();
        try {
            ins = new Scanner(this.getAssets().open("help.html"), "utf-8");
            while (ins.hasNext()) {
                output.append(ins.nextLine() + " ");
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        mWebView.loadDataWithBaseURL(null, output.toString(), "text/html", "utf-8", null);
    }

    /** Called by Android when ready to display. */
    @Override protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.web);
        mWebView = (WebView) findViewById(R.id.webview);
        // mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new HelpClient());
        fillContent();
    }
}
