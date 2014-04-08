
package uk.addie.xyzzy.zobjects;

import java.util.HashSet;
import java.util.Set;

import uk.addie.xyzzy.error.Error;
import uk.addie.xyzzy.header.Header;
import uk.addie.xyzzy.os.Debug;
import uk.addie.xyzzy.state.Memory;
import android.util.Log;
import android.util.SparseArray;

public class ZObject {
    public static ZObject count(final int oCount) {
        int count = oCount;
        if (count == 0) {
            Log.e("Xyzzy", "Object zero");
            Error.ILL_OBJ.invoke();
            return ZObject.count(1); // TODO bad
        } else if (count < 0) {
            Log.e("Xyzzy", "Negative object count");
            Error.ILL_OBJ.invoke();
        } else if (count > Memory.CURRENT.objectCount) {
            Log.e("Xyzzy", "Object count " + oCount + " > " + Memory.CURRENT.objectCount);
            //            Error.ERR_ILL_OBJ.invoke();
        }
        final ZObject rval = new ZObject();
        if (Header.VERSION.value() <= 3) {
            rval.offset = Header.OBJECTS.value() + 62 + 9 * (count - 1);
        } else {
            rval.offset = Header.OBJECTS.value() + 126 + 14 * (count - 1);
        }
        rval.count = count;
        return rval;
    }

    public static void detachFromTree(final short object) {
        if (Debug.moves) {
            Log.i("Xyzzy", "Detaching " + count(object) + " from tree");
        }
        final ZObject zo = count(object);
        // detach from the existing tree
        final int oldSibling = zo.sibling();
        Set<Integer> expectedSiblings = new HashSet<Integer>(getReverse(reverseSiblings, object));
        Set<Integer> expectedChildren = new HashSet<Integer>(getReverse(reverseChildren, object));
        for (int i : expectedSiblings) {
            count(i).setSibling(oldSibling);
        }
        for (int i : expectedChildren) {
            count(i).setChild(oldSibling);
        }
        zo.setSibling(0);
        zo.setParent(0);
    }

    public static void enumerateObjects() {
        Memory.CURRENT.objectCount = 0xffff; // suppress warnings for now
        int objectCount = 1;
        int lowestProperty = 0xffffff;
        while (true) {
            final ZObject zo = ZObject.count(objectCount);
            if (zo.offset >= lowestProperty) {
                objectCount--;
                break;
            }
            addToReverseMap(reverseParents, objectCount, zo.parent());
            addToReverseMap(reverseSiblings, objectCount, zo.sibling());
            addToReverseMap(reverseChildren, objectCount, zo.child());
            final int nameOffset = zo.properties();
            lowestProperty = Math.min(nameOffset, lowestProperty);
            objectCount++;
        }
        Memory.CURRENT.objectCount = objectCount;
    }

    private int                                    count;
    private int                                    offset;
    private final static SparseArray<Set<Integer>> reverseChildren = new SparseArray<Set<Integer>>();
    private final static SparseArray<Set<Integer>> reverseSiblings = new SparseArray<Set<Integer>>();
    private final static SparseArray<Set<Integer>> reverseParents  = new SparseArray<Set<Integer>>();

    private static void addToReverseMap(SparseArray<Set<Integer>> map, int object, int linked) {
        Set<Integer> target = getReverse(map, linked);
        target.add(object);
    }

    private static void clearFromReverseMap(SparseArray<Set<Integer>> map, int object, int linked) {
        Set<Integer> target = getReverse(map, linked);
        target.remove(object);
    }

    private static Set<Integer> getReverse(SparseArray<Set<Integer>> map, int linked) {
        Set<Integer> target = map.get(linked);
        if (target == null) {
            target = new HashSet<Integer>();
            map.put(linked, target);
        }
        return target;
    }

    private int[] attrcalc(final int myCount) {
        int localCount = myCount;
        int attrByte = offset;
        while (localCount >= 8) {
            attrByte++;
            localCount -= 8;
        }
        final int bitpattern = 1 << 7 - localCount;
        return new int[] { attrByte, bitpattern };
    }

    public int child() {
        if (Header.VERSION.value() <= 3) {
            return Memory.CURRENT.buff().getShort(offset + 8);
        }
        return Memory.CURRENT.buff().getShort(offset + 10);
    }

    public void clearAttribute(final int aCount) {
        final int[] attrcalc = attrcalc(aCount);
        byte b = (byte) Memory.CURRENT.buff().get(attrcalc[0]);
        if ((b & attrcalc[1]) == 0) {
            return;
        }
        b ^= attrcalc[1];
        Memory.CURRENT.buff().put(attrcalc[0], b);
    }

    public int count() {
        return count;
    }

    public int parent() {
        if (Header.VERSION.value() <= 3) {
            return Memory.CURRENT.buff().getShort(offset + 4);
        }
        return Memory.CURRENT.buff().getShort(offset + 6);
    }

    public int properties() {
        if (Header.VERSION.value() <= 3) {
            return Memory.CURRENT.buff().getShort(offset + 10) & 0xffff;
        }
        return Memory.CURRENT.buff().getShort(offset + 12) & 0xffff;
    }

    public void setAttribute(final int count) {
        final int[] attrcalc = attrcalc(count);
        byte b = (byte) Memory.CURRENT.buff().get(attrcalc[0]);
        b |= attrcalc[1];
        Memory.CURRENT.buff().put(attrcalc[0], b);
    }

    public void setChild(final int object) {
        if (count == object) {
            Error.MAKING_OBJECT_OWN_CHILD.invoke();
        }
        clearFromReverseMap(reverseChildren, count, child());
        addToReverseMap(reverseChildren, count, object);
        if (Header.VERSION.value() <= 3) {
            Memory.CURRENT.buff().putShort(offset + 8, (short) object);
        } else {
            Memory.CURRENT.buff().putShort(offset + 10, (short) object);
        }
        if (Debug.moves) {
            Log.i("Xyzzy", count + " new child is " + object);
        }
    }

    public void setParent(final int object) {
        if (count == object) {
            Error.MAKING_OBJECT_OWN_PARENT.invoke();
        }
        clearFromReverseMap(reverseParents, count, parent());
        addToReverseMap(reverseParents, count, object);
        if (Header.VERSION.value() <= 3) {
            Memory.CURRENT.buff().putShort(offset + 4, (short) object);
        } else {
            Memory.CURRENT.buff().putShort(offset + 6, (short) object);
        }
        if (Debug.moves) {
            Log.i("Xyzzy", count + " new parent is " + object);
        }
    }

    public void setSibling(final int object) {
        if (count == object) {
            Error.MAKING_OBJECT_OWN_SIBLING.invoke();
        }
        clearFromReverseMap(reverseSiblings, count, sibling());
        addToReverseMap(reverseSiblings, count, object);
        if (Header.VERSION.value() <= 3) {
            Memory.CURRENT.buff().putShort(offset + 6, (short) object);
        } else {
            Memory.CURRENT.buff().putShort(offset + 8, (short) object);
        }
        if (Debug.moves) {
            Log.i("Xyzzy", count + " new sibling is " + object);
        }
    }

    public int sibling() {
        if (Header.VERSION.value() <= 3) {
            return Memory.CURRENT.buff().getShort(offset + 6);
        }
        return Memory.CURRENT.buff().getShort(offset + 8);
    }

    public boolean testAttribute(final int aCount) {
        final int[] attrcalc = attrcalc(aCount);
        final byte b = (byte) Memory.CURRENT.buff().get(attrcalc[0]);
        return (b & attrcalc[1]) != 0;
    }

    @Override public String toString() {
        return "object:" + count + " " + new ZProperty(properties()) + " [p" + parent() + ",s" + sibling() + ",c"
                + child() + "]";
    }
}
