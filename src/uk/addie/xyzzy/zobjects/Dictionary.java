
package uk.addie.xyzzy.zobjects;

import java.util.HashMap;
import java.util.Map;

import uk.addie.xyzzy.header.Header;
import uk.addie.xyzzy.state.Memory;

public class Dictionary {
    public static Dictionary DEFAULT;

    public static void initDefault() {
        DEFAULT = new Dictionary(Header.DICTIONARY.value());
    }

    private final Map<String, Integer> entryMap = new HashMap<String, Integer>();
    private final int                  offset;

    private Dictionary(final int offset) {
        this.offset = offset;
        buildHashMap();
    }

    int addressOfWord(final String aWord) {
        final String word = aWord.substring(0, Math.min(aWord.length(), 9));
        if (entryMap.containsKey(word)) {
            return entryMap.get(word);
        }
        return 0;
    }

    private void buildHashMap() {
        final int entryLength = entryLength();
        final int moffset = offset + numberOfInputCodes() + 4;
        for (int wordCount = 0, total = numberOfEntries(); wordCount < total; wordCount++) {
            final int wordByte = moffset + wordCount * entryLength;
            final String word = ZText.encodedAtOffset(wordByte);
            entryMap.put(word, wordByte);
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
