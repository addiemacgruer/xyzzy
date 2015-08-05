package uk.addie.xyzzy.zobjects;

import java.util.HashSet;
import java.util.Set;

import uk.addie.xyzzy.error.Error;
import uk.addie.xyzzy.header.Header;
import uk.addie.xyzzy.state.Memory;
import android.util.Log;
import android.util.SparseArray;

abstract public class ZObject {
  ZObject(final int count2) {
    count = count2;
  }

  private final int count;

  protected int offset;

  abstract public int child();

  public void clearAttribute(final int aCount) {
    final int[] attrcalc = attrcalc(aCount);
    byte b = (byte) Memory.current().buff().get(attrcalc[0]);
    if ((b & attrcalc[1]) == 0) {
      return;
    }
    b ^= attrcalc[1];
    Memory.current().buff().put(attrcalc[0], b);
  }

  abstract public int parent();

  public void setAttribute(final int count) {
    if (count > maxAttributes()) {
      Error.ATTRIBUTE_TOO_HIGH.invoke();
      return;
    }
    final int[] attrcalc = attrcalc(count);
    byte b = (byte) Memory.current().buff().get(attrcalc[0]);
    b |= attrcalc[1];
    Memory.current().buff().put(attrcalc[0], b);
  }

  public void setChild(final int object) {
    if (count == object) {
      Error.MAKING_OBJECT_OWN_CHILD.invoke();
    }
    clearFromReverseMap(reverseChildren, count, child());
    addToReverseMap(reverseChildren, count, object);
  }

  public void setParent(final int object) {
    if (count == object) {
      Error.MAKING_OBJECT_OWN_PARENT.invoke();
    }
    clearFromReverseMap(reverseParents, count, parent());
    addToReverseMap(reverseParents, count, object);
  }

  public void setSibling(final int object) {
    if (count == object) {
      Error.MAKING_OBJECT_OWN_SIBLING.invoke();
    }
    clearFromReverseMap(reverseSiblings, count, sibling());
    addToReverseMap(reverseSiblings, count, object);
  }

  abstract public int sibling();

  public boolean testAttribute(final int aCount) {
    if (aCount > maxAttributes()) {
      Error.ATTRIBUTE_TOO_HIGH.invoke();
      return false;
    }
    final int[] attrcalc = attrcalc(aCount);
    final byte b = (byte) Memory.current().buff().get(attrcalc[0]);
    return (b & attrcalc[1]) != 0;
  }

  @Override public String toString() {
    return "object:" + count + " " + new ZProperty(properties()) + " [p" + parent() + ",s"
        + sibling() + ",c" + child() + "]";
  }

  public ZProperty zProperty() {
    return new ZProperty(properties());
  }

  abstract protected int maxAttributes();

  abstract int properties();

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

  private final static SparseArray<Set<Integer>> reverseChildren = new SparseArray<Set<Integer>>();

  private final static SparseArray<Set<Integer>> reverseParents = new SparseArray<Set<Integer>>();

  private final static SparseArray<Set<Integer>> reverseSiblings = new SparseArray<Set<Integer>>();

  public static ZObject count(final int oCount) {
    final int count = oCount;
    if (count == 0) {
      Log.e("Xyzzy", "Object zero");
      Error.OBJECT_ZERO.invoke();
      return ZObject.count(1); // should really be a fatal error, but a lot of games request it.
    } else if (count < 0) {
      Log.e("Xyzzy", "Negative object count");
      Error.ILL_OBJ.invoke();
    } else if (count > Memory.current().objectCount) {
      Log.w("Xyzzy", "Object count " + oCount + " > " + Memory.current().objectCount);
      enumerateUpTo(count);
    }
    if (Header.VERSION.value() <= 3) {
      return new ZObjectV3(count);
    }
    return new ZObjectV5(count);
  }

  public static void detachFromTree(final short object) {
    final ZObject zo = count(object);
    // detach from the existing tree
    final int oldSibling = zo.sibling();
    final Set<Integer> expectedSiblings = new HashSet<Integer>(getReverse(reverseSiblings, object));
    final Set<Integer> expectedChildren = new HashSet<Integer>(getReverse(reverseChildren, object));
    for (final int i : expectedSiblings) {
      count(i).setSibling(oldSibling);
    }
    for (final int i : expectedChildren) {
      count(i).setChild(oldSibling);
    }
    zo.setSibling(0);
    zo.setParent(0);
  }

  public static void enumerateObjects() {
    Memory.current().objectCount = 0xffff; // suppress warnings for now
    reverseParents.clear();
    reverseSiblings.clear();
    reverseChildren.clear();
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
    Memory.current().objectCount = objectCount;
    Log.d("Xyzzy", "Found " + objectCount + " objects");
  }

  private static void addToReverseMap(final SparseArray<Set<Integer>> map, final int object,
      final int linked) {
    final Set<Integer> target = getReverse(map, linked);
    target.add(object);
  }

  private static void clearFromReverseMap(final SparseArray<Set<Integer>> map, final int object,
      final int linked) {
    final Set<Integer> target = getReverse(map, linked);
    target.remove(object);
  }

  private static void enumerateUpTo(final int newHigh) {
    final int oldObjectCount = Memory.current().objectCount;
    Memory.current().objectCount = 0xffff;
    for (int i = oldObjectCount; i < newHigh; i++) {
      final ZObject zo = ZObject.count(i);
      addToReverseMap(reverseParents, i, zo.parent());
      addToReverseMap(reverseSiblings, i, zo.sibling());
      addToReverseMap(reverseChildren, i, zo.child());
    }
    Log.d("Xyzzy", "Found " + (newHigh - oldObjectCount) + " more objects");
    Memory.current().objectCount = newHigh;
  }

  private static Set<Integer> getReverse(final SparseArray<Set<Integer>> map, final int linked) {
    Set<Integer> target = map.get(linked);
    if (target == null) {
      target = new HashSet<Integer>();
      map.put(linked, target);
    }
    return target;
  }
}
