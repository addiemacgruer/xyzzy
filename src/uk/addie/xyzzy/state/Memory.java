
package uk.addie.xyzzy.state;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import uk.addie.xyzzy.MainActivity;
import uk.addie.xyzzy.Random;
import uk.addie.xyzzy.error.Error;
import uk.addie.xyzzy.header.GameFlag;
import uk.addie.xyzzy.header.Header;
import uk.addie.xyzzy.header.InterpreterFlag1;
import uk.addie.xyzzy.header.InterpreterType;
import uk.addie.xyzzy.opcodes.OpMap;
import uk.addie.xyzzy.util.Bit;
import uk.addie.xyzzy.zmachine.CallStack;
import uk.addie.xyzzy.zmachine.ZStack;
import uk.addie.xyzzy.zmachine.ZStream;
import uk.addie.xyzzy.zobjects.Dictionary;
import uk.addie.xyzzy.zobjects.ZObject;
import uk.addie.xyzzy.zobjects.ZWindow;
import android.util.Log;
import android.util.SparseArray;

public class Memory implements Serializable {
    private static final long serialVersionUID = 1L;
    private static Memory     CURRENT          = new Memory();
    private static byte[]     UNDO             = new byte[0];
    private static ZStream    streams          = new ZStream();

    public static Memory current() {
        return CURRENT;
    }

    public static void loadDataFromFile() {
        CURRENT.buffer = new FileBuffer(Memory.CURRENT.storyPath);
        if (Header.VERSION.value() < 1 || Header.VERSION.value() > 8) {
            Error.UNKNOWN_ZCODE_VERSION.invoke();
        }
        if (Header.VERSION.value() == 3 && Bit.bit0(Header.CONFIG.value())) {
            Error.BYTE_SWAPPED_STORY_FILE.invoke();
        }
        if (storyid() == Story.Game.ZORK_ZERO && Header.RELEASE.value() == 296) {
            Header.FLAGS.put(Header.FLAGS.value() | GameFlag.GRAPHICS);
        }
        int config = 0;
        config |= InterpreterFlag1.CONFIG_COLOUR;
        config |= InterpreterFlag1.CONFIG_BOLDFACE;
        config |= InterpreterFlag1.CONFIG_EMPHASIS;
        config |= InterpreterFlag1.CONFIG_PROPORTIONAL;
        config |= InterpreterFlag1.CONFIG_FIXED;
        config |= InterpreterFlag1.CONFIG_TIMEDINPUT; // we don't, it's exceptionally annoying
        Header.CONFIG.put(config);
        int width = MainActivity.width;
        final int columns = width / ZWindow.textSize;
        Header.SCREEN_WIDTH.put(columns);
        Header.SCREEN_HEIGHT.put(24); // a lie, but to try and prevent too much buffering required
        Header.SCREEN_COLS.put(columns);
        Header.SCREEN_ROWS.put(24);
        Header.FONT_WIDTH.put(1);
        Header.FONT_HEIGHT.put(1);
        Header.INTERPRETER_NUMBER.put(InterpreterType.DEC_20);
        Header.INTERPRETER_VERSION.put('F');
        Header.DEFAULT_FOREGROUND.put(0);
        Header.DEFAULT_BACKGROUND.put(1);
        Header.STANDARD_HIGH.put(1);
        Header.STANDARD_LOW.put(0);
        OpMap.adjustForVersion(Header.VERSION.value());
        Dictionary.initDefault();
        ZObject.enumerateObjects();
    }

    public static void setCurrent(Memory cURRENT) {
        CURRENT = cURRENT;
    }

    public static void storeUndo(byte[] uNDO) {
        UNDO = uNDO;
    }

    public static int story_size() {
        int story_size = Header.FILE_SIZE.value() << 1;
        if (story_size > 0) {
            if (Header.VERSION.value() >= 4) {
                story_size <<= 1;
            }
            if (Header.VERSION.value() >= 4) {
                story_size <<= 1;
            }
        } else {
            story_size = CURRENT.buff().capacity();
        }
        return story_size;
    }

    private static Story.Game storyid() {
        final String serial = Header.serial(CURRENT.buff());
        Story.Game storyid = Story.Game.UNKNOWN;
        for (final Story s : Story.stories) {
            if (s.release == Header.RELEASE.value() && s.serial == serial) {
                storyid = s.story;
                break;
            }
        }
        return storyid;
    }

    public static ZStream streams() {
        return streams;
    }

    public static byte[] undo() {
        return UNDO;
    }

    public static int unpackAddress(final int packed) {
        switch (Header.VERSION.value()) {
        case 1:
        case 2:
        case 3:
            return packed << 1;
        case 4:
        case 5:
            return packed << 2;
        case 6:
        case 7:
            return packed << 2 + Header.FUNCTIONS_OFFSET.value();
        case 8:
            return packed << 3;
        default:
            Log.i("Xyzzy", "Unknown v-type");
            return packed << 3;
        }
    }

    public int                            currentScreen = 0;
    public transient SparseArray<ZWindow> zwin          = new SparseArray<ZWindow>();
    public ZStack<CallStack>              callStack     = new ZStack<CallStack>(null);
    public Random                         random        = new Random();
    public int                            objectCount;
    public String                         storyPath;
    public FileBuffer                     buffer;

    Memory() {
        callStack.add(new CallStack());
        resetZWindows();
    }

    public FileBuffer buff() {
        return buffer;
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        int zwc = in.readInt();
        zwin = new SparseArray<ZWindow>();
        for (int i = 0; i < zwc; i++) {
            int key = in.readInt();
            ZWindow zobj = (ZWindow) in.readObject();
            zwin.put(key, zobj);
        }
    }

    public void resetZWindows() {
        int zwinsize = zwin.size();
        for (int i = 0; i < zwinsize; i++) {
            int z = zwin.keyAt(i);
            zwin.get(z).reset();
        }
        zwin.clear();
        zwin.put(0, new ZWindow(0));
        currentScreen = 0;
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        int zwc = zwin.size();
        out.writeInt(zwc);
        for (int i = 0; i < zwc; i++) {
            final int keyAtI = zwin.keyAt(i);
            out.writeInt(keyAtI);
            out.writeObject(zwin.get(keyAtI));
        }
    }
}
