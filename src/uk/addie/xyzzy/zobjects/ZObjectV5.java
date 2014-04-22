
package uk.addie.xyzzy.zobjects;

import uk.addie.xyzzy.header.Header;
import uk.addie.xyzzy.state.Memory;

public class ZObjectV5 extends ZObject {
    public ZObjectV5(int count) {
        super(count);
        offset = Header.OBJECTS.value() + 126 + 14 * (count - 1);
    }

    @Override public int child() {
        return Memory.current().buff().getShort(offset + 10);
    }

    @Override protected int maxAttributes() {
        return 48;
    }

    @Override public int parent() {
        return Memory.current().buff().getShort(offset + 6);
    }

    @Override int properties() {
        return Memory.current().buff().getShort(offset + 12);
    }

    @Override public void setChild(int object) {
        super.setChild(object);
        Memory.current().buff().putShort(offset + 10, (short) object);
    }

    @Override public void setParent(int object) {
        super.setParent(object);
        Memory.current().buff().putShort(offset + 6, (short) object);
    }

    @Override public void setSibling(int object) {
        super.setSibling(object);
        Memory.current().buff().putShort(offset + 8, (short) object);
    }

    @Override public int sibling() {
        return Memory.current().buff().getShort(offset + 8);
    }
}
