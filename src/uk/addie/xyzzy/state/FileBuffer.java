
package uk.addie.xyzzy.state;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import uk.addie.xyzzy.MainActivity;
import uk.addie.xyzzy.header.Header;
import android.annotation.SuppressLint;
import android.util.Log;

@SuppressLint("UseSparseArrays") public class FileBuffer implements Serializable {
    private static final long serialVersionUID = 1L;

    private static int byteInt(byte[] array, int start, int length) {
        int rval = 0;
        for (int i = start; i < start + length; i++) {
            rval <<= 8;
            final int byteAtI = array[i] & 0xff;
            rval += byteAtI;
        }
        return rval;
    }

    private static String byteText(byte[] array, int start, int length) {
        StringBuffer sb = new StringBuffer();
        for (int i = start; i < start + length; i++) {
            sb.append((char) array[i]);
        }
        Log.d("Xyzzy", "byteText:" + sb.toString());
        return sb.toString();
    }

    private transient boolean[]          changed;
    private transient Map<Integer, Byte> changes = new HashMap<Integer, Byte>();
    private final int                    staticMemory;
    private final String                 storyPath;
    private transient byte[]             zmp;

    FileBuffer(final String storyPath) {
        this.storyPath = storyPath;
        loadUpFile(storyPath);
        staticMemory = Header.DYNAMIC_SIZE.value(this);
    }

    int capacity() {
        return zmp.length;
    }

    public int get(final int i) {
        if (i < staticMemory && changed[i]) {
            return changes.get(i) & 0xff;
        }
        if (i >= zmp.length) {
            Log.e("Xyzzy", "Array index out-of-bounds (" + i + "/" + zmp.length + ")");
            return 0;
        }
        return zmp[i] & 0xff;
    }

    public int getShort(final int b) {
        return (get(b) << 8) + get(b + 1);
    }

    @SuppressWarnings("resource") private void loadUpFile(final String path) {
        InputStream fis;
        try {
            if (path.charAt(0) == '@') {
                fis = MainActivity.activity.getAssets().open(path.substring(1));
            } else {
                fis = new FileInputStream(path);
            }
            final long length = fis.available(); //storyFile.length();
            Log.d("Xyzzy", path + ": " + length + " bytes read");
            zmp = new byte[(int) length];
            fis.read(zmp);
            fis.close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        if (byteText(zmp, 0, 4).equals("FORM")) { // then it's a zblorb
            Log.i("Xyzzy", "Blorb file");
            performBlorbCrop();
        }
        changed = new boolean[zmp.length];
    }

    private void performBlorbCrop() {
        int offset = 0;
        long fileSize = byteInt(zmp, offset += 4, 4);
        if (!byteText(zmp, offset += 4, 4).equals("IFRS")) {
            throw new UnsupportedOperationException("Not a zblorb file");
        }
        if (!byteText(zmp, offset += 4, 4).equals("RIdx")) {
            throw new UnsupportedOperationException("Missing zblorb resource index");
        }
        int rIdxLength = byteInt(zmp, offset += 4, 4);
        int numResources = byteInt(zmp, offset += 4, 4);
        int execOffset = 0;
        for (int i = 0; i < numResources; i++) {
            String resourceName = byteText(zmp, offset += 4, 4);
            int resourceNumber = byteInt(zmp, offset += 4, 4);
            int resourceOffset = byteInt(zmp, offset += 4, 4);
            Log.d("Xyzzy", "Zblorb resource:" + resourceName + "," + resourceNumber + "," + resourceOffset);
            if (resourceName.equals("Exec")) {
                execOffset = resourceOffset;
            }
        }
        if (execOffset == 0) {
            throw new IllegalArgumentException("No Exec resource in ZBlorb");
        }
        String execType = byteText(zmp, execOffset, 4);
        if (!execType.equals("ZCOD")) {
            throw new IllegalArgumentException("EXEC type is not ZCOD");
        }
        int codeSize = byteInt(zmp, execOffset + 4, 4);
        byte[] zcode = Arrays.copyOfRange(zmp, execOffset + 8, execOffset + 8 + codeSize);
        zmp = zcode;
    }

    public void put(final int offset, final int s) {
        //        if (offset == 0x10 && Header.VERSION.value() > 3 && Bit.bit1(s) != fixedWidthFont) {
        //            fontBitInHeader(s);
        //        }
        if (offset < staticMemory && changed[offset]) {
            final byte underlying = zmp[offset];
            if (underlying == (byte) s) {
                changes.remove(offset);
                changed[offset] = false;
            } else {
                changes.put(offset, (byte) s);
            }
        } else if (offset < staticMemory && zmp[offset] != (byte) s) {
            changes.put(offset, (byte) s);
            changed[offset] = true;
        } else if (offset > staticMemory) {
            final String errorString = "Tried to write above static memory mark! " + offset + "/" + staticMemory;
            Log.e("Xyzzy", errorString);
            throw new IllegalArgumentException(errorString);
        }
    }

    public void putShort(final int offset, final int s) {
        put(offset, (s & 0xff00) >> 8);
        put(offset + 1, s & 0xff);
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        loadUpFile(storyPath);
        final int total = in.readInt();
        changes = new HashMap<Integer, Byte>();
        for (int i = 0; i < total; i++) {
            final int key = in.readInt();
            final byte val = in.readByte();
            changes.put(key, val);
            changed[key] = true;
        }
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(changes.size());
        for (final int i : changes.keySet()) {
            out.writeInt(i);
            out.writeByte(changes.get(i));
        }
    }
}
