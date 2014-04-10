
package uk.addie.xyzzy.zobjects;

import uk.addie.xyzzy.state.Memory;
import android.util.Log;

class ParseTable {
    private final int        offset;
    private final Dictionary dictionary;

    ParseTable(final int offset, final Dictionary dictionary) {
        this.offset = offset;
        this.dictionary = dictionary;
        Memory.current().buff().put(offset + 1, (byte) 0);
    }

    private void incWordCount() {
        Memory.current().buff().put(offset + 1, (byte) (wordCount() + 1));
    }

    private int maxWords() {
        return Memory.current().buff().get(offset);
    }

    void parse(final String wordFound, final int stringArrayOffset) {
        if (wordCount() == maxWords()) {
            Log.e("Xyzzy", "Too many words to parse");
            return;
        }
        final int location = offset + 2 + 4 * wordCount();
        final int address = dictionary.addressOfWord(wordFound);
        Memory.current().buff().putShort(location, (short) address);
        Memory.current().buff().put(location + 2, (byte) wordFound.length());
        Memory.current().buff().put(location + 3, (byte) stringArrayOffset);
        incWordCount();
    }

    private int wordCount() {
        return Memory.current().buff().get(offset + 1);
    }
}
