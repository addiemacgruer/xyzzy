
package uk.addie.xyzzy.error;

import uk.addie.xyzzy.state.Memory;
import uk.addie.xyzzy.zmachine.Decoder;
import uk.addie.xyzzy.zobjects.ZWindow;
import android.util.Log;

public enum Error {/* Error codes */
    /* Fatal errors */
    RUNTIME_FAILURE("Interpreter runtime failure", true), /* failure of the runtime */
    TEXT_BUF_OVF("Text buffer overflow", true), /* Text buffer overflow */
    STORE_RANGE("Store out of dynamic memory", true), /* Store out of dynamic memory */
    DIV_ZERO("Division by zero", true), /* Division by zero */
    ILL_OBJ("Illegal object", true), /* Illegal object */
    ILL_ATTR("Illegal attribute", true), /* Illegal attribute */
    NO_PROP("No such property", true), /* No such property */
    STK_OVF("Stack overflow", true), /* Stack overflow */
    ILL_CALL_ADDR("Call to illegal address", true), /* Call to illegal address */
    CALL_NON_RTN("Call to non-routine", true), /* Call to non-routine */
    STK_UNDF("Stack underflow", true), /* Stack underflow */
    ILL_OPCODE("Illegal opcode", true), /* Illegal opcode */
    BAD_FRAME("Bad stack frame", true), /* Bad stack frame */
    ILL_JUMP_ADDR("Jump to illegal address", true), /* Jump to illegal address */
    SAVE_IN_INTER("Can't save while in interrupt", true), /* Can't save while in interrupt */
    STR3_NESTING("Nesting stream #3 too deep", true), /* Nesting stream #3 too deep */
    ILL_WIN("Illegal window", true), /* Illegal window */
    ILL_WIN_PROP("Illegal window property", true), /* Illegal window property */
    ILL_PRINT_ADDR("Print at illegal address", true), /* Print at illegal address */
    /* Less serious errors */
    JIN_0("@jin called with object 0"), /* @jin called with object 0 */
    GET_CHILD_0("@get_child called with object 0"), /* @get_child called with object 0 */
    GET_PARENT_0("@get_parent called with object 0"), /* @get_parent called with object 0 */
    GET_SIBLING_0("@get_sibling called with object 0"), /* @get_sibling called with object 0 */
    GET_PROP_ADDR_0("@get_prop_addr called with object 0"), /* @get_prop_addr called with object 0 */
    GET_PROP_0("@get_prop called with object 0"), /* @get_prop called with object 0 */
    PUT_PROP_0("@put_prop called with object 0"), /* @put_prop called with object 0 */
    CLEAR_ATTR_0("@clear_attr called with object 0"), /* @clear_attr called with object 0 */
    SET_ATTR_0("@set_attr called with object 0"), /* @set_attr called with object 0 */
    TEST_ATTR_0("@test_attr called with object 0"), /* @test_attr called with object 0 */
    MOVE_OBJECT_0("@move_object called moving object 0"), /* @move_object called moving object 0 */
    MOVE_OBJECT_TO_0("@move_object called moving into object 0"), /* @move_object called moving into object 0 */
    REMOVE_OBJECT_0("@remove_object called with object 0"), /* @remove_object called with object 0 */
    GET_NEXT_PROP_0("@get_next_prop called with object 0"), //
    MAKING_OBJECT_OWN_CHILD("Making object its own child.", true), //
    MAKING_OBJECT_OWN_PARENT("Making object its own parent", true), //
    MAKING_OBJECT_OWN_SIBLING("Making object its own sibling", true), //
    OBJECT_ZERO("Called object zero.  Substituting object one, and hoping for the best"); /* @get_next_prop called with object 0 */
    public final String  string;
    public final boolean fatal;
    public boolean       seenBefore = false;

    Error(String string) {
        this.string = string;
        this.fatal = false;
    }

    Error(String string, boolean fatal) {
        this.string = string;
        this.fatal = fatal;
    }

    public void invoke() {
        if (Decoder.isShuttingDown()) {
            return;
        }
        Log.e("Xyzzy", string);
        if (!seenBefore) {
            Memory.currentScreen().append((fatal ? "*** FATAL: " : "*** ERROR: ") + string + " ***");
        }
        seenBefore = true;
        if (fatal) {
            ZWindow.printAllScreens();
            throw new XyzzyException(string);
        }
    }

    @Override public String toString() {
        return string;
    }
}
