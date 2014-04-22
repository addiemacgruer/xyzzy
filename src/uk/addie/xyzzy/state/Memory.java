
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
import uk.addie.xyzzy.preferences.Preferences;
import uk.addie.xyzzy.util.Bit;
import uk.addie.xyzzy.util.FontWidth;
import uk.addie.xyzzy.zmachine.CallStack;
import uk.addie.xyzzy.zmachine.ZStack;
import uk.addie.xyzzy.zmachine.ZStream;
import uk.addie.xyzzy.zobjects.Dictionary;
import uk.addie.xyzzy.zobjects.ZWindow;
import android.util.Log;
import android.util.SparseArray;

public class Memory implements Serializable {
    private static Memory     CURRENT          = new Memory();
    private static final long serialVersionUID = 1L;
    private static ZStream    streams          = new ZStream();
    private static byte[]     UNDO             = new byte[0];

    public static Memory current() {
        return CURRENT;
    }

    static int getScreenColumns() {
        final int width = MainActivity.width;
        final double textSize;
        if ((Boolean) Preferences.UPPER_SCREENS_ARE_MONOSPACED.getValue(MainActivity.activity)) {
            textSize = FontWidth.widthOfMonospacedString(MainActivity.activity, "          ");
        } else {
            textSize = FontWidth.widthOfString(MainActivity.activity, "MMMMMMMMMM");
        }
        final int numberOfZeroes = (int) (width * 10. / textSize);
        final int columns = Math.min(numberOfZeroes, 254); // some games will complain if they're less than 80 columns, but it has to fit in a byte
        Log.d("Xyzzy", "Screen width (columns):" + columns + " from " + width + "/" + numberOfZeroes);
        return columns;
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
        int config = Header.CONFIG.value();
        config |= InterpreterFlag1.CONFIG_COLOUR;
        config |= InterpreterFlag1.CONFIG_BOLDFACE;
        config |= InterpreterFlag1.CONFIG_EMPHASIS;
        config |= InterpreterFlag1.CONFIG_PROPORTIONAL;
        config |= InterpreterFlag1.CONFIG_FIXED;
        config |= InterpreterFlag1.CONFIG_TIMEDINPUT; // we don't, it's exceptionally annoying
        Header.CONFIG.put(config);
        final int columns = getScreenColumns();
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
        //        OpMap.logAllOpcodes();
        Dictionary.initDefault();
    }

    public static void setCurrent(final Memory cURRENT) {
        CURRENT = cURRENT;
    }

    public static void storeUndo(final byte[] uNDO) {
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
            if (s.release == Header.RELEASE.value() && s.serial.equals(serial)) {
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

    public FileBuffer                     buffer;
    public ZStack<CallStack>              callStack     = new ZStack<CallStack>(null);
    public int                            currentScreen = 0;
    public int                            objectCount;
    public Random                         random        = new Random();
    public String                         storyPath;
    public transient SparseArray<ZWindow> zwin          = new SparseArray<ZWindow>();

    Memory() {
        callStack.add(new CallStack());
        resetZWindows();
    }

    public FileBuffer buff() {
        return buffer;
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        final int zwc = in.readInt();
        zwin = new SparseArray<ZWindow>();
        for (int i = 0; i < zwc; i++) {
            final int key = in.readInt();
            final ZWindow zobj = (ZWindow) in.readObject();
            zwin.put(key, zobj);
        }
    }

    public void resetZWindows() {
        final int zwinsize = zwin.size();
        for (int i = 0; i < zwinsize; i++) {
            final int z = zwin.keyAt(i);
            zwin.get(z).reset();
        }
        zwin.clear();
        zwin.put(0, new ZWindow(0));
        currentScreen = 0;
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        final int zwc = zwin.size();
        out.writeInt(zwc);
        for (int i = 0; i < zwc; i++) {
            final int keyAtI = zwin.keyAt(i);
            out.writeInt(keyAtI);
            out.writeObject(zwin.get(keyAtI));
        }
    }
}
