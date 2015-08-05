package uk.addie.xyzzy.zobjects;

import uk.addie.xyzzy.error.Error;
import uk.addie.xyzzy.header.Header;
import uk.addie.xyzzy.state.Memory;
import uk.addie.xyzzy.util.Bit;
import android.util.Log;

public class ZProperty {
  ZProperty(final int offset) {
    this.offset = offset;
  }

  private final int offset;

  public int getNextProperty(final int property) {
    int propOffset;
    if (property == 0) { // get first property
      propOffset = offset + (Memory.current().buff().get(offset)) * 2 + 1;
      final int sizeByte = Memory.current().buff().get(propOffset);
      if (Header.VERSION.value() > 3) {
        return sizeByte & 0x3f;
      }
      return sizeByte & 31;
    }
    propOffset = getPropertyAddress(property);
    if (Header.VERSION.value() > 3) {
      final int size;
      size = calcSize(propOffset);
      if (size <= 2) {
        propOffset += size + 1;
      } else {
        propOffset += size + 2;
      }
      final int sizeByte = Memory.current().buff().get(propOffset);
      return sizeByte & 0x3f;
    }
    final int sizeByte = Memory.current().buff().get(propOffset);
    propOffset += (sizeByte / 32) + 2;
    return Memory.current().buffer.get(propOffset) & 31;
  }

  public int getProperty(final int number) {
    final int prop = getPropertyAddress(number);
    if (prop == 0) {
      return Memory.current().buff().getShort(Header.OBJECTS.value() + (number - 1) * 2);
    }
    if (Header.VERSION.value() > 3) {
      return returnV4property(prop);
    }
    return returnV3property(prop);
  }

  public int getPropertyAddress(final int number) {
    int prop = offset + (Memory.current().buff().get(offset) & 0xff) * 2 + 1;
    while (true) {
      final int sn;
      final int thisnumber;
      final int size;
      if (Header.VERSION.value() > 3) {
        sn = Memory.current().buff().get(prop) & 0xff;
        thisnumber = sn & 0x3f;
        size = calcSize(prop);
      } else {
        sn = Memory.current().buff().get(prop) & 0xff;
        thisnumber = sn & 31;
        size = sn / 0x20 + 1;
      }
      if (thisnumber == number) {
        return prop;
      }
      if (thisnumber < number) {
        return 0;
      }
      if (size <= 2 || Header.VERSION.value() <= 3) {
        prop += size + 1;
      } else {
        prop += size + 2;
      }
    }
  }

  public void putProperty(final int number, final int value) {
    final int prop = getPropertyAddress(number);
    if (prop == 0) {
      Error.PUT_PROP_0.invoke();
      return;
    }
    if (Header.VERSION.value() > 3) {
      putV4property(value, prop);
    } else {
      putV3property(value, prop);
    }
  }

  @Override public String toString() {
    return ZText.encodedAtOffset(offset + 1);
  }

  public static int getPropLen(final int prop) {
    if (prop == 0) {
      return 0;
    }
    final int sn;
    try {
      sn = Memory.current().buff().get(prop);
    } catch (final IndexOutOfBoundsException ioobe) {
      Log.e("Xyzzy", "ZProperty.calcProplenSize:", ioobe);
      return 0;
    }
    final int size;
    if (Header.VERSION.value() >= 4) {
      size = calculateV4Size(prop, sn);
    } else {
      size = calculateV3Size(sn);
    }
    return size;
  }

  private static int calcSize(final int prop) {
    final int sn = Memory.current().buff().get(prop) & 0xff;
    int size;
    if (Bit.bit7(sn)) {
      size = Memory.current().buff().get(prop + 1) & 0x3f;
      if (size == 0) {
        size = 64;
      }
    } else if (Bit.bit6(sn)) {
      size = 2;
    } else {
      size = 1;
    }
    return size;
  }

  private static int calculateV3Size(final int sn) {
    return (sn / 32) + 1;
  }

  private static int calculateV4Size(final int prop, final int sn) {
    int size;
    if (Bit.bit7(sn)) {
      size = Memory.current().buff().get(prop) & 0x3f;
      if (size == 0) {
        size = 64;
      }
    } else if (Bit.bit6(sn)) {
      size = 2;
    } else {
      size = 1;
    }
    return size;
  }

  private static void putV3property(final int value, final int prop) {
    final int size = Memory.current().buffer.get(prop) / 32 + 1;
    if (size == 2) {
      Memory.current().buff().putShort(prop + 1, (short) value);
    } else if (size == 1) {
      Memory.current().buff().put(prop + 1, (byte) value);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private static void putV4property(final int value, final int prop) {
    final int size = calcSize(prop);
    if (size == 2) {
      Memory.current().buff().putShort(prop + 1, (short) value);
    } else if (size == 1) {
      Memory.current().buff().put(prop + 1, (byte) value);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private static int returnV3property(final int prop) {
    final int size = (Memory.current().buffer.get(prop) / 0x20) + 1;
    if (size == 2) {
      return Memory.current().buff().getShort(prop + 1);
    } else if (size == 1) {
      return Memory.current().buff().get(prop + 1);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private static int returnV4property(final int prop) {
    final int size = calcSize(prop);
    if (size == 2) {
      return Memory.current().buff().getShort(prop + 1);
    } else if (size == 1) {
      return Memory.current().buff().get(prop + 1);
    } else {
      throw new UnsupportedOperationException();
    }
  }
}