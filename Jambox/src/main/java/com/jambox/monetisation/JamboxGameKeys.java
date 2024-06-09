package com.jambox.monetisation;

public enum JamboxGameKeys
{
    dunk_hit("1-dunk-hit"),
    patakha_dhamaka("12-patakha-dhamaka"),
    missile_boom("13-missile-boom"),
    space_escape("14-space-escape"),
    luck_toss("15-lucky-toss"),
    hangman("16-hangman"),
    knife_flip("17-knife-flip"),
    egg_car("18-egg-car"),
    frog_jump("21-frog-jump"),
    cricket_gunda("22-cricket-gunda"),
    spiral_tower("23-spiral-tower"),
    ball_blast("24-ball-blast"),
    flip_jump("25-flip-jump"),
    rise_up("26-rise-up"),
    snakes_vs_blocks("27-snakes-vs-blocks"),
    sudoku("28-sudoku"),
    _2048("29-2048"),
    air_hockey("3-air-hockey"),
    highway_rush("31-highway-rush"),
    dart_cricket("34-dart-cricket"),
    chicken_smasher("35-chicken-smasher"),
    lor_rings_puzzle("36-color-rings-puzzle"),
    pivot_go("37-pivot-go"),
    movie_trivia("38-movie-trivia"),
    aerial_hit("39-aerial-hit"),
    focus_locus("4-focus-locus"),
    shark_riders("40-shark-riders"),
    cricket_trivia("43-cricket-trivia"),
    sling_the_kong("5-sling-the-kong"),
    knife_hit("6-knife-hit"),
    pop_the_ice("7-pop-the-ice"),
    bottle_flip("8-bottle-flip"),
    egg_me_up("9-egg-me-up");

    private String gameId;

    JamboxGameKeys(String gameId) {
        this.gameId = gameId;
    }

    public String GetGameId() {
        return gameId;
    }
}
