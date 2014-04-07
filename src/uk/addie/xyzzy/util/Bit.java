package uk.addie.xyzzy.util;
public class Bit {
    public static boolean bit0(final int b) {
        return (b & 0x1) != 0;
    }

    public static boolean bit1(final int b) {
        return (b & 0x2) != 0;
    }

    public static boolean bit2(final int b) {
        return (b & 0x4) != 0;
    }

    public static boolean bit3(final int b) {
        return (b & 0x8) != 0;
    }

    public static boolean bit4(final int b) {
        return (b & 0x10) != 0;
    }

    public static boolean bit5(final int b) {
        return (b & 0x20) != 0;
    }

    public static boolean bit6(final int b) {
        return (b & 0x40) != 0;
    }

    public static boolean bit7(final int b) {
        return (b & 0x80) != 0;
    }

    public static int low(final int b, final int low) {
        switch (low) {
        case 0:
            return 0;
        case 1:
            return b & 0x1;
        case 2:
            return b & 0x3;
        case 3:
            return b & 0x7;
        case 4:
            return b & 0xf;
        case 5:
            return b & 0x1f;
        case 6:
            return b & 0x3f;
        case 7:
            return b & 0x7f;
        case 8:
            return b & 0xff;
        default:
            throw new UnsupportedOperationException("low bits?" + low);
        }
    }
}
