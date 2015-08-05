package uk.addie.xyzzy.opcodes;

import java.io.Serializable;

import uk.addie.xyzzy.interfaces.IInvokeable;
import uk.addie.xyzzy.state.Memory;

class StackStore implements IInvokeable, Serializable {
  StackStore(final int destination) {
    this.destination = destination;
  }

  private final int destination;

  @Override public void invoke() {
    Opcode.storeValue(destination, Memory.current().callStack.peek().pop());
  }

  private static final long serialVersionUID = 1L;
}
