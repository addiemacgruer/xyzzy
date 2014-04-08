
package uk.addie.xyzzy.state;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import uk.addie.xyzzy.MainActivity;
import uk.addie.xyzzy.Random;
import uk.addie.xyzzy.header.GameFlag;
import uk.addie.xyzzy.header.Header;
import uk.addie.xyzzy.header.InterpreterFlag1;
import uk.addie.xyzzy.header.InterpreterType;
import uk.addie.xyzzy.os.OS;
import uk.addie.xyzzy.util.Bit;
import uk.addie.xyzzy.zmachine.CallStack;
import uk.addie.xyzzy.zmachine.ZStack;
import uk.addie.xyzzy.zobjects.Dictionary;
import uk.addie.xyzzy.zobjects.ZObject;
import uk.addie.xyzzy.zobjects.ZWindow;
import android.util.Log;

public class Memory implements Serializable {
    public static class Story {
        public enum Game {
            ARTHUR, BEYOND_ZORK, JOURNEY, LURKING_HORROR, SHERLOCK, SHOGUN, UNKNOWN, ZORK_ZERO;
        }

        static final Story[] stories = { new Story(Game.SHERLOCK, 21, "871214"),
                                             new Story(Game.SHERLOCK, 26, "880127"),
                                             new Story(Game.BEYOND_ZORK, 47, "870915"),
                                             new Story(Game.BEYOND_ZORK, 49, "870917"),
                                             new Story(Game.BEYOND_ZORK, 51, "870923"),
                                             new Story(Game.BEYOND_ZORK, 57, "871221"),
                                             new Story(Game.ZORK_ZERO, 296, "881019"),
                                             new Story(Game.ZORK_ZERO, 366, "890323"),
                                             new Story(Game.ZORK_ZERO, 383, "890602"),
                                             new Story(Game.ZORK_ZERO, 393, "890714"),
                                             new Story(Game.SHOGUN, 292, "890314"),
                                             new Story(Game.SHOGUN, 295, "890321"),
                                             new Story(Game.SHOGUN, 311, "890510"),
                                             new Story(Game.SHOGUN, 322, "890706"),
                                             new Story(Game.ARTHUR, 54, "890606"),
                                             new Story(Game.ARTHUR, 63, "890622"),
                                             new Story(Game.ARTHUR, 74, "890714"),
                                             new Story(Game.JOURNEY, 26, "890316"),
                                             new Story(Game.JOURNEY, 30, "890322"),
                                             new Story(Game.JOURNEY, 77, "890616"),
                                             new Story(Game.JOURNEY, 83, "890706"),
                                             new Story(Game.LURKING_HORROR, 203, "870506"),
                                             new Story(Game.LURKING_HORROR, 219, "870912"),
                                             new Story(Game.LURKING_HORROR, 221, "870918"),
                                             new Story(Game.UNKNOWN, 0, "------") };
        final int            release;
        final String         serial;
        final Game           story;

        private Story(final Game story, final int release, final String version) {
            this.story = story;
            this.release = release;
            serial = version;
        }
    }

    private static final long serialVersionUID = 1L;
    public static Memory      CURRENT          = new Memory();
    public static byte[]      UNDO             = new byte[0];

    public static ZWindow currentScreen() {
        return CURRENT.zwin.get(CURRENT.currentScreen);
    }

    public static void loadDataFromFile() {
        CURRENT.buffer = new FileBuffer(Memory.CURRENT.storyPath);
        if (Header.VERSION.value() < 1 || Header.VERSION.value() > 8) {
            OS.os_fatal("Unknown Z-code version");
        }
        if (Header.VERSION.value() == 3 && Bit.bit0(Header.CONFIG.value())) {
            OS.os_fatal("Byte swapped story file");
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
        final int columns = 2 * width / ZWindow.textSize;
        Header.SCREEN_WIDTH.put(columns);
        Header.SCREEN_HEIGHT.put(255);
        Header.SCREEN_COLS.put(columns);
        Header.SCREEN_ROWS.put(255);
        Header.FONT_WIDTH.put(1);
        Header.FONT_HEIGHT.put(1);
        Header.INTERPRETER_NUMBER.put(InterpreterType.DEC_20);
        Header.INTERPRETER_VERSION.put('F');
        Header.DEFAULT_FOREGROUND.put(0);
        Header.DEFAULT_BACKGROUND.put(1);
        Header.STANDARD_HIGH.put(1);
        Dictionary.initDefault();
        ZObject.enumerateObjects();
        //        throw new UnsupportedOperationException();
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

    public int                   currentScreen = 0;
    public Map<Integer, ZWindow> zwin          = new HashMap<Integer, ZWindow>();
    public ZStack<CallStack>     callStack     = new ZStack<CallStack>();
    public Random                random        = new Random();
    public int                   objectCount;
    public String                storyPath;
    public FileBuffer            buffer;

    Memory() {
        callStack.add(new CallStack());
        resetZWindows();
    }

    public FileBuffer buff() {
        return buffer;
    }

    public void resetZWindows() {
        for (int z : zwin.keySet()) {
            zwin.get(z).reset();
        }
        zwin.clear();
        zwin.put(0, new ZWindow(0));
        currentScreen = 0;
    }
}
