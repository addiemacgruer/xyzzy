
package uk.addie.xyzzy.opcodes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EmptyStackException;
import java.util.Locale;

import uk.addie.xyzzy.Invokeable;
import uk.addie.xyzzy.MainActivity;
import uk.addie.xyzzy.error.Error;
import uk.addie.xyzzy.header.Header;
import uk.addie.xyzzy.os.Debug;
import uk.addie.xyzzy.os.Main;
import uk.addie.xyzzy.state.Memory;
import uk.addie.xyzzy.util.Bit;
import uk.addie.xyzzy.zmachine.CallStack;
import uk.addie.xyzzy.zmachine.Decoder;
import uk.addie.xyzzy.zmachine.ZStack;
import uk.addie.xyzzy.zobjects.ZObject;
import uk.addie.xyzzy.zobjects.ZProperty;
import uk.addie.xyzzy.zobjects.ZText;
import uk.addie.xyzzy.zobjects.ZWindow;
import android.content.Context;
import android.util.Log;

@SuppressWarnings("unused") public enum Opcode {
    ADD(2, 0x14) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short a = arguments.get(0);
            final short b = arguments.get(1);
            final short result = (short) (a + b);
            readDestinationAndStoreResult(result);
        }
    },
    AND(2, 0x9) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short a = arguments.get(0);
            final short b = arguments.get(1);
            final short result = (short) (a & b);
            readDestinationAndStoreResult(result);
        }
    },
    AREAD(3, 0x4) {
        @Override public void invoke(ZStack<Short> arguments) {
            ZWindow.printAllScreens();
            int text = 0, parse = 0, time = 0, routine = 0;
            switch (arguments.size()) {
            default:
            case 4:
                routine = arguments.get(3) & 0xffff;
                //$FALL-THROUGH$
            case 3:
                time = arguments.get(2) & 0xffff;
                //$FALL-THROUGH$
            case 2:
                parse = arguments.get(1) & 0xffff;
                //$FALL-THROUGH$
            case 1:
                text = arguments.get(0) & 0xffff;
                break;
            }
            if (routine != 0 || time != 0) {
                Log.i("Xyzzy", "Read routine should be timed...");
            }
            final int maxCharacters = Memory.CURRENT.buff().get(text);
            String inputString = Memory.currentScreen().promptForInput().toLowerCase(Locale.UK);
            inputString = inputString.substring(0, Math.min(maxCharacters, inputString.length()));
            Memory.CURRENT.buff().put(text + 1, (byte) inputString.length());
            for (int i = 0, j = inputString.length(); i < j; i++) {
                Memory.CURRENT.buff().put(text + 2 + i, (byte) inputString.codePointAt(i));
            }
            ZText.tokeniseInputToBuffers(text, parse, inputString);
            if (Header.VERSION.value() >= 5) {
                readDestinationAndStoreResult((short) 10);
            }
        }
    },
    ART_SHIFT(4, 0x3) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short number = arguments.get(0);
            final short places = arguments.get(1);
            final short result;
            if (places > 0) {
                result = (short) (number << places);
            } else if (places < 0) {
                result = (short) (number >>> -places);
            } else {
                result = number;
            }
            readDestinationAndStoreResult(result);
        }
    },
    BUFFER_MODE(3, 0x12) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short flag = arguments.get(0);
            Memory.currentScreen().setBuffered(flag == 1);
            if (Debug.screen) {
                Log.i("Xyzzy", "BUFFER MODE: " + (flag == 1 ? "ON" : "OFF"));
            }
        }
    },
    CALL_1N(1, 0xf) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short routine = arguments.get(0);
            CallStack.call(routine & 0xffff, arguments, new StackDiscard());
        }
    },
    CALL_1S(1, 0x8) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short routine = arguments.get(0);
            final int result = Memory.CURRENT.callStack.peek().getProgramByte();
            CallStack.call(routine & 0xffff, arguments, new StackStore(result));
        }
    },
    CALL_2N(2, 0x1a) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short routine = arguments.get(0);
            CallStack.call(routine & 0xffff, arguments, new StackDiscard());
        }
    },
    CALL_2S(2, 0x19) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short routine = arguments.get(0);
            final int result = Memory.CURRENT.callStack.peek().getProgramByte();
            CallStack.call(routine & 0xffff, arguments, new StackStore(result));
        }
    },
    CALL_VN(3, 0x19) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short routine = arguments.get(0);
            CallStack.call(routine & 0xffff, arguments, new StackDiscard());
        }
    },
    CALL_VN2(3, 0x1a) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short routine = arguments.get(0);
            CallStack.call(routine & 0xffff, arguments, new StackDiscard());
        }
    },
    CALL_VS(3, 0x0) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short routine = arguments.get(0);
            final int result = Memory.CURRENT.callStack.peek().getProgramByte();
            CallStack.call(routine & 0xffff, arguments, new StackStore(result));
        }
    },
    CALL_VS2(3, 0xc) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short routine = arguments.get(0);
            final int result = Memory.CURRENT.callStack.peek().getProgramByte();
            CallStack.call(routine & 0xffff, arguments, new StackStore(result));
        }
    },
    CATCH(0, 0x9) {
        @Override public void invoke(ZStack<Short> arguments) {
            //TODO catch
            final int destination = Memory.CURRENT.callStack.peek().getProgramByte();
            Log.w("Xyzzy", "Catch:" + arguments + " destination:" + destination);
            logCallStack();
        }
    },
    CHECK_ARG_COUNT(3, 0x1f) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short argumentNumber = arguments.get(0);
            final short actual = (short) Memory.CURRENT.callStack.peek().calledWithCount;
            branchOnTest(argumentNumber <= actual);
        }
    },
    CHECK_UNICODE(4, 0xc) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short charNumber = arguments.get(0);
            int result;
            switch (charNumber) {
            default:
                result = 3; //we can print and receive any unicode char
            }
            readDestinationAndStoreResult(result);
        }
    },
    CLEAR_ATTR(2, 0xc) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short object = arguments.get(0);
            final short attribute = arguments.get(1);
            final ZObject zo = ZObject.count(object);
            zo.clearAttribute(attribute);
        }
    },
    COPY_TABLE(3, 0x1d) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short first = arguments.get(0);
            final short second = arguments.get(1);
            final short size = arguments.get(2);
            if (second == 0) { // zero first
                for (int i = 0; i < Math.abs(size); i++) {
                    Memory.CURRENT.buff().put(first + i, 0);
                }
                return;
            }
            // decide copy direction
            if (size < 0 || first + size < second) { // copy forwards
                for (int i = 0; i < Math.abs(size); i++) {
                    byte b = (byte) Memory.CURRENT.buff().get(second + i);
                    Memory.CURRENT.buff().put(first + i, b);
                }
            } else { // copy backwards
                for (int i = Math.abs(size) - 1; i >= 0; i--) {
                    byte b = (byte) Memory.CURRENT.buff().get(second + i);
                    Memory.CURRENT.buff().put(first + i, b);
                }
            }
        }
    },
    DEC(1, 0x6) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short variable = arguments.get(0);
            final short value = (short) readValue(variable);
            storeValue(variable, value - 1);
        }
    },
    DEC_CHK(2, 0x4) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short variable = (short) (readValue(arguments.get(0)) - 1);
            final short value = arguments.get(1);
            storeValue(arguments.get(0), variable);
            branchOnTest(variable < value);
        }
    },
    DIV(2, 0x17) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short a = arguments.get(0);
            final short b = arguments.get(1);
            if (b == 0) {
                Error.DIV_ZERO.invoke();
            }
            final short value = (short) (a / b);
            readDestinationAndStoreResult(value);
        }
    },
    DRAW_PICTURE(4, 0x5) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short pictureNumber = arguments.get(0);
            final short y = arguments.get(1);
            final short x = arguments.get(2);
            // TODO draw_picture
            throw new UnsupportedOperationException("@draw_picture");
        }
    },
    ENCODE_TEXT(3, 0x1c) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short zsciiText = arguments.get(0);
            final short length = arguments.get(1);
            final short from = arguments.get(2);
            final short codedText = arguments.get(0);
            // TODO encode_text
            throw new UnsupportedOperationException("@encode_text");
        }
    },
    ERASE_LINE(3, 0xe) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short value = arguments.get(0);
            // TODO erase_line
            throw new UnsupportedOperationException("@erase_line");
        }
    },
    ERASE_PICTURE(4, 0x7) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short pictureNumber = arguments.get(0);
            final short y = arguments.get(1);
            final short x = arguments.get(2);
            // TODO erase_picture
            throw new UnsupportedOperationException("@erase_picture");
        }
    },
    ERASE_WINDOW(3, 0xd) {
        @Override public void invoke(ZStack<Short> arguments) {
            final int window = arguments.get(0);
            switch (window) {
            case -2:
                if (Debug.screen) {
                    Log.i("Xyzzy", "CLEARS ALL WINDOWS");
                }
                for (int i : Memory.CURRENT.zwin.keySet()) {
                    Memory.CURRENT.zwin.get(i).flush();
                }
                break;
            case -1:
                if (Debug.screen) {
                    Log.i("Xyzzy", "UNSPLIT SCREEN AND ERASE ALL");
                }
                Memory.CURRENT.resetZWindows();
                break;
            default:
                if (Debug.screen) {
                    Log.i("Xyzzy", "CLEAR SCREEN: " + window);
                }
                Memory.CURRENT.zwin.get(window).flush();
            }
        }
    },
    GET_CHILD(1, 0x2) {
        @Override public void invoke(ZStack<Short> arguments) {
            final int object = arguments.get(0);
            final int value;
            if (object == 0) {
                Error.GET_CHILD_0.invoke();
                value = 0;
            } else {
                value = ZObject.count(object).child();
            }
            readDestinationAndStoreResult((short) value);
            branchOnTest(value != 0);
        }
    },
    GET_CURSOR(3, 0x10) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short array = arguments.get(0);
            // TODO get cursor
            throw new UnsupportedOperationException("@get_cursor");
        }
    },
    GET_NEXT_PROP(2, 0x13) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short object = arguments.get(0);
            final short property = arguments.get(1);
            final ZObject zo = ZObject.count(object);
            final ZProperty zp = new ZProperty(zo);
            final int value = zp.getNextProperty(property);
            readDestinationAndStoreResult((short) value);
        }
    },
    GET_PARENT(1, 0x3) {
        @Override public void invoke(ZStack<Short> arguments) {
            final Short object = arguments.get(0);
            if (object == 0) {
                Error.GET_PARENT_0.invoke();
                readDestinationAndStoreResult(0);
                return;
            }
            final short zo = (short) ZObject.count(object).parent();
            readDestinationAndStoreResult(zo);
        }
    },
    GET_PROP(2, 0x11) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short object = arguments.get(0);
            final short property = arguments.get(1);
            if (object == 0) {
                Error.GET_PROP_0.invoke();
                readDestinationAndStoreResult(0);
                return;
            }
            final ZObject zo = ZObject.count(object);
            final ZProperty zp = new ZProperty(zo);
            final int value = zp.getProperty(property);
            readDestinationAndStoreResult(value);
        }
    },
    GET_PROP_ADDR(2, 0x12) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short object = arguments.get(0);
            final short property = arguments.get(1);
            if (object == 0) {
                Error.GET_PROP_ADDR_0.invoke();
                readDestinationAndStoreResult(0);
                return;
            }
            final ZProperty zp = new ZProperty(ZObject.count(object));
            int addr = zp.getPropertyAddress(property);
            if (addr != 0) {
                if (Header.VERSION.value() >= 4 && (Memory.CURRENT.buff().get(addr) & 0x80) != 0) {
                    addr += 2;
                } else {
                    addr++;
                }
            }
            readDestinationAndStoreResult(addr);
        }
    },
    GET_PROP_LEN(1, 0x4) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short propertyAddress = (short) (arguments.get(0) - 1);
            if (propertyAddress == -1) {
                readDestinationAndStoreResult(0);
                return;
            }
            final short value = (short) ZProperty.calcProplenSize(propertyAddress & 0xffff);
            readDestinationAndStoreResult(value);
        }
    },
    GET_SIBLING(1, 0x1) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short object = arguments.get(0);
            final int value = ZObject.count(object).sibling();
            readDestinationAndStoreResult((short) value);
            branchOnTest(value != 0);
        }
    },
    GET_WIND_PROP(4, 0x13) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short window = arguments.get(0);
            final short propertyNumber = arguments.get(0);
            // TODO get_wind_prop.
            throw new UnsupportedOperationException("@get_wind_prop");
        }
    },
    INC(1, 0x5) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short variable = arguments.get(0);
            final short value = (short) readValue(variable);
            storeValue(variable, value + 1);
        }
    },
    INC_CHK(2, 0x5) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short variable = arguments.get(0);
            final short value = arguments.get(1);
            final short presentValue = (short) (readValue(variable) + 1);
            storeValue(variable, presentValue);
            branchOnTest(presentValue > value);
        }
    },
    INPUT_STREAM(3, 0x14) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short number = arguments.get(0);
            // TODO input stream
            throw new UnsupportedOperationException("@input_stream");
        }
    },
    INSERT_OBJ(2, 0xe) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short object = arguments.get(0);
            final short destination = arguments.get(1);
            final ZObject zo = ZObject.count(object);
            final ZObject zd = ZObject.count(destination);
            if (Debug.moves) {
                Log.i("Xyzzy", "Moving " + zo + " to " + zd);
            }
            // if the destination parent already has a child, make a note of it.
            final int newSibling = zd.child();
            if (newSibling == object) { //moving an object to where it already is
                if (Debug.moves) {
                    Log.i("Xyzzy", "...but it was already there");
                }
                return;
            }
            ZObject.detachFromTree(object);
            zd.setChild(object);
            zo.setParent(destination);
            if (newSibling != zo.sibling()) {
                zo.setSibling(newSibling);
            }
            if (Debug.moves) {
                Log.i("Xyzzy", "Objects now " + zo + " / " + zd);
            }
        }
    },
    JE(2, 0x1) {
        @Override public void invoke(ZStack<Short> arguments) {
            boolean anymatch = false;
            final short a = arguments.get(0);
            for (int i = 1; i < arguments.size(); i++) {
                final short b = arguments.get(i);
                if (a == b) {
                    anymatch = true;
                    break;
                }
            }
            branchOnTest(anymatch);
        }
    },
    JG(2, 0x3) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short a = arguments.get(0);
            final short b = arguments.get(1);
            branchOnTest(a > b);
        }
    },
    JIN(2, 0x6) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short obj1 = arguments.get(0);
            final short obj2 = arguments.get(1);
            if (obj2 == 0) {
                Error.JIN_0.invoke();
                branchOnTest(false);
                return;
            }
            final ZObject zo = ZObject.count(obj1);
            branchOnTest(zo.parent() == obj2);
        }
    },
    JL(2, 0x2) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short a = arguments.get(0);
            final short b = arguments.get(1);
            branchOnTest(a < b);
        }
    },
    JUMP(1, 0xc) {
        @Override public void invoke(ZStack<Short> arguments) {
            final int label = Memory.CURRENT.callStack.peek().programCounter() + arguments.get(0) - 2;
            Memory.CURRENT.callStack.peek().setProgramCounter(label);
        }
    },
    JZ(1, 0x0) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short a = arguments.get(0);
            branchOnTest(a == 0);
        }
    },
    LOAD(1, 0xe) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short variable = arguments.get(0);
            final short value;
            if (variable == 0) {
                value = (short) Memory.CURRENT.callStack.peek().peek();
            } else {
                value = (short) readValue(variable);
            }
            readDestinationAndStoreResult(value);
        }
    },
    LOADB(2, 0x10) {
        @Override public void invoke(ZStack<Short> arguments) {
            final int array = arguments.get(0) & 0xffff;
            final int byteIndex = arguments.get(1) & 0xffff;
            final int value = Memory.CURRENT.buff().get(array + byteIndex);
            readDestinationAndStoreResult((short) value);
        }
    },
    LOADW(2, 0xf) {
        @Override public void invoke(ZStack<Short> arguments) {
            final int array = arguments.get(0) & 0xffff;
            final int wordIndex = arguments.get(1);// & 0xffff;
            final int address = array + 2 * wordIndex;
            final int value = Memory.CURRENT.buff().getShort(address) & 0xffff;
            readDestinationAndStoreResult(value);
        }
    },
    LOG_SHIFT(4, 0x2) {
        @Override public void invoke(ZStack<Short> arguments) {
            final int number = arguments.get(0) & 0xffff; //remove sign digit
            final short places = arguments.get(1);
            int result = number; // Process.zargs.get(0);
            if (places > 0) {
                result <<= places;
            } else if (places < 0) {
                result >>= -places;
            }
            readDestinationAndStoreResult(result);
        }
    },
    MOD(2, 0x18) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short a = arguments.get(0);
            final short b = arguments.get(1);
            if (b == 0) {
                Error.DIV_ZERO.invoke();
            }
            final short value = (short) (a % b);
            readDestinationAndStoreResult(value);
        }
    },
    MUL(2, 0x16) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short a = arguments.get(0);
            final short b = arguments.get(1);
            final short value = (short) (a * b);
            readDestinationAndStoreResult(value);
        }
    },
    NEW_LINE(0, 0xb) {
        @Override public void invoke(ZStack<Short> arguments) {
            //            if (Debug.screen) {
            //                Log.i("Xyzzy", "NEW LINE");
            //            }
            Memory.currentScreen().println();
        }
    },
    NOP(0, 0x4) {
        @Override public void invoke(ZStack<Short> arguments) {
            //no-op
        }
    },
    NOT(3, 0x18) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short value = arguments.get(0);
            final short result = (short) ~value;
            readDestinationAndStoreResult(result);
        }
    },
    OR(2, 0x8) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short a = arguments.get(0);
            final short b = arguments.get(1);
            final short value = (short) (a | b);
            readDestinationAndStoreResult(value);
        }
    },
    OUTPUT_STREAM(3, 0x13) {
        @Override public void invoke(ZStack<Short> arguments) {
            //TODO output_stream
            if (Debug.screen) {
                Log.i("Xyzzy", "OUTPUT STREAM: " + arguments);
            }
        }
    },
    PICTURE_DATA(4, 0x6) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short pictureNumber = arguments.get(0);
            final short array = arguments.get(1);
            // TODO picture_data
            throw new UnsupportedOperationException("@picture_data");
        }
    },
    PIRACY(0, 0xf) {
        @Override public void invoke(ZStack<Short> arguments) {
            branchOnTest(true);
        }
    },
    PRINT(0, 0x2) {
        @Override public void invoke(ZStack<Short> arguments) {
            Memory.currentScreen().append(ZText.atOffset(Memory.CURRENT.callStack.peek().programCounter()));
            Memory.CURRENT.callStack.peek().setProgramCounter(ZText.bytePosition + 2);
        }
    },
    PRINT_ADDR(1, 0x7) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short byteAddressOfString = arguments.get(0);
            Memory.currentScreen().append(ZText.atOffset(byteAddressOfString & 0xffff));
        }
    },
    PRINT_CHAR(3, 0x5) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short outputCharacterCode = arguments.get(0);
            Memory.currentScreen().append(Character.valueOf((char) outputCharacterCode));
        }
    },
    PRINT_NUM(3, 0x6) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short value = arguments.get(0);
            Memory.currentScreen().append(value);
        }
    },
    PRINT_OBJ(1, 0xa) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short object = arguments.get(0);
            //TODO halt on invalid number
            final ZObject zo = ZObject.count(object);
            if (Debug.screen) {
                Log.i("Xyzzy", "PRINT OBJECT: " + zo);
            }
            Memory.currentScreen().append(new ZProperty(zo).toString());
        }
    },
    PRINT_PADDR(1, 0xd) {
        @Override public void invoke(ZStack<Short> arguments) {
            final int packedAddressOfString = arguments.get(0) & 0xffff;
            final int address = Memory.unpackAddress(packedAddressOfString);
            Memory.currentScreen().append(ZText.atOffset(address));
        }
    },
    PRINT_RET(0, 0x3) {
        @Override public void invoke(ZStack<Short> arguments) {
            PRINT.invoke(arguments);
            NEW_LINE.invoke(arguments);
            RTRUE.invoke(arguments);
        }
    },
    PRINT_TABLE(3, 0x1e) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short zsciiText = arguments.get(0);
            final short width = arguments.get(1);
            final short height = arguments.get(2);
            final short skip = arguments.get(3);
            // TODO print_table
            throw new UnsupportedOperationException("@print_table");
        }
    },
    PRINT_UNICODE(4, 0xb) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short charNumber = arguments.get(0);
            // TODO print_unicode
            throw new UnsupportedOperationException("@print_unicode");
        }
    },
    PULL(3, 0x9) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short variable = arguments.get(0);
            //TODO error on underflow
            final short value = Memory.CURRENT.callStack.peek().pop();
            if (variable == 0) {
                Memory.CURRENT.callStack.peek().pop();
            }
            storeValue(variable, value);
        }
    },
    PUSH(3, 0x8) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short value = arguments.get(0);
            Memory.CURRENT.callStack.peek().push(value);
        }
    },
    PUT_PROP(3, 0x3) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short object = arguments.get(0);
            final short property = arguments.get(1);
            final short value = arguments.get(2);
            final ZObject zo = ZObject.count(object);
            final ZProperty zp = new ZProperty(zo);
            zp.putProperty(property, value);
        }
    },
    QUIT(0, 0xa) {
        @Override public void invoke(ZStack<Short> arguments) {
            NEW_LINE.invoke(arguments);
            ZWindow.printAllScreens();
            Decoder.terminate();
        }
    },
    RANDOM(3, 0x7) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short range = arguments.get(0);
            int value = 0;
            if (range < 0) {
                Memory.CURRENT.random.seed_random(range);
            } else if (range == 0) {
                Memory.CURRENT.random.seed_random((int) System.currentTimeMillis());
            } else {
                value = Memory.CURRENT.random.random(range);
            }
            readDestinationAndStoreResult(value);
        }
    },
    READ_CHAR(3, 0x16) {
        @Override public void invoke(ZStack<Short> arguments) {
            ZWindow.printAllScreens();
            int value;
            final short one = arguments.get(0);
            //            final short time = Process.zargs.get(1);
            //            final short routine = Process.zargs.get(2);
            value = MainActivity.activity.waitOnKey();
            Log.i("Xyzzy", "Got keyboard value:" + value);
            switch (value) {
            case '#':
                value = 130;
                break;
            default:
                break;
            }
            readDestinationAndStoreResult(value);
        }
    },
    REMOVE_OBJ(1, 0x9) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short object = arguments.get(0);
            ZObject.detachFromTree(object);
        }
    },
    RESTART(0, 0x7) {
        @Override public void invoke(ZStack<Short> arguments) {
            Memory.CURRENT.random.seed_random(0);
            Memory.loadDataFromFile();
            Memory.CURRENT.callStack.peek().clearStack();
            Main.frame_count = 0;
            if (Header.VERSION.value() != 6) {
                final int pc = Header.START_PC.value();
                Memory.CURRENT.callStack.peek().setProgramCounter(pc);
            } else {
                throw new UnsupportedOperationException("@restart (v6)");
            }
        }
    },
    RESTORE(4, 0x1) {
        @Override public void invoke(ZStack<Short> arguments) {
            Memory loaded = null;
            try {
                final FileInputStream save = MainActivity.activity.openFileInput(Memory.CURRENT.storyPath + ".save");
                final ObjectInputStream ois = new ObjectInputStream(save);
                loaded = (Memory) ois.readObject();
                ois.close();
            } catch (final FileNotFoundException e) {
                readDestinationAndStoreResult(0);
                return;
            } catch (final IOException e) {
                throw new RuntimeException(e);
            } catch (final ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            if (!loaded.storyPath.equals(Memory.CURRENT.storyPath)) {
                Log.i("Xyzzy", "Not a save from this story. " + loaded.storyPath + " v " + Memory.CURRENT.storyPath);
                readDestinationAndStoreResult(0);
            } else {
                Memory.CURRENT = loaded;
                readDestinationAndStoreResult(2);
            }
        }
    },
    RESTORE_UNDO(4, 0xa) {
        @Override public void invoke(ZStack<Short> arguments) {
            Memory loaded = null;
            try {
                final ByteArrayInputStream bais = new ByteArrayInputStream(Memory.UNDO);
                final ObjectInputStream ois = new ObjectInputStream(bais);
                loaded = (Memory) ois.readObject();
                ois.close();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            } catch (final ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            if (!loaded.storyPath.equals(Memory.CURRENT.storyPath)) {
                Log.i("Xyzzy", "Not a save from this story. " + loaded.storyPath + " v " + Memory.CURRENT.storyPath);
                readDestinationAndStoreResult(0);
            } else {
                Memory.CURRENT = loaded;
                readDestinationAndStoreResult(2);
            }
        }
    },
    RET(1, 0xb) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short value = arguments.get(0);
            returnValue(value);
        }
    },
    RET_POPPED(0, 8) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short value = Memory.CURRENT.callStack.peek().pop();
            returnValue(value);
        }
    },
    RFALSE(0, 0x1) {
        @Override public void invoke(ZStack<Short> arguments) {
            returnValue(0);
        }
    },
    RTRUE(0, 0x0) {
        @Override public void invoke(ZStack<Short> arguments) {
            returnValue(1);
        }
    },
    SAVE(4, 0x0) {
        @Override public void invoke(ZStack<Short> arguments) {
            try {
                //                MainActivity.activity.getFilesDir().
                //                final FileOutputStream save = new FileOutputStream("/home/addie/Inform/JFrotz.save");
                final FileOutputStream save = MainActivity.activity.openFileOutput(Memory.CURRENT.storyPath + ".save",
                        Context.MODE_PRIVATE);
                final ObjectOutputStream oos = new ObjectOutputStream(save);
                oos.writeObject(Memory.CURRENT);
                oos.close();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
            readDestinationAndStoreResult(1);
        }
    },
    SAVE_UNDO(4, 0x9) {
        @Override public void invoke(ZStack<Short> arguments) {
            long time = System.currentTimeMillis();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(Memory.UNDO.length);
            try {
                final ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(Memory.CURRENT);
                oos.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Memory.UNDO = baos.toByteArray();
            Log.i("Xyzzy", "Made undo file:" + Memory.UNDO.length + " bytes, " + (System.currentTimeMillis() - time)
                    + " ms");
            readDestinationAndStoreResult(1);
        }
    },
    SCAN_TABLE(3, 0x17) {
        @Override public void invoke(ZStack<Short> arguments) {
            final int x = arguments.get(0) & 0xffff;
            final int table = arguments.get(1) & 0xffff;
            final int len = arguments.get(2) & 0xffff;
            int form = 0;
            if (arguments.size() == 4) { // and implicitly, version 5
                form = arguments.get(3);
            }
            for (int i = 0; i < len; i++) {
                int wordAtI = Memory.CURRENT.buffer.getShort(table + i * 2) & 0xffff;
                if (x == wordAtI) {
                    readDestinationAndStoreResult(table + i * 2);
                    branchOnTest(true);
                    return;
                }
            }
            readDestinationAndStoreResult(0);
            branchOnTest(false); // eat the branch address
        }
    },
    SET_ATTR(2, 0xb) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short object = arguments.get(0);
            final short attribute = arguments.get(1);
            final ZObject zo = ZObject.count(object);
            zo.setAttribute(attribute);
        }
    },
    SET_COLOUR(2, 0x1b) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short foreground = arguments.get(0);
            final short background = arguments.get(1);
            ZWindow.setColour(foreground, background);
            if (Debug.screen) {
                Log.w("Xyzzy", "set_colour " + foreground + " " + background);
            }
        }
    },
    SET_CURSOR(3, 0xf) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short line = arguments.get(0);
            short column = 0;
            if (arguments.size() >= 2) {
                column = arguments.get(1);
            }
            if (Debug.screen) {
                Log.i("Xyzzy", "SET CURSOR: " + line + ", " + column);
            }
        }
    },
    SET_FONT(4, 0x4) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short font = arguments.get(0);
            // TODO set_font
            throw new UnsupportedOperationException("@set_font");
        }
    },
    SET_MARGINS(4, 0x8) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short left = arguments.get(0);
            final short right = arguments.get(0);
            final short window = arguments.get(0);
            // TODO set_margins
            throw new UnsupportedOperationException("@set_margins");
        }
    },
    SET_TEXT_STYLE(3, 0x11) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short style = arguments.get(0);
            //            if (!Debug.screen) {
            //                return;
            //            }
            if (Debug.screen) {
                Log.i("Xyzzy", "SET TEXT STYLE: ");
            }
            switch (style) {
            case 0:
            default:
                if (Debug.screen) {
                    Log.i("Xyzzy", "ROMAN");
                }
                Memory.currentScreen().clearStyles();
                break;
            case 1:
                if (Debug.screen) {
                    Log.i("Xyzzy", "REVERSE VIDEO");
                }
                Memory.currentScreen().addStyle(ZWindow.TextStyle.REVERSE_VIDEO);
                break;
            case 2:
                if (Debug.screen) {
                    Log.i("Xyzzy", "BOLD");
                }
                Memory.currentScreen().addStyle(ZWindow.TextStyle.BOLD);
                break;
            case 4:
                if (Debug.screen) {
                    Log.i("Xyzzy", "ITALIC");
                }
                Memory.currentScreen().addStyle(ZWindow.TextStyle.ITALIC);
                break;
            case 8:
                if (Debug.screen) {
                    Log.i("Xyzzy", "FIXED PITCH");
                }
                Memory.currentScreen().addStyle(ZWindow.TextStyle.FIXED_PITCH);
                break;
            }
        }
    },
    SET_WINDOW(3, 0xb) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short window = arguments.get(0);
            if (Debug.screen) {
                Log.i("Xyzzy", "SET WINDOW: " + arguments.get(0));
            }
            Memory.CURRENT.currentScreen = window;
            if (!Memory.CURRENT.zwin.containsKey((int) window)) {
                Memory.CURRENT.zwin.put((int) window, new ZWindow(window));
            }
        }
    },
    SOUND_EFFECT(3, 0x15) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short number = arguments.get(0);
            final short effect = arguments.get(1);
            final short volume = arguments.get(2);
            final short routine = arguments.get(3);
            // TODO sound_effect
            //            throw new UnsupportedOperationException("@sound_effect");
        }
    },
    SPLIT_WINDOW(3, 0xa) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short lines = arguments.get(0);
            if (Debug.screen) {
                Log.i("Xyzzy", "SPLIT WINDOW: " + lines + " LINES (CURRENT:" + Memory.currentScreen() + ")");
            }
            int splitScreen = Memory.CURRENT.currentScreen + 1;
            if (Memory.CURRENT.zwin.containsKey(splitScreen)) {
                Memory.CURRENT.zwin.get(splitScreen).reset();
            }
        }
    },
    STORE(2, 0xd) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short variable = arguments.get(0);
            final short value = arguments.get(1);
            if (variable == 0) {
                Memory.CURRENT.callStack.peek().pop(); // discard the top-of-stack
                Memory.CURRENT.callStack.peek().push(value);
            } else {
                storeValue(variable, value);
            }
        }
    },
    STOREB(3, 0x2) {
        @Override public void invoke(ZStack<Short> arguments) {
            final int array = arguments.get(0) & 0xffff;
            final int byteIndex = arguments.get(1) & 0xffff;
            final byte value = arguments.get(2).byteValue();
            Memory.CURRENT.buff().put(array + byteIndex, value);
        }
    },
    STOREW(3, 0x1) {
        @Override public void invoke(ZStack<Short> arguments) {
            final int array = arguments.get(0) & 0xffff;
            final int wordIndex = arguments.get(1) & 0xffff;
            final short value = arguments.get(2);
            final int address = array + 2 * wordIndex;
            Memory.CURRENT.buff().putShort(address, value);
        }
    },
    SUB(2, 21) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short a = arguments.get(0);
            final short b = arguments.get(1);
            readDestinationAndStoreResult(a - b);
        }
    },
    TEST(2, 0x7) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short bitmap = arguments.get(0);
            final short flags = arguments.get(1);
            branchOnTest((bitmap & flags) == flags);
        }
    },
    TEST_ATTR(2, 0xa) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short object = arguments.get(0);
            final short attribute = arguments.get(1);
            final ZObject zo = ZObject.count(object);
            final boolean value = zo.testAttribute(attribute);
            branchOnTest(value);
        }
    },
    THROW(2, 0x1c) {
        @Override public void invoke(ZStack<Short> arguments) {
            final short value = arguments.get(0);
            final short stackFrame = arguments.get(1);
            Log.w("Xyzzy", "Throw:" + value + " stackFrame:" + stackFrame);
            logCallStack();
            throw new UnsupportedOperationException("@throw");
        }
    },
    TOKENISE(3, 0x1b) {
        @Override public void invoke(ZStack<Short> arguments) {
            final int text = arguments.get(0) & 0xffff;
            final int parse = arguments.get(1) & 0xffff;
            if (arguments.size() != 2) {
                throw new UnsupportedOperationException("@tokenise (long arguments)");
            }
            final int length = Memory.CURRENT.buff().get(text + 1) & 0xff;
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                sb.append((char) Memory.CURRENT.buff().get(text + i + 2));
            }
            final String inputString = sb.toString().toLowerCase(Locale.UK);
            ZText.tokeniseInputToBuffers(text, parse, inputString);
        }
    },
    VERIFY(0, 0xd) {
        @Override public void invoke(ZStack<Short> arguments) {
            //TODO verify
            branchOnTest(true);
        }
    },
    POP {
        @Override public void invoke(ZStack<Short> arguments) {
            Memory.CURRENT.callStack.peek().pop();
        }
    };
    protected static void branch(final int offset) {
        if (offset == 0) {
            RFALSE.invoke(null);
        } else if (offset == 1) {
            RTRUE.invoke(null);
        } else {
            Memory.CURRENT.callStack.peek().adjustProgramCounter(offset - 2);
        }
    }

    protected static void branchOnTest(boolean test) {
        final int lobit = Memory.CURRENT.callStack.peek().getProgramByte();
        final boolean branchCondition = Bit.bit7(lobit);
        final int offset = calculateOffset(lobit);
        if (!branchCondition ^ test) {
            branch(offset);
        }
    }

    public static int calculateOffset(final int lobit) {
        int offset;
        if (!Bit.bit6(lobit)) { // two bytes
            final boolean negative = Bit.bit5(lobit);
            offset = Bit.low(lobit, 4) << 8;
            offset += Memory.CURRENT.callStack.peek().getProgramByte();
            if (negative) {
                offset = offset - 4096;
            }
        } else { // one byte
            offset = Bit.low(lobit, 6);
        }
        return offset;
    }

    private static int globalVariableAddress(final int ldestination) {
        return (Header.GLOBALS.value() & 0xffff) + 2 * (ldestination - 16);
    }

    public static void readDestinationAndStoreResult(final int value) {
        final int destination = Memory.CURRENT.callStack.peek().getProgramByte();
        storeValue(destination, (short) value);
    }

    public static int readValue(final int destination) {
        if (destination == 0) {
            try {
                return Memory.CURRENT.callStack.peek().pop();
            } catch (final EmptyStackException ese) {
                Error.STK_UNDF.invoke(); // fatal
                return 0;
            }
        } else if (destination < 16) {
            return Memory.CURRENT.callStack.peek().get(destination);
        } else if (destination < 256) {
            final int addr = globalVariableAddress(destination);
            return Memory.CURRENT.buff().getShort(addr);
        } else {
            throw new UnsupportedOperationException("Read value from invalid store:" + destination);
        }
    }

    public static void returnValue(final int value) {
        final Invokeable i = Memory.CURRENT.callStack.peek().returnFunction();
        Memory.CURRENT.callStack.pop();
        Memory.CURRENT.callStack.peek().push(value);
        i.invoke();
        if (Debug.callstack) {
            Log.i("Xyzzy", "<--");
        }
    }

    public static void storeValue(final int destination, final int value) {
        final int ldestination = destination & 0xff;
        if (Debug.stores) {
            Log.i("Xyzzy", "->" + ldestination + "=" + (value & 0xffff));
        }
        if (ldestination == 0) {
            Memory.CURRENT.callStack.peek().push(value);
        } else if (ldestination < 16) {
            Memory.CURRENT.callStack.peek().put(ldestination, value);
        } else {
            final int addr = globalVariableAddress(ldestination);
            Memory.CURRENT.buff().putShort(addr, (short) value);
        }
    }

    public final int hex;
    public final int operands;

    private Opcode() { // constructor for pre-V5 opcodes
        this.operands = 0;
        this.hex = 0x0;
    }

    private Opcode(final int operands, final int hex) {
        this.operands = operands;
        this.hex = hex;
        OpMap.map(this);
    }

    abstract public void invoke(ZStack<Short> arguments);

    void logCallStack() {
        for (CallStack cs : Memory.CURRENT.callStack) {
            Log.d("Xyzzy", cs.toString());
        }
    }

    @Override public String toString() {
        return "(" + operands + "," + Integer.toHexString(hex) + ") " + super.toString().toLowerCase(Locale.UK);
    }
}