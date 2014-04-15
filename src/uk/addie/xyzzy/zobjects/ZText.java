
package uk.addie.xyzzy.zobjects;

import java.util.ArrayList;
import java.util.List;

import uk.addie.xyzzy.header.Header;
import uk.addie.xyzzy.state.Memory;

public class ZText {
    private static char[] a0 = { ' ', '~', '~', '~', '~', '~', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
            'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };
    private static char[] a1 = { ' ', '~', '~', '~', '~', '~', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K',
            'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };
    private static char[] a2 = { ' ', '~', '~', '~', '~', '~', ' ', '\n', '0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9', '.', ',', '!', '?', '_', '#', '\'', '"', '/', '\\', '-', ':', '(', ')' };
    public static int     bytePosition;

    public static String encodedAtOffset(final int offset) {
        final List<Character> cb = new ArrayList<Character>();
        //        int alphabet = Header.H_ALPHABET.value(FastMem.CURRENT.zmp); //TODO v5 alternative alphabets
        int b = 0;
        for (b = offset; true; b += 2) {
            final int word = Memory.current().buff().getShort(b) & 0xffff;
            final int first = (word & 0x7c00) >> 10;
            final int second = (word & 0x3e0) >> 5;
            final int third = word & 0x1f;
            cb.add((char) first);
            cb.add((char) second);
            cb.add((char) third);
            if ((word & 0x8000) == 0x8000) {
                break;
            }
        }
        final StringBuilder sb = new StringBuilder();
        int set = 0;
        int abbreviation = 0;
        int zchar = 0;
        for (final char c : cb) {
            if (abbreviation != 0) {
                final int ptr_addr = Header.ABBREVIATIONS.value() + 64 * (abbreviation - 1) + 2 * c;
                final int abbrOffset = Memory.current().buff().getShort(ptr_addr) & 0xffff;
                sb.append(ZText.encodedAtOffset(abbrOffset << 1));
                abbreviation = 0;
                continue;
            }
            if (zchar == -1) {
                zchar = c << 5;
                continue;
            } else if (zchar != 0) {
                zchar |= c;
                sb.append((char) zchar);
                zchar = 0;
                continue;
            }
            switch (c) {
            case 1:
            case 2:
            case 3:
                abbreviation = c;
                continue;
            case 4:
                set = 1;
                continue;
            case 5:
                set = 2;
                continue;
            default:
                switch (set) {
                case 0:
                default:
                    sb.append(a0[c]);
                    break;
                case 1:
                    sb.append(a1[c]);
                    break;
                case 2:
                    if (c == 6) {
                        zchar = -1;
                        break;
                    }
                    sb.append(a2[c]);
                    break;
                }
            }
            set = 0;
        }
        bytePosition = b;
        return sb.toString();
    }

    public static long encodeString(final String string) {
        final int wordLength = Header.VERSION.value() <= 3 ? 6 : 9;
        final byte chars[] = new byte[9];
        int position = 0;
        for (int i = 0; i < string.length(); i++) {
            final char character = string.charAt(i);
            final byte a0Value = zvalue(a0, character);
            if (a0Value != 0) {
                chars[position] = a0Value;
                position++;
            } else {
                chars[position++] = 5;
                if (position < chars.length) {
                    chars[position++] = zvalue(a2, character);
                }
            }
            if (position >= chars.length) {
                break;
            }
        }
        for (; position < chars.length; position++) {
            chars[position] = 5;
        }
        long rval = 0;
        for (int i = 0; i < chars.length; i += 3) {
            rval <<= 16;
            final int shortValue = (chars[i] << 10) + (chars[i + 1] << 5) + chars[i + 2];
            rval += shortValue;
        }
        rval |= 0x8000;
        return rval;
    }

    public static void tokeniseInputToBuffers(final int text, final int parse, final String inputString) {
        int offset = text + 2;
        final StringBuffer currentWord = new StringBuffer();
        final ParseTable pt = new ParseTable(parse, Dictionary.DEFAULT);
        for (final char ch : inputString.toCharArray()) {
            Memory.current().buff().put(offset, (byte) ch);
            final int wordSplit = Dictionary.DEFAULT.wordSplit(ch);
            if (parse != 0 && (ch == ' ' || ch == '\n' || wordSplit != 0) && currentWord.length() != 0) {
                final String wordFound = currentWord.toString();
                pt.parse(wordFound, offset - text - wordFound.length());
                currentWord.setLength(0);
            }
            if (ch == ' ' || ch == '\n') {
                offset++;
                continue;
            }
            if (wordSplit != 0) {
                pt.parse(Character.toString(ch), offset - text);
                offset++;
                continue;
            }
            if (parse != 0) {
                currentWord.append(ch);
            }
            offset++;
        }
    }

    public static String unencodedAtOffset(final int offset) {
        final StringBuilder sb = new StringBuilder();
        int start = offset;
        while (true) {
            final char c = (char) Memory.current().buffer.get(start);
            if (c == 0) {
                break;
            }
            sb.append(c);
            start++;
        }
        return sb.toString();
    }

    private static byte zvalue(final char[] set, final char character) {
        for (int i = 0; i < set.length; i++) {
            if (set[i] == character) {
                return (byte) i;
            }
        }
        return 0;
    }
}
