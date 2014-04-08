
package uk.addie.xyzzy.state;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import uk.addie.xyzzy.MainActivity;
import uk.addie.xyzzy.header.Header;
import android.annotation.SuppressLint;
import android.util.Log;

@SuppressLint("UseSparseArrays") public class FileBuffer implements Serializable {
    private static final long           serialVersionUID = 1L;
    public transient byte[]             zmp;
    public transient boolean[]          changed;
    public String                       storyPath;
    public final int                    staticMemory;
    public final int                    highMemory;
    public transient Map<Integer, Byte> changes          = new HashMap<Integer, Byte>();

    public FileBuffer(final String storyPath) {
        this.storyPath = storyPath;
        loadUpFile(storyPath);
        staticMemory = Header.DYNAMIC_SIZE.value(this);
        highMemory = Header.RESIDENT_SIZE.value(this);
    }

    public int capacity() {
        return zmp.length;
    }

    public int get(int i) {
        if (i < staticMemory && changed[i]) {
            return changes.get(i) & 0xff;
        }
        if (i >= zmp.length) {
            Log.w("Xyzzy", "Array index out-of-bounds");
            return 0;
        }
        return zmp[i] & 0xff;
    }

    public int getShort(int b) {
        return (get(b) << 8) + (get(b + 1));
    }

    @SuppressWarnings("resource") private void loadUpFile(final String path) {
        InputStream fis;
        try {
            if (path.startsWith("@")) {
                fis = MainActivity.activity.getAssets().open(path.substring(1));
            } else {
                fis = new FileInputStream(path);
            }
            final long length = fis.available(); //storyFile.length();
            Log.d("Xyzzy", path + ": " + length + " bytes read");
            zmp = new byte[(int) length];
            fis.read(zmp);
            fis.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        changed = new boolean[zmp.length];
    }

    public void put(int offset, int s) {
        //        if (offset == 0x10 && Header.VERSION.value() > 3 && Bit.bit1(s) != fixedWidthFont) {
        //            fontBitInHeader(s);
        //        }
        if (offset < staticMemory && changed[offset]) {
            byte underlying = zmp[offset];
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

    public void putShort(int offset, int s) {
        put(offset, (s & 0xff00) >> 8);
        put(offset + 1, s & 0xff);
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        loadUpFile(storyPath);
        int total = in.readInt();
        changes = new HashMap<Integer, Byte>();
        for (int i = 0; i < total; i++) {
            int key = in.readInt();
            byte val = in.readByte();
            changes.put(key, val);
            changed[key] = true;
        }
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(changes.size());
        for (int i : changes.keySet()) {
            out.writeInt(i);
            out.writeByte(changes.get(i));
        }
    }
}
