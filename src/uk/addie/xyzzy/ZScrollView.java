
package uk.addie.xyzzy;

import uk.addie.xyzzy.preferences.Preferences;
import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

public class ZScrollView extends ScrollView {
    private static int       scrollSpeed  = 3;
    private final static int scrollUpdate = 10;

    static void updateScrollingSpeed() {
        scrollSpeed = (Integer) Preferences.AUTOSCROLL_SPEED.getValue(MainActivity.activity);
    }

    int                    destinationX;
    int                    destinationY;
    final Handler          handler  = new Handler();
    private final Runnable runnable = new Runnable() {
                                        @Override public void run() {
                                            int currentX = getScrollX();
                                            int currentY = getScrollY();
                                            boolean cont = false;
                                            if (destinationX < currentX) {
                                                currentX = Math.max(currentX - scrollSpeed, destinationX);
                                                cont = true;
                                            } else if (destinationX > currentX) {
                                                currentX = Math.min(currentX + scrollSpeed, destinationX);
                                                cont = true;
                                            }
                                            if (destinationY < currentY) {
                                                currentY = Math.max(currentY - scrollSpeed, destinationY);
                                                cont = true;
                                            } else if (destinationY > currentY) {
                                                currentY = Math.min(currentY + scrollSpeed, destinationY);
                                                cont = true;
                                            }
                                            scrollTo(currentX, currentY);
                                            if (cont) {
                                                handler.postDelayed(this, scrollUpdate);
                                            } else {
                                                running = false;
                                            }
                                        }
                                    };
    boolean                running  = false;

    public ZScrollView(final Context context) {
        super(context);
        setOverScrollMode(View.OVER_SCROLL_ALWAYS);
    }

    public ZScrollView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setOverScrollMode(View.OVER_SCROLL_ALWAYS);
    }

    public ZScrollView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        setOverScrollMode(View.OVER_SCROLL_ALWAYS);
    }

    @Override protected int computeScrollDeltaToGetChildRectOnScreen(final Rect rect) {
        final int rval = super.computeScrollDeltaToGetChildRectOnScreen(rect);
        destinationX = getScrollX();
        destinationY = getScrollY() + rval;
        if (!running) {
            running = true;
            handler.postDelayed(runnable, scrollUpdate);
        }
        return 0;
    }

    public void smoothScrollToBottom() {
        destinationY = this.getBottom();
        if (!running) {
            running = true;
            handler.postDelayed(runnable, scrollUpdate);
        }
    }
}
