
package uk.addie.xyzzy.opcodes;

import java.io.Serializable;

import uk.addie.xyzzy.interfaces.IInvokeable;
import uk.addie.xyzzy.state.Memory;

class StackStore implements IInvokeable, Serializable {
    private static final long serialVersionUID = 1L;
    private final int         destination;

    StackStore(final int destination) {
        this.destination = destination;
    }

    @Override public void invoke() {
        Opcode.storeValue(destination, Memory.current().callStack.peek().pop());
    }
}
