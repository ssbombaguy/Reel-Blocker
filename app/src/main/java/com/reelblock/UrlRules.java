package com.reelblock;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Decides what to block from the text in a browser's address bar. */
final class UrlRules {
    private UrlRules() {}

    /** Matched anywhere in the text, so "pornpics.com" and "xvideos2.com" count too. */
    private static final String[] NAMES = {
        "porn", "xvideos", "xnxx", "xhamster", "redtube", "brazzers", "chaturbate",
        "stripchat", "spankbang", "onlyfans", "rule34", "hentai", "youjizz", "tube8",
        "bongacams", "livejasmin", "eporner", "fapello", "thisvid", "motherless.com",
    };

    /** Whole words only, so "sex" doesn't match "Essex". */
    private static final Set<String> WORDS = new HashSet<>(Arrays.asList(
        "sex", "sexy", "xxx", "nude", "nudes", "nsfw", "milf", "boobs", "tits",
        "blowjob", "cumshot", "gangbang"
    ));

    /** Looking for a way out shouldn't get blocked. */
    private static final Set<String> HELP_WORDS = new HashSet<>(Arrays.asList(
        "addiction", "addicted", "recovery", "recover", "quit", "quitting"
    ));

    static boolean isPorn(String url) {
        String text = url.toLowerCase(Locale.ROOT);
        String[] tokens = text.split("[^a-z0-9]+");
        for (String token : tokens) {
            if (HELP_WORDS.contains(token)) return false;
        }
        for (String name : NAMES) {
            if (text.contains(name)) return true;
        }
        for (String token : tokens) {
            if (WORDS.contains(token)) return true;
        }
        return false;
    }

    static boolean isShortVideo(String url) {
        String text = url.toLowerCase(Locale.ROOT);
        return text.contains("instagram.com/reel") || text.contains("youtube.com/shorts");
    }
}
