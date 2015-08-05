package uk.addie.xyzzy.zobjects;

import uk.addie.xyzzy.state.Memory;
import android.util.Log;

class ParseTable {
  ParseTable(final int offset, final Dictionary dictionary) {
    this.offset = offset;
    this.dictionary = dictionary;
    Memory.current().buff().put(offset + 1, (byte) 0);
  }

  private final Dictionary dictionary;

  private final int offset;

  void parse(final String wordFound, final int stringArrayOffset, final int flag) {
    if (wordCount() == maxWords()) {
      Log.e("Xyzzy", "Too many words to parse");
      return;
    }
    final int location = offset + 2 + 4 * wordCount();
    final int address = dictionary.addressOfWord(wordFound);
    Log.d("Xyzzy", "Word:" + wordFound + " @" + address);
    if (address != 0 || flag == 0) {
      Memory.current().buff().putShort(location, (short) address);
      Memory.current().buff().put(location + 2, (byte) wordFound.length());
      Memory.current().buff().put(location + 3, (byte) stringArrayOffset);
    }
    incWordCount();
  }

  private void incWordCount() {
    Memory.current().buff().put(offset + 1, (byte) (wordCount() + 1));
  }

  private int maxWords() {
    return Memory.current().buff().get(offset);
  }

  private int wordCount() {
    return Memory.current().buff().get(offset + 1);
  }
}
