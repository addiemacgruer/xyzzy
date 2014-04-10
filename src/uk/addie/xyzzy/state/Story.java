
package uk.addie.xyzzy.state;

class Story {
    enum Game {
        ARTHUR, BEYOND_ZORK, JOURNEY, LURKING_HORROR, SHERLOCK, SHOGUN, UNKNOWN, ZORK_ZERO;
    }

    static final Story[] stories = { new Story(Game.SHERLOCK, 21, "871214"), new Story(Game.SHERLOCK, 26, "880127"),
            new Story(Game.BEYOND_ZORK, 47, "870915"), new Story(Game.BEYOND_ZORK, 49, "870917"),
            new Story(Game.BEYOND_ZORK, 51, "870923"), new Story(Game.BEYOND_ZORK, 57, "871221"),
            new Story(Game.ZORK_ZERO, 296, "881019"), new Story(Game.ZORK_ZERO, 366, "890323"),
            new Story(Game.ZORK_ZERO, 383, "890602"), new Story(Game.ZORK_ZERO, 393, "890714"),
            new Story(Game.SHOGUN, 292, "890314"), new Story(Game.SHOGUN, 295, "890321"),
            new Story(Game.SHOGUN, 311, "890510"), new Story(Game.SHOGUN, 322, "890706"),
            new Story(Game.ARTHUR, 54, "890606"), new Story(Game.ARTHUR, 63, "890622"),
            new Story(Game.ARTHUR, 74, "890714"), new Story(Game.JOURNEY, 26, "890316"),
            new Story(Game.JOURNEY, 30, "890322"), new Story(Game.JOURNEY, 77, "890616"),
            new Story(Game.JOURNEY, 83, "890706"), new Story(Game.LURKING_HORROR, 203, "870506"),
            new Story(Game.LURKING_HORROR, 219, "870912"), new Story(Game.LURKING_HORROR, 221, "870918"),
            new Story(Game.UNKNOWN, 0, "------") };
    final int            release;
    final String         serial;
    final Game           story;

    private Story(final Game story, final int release, final String version) {
        this.story = story;
        this.release = release;
        serial = version;
    }
}