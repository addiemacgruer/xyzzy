
package uk.addie.xyzzy.opcodes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EmptyStackException;
import java.util.Locale;

import uk.addie.xyzzy.MainActivity;
import uk.addie.xyzzy.SaveChooserActivity;
import uk.addie.xyzzy.error.Error;
import uk.addie.xyzzy.header.Header;
import uk.addie.xyzzy.interfaces.IInvokeable;
import uk.addie.xyzzy.preferences.Preferences;
import uk.addie.xyzzy.state.Memory;
import uk.addie.xyzzy.util.Bit;
import uk.addie.xyzzy.zmachine.CallStack;
import uk.addie.xyzzy.zmachine.Decoder;
import uk.addie.xyzzy.zmachine.ShortStack;
import uk.addie.xyzzy.zobjects.Beep;
import uk.addie.xyzzy.zobjects.TextStyle;
import uk.addie.xyzzy.zobjects.ZObject;
import uk.addie.xyzzy.zobjects.ZProperty;
import uk.addie.xyzzy.zobjects.ZText;
import uk.addie.xyzzy.zobjects.ZWindow;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.text.format.Time;
import android.util.Log;

@SuppressWarnings({ "unused" }) public enum Opcode {
    ADD(2, 0x14) {
        @Override public void invoke(final ShortStack arguments) {
            final short a = arguments.get(0);
            final short b = arguments.get(1);
            final short result = (short) (a + b);
            readDestinationAndStoreResult(result);
        }
    },
    AND(2, 0x9) {
        @Override public void invoke(final ShortStack arguments) {
            final short a = arguments.get(0);
            final short b = arguments.get(1);
            final short result = (short) (a & b);
            readDestinationAndStoreResult(result);
        }
    },
    ART_SHIFT(4, 0x3) {
        @Override public void invoke(final ShortStack arguments) {
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
        @Override public void invoke(final ShortStack arguments) {
            final short flag = arguments.get(0);
            Memory.streams().setBuffered(flag == 1);
            if (false) {
                Log.i("Xyzzy", "BUFFER MODE: " + (flag == 1 ? "ON" : "OFF"));
            }
        }
    },
    CALL_1N(1, 0xf) {
        @Override public void invoke(final ShortStack arguments) {
            callAndDiscard(arguments);
        }
    },
    CALL_1S(1, 0x8) {
        @Override public void invoke(final ShortStack arguments) {
            callAndStore(arguments);
        }
    },
    CALL_2N(2, 0x1a) {
        @Override public void invoke(final ShortStack arguments) {
            callAndDiscard(arguments);
        }
    },
    CALL_2S(2, 0x19) {
        @Override public void invoke(final ShortStack arguments) {
            callAndStore(arguments);
        }
    },
    CALL_VN(3, 0x19) {
        @Override public void invoke(final ShortStack arguments) {
            callAndDiscard(arguments);
        }
    },
    CALL_VN2(3, 0x1a) {
        @Override public void invoke(final ShortStack arguments) {
            callAndDiscard(arguments);
        }
    },
    CALL_VS(3, 0x0) {
        @Override public void invoke(final ShortStack arguments) {
            callAndStore(arguments);
        }
    },
    CALL_VS2(3, 0xc) {
        @Override public void invoke(final ShortStack arguments) {
            callAndStore(arguments);
        }
    },
    CATCH(0, 0x9) {
        @Override public void invoke(final ShortStack arguments) {
            //TODO catch
            final int destination = Memory.current().callStack.peek().getProgramByte();
            Log.w("Xyzzy", "Catch:" + arguments + " destination:" + destination);
            logCallStack();
        }
    },
    CHECK_ARG_COUNT(3, 0x1f) {
        @Override public void invoke(final ShortStack arguments) {
            final short argumentNumber = arguments.get(0);
            final short actual = (short) Memory.current().callStack.peek().calledWithCount;
            branchOnTest(argumentNumber <= actual);
        }
    },
    CHECK_UNICODE(4, 0xc) {
        @Override public void invoke(final ShortStack arguments) {
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
        @Override public void invoke(final ShortStack arguments) {
            final short object = arguments.get(0);
            final short attribute = arguments.get(1);
            final ZObject zo = ZObject.count(object);
            zo.clearAttribute(attribute);
        }
    },
    COPY_TABLE(3, 0x1d) {
        @Override public void invoke(final ShortStack arguments) {
            final int first = arguments.get(0) & 0xffff;
            final int second = arguments.get(1) & 0xffff;
            final short size = arguments.get(2); // signed;
            final int length = Math.abs(size);
            if (second == 0) { // zero first
                for (int i = 0; i < length; i++) {
                    Memory.current().buff().put(first + i, 0);
                }
                return;
            }
            // decide copy direction
            if (size < 0 || first > second) { // copy forwards
                for (int i = 0; i < length; i++) {
                    final byte b = (byte) Memory.current().buff().get(first + i);
                    Memory.current().buff().put(second + i, b);
                }
            } else { // copy backwards
                for (int i = length - 1; i >= 0; i--) {
                    final byte b = (byte) Memory.current().buff().get(first + i);
                    Memory.current().buff().put(second + i, b);
                }
            }
        }
    },
    DEC(1, 0x6) {
        @Override public void invoke(final ShortStack arguments) {
            final short variable = arguments.get(0);
            final short value = (short) readValue(variable);
            storeValue(variable, value - 1);
        }
    },
    DEC_CHK(2, 0x4) {
        @Override public void invoke(final ShortStack arguments) {
            final short variable = (short) (readValue(arguments.get(0)) - 1);
            final short value = arguments.get(1);
            storeValue(arguments.get(0), variable);
            branchOnTest(variable < value);
        }
    },
    DIV(2, 0x17) {
        @Override public void invoke(final ShortStack arguments) {
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
        @Override public void invoke(final ShortStack arguments) {
            final short pictureNumber = arguments.get(0);
            final short y = arguments.get(1);
            final short x = arguments.get(2);
            // TODO draw_picture
        }
    },
    ENCODE_TEXT(3, 0x1c) {
        @Override public void invoke(final ShortStack arguments) {
            // TODO needs testing, few games use it.
            Log.w("Xyzzy", "Encode Text");
            final short zsciiText = arguments.get(0);
            final short length = arguments.get(1);
            final short from = arguments.get(2);
            final short codedText = arguments.get(3);
            final StringBuilder sb = new StringBuilder();
            for (int i = from; i < from + length; i++) {
                sb.append((char) Memory.current().buffer.get(i));
            }
            final long encodedText = ZText.encodeString(sb.toString());
            final int bytesToStore = Header.VERSION.value() <= 3 ? 4 : 6;
            for (int i = 0; i < bytesToStore; i++) {
                final byte b = (byte) (encodedText >> 8 * (bytesToStore - i) & 0xff);
                Memory.current().buffer.put(codedText + i, b);
            }
        }
    },
    ERASE_LINE(3, 0xe) {
        @Override public void invoke(final ShortStack arguments) {
            final short value = arguments.get(0);
            Memory.streams().eraseLine(value);
        }
    },
    ERASE_PICTURE(4, 0x7) {
        @Override public void invoke(final ShortStack arguments) {
            final short pictureNumber = arguments.get(0);
            final short y = arguments.get(1);
            final short x = arguments.get(2);
            // TODO erase_picture
        }
    },
    ERASE_WINDOW(3, 0xd) {
        @Override public void invoke(final ShortStack arguments) {
            final int window = arguments.get(0);
            switch (window) {
            case -2:
                if (false) {
                    Log.i("Xyzzy", "CLEARS ALL WINDOWS");
                }
                ZWindow.printAllScreens();
                break;
            case -1:
                if (false) {
                    Log.i("Xyzzy", "UNSPLIT SCREEN AND ERASE ALL");
                }
                Memory.current().resetZWindows();
                break;
            default:
                if (false) {
                    Log.i("Xyzzy", "CLEAR SCREEN: " + window);
                }
                Memory.current().zwin.get(window).flush();
            }
        }
    },
    GET_CHILD(1, 0x2) {
        @Override public void invoke(final ShortStack arguments) {
            final int object = arguments.get(0) & 0xffff;
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
        @Override public void invoke(final ShortStack arguments) {
            final short array = arguments.get(0);
            final Point cursor = Memory.current().zwin.get(Memory.current().currentScreen).cursorPosition();
            Memory.current().buffer.put(array, cursor.y); // row first
            Memory.current().buffer.put(array + 1, cursor.x);// column second 
        }
    },
    GET_NEXT_PROP(2, 0x13) {
        @Override public void invoke(final ShortStack arguments) {
            final short object = arguments.get(0);
            final short property = arguments.get(1);
            final ZProperty zp = ZObject.count(object).zProperty();
            final int value = zp.getNextProperty(property);
            readDestinationAndStoreResult((short) value);
        }
    },
    GET_PARENT(1, 0x3) {
        @Override public void invoke(final ShortStack arguments) {
            final int object = arguments.get(0) & 0xffff;
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
        @Override public void invoke(final ShortStack arguments) {
            final int object = arguments.get(0) & 0xffff;
            final int property = arguments.get(1) & 0xffff;
            if (object == 0) {
                Error.GET_PROP_0.invoke();
                readDestinationAndStoreResult(0);
                return;
            }
            final ZProperty zp = ZObject.count(object).zProperty();
            final int value = zp.getProperty(property);
            readDestinationAndStoreResult(value);
        }
    },
    GET_PROP_ADDR(2, 0x12) {
        @Override public void invoke(final ShortStack arguments) {
            final int object = arguments.get(0) & 0xffff;
            final int property = arguments.get(1) & 0xffff;
            if (object == 0) {
                Error.GET_PROP_ADDR_0.invoke();
                readDestinationAndStoreResult(0);
                return;
            }
            final ZProperty zp = ZObject.count(object).zProperty();
            int addr = zp.getPropertyAddress(property);
            if (addr != 0) {
                if (Header.VERSION.value() >= 4 && (Memory.current().buff().get(addr) & 0x80) != 0) {
                    addr += 2;
                } else {
                    addr++;
                }
            }
            readDestinationAndStoreResult(addr);
        }
    },
    GET_PROP_LEN(1, 0x4) {
        @Override public void invoke(final ShortStack arguments) {
            final int propertyAddress = (arguments.get(0) - 1);
            if (propertyAddress == -1) {
                readDestinationAndStoreResult(0);
                return;
            }
            final int value = ZProperty.getPropLen(propertyAddress & 0xffff);
            readDestinationAndStoreResult(value);
        }
    },
    GET_SIBLING(1, 0x1) {
        @Override public void invoke(final ShortStack arguments) {
            final int object = arguments.get(0) & 0xffff;
            if (object == 0) {
                Error.GET_SIBLING_0.invoke();
                readDestinationAndStoreResult(0);
                branchOnTest(false);
                return;
            }
            final int value = ZObject.count(object).sibling();
            readDestinationAndStoreResult((short) value);
            branchOnTest(value != 0);
        }
    },
    GET_WIND_PROP(4, 0x13) {
        @Override public void invoke(final ShortStack arguments) {
            final short window = arguments.get(0);
            final short propertyNumber = arguments.get(0);
            // TODO get_wind_prop.
            readDestinationAndStoreResult(0);
        }
    },
    INC(1, 0x5) {
        @Override public void invoke(final ShortStack arguments) {
            final short variable = arguments.get(0);
            final short value = (short) readValue(variable);
            storeValue(variable, value + 1);
        }
    },
    INC_CHK(2, 0x5) {
        @Override public void invoke(final ShortStack arguments) {
            final short variable = arguments.get(0);
            final short value = arguments.get(1);
            final short presentValue = (short) (readValue(variable) + 1);
            storeValue(variable, presentValue);
            branchOnTest(presentValue > value);
        }
    },
    INPUT_STREAM(3, 0x14) {
        @Override public void invoke(final ShortStack arguments) {
            final short number = arguments.get(0);
            // TODO input stream
        }
    },
    INSERT_OBJ(2, 0xe) {
        @Override public void invoke(final ShortStack arguments) {
            final short object = arguments.get(0);
            final short destination = arguments.get(1);
            final ZObject zo = ZObject.count(object);
            final ZObject zd = ZObject.count(destination);
            if (false) {
                Log.i("Xyzzy", "Moving " + zo + " to " + zd);
            }
            // if the destination parent already has a child, make a note of it.
            final int newSibling = zd.child();
            if (newSibling == object) { //moving an object to where it already is
                if (false) {
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
            if (false) {
                Log.i("Xyzzy", "Objects now " + zo + " / " + zd);
            }
        }
    },
    JE(2, 0x1) {
        @Override public void invoke(final ShortStack arguments) {
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
        @Override public void invoke(final ShortStack arguments) {
            final short a = arguments.get(0);
            final short b = arguments.get(1);
            branchOnTest(a > b);
        }
    },
    JIN(2, 0x6) {
        @Override public void invoke(final ShortStack arguments) {
            final int obj1 = arguments.get(0) & 0xffff;
            final int obj2 = arguments.get(1) & 0xffff;
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
        @Override public void invoke(final ShortStack arguments) {
            final short a = arguments.get(0);
            final short b = arguments.get(1);
            branchOnTest(a < b);
        }
    },
    JUMP(1, 0xc) {
        @Override public void invoke(final ShortStack arguments) {
            final int label = Memory.current().callStack.peek().programCounter() + arguments.get(0) - 2;
            Memory.current().callStack.peek().setProgramCounter(label);
        }
    },
    JZ(1, 0x0) {
        @Override public void invoke(final ShortStack arguments) {
            final short a = arguments.get(0);
            branchOnTest(a == 0);
        }
    },
    LOAD(1, 0xe) {
        @Override public void invoke(final ShortStack arguments) {
            final short variable = arguments.get(0);
            final short value;
            if (variable == 0) {
                value = (short) Memory.current().callStack.peek().peek();
            } else {
                value = (short) readValue(variable);
            }
            readDestinationAndStoreResult(value);
        }
    },
    LOADB(2, 0x10) {
        @Override public void invoke(final ShortStack arguments) {
            final int array = arguments.get(0) & 0xffff;
            final int byteIndex = arguments.get(1) & 0xffff;
            final int value = Memory.current().buff().get(array + byteIndex);
            readDestinationAndStoreResult((short) value);
        }
    },
    LOADW(2, 0xf) {
        @Override public void invoke(final ShortStack arguments) {
            final int array = arguments.get(0) & 0xffff;
            final int wordIndex = arguments.get(1);// & 0xffff;
            final int address = array + 2 * wordIndex;
            final int value = Memory.current().buff().getShort(address) & 0xffff;
            readDestinationAndStoreResult(value);
        }
    },
    LOG_SHIFT(4, 0x2) {
        @Override public void invoke(final ShortStack arguments) {
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
    MAKE_MENU(4, 0x1b) {
        @Override public void invoke(final ShortStack arguments) {
            // TODO make-menu
            final short number = arguments.get(0);
            final short table = arguments.get(1);
            branchOnTest(false);
        }
    },
    MOD(2, 0x18) {
        @Override public void invoke(final ShortStack arguments) {
            final short a = arguments.get(0);
            final short b = arguments.get(1);
            if (b == 0) {
                Error.DIV_ZERO.invoke();
            }
            final short value = (short) (a % b);
            readDestinationAndStoreResult(value);
        }
    },
    MOUSE_WINDOW(4, 0x17) {
        @Override public void invoke(final ShortStack arguments) {
            // TODO mouse_window
            final short window = arguments.get(0);
        }
    },
    MOVE_WINDOW(4, 0x10) {
        @Override public void invoke(final ShortStack arguments) {
            // TODO move_window
            final short window = arguments.get(0);
            final short y = arguments.get(1);
            final short x = arguments.get(2);
        }
    },
    MUL(2, 0x16) {
        @Override public void invoke(final ShortStack arguments) {
            final short a = arguments.get(0);
            final short b = arguments.get(1);
            final short value = (short) (a * b);
            readDestinationAndStoreResult(value);
        }
    },
    NEW_LINE(0, 0xb) {
        @Override public void invoke(final ShortStack arguments) {
            Memory.streams().println();
        }
    },
    NOP(0, 0x4) {
        @Override public void invoke(final ShortStack arguments) {
            //no-op
        }
    },
    NOT(3, 0x18) {
        @Override public void invoke(final ShortStack arguments) {
            final short value = arguments.get(0);
            final short result = (short) ~value;
            readDestinationAndStoreResult(result);
        }
    },
    OR(2, 0x8) {
        @Override public void invoke(final ShortStack arguments) {
            final short a = arguments.get(0);
            final short b = arguments.get(1);
            final short value = (short) (a | b);
            readDestinationAndStoreResult(value);
        }
    },
    OUTPUT_STREAM(3, 0x13) {
        @Override public void invoke(final ShortStack arguments) {
            final int number = arguments.get(0);
            final int table = arguments.get(1) & 0xffff;
            final int width = arguments.get(2) & 0xffff;
            Memory.streams().setOutputStream(number, table, width);
        }
    },
    PICTURE_DATA(4, 0x6) {
        @Override public void invoke(final ShortStack arguments) {
            final short pictureNumber = arguments.get(0);
            final short array = arguments.get(1);
            // TODO picture_data
            branchOnTest(false);
        }
    },
    PICTURE_TABLE(4, 0x1c) {
        @Override public void invoke(final ShortStack arguments) {
            final short table = arguments.get(0);
            //TODO picture-table.  Should cache the pictures in the table given.
        }
    },
    PIRACY(0, 0xf) {
        @Override public void invoke(final ShortStack arguments) {
            branchOnTest(!(Boolean) Preferences.PIRACY.getValue(MainActivity.activity));
        }
    },
    POP {
        @Override public void invoke(final ShortStack arguments) {
            Memory.current().callStack.peek().pop();
        }
    },
    POP_STACK(4, 0x15) {
        @Override public void invoke(final ShortStack arguments) {
            final short items = arguments.get(0);
            if (arguments.size() > 1) {
                final short stack = arguments.get(1);
                //TODO user stacks
            } else {
                for (int i = 0; i < items; i++) {
                    Memory.current().callStack.peek().pop();
                }
            }
        }
    },
    PRINT(0, 0x2) {
        @Override public void invoke(final ShortStack arguments) {
            Memory.streams().append(ZText.encodedAtOffset(Memory.current().callStack.peek().programCounter()));
            Memory.current().callStack.peek().setProgramCounter(ZText.bytePosition + 2);
        }
    },
    PRINT_ADDR(1, 0x7) {
        @Override public void invoke(final ShortStack arguments) {
            final short byteAddressOfString = arguments.get(0);
            Memory.streams().append(ZText.encodedAtOffset(byteAddressOfString & 0xffff));
        }
    },
    PRINT_CHAR(3, 0x5) {
        @Override public void invoke(final ShortStack arguments) {
            final short outputCharacterCode = arguments.get(0);
            Memory.streams().append(Character.toString((char) outputCharacterCode));
        }
    },
    PRINT_FORM(4, 0x1a) {
        @Override public void invoke(final ShortStack arguments) {
            final short formattedTable = arguments.get(0);
            // TODO print-form
        }
    },
    PRINT_NUM(3, 0x6) {
        @Override public void invoke(final ShortStack arguments) {
            final short value = arguments.get(0);
            Memory.streams().append(Short.toString(value));
        }
    },
    PRINT_OBJ(1, 0xa) {
        @Override public void invoke(final ShortStack arguments) {
            final int object = arguments.get(0) & 0xffff;
            if (object == 0) {
                Error.PRINT_OBJECT_0.invoke();
                return;
            }
            final ZObject zo = ZObject.count(object);
            if (false) {
                Log.i("Xyzzy", "PRINT OBJECT: " + zo);
            }
            Memory.streams().append(zo.zProperty().toString());
        }
    },
    PRINT_PADDR(1, 0xd) {
        @Override public void invoke(final ShortStack arguments) {
            final int packedAddressOfString = arguments.get(0) & 0xffff;
            final int address = Memory.unpackAddress(packedAddressOfString);
            Memory.streams().append(ZText.encodedAtOffset(address));
        }
    },
    PRINT_RET(0, 0x3) {
        @Override public void invoke(final ShortStack arguments) {
            PRINT.invoke(arguments);
            NEW_LINE.invoke(arguments);
            RTRUE.invoke(arguments);
        }
    },
    PRINT_TABLE(3, 0x1e) {
        @Override public void invoke(final ShortStack arguments) {
            final short zsciiText = arguments.get(0);
            final short width = arguments.get(1);
            final short height = arguments.get(2);
            final short skip = arguments.get(3);
            final String text = ZText.unencodedAtOffset(zsciiText);
            int charcount = 0;
            // TODO get cursor position
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    Memory.streams().append(Character.toString(text.charAt(charcount++)));
                }
                charcount += skip;
                // TODO should reset to cursor x position
                Memory.streams().append("\n");
            }
        }
    },
    PRINT_UNICODE(4, 0xb) {
        @Override public void invoke(final ShortStack arguments) {
            final short charNumber = arguments.get(0);
            Memory.streams().append(Character.toString((char) charNumber));
        }
    },
    PULL(3, 0x9) {
        @Override public void invoke(final ShortStack arguments) {
            final short variable = arguments.get(0);
            //TODO error on underflow
            final short value = Memory.current().callStack.peek().pop();
            if (variable == 0) {
                Memory.current().callStack.peek().pop();
            }
            storeValue(variable, value);
        }
    },
    PUSH(3, 0x8) {
        @Override public void invoke(final ShortStack arguments) {
            final short value = arguments.get(0);
            Memory.current().callStack.peek().push(value);
        }
    },
    PUSH_STACK(4, 0x18) {
        @Override public void invoke(final ShortStack arguments) {
            final short value = arguments.get(0);
            final short stack = arguments.get(1);
            // TODO push-stack
            branchOnTest(false);
        }
    },
    PUT_PROP(3, 0x3) {
        @Override public void invoke(final ShortStack arguments) {
            final short object = arguments.get(0);
            final short property = arguments.get(1);
            final short value = arguments.get(2);
            final ZProperty zp = ZObject.count(object).zProperty();
            zp.putProperty(property, value);
        }
    },
    PUT_WIND_PROP(4, 0x19) {
        @Override public void invoke(final ShortStack arguments) {
            final short window = arguments.get(0);
            final short propertyNmber = arguments.get(1);
            final short value = arguments.get(2);
            // TODO put-wind-prop
        }
    },
    QUIT(0, 0xa) {
        @Override public void invoke(final ShortStack arguments) {
            NEW_LINE.invoke(arguments);
            ZWindow.printAllScreens();
            Decoder.terminate();
        }
    },
    RANDOM(3, 0x7) {
        @Override public void invoke(final ShortStack arguments) {
            final short range = arguments.get(0);
            int value = 0;
            if (range < 0) {
                Memory.current().random.seed_random(range);
            } else if (range == 0) {
                Memory.current().random.seed_random((int) System.currentTimeMillis());
            } else {
                value = Memory.current().random.random(range);
            }
            readDestinationAndStoreResult(value);
        }
    },
    READ(3, 0x4) {
        @Override public void invoke(final ShortStack arguments) {
            if (Header.VERSION.value() <= 3) {
                SHOW_STATUS.invoke(arguments);
            }
            ZWindow.printAllScreens();
            final int text = arguments.get(0) & 0xffff;
            final int parse = arguments.get(1) & 0xffff;
            final int time = arguments.get(2) & 0xffff;
            final int routine = arguments.get(3) & 0xffff;
            if (routine != 0 || time != 0) {
                Log.i("Xyzzy", "Read routine should be timed...");
            }
            final int maxCharacters = Memory.current().buff().get(text);
            String inputString = Memory.streams().promptForInput().toLowerCase(Locale.UK);
            Memory.streams().userInput(inputString);
            inputString = inputString.substring(0, Math.min(maxCharacters, inputString.length()));
            Memory.current().buff().put(text + 1, (byte) inputString.length());
            ZText.tokeniseInputToBuffers(text, parse, inputString);
            if (Header.VERSION.value() >= 5) {
                readDestinationAndStoreResult((short) 13);
            }
        }
    },
    READ_CHAR(3, 0x16) {
        @Override public void invoke(final ShortStack arguments) {
            ZWindow.printAllScreens();
            int value;
            final short one = arguments.get(0);
            final short time = arguments.get(1);
            final short routine = arguments.get(2);
            value = MainActivity.waitOnKey();
            Memory.streams().userInput(Character.toString((char) value));
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
    READ_MOUSE(4, 0x16) {
        @Override public void invoke(final ShortStack arguments) {
            //TODO read-mouse
            final short array = arguments.get(0);
            Memory.current().buffer.put(array + 0, 0); // mouse X
            Memory.current().buffer.put(array + 1, 0); // mouse Y
            Memory.current().buffer.put(array + 2, 0); // mouse buttons
            Memory.current().buffer.put(array + 3, 0); // menu word
        }
    },
    REMOVE_OBJ(1, 0x9) {
        @Override public void invoke(final ShortStack arguments) {
            final short object = arguments.get(0);
            ZObject.detachFromTree(object);
        }
    },
    RESTART(0, 0x7) {
        @Override public void invoke(final ShortStack arguments) {
            Memory.current().random.seed_random(0);
            Memory.loadDataFromFile();
            Memory.current().callStack.peek().clearStack();
            final int pc = Header.START_PC.value();
            if (Header.VERSION.value() != 6 || true) {
                Memory.current().callStack.peek().setProgramCounter(pc);
            } else {
                CallStack.call(pc, arguments, new StackDiscard());
            }
        }
    },
    RESTORE(4, 0x1) {
        @Override public void invoke(final ShortStack arguments) {
            Memory loaded = null;
            try {
                final String getSaveGame = selectSaveGame();
                final FileInputStream save = MainActivity.activity.openFileInput(getSaveGame);
                final ObjectInputStream ois = new ObjectInputStream(save);
                loaded = (Memory) ois.readObject();
                ois.close();
            } catch (final Exception e) {
                Log.e("Xyzzy", "Couldn't restore save:", e);
                readDestinationAndStoreResult(0);
                return;
            }
            if (!loaded.storyPath.equals(Memory.current().storyPath)) {
                Log.i("Xyzzy", "Not a save from this story. " + loaded.storyPath + " v " + Memory.current().storyPath);
                readDestinationAndStoreResult(0);
            } else {
                Memory.setCurrent(loaded);
                Memory.setScreenColumns();
                ZObject.enumerateObjects();
                readDestinationAndStoreResult(2);
            }
        }
    },
    RESTORE_UNDO(4, 0xa) {
        @Override public void invoke(final ShortStack arguments) {
            Memory loaded = null;
            try {
                final ByteArrayInputStream bais = new ByteArrayInputStream(Memory.undo());
                final ObjectInputStream ois = new ObjectInputStream(bais);
                loaded = (Memory) ois.readObject();
                ois.close();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            } catch (final ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            if (!loaded.storyPath.equals(Memory.current().storyPath)) {
                Log.i("Xyzzy", "Not a save from this story. " + loaded.storyPath + " v " + Memory.current().storyPath);
                readDestinationAndStoreResult(0);
            } else {
                Memory.setCurrent(loaded);
                ZObject.enumerateObjects();
                readDestinationAndStoreResult(2);
            }
        }
    },
    RET(1, 0xb) {
        @Override public void invoke(final ShortStack arguments) {
            final short value = arguments.get(0);
            returnValue(value);
        }
    },
    RET_POPPED(0, 8) {
        @Override public void invoke(final ShortStack arguments) {
            final short value = Memory.current().callStack.peek().pop();
            returnValue(value);
        }
    },
    RFALSE(0, 0x1) {
        @Override public void invoke(final ShortStack arguments) {
            returnValue(0);
        }
    },
    RTRUE(0, 0x0) {
        @Override public void invoke(final ShortStack arguments) {
            returnValue(1);
        }
    },
    SAVE(4, 0x0) {
        @Override public void invoke(final ShortStack arguments) {
            try {
                String saveGameName = saveGameName();
                Time now = new Time();
                now.setToNow();
                saveGameName = saveGameName + now.format("%H.%M on %d %B %y");
                final FileOutputStream save = MainActivity.activity.openFileOutput(saveGameName, Context.MODE_PRIVATE);
                final ObjectOutputStream oos = new ObjectOutputStream(save);
                oos.writeObject(Memory.current());
                oos.close();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
            if (Header.VERSION.value() > 4) {
                readDestinationAndStoreResult(1);
            } else {
                branchOnTest(true);
            }
        }
    },
    SAVE_UNDO(4, 0x9) {
        @Override public void invoke(final ShortStack arguments) {
            final long time = System.currentTimeMillis();
            final ByteArrayOutputStream baos = new ByteArrayOutputStream(Memory.undo().length);
            try {
                final ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(Memory.current());
                oos.close();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
            Memory.storeUndo(baos.toByteArray());
            Log.i("Xyzzy", "Made undo file:" + Memory.undo().length + " bytes, " + (System.currentTimeMillis() - time)
                    + " ms");
            readDestinationAndStoreResult(1);
        }
    },
    SCAN_TABLE(3, 0x17) {
        @Override public void invoke(final ShortStack arguments) {
            final int x = arguments.get(0) & 0xffff;
            final int table = arguments.get(1) & 0xffff;
            final int len = arguments.get(2) & 0xffff;
            int form = 0x82;
            if (arguments.size() == 4) { // and implicitly, version 5
                form = arguments.get(3);
            }
            final boolean isWord = Bit.bit7(form);
            final int skip = Bit.low(form, 7);
            for (int i = 0; i < len; i += skip) {
                final int wordAtI = isWord ? Memory.current().buffer.getShort(table + i * skip) & 0xffff : Memory
                        .current().buffer.get(table + i * skip) & 0xff;
                if (x == wordAtI) {
                    readDestinationAndStoreResult(table + i * skip);
                    branchOnTest(true);
                    return;
                }
            }
            readDestinationAndStoreResult(0);
            branchOnTest(false); // eat the branch address
        }
    },
    SCROLL_WINDOW(4, 0x14) {
        @Override public void invoke(final ShortStack arguments) {
            // TODO scroll-window
            final short window = arguments.get(0);
            final short pixels = arguments.get(1);
        }
    },
    SET_ATTR(2, 0xb) {
        @Override public void invoke(final ShortStack arguments) {
            final short object = arguments.get(0);
            final short attribute = arguments.get(1);
            final ZObject zo = ZObject.count(object);
            zo.setAttribute(attribute);
        }
    },
    SET_COLOUR(2, 0x1b) {
        @Override public void invoke(final ShortStack arguments) {
            if (!(Boolean) Preferences.USE_COLOUR.getValue(MainActivity.activity)) {
                return;
            }
            final short foreground = arguments.get(0);
            final short background = arguments.get(1);
            ZWindow.setColour(foreground, background);
            if (false) {
                Log.w("Xyzzy", "set_colour " + foreground + " " + background);
            }
        }
    },
    SET_CURSOR(3, 0xf) {
        @Override public void invoke(final ShortStack arguments) {
            final short line = arguments.get(0);
            short column = 0;
            if (arguments.size() >= 2) {
                column = arguments.get(1);
            }
            if (false) {
                Log.i("Xyzzy", "SET CURSOR: " + line + ", " + column);
            }
            Memory.streams().setCursor(column, line);
        }
    },
    SET_FONT(4, 0x4) {
        @Override public void invoke(final ShortStack arguments) {
            final short font = arguments.get(0);
            // TODO set_font
        }
    },
    SET_MARGINS(4, 0x8) {
        @Override public void invoke(final ShortStack arguments) {
            final short left = arguments.get(0);
            final short right = arguments.get(0);
            final short window = arguments.get(0);
            // TODO set_margins
        }
    },
    SET_TEXT_STYLE(3, 0x11) {
        @Override public void invoke(final ShortStack arguments) {
            final short style = arguments.get(0);
            switch (style) {
            case 0:
            default:
                if (false) {
                    Log.i("Xyzzy", "ROMAN");
                }
                Memory.streams().clearStyles();
                break;
            case 1:
                if (false) {
                    Log.i("Xyzzy", "REVERSE VIDEO");
                }
                Memory.streams().addStyle(TextStyle.REVERSE_VIDEO);
                break;
            case 2:
                if (false) {
                    Log.i("Xyzzy", "BOLD");
                }
                Memory.streams().addStyle(TextStyle.BOLD);
                break;
            case 4:
                if (false) {
                    Log.i("Xyzzy", "ITALIC");
                }
                Memory.streams().addStyle(TextStyle.ITALIC);
                break;
            case 8:
                if (false) {
                    Log.i("Xyzzy", "FIXED PITCH");
                }
                Memory.streams().addStyle(TextStyle.FIXED_PITCH);
                break;
            }
        }
    },
    SET_TRUE_COLOUR(4, 0xd) {
        @Override public void invoke(final ShortStack arguments) {
            if (!(Boolean) Preferences.USE_COLOUR.getValue(MainActivity.activity)) {
                return;
            }
            final short foreground = arguments.get(0);
            final short background = arguments.get(1);
            ZWindow.setTrueColour(foreground, background);
            if (false) {
                Log.w("Xyzzy", "set_true_colour " + foreground + " " + background);
            }
        }
    },
    SET_WINDOW(3, 0xb) {
        @Override public void invoke(final ShortStack arguments) {
            final short window = arguments.get(0);
            if (false) {
                Log.i("Xyzzy", "SET WINDOW: " + arguments.get(0));
            }
            Memory.current().currentScreen = window;
            if (Memory.current().zwin.get(window) == null) {
                Memory.current().zwin.put(window, new ZWindow(window));
            }
        }
    },
    SHOW_STATUS(0, 0xc) {
        @Override public void invoke(final ShortStack arguments) {
            if (Header.VERSION.value() > 3) {
                NOP.invoke(arguments);
            } else {
                //TODO show-status
                final int flags = Header.CONFIG.value();
                boolean scoreGame = Header.VERSION.value() < 2 || !Bit.bit1(flags);
                StringBuilder sb = new StringBuilder();
                int firstGlobal = readValue(16);
                ZObject zo = ZObject.count(firstGlobal);
                sb.append(zo.zProperty().toString());
                sb.append("     ");
                if (scoreGame) {
                    final int score = readValue(17);
                    final int turns = readValue(18);
                    sb.append("Score: ");
                    sb.append(score);
                    sb.append(" Turns: ");
                    sb.append(turns);
                } else { // timegame
                    final int hours = readValue(17);
                    final int minutes = readValue(18);
                    if ((Boolean) Preferences.TWENTYFOURHOUR.getValue(MainActivity.activity)) {
                        sb.append(String.format("Time: %d:%02d", hours, minutes));
                    } else {
                        int displayHours = hours % 12;
                        if (displayHours == 0) {
                            displayHours = 12;
                        }
                        sb.append(String.format("Time: %d:%02d %s", displayHours, minutes, hours < 12 ? "am" : "pm"));
                    }
                }
                final ZWindow statusLine = Memory.current().zwin.get(1);
                statusLine.setNaturalHeight(1);
                statusLine.setCursor(1, 1);
                statusLine.eraseLine(1);
                statusLine.addStyle(TextStyle.REVERSE_VIDEO);
                statusLine.append(sb.toString());
                statusLine.clearStyles();
            }
        }
    },
    SOUND_EFFECT(3, 0x15) {
        @Override public void invoke(final ShortStack arguments) {
            if (!(Boolean) Preferences.SOUND_ON.getValue(MainActivity.activity)) {
                return;
            }
            final short number = arguments.get(0);
            //            final short effect = arguments.get(1);
            //            final short volume = arguments.get(2);
            //            final short routine = arguments.get(3);
            // TODO sound_effect
            if (number == 1) {
                Beep.beep1.playSound();
            } else if (number == 2) {
                Beep.beep2.playSound();
            } else {
                Log.i("Xyzzy", "Sound effect:" + arguments);
            }
        }
    },
    SPLIT_WINDOW(3, 0xa) {
        @Override public void invoke(final ShortStack arguments) {
            final short lines = arguments.get(0);
            if (false) {
                Log.i("Xyzzy", "SPLIT WINDOW: " + lines + " LINES (CURRENT:" + Memory.streams() + ")");
            }
            final int splitScreen = Memory.current().currentScreen + 1;
            final ZWindow nextScreen = Memory.current().zwin.get(splitScreen);
            if (nextScreen != null) {
                nextScreen.reset();
                nextScreen.setNaturalHeight(lines);
            }
        }
    },
    STORE(2, 0xd) {
        @Override public void invoke(final ShortStack arguments) {
            final short variable = arguments.get(0);
            final short value = arguments.get(1);
            if (variable == 0) {
                Memory.current().callStack.peek().pop(); // discard the top-of-stack
                Memory.current().callStack.peek().push(value);
            } else {
                storeValue(variable, value);
            }
        }
    },
    STOREB(3, 0x2) {
        @Override public void invoke(final ShortStack arguments) {
            final int array = arguments.get(0) & 0xffff;
            final int byteIndex = arguments.get(1) & 0xffff;
            final byte value = (byte) arguments.get(2);
            Memory.current().buff().put(array + byteIndex, value);
        }
    },
    STOREW(3, 0x1) {
        @Override public void invoke(final ShortStack arguments) {
            final int array = arguments.get(0) & 0xffff;
            final int wordIndex = arguments.get(1) & 0xffff;
            final short value = arguments.get(2);
            final int address = array + 2 * wordIndex;
            Memory.current().buff().putShort(address, value);
        }
    },
    SUB(2, 21) {
        @Override public void invoke(final ShortStack arguments) {
            final short a = arguments.get(0);
            final short b = arguments.get(1);
            readDestinationAndStoreResult(a - b);
        }
    },
    TEST(2, 0x7) {
        @Override public void invoke(final ShortStack arguments) {
            final short bitmap = arguments.get(0);
            final short flags = arguments.get(1);
            branchOnTest((bitmap & flags) == flags);
        }
    },
    TEST_ATTR(2, 0xa) {
        @Override public void invoke(final ShortStack arguments) {
            final int object = arguments.get(0) & 0xffff;
            final int attribute = arguments.get(1) & 0xffff;
            if (object == 0) {
                Error.TEST_ATTR_0.invoke();
                branchOnTest(false);
                return;
            }
            final ZObject zo = ZObject.count(object);
            final boolean value = zo.testAttribute(attribute);
            branchOnTest(value);
        }
    },
    THROW(2, 0x1c) {
        @Override public void invoke(final ShortStack arguments) {
            final short value = arguments.get(0);
            final short stackFrame = arguments.get(1);
            Log.w("Xyzzy", "Throw:" + value + " stackFrame:" + stackFrame);
            logCallStack();
            throw new UnsupportedOperationException("@throw");
        }
    },
    TOKENISE(3, 0x1b) {
        @Override public void invoke(final ShortStack arguments) {
            final int text = arguments.get(0) & 0xffff;
            final int parse = arguments.get(1) & 0xffff;
            if (arguments.size() != 2) {
                throw new UnsupportedOperationException("@tokenise (long arguments)");
            }
            final int length = Memory.current().buff().get(text + 1);
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                sb.append((char) Memory.current().buff().get(text + i + 2));
            }
            final String inputString = sb.toString().toLowerCase(Locale.UK);
            ZText.tokeniseInputToBuffers(text, parse, inputString);
        }
    },
    VERIFY(0, 0xd) {
        @Override public void invoke(final ShortStack arguments) {
            //TODO verify
            branchOnTest(true);
        }
    },
    WINDOW_SIZE(4, 0x11) {
        @Override public void invoke(final ShortStack arguments) {
            // TODO window-size
            final short window = arguments.get(0);
            final short y = arguments.get(1);
            final short x = arguments.get(2);
        }
    },
    WINDOW_STYLE(4, 0x12) {
        @Override public void invoke(final ShortStack arguments) {
            // TODO window-style
            final short window = arguments.get(0);
            final short flags = arguments.get(1);
            final short operation = arguments.get(2);
        }
    };
    protected static void branch(final int offset) {
        if (offset == 0) {
            RFALSE.invoke(null);
        } else if (offset == 1) {
            RTRUE.invoke(null);
        } else {
            Memory.current().callStack.peek().adjustProgramCounter(offset - 2);
        }
    }

    protected static void branchOnTest(final boolean test) {
        final int lobit = Memory.current().callStack.peek().getProgramByte();
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
            offset += Memory.current().callStack.peek().getProgramByte();
            if (negative) {
                offset = offset - 4096;
            }
        } else { // one byte
            offset = Bit.low(lobit, 6);
        }
        return offset;
    }

    static void callAndDiscard(final ShortStack arguments) {
        final int routine = arguments.get(0) & 0xffff;
        if (routine != 0) {
            CallStack.call(routine, arguments, new StackDiscard());
        }
    }

    static void callAndStore(final ShortStack arguments) {
        final int routine = arguments.get(0) & 0xffff;
        final int result = Memory.current().callStack.peek().getProgramByte();
        if (routine == 0) {
            storeValue(result, 0);
        } else {
            CallStack.call(routine, arguments, new StackStore(result));
        }
    }

    private static int globalVariableAddress(final int ldestination) {
        return (Header.GLOBALS.value() & 0xffff) + 2 * (ldestination - 16);
    }

    static void logCallStack() {
        for (final CallStack cs : Memory.current().callStack) {
            Log.d("Xyzzy", cs.toString());
        }
    }

    public static void readDestinationAndStoreResult(final int value) {
        final int destination = Memory.current().callStack.peek().getProgramByte();
        storeValue(destination, (short) value);
    }

    public static int readValue(final int destination) {
        if (destination == 0) {
            try {
                return Memory.current().callStack.peek().pop();
            } catch (final EmptyStackException ese) {
                Log.e("Xyzzy", "Opcode.readValue:", ese);
                Error.STK_UNDF.invoke(); // fatal
                return 0;
            }
        } else if (destination < 16) {
            return Memory.current().callStack.peek().get(destination);
        } else if (destination < 256) {
            final int addr = globalVariableAddress(destination);
            return Memory.current().buff().getShort(addr);
        } else {
            throw new UnsupportedOperationException("Read value from invalid store:" + destination);
        }
    }

    public static void returnValue(final int value) {
        final IInvokeable i = Memory.current().callStack.peek().returnFunction();
        Memory.current().callStack.pop();
        Memory.current().callStack.peek().push(value);
        i.invoke();
        if (false) {
            Log.i("Xyzzy", "<--");
        }
    }

    static String saveGameName() {
        final File story = new File(Memory.current().storyPath);
        final String namepart = story.getName();
        return namepart;
    }

    protected static String selectSaveGame() {
        SaveChooserActivity.gameName = saveGameName();
        final Intent intent = new Intent(MainActivity.activity, SaveChooserActivity.class);
        MainActivity.activity.startActivity(intent);
        synchronized (SaveChooserActivity.syncObject) {
            try {
                SaveChooserActivity.syncObject.wait();
            } catch (InterruptedException e) {
                Log.e("Xyzzy", "Opcode.selectSaveGame interrupted", e);
            }
            return SaveChooserActivity.syncObject.toString();
        }
    }

    public static void storeValue(final int destination, final int value) {
        final int ldestination = destination & 0xff;
        if (false) {
            Log.i("Xyzzy", "->" + ldestination + "=" + (value & 0xffff));
        }
        if (ldestination == 0) {
            Memory.current().callStack.peek().push(value);
        } else if (ldestination < 16) {
            Memory.current().callStack.peek().put(ldestination, value);
        } else {
            final int addr = globalVariableAddress(ldestination);
            Memory.current().buff().putShort(addr, (short) value);
        }
    }

    public final int hex;
    public final int operands;

    private Opcode() { // constructor for pre-V5 opcodes
        operands = 0;
        hex = 0x0;
    }

    private Opcode(final int operands, final int hex) {
        this.operands = operands;
        this.hex = hex;
        OpMap.map(this);
    }

    abstract public void invoke(final ShortStack arguments);

    @Override public String toString() {
        return "(" + operands + "," + Integer.toHexString(hex) + ") " + super.toString().toLowerCase(Locale.UK);
    }
}