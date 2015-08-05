package uk.addie.xyzzy.zobjects;

import android.graphics.Typeface;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

public enum TextStyle {
  BOLD {
    @Override CharacterStyle characterStyle() {
      return new StyleSpan(Typeface.BOLD);
    }
  },
  FIXED_PITCH {
    @Override CharacterStyle characterStyle() {
      return new TypefaceSpan("monospace");
    }
  },
  ITALIC {
    @Override CharacterStyle characterStyle() {
      return new StyleSpan(Typeface.ITALIC);
    }
  },
  REVERSE_VIDEO {
    @Override CharacterStyle characterStyle() {
      return new TypefaceSpan("monospace");
    }
  };
  abstract CharacterStyle characterStyle();
}
