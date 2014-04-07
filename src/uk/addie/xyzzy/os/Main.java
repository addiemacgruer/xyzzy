
package uk.addie.xyzzy.os;


public class Main {
    public static short frame_count      = 0;
    /* IO streams */
    static boolean      ostream_screen   = true;
    static boolean      ostream_script   = false;
    static boolean      ostream_memory   = false;
    static boolean      ostream_record   = false;
    static boolean      istream_replay   = false;
    static boolean      message          = false;
    /* Window attributes */
    static boolean      enable_wrapping  = false;
    static boolean      enable_scripting = false;
    static boolean      enable_scrolling = false;
    static boolean      enable_buffering = false;
    /*
     * main
     *
     * Prepare and run the game.
     *
     */
    //    public static void main(final String[] args) {
    //        OS.os_process_arguments(args);
    //        Err.init_err();
    //        Process.init_process();
    //        Z.RESTART.invoke();
    //        Process.interpret();
    //    }
}
