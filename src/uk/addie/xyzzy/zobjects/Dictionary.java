
package uk.addie.xyzzy.zobjects;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import uk.addie.xyzzy.header.Header;
import uk.addie.xyzzy.state.Memory;
import android.util.Log;

public class Dictionary {
    public static Dictionary DEFAULT;

    public static void initDefault() {
        DEFAULT = new Dictionary(Header.DICTIONARY.value());
    }

    private final Map<Long, Integer> entryMap = new HashMap<Long, Integer>();
    private final int                offset;

    Dictionary(final int offset) {
        this.offset = offset;
        buildHashMap();
    }

    int addressOfWord(final String aWord) {
        final long encoded = ZText.encodeString(aWord.toLowerCase(Locale.UK));
        if (entryMap.containsKey(encoded)) {
            return entryMap.get(encoded);
        }
        return 0;
    }

    private void buildHashMap() {
        final int entryLength = entryLength();
        final int bytesPerWord = Header.VERSION.value() <= 3 ? 4 : 6;
        final int moffset = offset + numberOfInputCodes() + 4;
        for (int wordCount = 0, total = numberOfEntries(); wordCount < total; wordCount++) {
            final int wordByte = moffset + wordCount * entryLength;
            // TODO $signs &c.
            final String word = ZText.encodedAtOffset(wordByte);
            long value = 0;
            for (int i = 0; i < bytesPerWord; i++) {
                value <<= 8;
                value += Memory.current().buffer.get(wordByte + i);
            }
            final long encoded = ZText.encodeString(word);
            if (value != encoded) {
                Log.d("Xyzzy", "Dictionary:" + word + " " + Long.toHexString(value) + "=" + Long.toHexString(encoded)
                        + "?");
            }
            entryMap.put(value, wordByte);
        }
    }

    private int entryLength() {
        return Memory.current().buff().get(offset + numberOfInputCodes() + 1);
    }

    private int numberOfEntries() {
        return Memory.current().buff().getShort(offset + numberOfInputCodes() + 2);
    }

    private int numberOfInputCodes() {
        return Memory.current().buff().get(offset) & 0xff;
    }

    public int wordSplit(final char ch) {
        final int dictStart = offset;
        for (int i = 0, codes = numberOfInputCodes(); i < codes; i++) {
            if (Memory.current().buff().get(dictStart + i + 1) == ch) {
                return dictStart + i + 1;
            }
        }
        return 0;
    }
}
