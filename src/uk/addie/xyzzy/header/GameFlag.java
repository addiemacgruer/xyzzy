
package uk.addie.xyzzy.header;

public class GameFlag {
    public final static short COLOUR      = 0x0040; /* Game wants to use colours          - V5+ */
    public final static short FIXED_FONT  = 0x0002; /* Use fixed width font               - V3+ */
    public final static short GRAPHICS    = 0x0008; /* Game wants to use graphics         - V5+ */
    public final static short MENU        = 0x0100; /* Game wants to use menus            - V6  */
    public final static short MOUSE       = 0x0020; /* Game wants to use a mouse          - V5+ */
    public final static short REFRESH     = 0x0004; /* Refresh the screen                 - V6  */
    public final static short SCRIPTING   = 0x0001; /* Outputting to transcription file  - V1+ */
    public final static short SOUND       = 0x0080; /* Game wants to use sound effects    - V5+ */
    public final static short TRANSPARENT = 0x0001; /* Game wants to use transparency     - V6  */
    public final static short UNDO        = 0x0010; /* Game wants to use UNDO feature     - V5+ */
    public final static short V3_SOUND    = 0x0010; /* Game wants to use sound effects    - V3  */
}
