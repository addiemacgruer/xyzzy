
package uk.addie.xyzzy.opcodes;

import java.io.Serializable;

import uk.addie.xyzzy.interfaces.IInvokeable;
import uk.addie.xyzzy.state.Memory;

class StackDiscard implements IInvokeable, Serializable {
    private static final long serialVersionUID = 1L;

    @Override public void invoke() {
        Memory.current().callStack.peek().pop(); // throw away result.
    }
}
