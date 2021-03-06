package uk.addie.xyzzy.zobjects;

import java.util.ArrayList;
import java.util.List;

import uk.addie.xyzzy.header.Header;
import uk.addie.xyzzy.state.Memory;
import android.util.Log;

public class ZText {
  private static final char[] a0 = { ' ', '~', '~', '~', '~', '~', 'a', 'b', 'c', 'd', 'e', 'f',
      'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
      'y', 'z' };

  private static final char[] a1 = { ' ', '~', '~', '~', '~', '~', 'A', 'B', 'C', 'D', 'E', 'F',
      'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
      'Y', 'Z' };

  private static final char[] a2 = { ' ', '~', '~', '~', '~', '~', ' ', '\n', '0', '1', '2', '3',
      '4', '5', '6', '7', '8', '9', '.', ',', '!', '?', '_', '#', '\'', '"', '/', '\\', '-', ':',
      '(', ')' };

  private static final char[] unicode = { // starts at 155
  'ä', 'ö', 'ü', 'Ä', 'Ö', 'Ü', 'ß', '»', '«', 'ë', // 164
      'ï', 'ÿ', 'Ë', 'Ï', 'á', 'é', 'í', 'ó', 'ú', 'ý', // 174
      'Á', 'É', 'Í', 'Ó', 'Ú', 'Ý', 'à', 'è', 'ì', 'ò', // 184
      'ù', 'À', 'È', 'Ì', 'Ò', 'Ù', 'â', 'ê', 'î', 'ô', // 194
      'û', 'Â', 'Ê', 'Î', 'Ô', 'Û', 'å', 'Å', 'ø', 'Ø', // 204
      'ã', 'ñ', 'õ', 'Ã', 'Ñ', 'Õ', 'æ', 'Æ', 'ç', 'Ç', // 214
      'þ', 'ð', 'Þ', 'Ð', '£', 'œ', 'Œ', '¡', '¿' // 223
  };

  private static int lastAlphabet = 0;

  public static int bytePosition;

  public static String encodedAtOffset(final int offset) {
    final List<Character> cb = new ArrayList<Character>();
    int alphabet = Header.ALPHABET.value(Memory.current().buff()); // TODO v5 alternative alphabets
    if (Header.VERSION.value() >= 5 && alphabet != lastAlphabet) {
      lastAlphabet = alphabet;
      Log.w("Xyzzy", "Alternative alphabet in use... @" + alphabet);
      String foreignAlphabet = alphabet != 0 ? unencodedAtOffset(alphabet)
          : "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ \n0123456789.,!?_#'\"/\\-:()\0";
      Log.e("Xyzzy", "Foreign? " + foreignAlphabet + " (" + foreignAlphabet.length() + ")");
      int count = 0;
      for (int x = 6; x < a0.length; x++) {
        a0[x] = foreignAlphabet.charAt(count++);
      }
      for (int x = 6; x < a1.length; x++) {
        a1[x] = foreignAlphabet.charAt(count++);
      }
      for (int x = 6; x < a2.length; x++) {
        a2[x] = foreignAlphabet.charAt(count++);
      }
      a2[7] = '\n';
    }
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
        addCharOrUnicode(sb, zchar);
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
          addCharOrUnicode(sb, a0[c]);
          break;
        case 1:
          addCharOrUnicode(sb, a1[c]);
          break;
        case 2:
          if (c == 6) {
            zchar = -1;
            break;
          }
          addCharOrUnicode(sb, a2[c]);
          break;
        }
      }
      set = 0;
    }
    bytePosition = b;
    // Log.v("Xyzzy", "Encoded string (alphabet " + alphabet + "):" + sb.toString());
    return sb.toString();
  }

  public static long encodeString(final String string) {
    final int wordLength = Header.VERSION.value() <= 3 ? 6 : 9;
    final byte chars[] = new byte[wordLength];
    int position = 0;
    for (int i = 0; i < string.length(); i++) {
      final char character = string.charAt(i);
      final byte a0Value = zvalue(a0, character);
      if (a0Value != 0) {
        chars[position] = a0Value;
        position++;
      } else {
        final byte a2value = zvalue(a2, character);
        if (a2value != 0) { // it's in A2
          chars[position++] = 5;
          if (position < chars.length) {
            chars[position++] = a2value;
          }
        } else { // need to encode ASCII. Not efficient.
          final byte hifive = (byte) (character >> 5);
          final byte lofive = (byte) (character & 31);
          tryInArray(position++, chars, (byte) 5);
          tryInArray(position++, chars, (byte) 6);
          tryInArray(position++, chars, hifive);
          tryInArray(position++, chars, lofive);
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

  public static void tokeniseInputToBuffers(final int text, final int parse,
      final String inputString, final int dictionary, final int flag) {
    int offset = text + 2;
    final StringBuffer currentWord = new StringBuffer();
    final Dictionary d = (dictionary == 0 ? Dictionary.DEFAULT : new Dictionary(dictionary));
    final ParseTable pt = new ParseTable(parse, d);
    for (final char ch : inputString.toCharArray()) {
      Memory.current().buff().put(offset, (byte) ch);
      final int wordSplit = d.wordSplit(ch);
      if (parse != 0 && (ch == ' ' || ch == '\n' || wordSplit != 0) && currentWord.length() != 0) {
        final String wordFound = currentWord.toString();
        pt.parse(wordFound, offset - text - wordFound.length(), flag);
        currentWord.setLength(0);
      }
      if (ch == ' ' || ch == '\n') {
        offset++;
        continue;
      }
      if (wordSplit != 0) {
        pt.parse(Character.toString(ch), offset - text, flag);
        offset++;
        continue;
      }
      if (parse != 0) {
        currentWord.append(ch);
      }
      offset++;
    }
    if (currentWord.length() != 0) {
      final String wordFound = currentWord.toString();
      pt.parse(wordFound, offset - text - wordFound.length(), flag);
    }
    if (Header.VERSION.value() <= 4) { // NULL terminate
      Memory.current().buffer.put(offset, 0);
    } else { // newline terminate
      Memory.current().buffer.put(offset, '\n');
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

  private static void addCharOrUnicode(final StringBuilder sb, int zchar) {
    int extension = Header.EXTENSION_TABLE.value(Memory.current().buff());
    /* TODO v5 alternative alphabets */
    if (extension != 0) {
      Log.w("Xyzzy", "Alternative extensions...");
    }
    if (zchar >= 155 && zchar - 155 < unicode.length) {
      sb.append(unicode[zchar - 155]);
    } else {
      sb.append((char) zchar);
    }
  }

  private static void tryInArray(final int i, final byte[] chars, final byte j) {
    if (i >= chars.length) {
      return;
    }
    chars[i] = j;
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
