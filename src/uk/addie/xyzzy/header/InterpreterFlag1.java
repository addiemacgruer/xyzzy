package uk.addie.xyzzy.header;

public class InterpreterFlag1 {
  public final static short CONFIG_BOLDFACE = 0x04; /* Interpr supports boldface style - V4+ */

  public final static byte CONFIG_BYTE_SWAPPED = 0x01; /* Story file is byte swapped - V3 */

  public final static byte CONFIG_COLOUR = 0x01; /* Interpr supports colour - V5+ */

  public final static short CONFIG_EMPHASIS = 0x08; /* Interpr supports emphasis style - V4+ */

  public final static short CONFIG_FIXED = 0x10; /* Interpr supports fixed width style - V4+ */

  public final static byte CONFIG_NOSTATUSLINE = 0x10; /* Interpr can't support status lines - V3 */

  public final static short CONFIG_PICTURES = 0x02; /* Interpr supports pictures - V6 */

  public final static byte CONFIG_PROPORTIONAL = 0x40; /* Interpr uses proportional font - V3 */

  public final static short CONFIG_SOUND = 0x20; /* Interpr supports sound - V6 */

  public final static byte CONFIG_SPLITSCREEN = 0x20; /* Interpr supports split screen mode - V3 */

  public final static byte CONFIG_TANDY = 0x08; /* Tandy licensed game - V3 */

  public final static byte CONFIG_TIME = 0x02; /* Status line displays time - V3 */

  public final static short CONFIG_TIMEDINPUT = 0x80; /* Interpr supports timed input - V4+ */

  public final static byte CONFIG_TWODISKS = 0x04; /* Story file occupied two disks - V3 */
}
