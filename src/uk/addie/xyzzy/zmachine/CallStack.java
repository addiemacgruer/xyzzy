
package uk.addie.xyzzy.zmachine;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import uk.addie.xyzzy.error.Error;
import uk.addie.xyzzy.header.Header;
import uk.addie.xyzzy.interfaces.IInvokeable;
import uk.addie.xyzzy.os.Debug;
import uk.addie.xyzzy.state.Memory;
import android.util.Log;

public class CallStack implements Serializable {
    private static final long serialVersionUID = 1L;

    private static int calculateProgramCounter(final int rpos) {
        final int pc = Memory.unpackAddress(rpos);
        if (pc >= Memory.story_size()) {
            Error.ILL_JUMP_ADDR.invoke();
        }
        return pc;
    }

    public static void call(final int routine, final ZStack<Short> args, final IInvokeable returnFunction) {
        final int rpos = routine & 0xffff;
        if (rpos == 0) {
            Log.e("Xyzzy", "Called a routine at 0 (shouldn't have made it to call())");
            return;
        }
        final int pc = calculateProgramCounter(rpos);
        final CallStack f = new CallStack(pc, args.size() - 1, returnFunction);
        Memory.current().callStack.add(f);
        if (Header.VERSION.value() <= 4) {
            for (int i = 1; i <= f.localsCount; i++) {
                int value = Memory.current().callStack.peek().getProgramByte() << 8;
                value += Memory.current().callStack.peek().getProgramByte();
                f.register.put((short) i, (short) value);
            }
        }
        for (int i = 1, j = args.size(); i < j; i++) {
            f.register.put((short) i, args.get(i));
        }
    }

    public final int                calledWithCount;
    private final int               localsCount;
    private int                     programCounter;
    private final Map<Short, Short> register = new HashMap<Short, Short>();
    private final IInvokeable        returnFunction;
    private final ZStack<Short>     stack    = new ZStack<Short>((short) 0);

    public CallStack() {
        calledWithCount = 0;
        returnFunction = null;
        localsCount = 0;
    }

    private CallStack(final int programCounter, final int calledWithCount, final IInvokeable returnFunction) {
        this.setProgramCounter(programCounter);
        this.calledWithCount = calledWithCount;
        this.returnFunction = returnFunction;
        localsCount = calculateLocalsCount(programCounter);
    }

    public void adjustProgramCounter(final int offset) {
        programCounter += offset;
    }

    private int calculateLocalsCount(final int rpos) {
        final int count = getProgramByte();
        if (Debug.callstack) {
            Log.i("Xyzzy", "--> 0x" + Integer.toHexString(rpos) + ", " + count + " locals.");
        }
        if (count > 15) {
            Error.CALL_NON_RTN.invoke();
        }
        return count;
    }

    public void clearStack() {
        stack.clear();
    }

    public short get(final int destination) {
        if (destination > localsCount) {
            Log.i("Xyzzy", "Requested local" + destination + " but only " + localsCount + " allocated");
        }
        if (register.containsKey((short) destination)) {
            return register.get((short) destination);
        }
        return 0;
    }

    public int getProgramByte() {
        return Memory.current().buff().get(programCounter++);
    }

    public int peek() {
        return stack.peek();
    }

    public short pop() {
        return stack.pop();
    }

    public int programCounter() {
        return programCounter;
    }

    public void push(final int value) {
        stack.push((short) value);
    }

    public void put(final int destination, final int value) {
        if (destination > localsCount) {
            Log.e("Xyzzy", "Set local " + destination + " but only " + localsCount + " allocated");
        }
        if (value != 0) {
            register.put((short) destination, (short) value);
        } else {
            register.remove((short) destination);
        }
    }

    public IInvokeable returnFunction() {
        return returnFunction;
    }

    public void setProgramCounter(final int programCounter) {
        this.programCounter = programCounter;
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < localsCount; i++) {
            sb.append(i + 1);
            sb.append('=');
            sb.append(get((short) (i + 1)));
            sb.append(',');
        }
        sb.append(']');
        sb.append(stack.toString());
        return sb.toString();
    }
}