
package uk.addie.xyzzy.zobjects;

import uk.addie.xyzzy.error.Error;
import uk.addie.xyzzy.header.Header;
import uk.addie.xyzzy.state.Memory;
import uk.addie.xyzzy.util.Bit;
import android.util.Log;

public class ZProperty {
    public static int calcProplenSize(final int prop) {
        if (prop == 0) {
            return 0;
        }
        final int sn;
        try {
            sn = Memory.current().buff().get(prop) & 0xff;
        } catch (final IndexOutOfBoundsException ioobe) {
            Log.e("Xyzzy", "ZProperty.calcProplenSize:", ioobe);
            return 0;
        }
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

    private final int offset;

    ZProperty(final int offset) {
        this.offset = offset;
    }

    public ZProperty(final ZObject zo) {
        offset = zo.properties();
    }

    public int getNextProperty(final int property) {
        int propOffset;
        if (property == 0) { // get first property
            propOffset = offset + (Memory.current().buff().get(offset) & 0xff) * 2 + 1;
        } else {
            propOffset = getPropertyAddress(property);
            final int size = calcSize(propOffset);
            if (size <= 2) {
                propOffset += size + 1;
            } else {
                propOffset += size + 2;
            }
        }
        return Memory.current().buff().get(propOffset) & 0x3f;
    }

    public int getProperty(final int number) {
        final int prop = getPropertyAddress(number);
        if (prop == 0) { //Should take from defaults
            return Memory.current().buff().getShort(Header.OBJECTS.value() + (number - 1) * 2);
        }
        final int size = calcSize(prop);
        if (size == 2) {
            return Memory.current().buff().getShort(prop + 1) & 0xffff;
        } else if (size == 1) {
            return Memory.current().buff().get(prop + 1) & 0xff;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public int getPropertyAddress(final int number) {
        int prop = offset + (Memory.current().buff().get(offset) & 0xff) * 2 + 1;
        while (true) {
            final int sn = Memory.current().buff().get(prop) & 0xff;
            final int thisnumber = sn & 0x3f;
            final int size = calcSize(prop);
            if (thisnumber == number) {
                return prop;
            }
            if (thisnumber < number) { // run out of things to test.  Should take from defaults
                return 0;
            }
            if (size <= 2) {
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
        final int size = calcSize(prop);
        if (size == 2) {
            Memory.current().buff().putShort(prop + 1, (short) value);
        } else if (size == 1) {
            Memory.current().buff().put(prop + 1, (byte) value);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override public String toString() {
        //        int length = (FastMem.CURRENT.zmp.get(offset) & 0xff) * 2;
        return ZText.encodedAtOffset(offset + 1);
    }
}
