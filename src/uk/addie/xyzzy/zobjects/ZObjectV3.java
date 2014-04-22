
package uk.addie.xyzzy.zobjects;

import uk.addie.xyzzy.header.Header;
import uk.addie.xyzzy.state.Memory;

public class ZObjectV3 extends ZObject {
    public ZObjectV3(int count) {
        super(count);
        offset = Header.OBJECTS.value() + 62 + 9 * (count - 1);
    }

    @Override public int child() {
        return Memory.current().buff().get(offset + 6);
    }

    @Override protected int maxAttributes() {
        return 32;
    }

    @Override public int parent() {
        return Memory.current().buff().get(offset + 4);
    }

    @Override int properties() {
        return Memory.current().buff().getShort(offset + 7);
    }

    @Override public void setChild(int object) {
        super.setChild(object);
        Memory.current().buff().put(offset + 6, (byte) object);
    }

    @Override public void setParent(int object) {
        super.setParent(object);
        Memory.current().buff().put(offset + 4, (byte) object);
    }

    @Override public void setSibling(int object) {
        super.setSibling(object);
        Memory.current().buff().put(offset + 5, (byte) object);
    }

    @Override public int sibling() {
        return Memory.current().buff().get(offset + 5);
    }
}
