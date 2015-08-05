package uk.addie.xyzzy.error;

import uk.addie.xyzzy.MainActivity;
import uk.addie.xyzzy.preferences.Preferences;
import uk.addie.xyzzy.state.Memory;
import uk.addie.xyzzy.zmachine.Decoder;
import uk.addie.xyzzy.zobjects.ZWindow;
import android.util.Log;

public enum Error {/* Error codes */
  BYTE_SWAPPED_STORY_FILE("Byte swapped story file", true), /* Text buffer overflow */
  CALL_NON_RTN("Call to non-routine", true), /* Store out of dynamic memory */
  DIV_ZERO("Division by zero", true), /* Illegal object */
  GET_CHILD_0("@get_child called with object 0"), /* Illegal attribute */
  GET_PARENT_0("@get_parent called with object 0"), /* Stack overflow */
  GET_PROP_0("@get_prop called with object 0"), /* Call to illegal address */
  GET_PROP_ADDR_0("@get_prop_addr called with object 0"), /* Call to non-routine */
  GET_SIBLING_0("@get_sibling called with object 0"), /* Stack underflow */
  ILL_JUMP_ADDR("Jump to illegal address", true), /* Jump to illegal address */
  ILL_OBJ("Illegal object", true), /* Can't save while in interrupt */
  ILL_OPCODE("Illegal opcode", true), /* Nesting stream #3 too deep */
  JIN_0("@jin called with object 0"), //
  MAKING_OBJECT_OWN_CHILD("Making object its own child.", true), //
  MAKING_OBJECT_OWN_PARENT("Making object its own parent", true), /* @jin called with object 0 */
  MAKING_OBJECT_OWN_SIBLING("Making object its own sibling", true), /* @get_child called with object
                                                                     * 0 */
  OBJECT_ZERO(
      "Called object zero, which is a programming mistake.  Substituting object one, and hoping for the best"), /* @
                                                                                                                 * get_prop
                                                                                                                 * called
                                                                                                                 * with
                                                                                                                 * object
                                                                                                                 * 0 */
  PUT_PROP_0("@put_prop called with object 0"), /* @put_prop called with object 0 */
  STK_UNDF("Stack underflow", true), /* @remove_object called with object 0 */
  STR3_NESTING("Nesting stream #3 too deep", true), //
  TEST_ATTR_0("@test_attr called with object 0"), //
  UNKNOWN_ZCODE_VERSION("Unknown Z-code version", true),
  PRINT_OBJECT_0("Tried to print object zero"),
  ATTRIBUTE_TOO_HIGH("Tried to set an attribute higher than the maximum allowed"); /* @get_next_prop
                                                                                    * called with
                                                                                    * object 0 */
  Error(final String string) {
    this.string = string;
    fatal = false;
  }

  Error(final String string, final boolean fatal) {
    this.string = string;
    this.fatal = fatal;
  }

  public final boolean fatal;

  public final String string;

  public void invoke() {
    if (Decoder.isShuttingDown()) {
      return;
    }
    Log.e("Xyzzy", string);
    if (fatal || (Boolean) Preferences.REPORT_MINOR.getValue(MainActivity.activity)) {
      Memory.streams().append((fatal ? "*** FATAL: " : "*** ERROR: ") + string + " ***");
    }
    if (fatal) {
      ZWindow.printAllScreens();
      throw new XyzzyException(string);
    }
  }

  @Override public String toString() {
    return string;
  }
}
