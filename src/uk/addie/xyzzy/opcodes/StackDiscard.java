
package uk.addie.xyzzy.opcodes;

import java.io.Serializable;

import uk.addie.xyzzy.Invokeable;
import uk.addie.xyzzy.state.Memory;

public class StackDiscard implements Invokeable, Serializable {
    private static final long serialVersionUID = 1L;

    @Override public void invoke() {
        Memory.current().callStack.peek().pop(); // throw away result.
    }
}
