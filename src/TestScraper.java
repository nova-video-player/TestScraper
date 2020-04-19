import java.io.*;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// note that ideone does not want public here
public class TestScraper {

    private final static boolean DBG = true;

    public static void main(String[] args) throws Exception {
        // for stdin
        //BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        // for file input
        FileReader inputFileReader = new FileReader("input.txt");
        BufferedReader reader = new BufferedReader(inputFileReader);
        String test;
        boolean isResultOk;
        while ((test = reader.readLine()) != null) {
            println("* Processing: %s", test);
            println("--> TvShowMatcher");
            isResultOk = TvShowMatcher(test);
            if (!isResultOk) {
                println("--> TvShowFolderMatcher");
                isResultOk = TvShowFolderMatcher(test);
            }
            if (!isResultOk) {
                println("--> TvShowPathMatcher");
                isResultOk = TvShowFolderMatcher(test);
            }
            if (!isResultOk) {
                println("--> MoviePathMatcher");
                isResultOk = MoviePathMatcher(test);
            }
            if (!isResultOk) {
                println("--> MovieDefaultMatcher");
                MovieDefaultMatcher(test);
            }
            println("_________________________________________________________________________________");
        }
    }

    // TvShowPathMatcher.java

    // pattern that allows
    // "stuff / [words/numbers] / Season XX / random stuff Episode XX random stuff"
    //           ^ show title            ^ season                  ^ episode
    // e.g. "/series/Galactica/Season 1/galactica.ep3.avi"

    private static final Pattern SHOW_SEASON_EPISODE_PATH_PATTERN =
            Pattern.compile("(?i).*/((?:[\\p{L}\\p{N}]++[\\s._-]*+)++)/[^/]*?(?<![\\p{L}])(?:S|SEAS|SEASON)[\\s._-]*+(\\d{1,2})(?!\\d)[^/]*+/[^/]*?(?<![\\p{L}])(?:E|EP|EPISODE)[\\s._-]*+(\\d{1,2})(?!\\d)[^/]*+");

    /**
     * Matches Tv Shows in folders like
     * <code>/Galactica/Season 1/galactica.ep3.avi</code><p>
     * see <a href="http://wiki.xbmc.org/index.php?title=Video_library/Naming_files/TV_shows">XBMC Docu</a>
     * <p>
     * Name may only contain Alphanumerics separated by spaces
     */
    private static boolean TvShowPathMatcher(String input) {
        if (DBG) println("input            : " + input);
        Matcher matcher = SHOW_SEASON_EPISODE_PATH_PATTERN.matcher(input);
        if (matcher.matches()) {
            String showName = removeInnerAndOutterSeparatorJunk(matcher.group(1));
            int season = parseInt(matcher.group(2), 0);
            int episode = parseInt(matcher.group(3), 0);
            println("RESULT           : show %s season:%s, episode:%s", showName, season, episode);
            return true;
        } else {
            if (DBG) println("NO RESULT!");
            return false;
        }
    }

    // TvShowFolderMatcher.java

    /**
     * Matches all sorts of "Tv Show title S01E01/randomgarbage.mkv" and similar things
     */
    private static boolean TvShowFolderMatcher(String input) {
        if (DBG) println("+++++ input            : " + input);
        input = removeLastSegment(input);
        if (DBG) println("+++++ fileNoPath       : " + input);
        if (isTvShow(input)) {
            Map<String, String> showName = parseShowName(input);
            if (showName != null) {
                String showTitle = showName.get(SHOW);
                String season = showName.get(SEASON);
                String episode = showName.get(EPNUM);
                int seasonInt = parseInt(season, 0);
                int episodeInt = parseInt(episode, 0);
                println("==== RESULT           : show %s season:%s, episode:%s",showTitle, seasonInt, episodeInt);
                return true;
            } else {
                if (DBG) println("==== NO RESULT (but tvShow...)!");
                return false;
            }
        } else
            if (DBG) println("==== NO RESULT (no tvShow)!");
        return false;
    }

    // TvShowMatcher.java

    /**
     * Matches all sorts of "Tv Show title S01E01" and similar things
     */
    private static boolean TvShowMatcher(String input) {
        if (DBG) println("+++++ input            : " + input);
        input = getFileNameWithoutExtension(input);
        if (DBG) println("+++++ fileNoExt        : " + input);
        if (isTvShow(input)) {
            Map<String, String> showName = parseShowName(input);
            if (showName != null) {
                String showTitle = showName.get(SHOW);
                String season = showName.get(SEASON);
                String episode = showName.get(EPNUM);
                int seasonInt = parseInt(season, 0);
                int episodeInt = parseInt(episode, 0);
                println("==== RESULT           : show %s season:%s, episode:%s",showTitle, seasonInt, episodeInt);
                return true;
            } else {
                if (DBG) println("==== NO RESULT (but tvShow...)!");
                return false;
            }
        } else
        if (DBG) println("==== NO RESULT (no tvShow)!");
        return false;
    }

    // ShowUtils.java

    public static final String EPNUM = "epnum";
    public static final String SEASON = "season";
    public static final String SHOW = "show";

    /** These are considered equivalent to space */
    public static final char[] REPLACE_ME = new char[] {
            '.', // "The.Good.Wife" will not be found
            '_', // "The_Good_Wife" would be found but we need the clean name for local display
    };

    // Separators: Punctuation or Whitespace
    private static final String SEP_OPTIONAL = "[\\p{Punct}\\s]*+";
    private static final String SEP_MANDATORY = "[\\p{Punct}\\s]++";

    // Name patterns where the show is present first. Examples below.
    private static final Pattern[] patternsShowFirst = {
            // almost anything that has S 00 E 00 in it
            Pattern.compile("(.+?)" + SEP_MANDATORY + "(?:s|seas|season)" + SEP_OPTIONAL + "(20\\d{2}|\\d{1,2})" + SEP_OPTIONAL + "(?:e|ep|episode)" + SEP_OPTIONAL + "(\\d{1,3})(?!\\d).*", Pattern.CASE_INSENSITIVE),
            // almost anything that has 00 x 00
            Pattern.compile("(.+?)" + SEP_MANDATORY + "(20\\d{2}|\\d{1,2})" + SEP_OPTIONAL + "x" + SEP_MANDATORY + "(\\d{1,3})(?!\\d).*", Pattern.CASE_INSENSITIVE),
            // special case to avoid x264 or x265
            Pattern.compile("(.+?)" + SEP_MANDATORY + "(20\\d{2}|\\d{1,2})" + SEP_OPTIONAL + "x" + SEP_OPTIONAL + "(?!(?:264|265|720))(\\d{1,3})(?!\\d).*", Pattern.CASE_INSENSITIVE),
            // foo.103 and similar
            // Note: can detect movies that contain 3 digit numbers like "127 hours" or shows that have such numbers in their name like "zoey 101"
            // Limit first digit to be >0 in order not to identify "James Bond 007" as tv show
            Pattern.compile("(.+)" + SEP_MANDATORY + "(?!(?:264|265|720))([1-9])(\\d{2,2})" + SEP_MANDATORY + ".*", Pattern.CASE_INSENSITIVE),
    };
    // Name patterns which begin with the number of the episode
    private static final Pattern[] patternsEpisodeFirst = {
            // anything that starts with S 00 E 00, text after "-" getting ignored
            Pattern.compile(SEP_OPTIONAL + "(?:s|seas|season)" + SEP_OPTIONAL + "(\\d{1,2})" + SEP_OPTIONAL + "(?:e|ep|episode)" + SEP_OPTIONAL + "(\\d{1,3})(?!\\d)" + SEP_OPTIONAL + "([^-]*+).*", Pattern.CASE_INSENSITIVE),
            // anything that starts with 00 x 00, text after "-" getting ignored like in "S01E15 - ShowName - Ignored - still ignored"
            Pattern.compile(SEP_OPTIONAL + "(\\d{1,2})" + SEP_OPTIONAL + "x" + SEP_OPTIONAL + "(\\d{1,3})(?!\\d)" + SEP_OPTIONAL + "([^-]*+).*", Pattern.CASE_INSENSITIVE),
    };

    private static String cleanUpName(String name) {
        name = unifyApostrophes(name);
        name = removeNumbering(name);
        name = replaceAcronyms(name);
        name = replaceAllChars(name, REPLACE_ME, ' ');
        name = name.trim();
        if (DBG) println("+++++ cleanUpName      : " + name);
        return name;
    }

    /**
     *  Parse the filename and returns a Map containing the keys "show",
     *  "season" and "epnum" and their associated values.
     *  If the filename doesn't match a tv show pattern, returns null.
     */
    public static Map<String, String> parseShowName(String filename) {
        final HashMap<String, String> buffer = new HashMap<String, String>();
        for(Pattern regexp: patternsShowFirst) {
            Matcher matcher = regexp.matcher(filename);
            try {
                if(matcher.find()) {
                    buffer.put(SHOW, cleanUpName(matcher.group(1)));
                    buffer.put(SEASON, matcher.group(2));
                    buffer.put(EPNUM, matcher.group(3));
                    return buffer;
                }
            } catch (IllegalArgumentException ignored) {}
        }
        for(Pattern regexp: patternsEpisodeFirst) {
            Matcher matcher = regexp.matcher(filename);
            try {
                if(matcher.find()) {
                    buffer.put(SHOW, cleanUpName(matcher.group(3)));
                    buffer.put(SEASON, matcher.group(1));
                    buffer.put(EPNUM, matcher.group(2));
                    return buffer;
                }
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    /**
     * Function to know if a given filename matches one of the patterns and as
     * such, can have show and episode number extracted.
     * Uses <b>searchString</b> instead of filename if present.
     */
    public static boolean isTvShow(String file, String searchString) {
        String filename;
        if (searchString != null && !searchString.isEmpty()) {
            filename = searchString;
        } else {
            if (file == null)
                return false;
            filename = getName(file);
        }
        for(Pattern regexp: patternsShowFirst) {
            Matcher m = regexp.matcher(filename);
            try {
                if(m.matches())
                    return true;
            } catch (IllegalArgumentException ignored) {}
        }
        for(Pattern regexp: patternsEpisodeFirst) {
            Matcher m = regexp.matcher(filename);
            try {
                if(m.matches())
                    return true;
            } catch (IllegalArgumentException ignored) {}
        }
        return false;
    }

    public static boolean isTvShow(String path) {
        return isTvShow(path, null);
    }

    public static String urlEncode(String input) {
        String encode = "";
        try {
            encode = URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            if (DBG) println("!!!!! urlEncode: caught UnsupportedEncodingException");
        }
        return encode;
    }

    // MoviePathMatcher.java

    /**
     * Matches Movies in folders like
     * <code>/Moviename (1996)/movie.avi</code>
     * <p>
     * see <a href="http://wiki.xbmc.org/index.php?title=Video_library/Naming_files/Movies">XBMC Docu</a>
     * <p>
     * may only contain text numbers and spaces followed by the year in brackets
     * everything after those brackets is ignored. The actual filename is also ignored.
     */
    // rather strict pattern that allows
    // "stuff / [space separated words/numbers] (year) random stuff / random stuff"
    // e.g. "/movies/Titanic (2001)/movie.avi"
    //      "/movies/Transformers (2009) [720p]/lala.mkv"
    //      "/movies/Th3 L33tspe4k (2009) [720p]/lala.mkv"
    // does not match
    //      "/movies/Transformers.2001/movie.avi"
    // will unfortunately match -> catch that before
    //      "/series/The A Team S01E02 (1978)/lala.avi

    private static final Pattern MOVIE_YEAR_PATH_PATTERN = Pattern.compile(".*/((?:[\\p{L}\\p{N}]++\\s*+)++)\\(((?:19|20)\\d{2})\\)[^/]*+/[^/]++");

    private static boolean MoviePathMatcher(String input) {
        // TODO MARC: seems not to work on examples!!!
        if (DBG) println("+++++ input            : " + input);
        Matcher matcher = MOVIE_YEAR_PATH_PATTERN.matcher(input);
        if (matcher.matches()) {
            String name = removeInnerAndOutterSeparatorJunk(matcher.group(1));
            String year = matcher.group(2);
            println("==== RESULT           : %s year:%s", name, year);
            return true;
        } else {
            if (DBG) println("==== NO RESULT!");
            return false;
        }
    }

    // MovieDefaultMatcher.java

    /**
     * Matches everything. Tries to strip away all junk, not very reliable.
     * <p>
     * Process is as follows:
     * <ul>
     * <li> Start with filename without extension: "100. [DVD]Starship_Troopers_1995.-HDrip--IT"
     * <li> Remove potential starting numbering of collections "[DVD]Starship_Troopers_1995.-HDrip--IT"
     * <li> Extract last year if any: "[DVD]Starship_Troopers_.-HDrip--IT"
     * <li> Remove anything in brackets: "Starship_Troopers_.-HDrip--IT"
     * <li> Assume from here on that the title is first followed by junk
     * <li> Trim CasE sensitive junk: "Starship_Troopers_.-HDrip" ("it" could be part of the movie name, "IT" probably not)
     * <li> Remove separators: "Starship Troopers HDrip"
     * <li> Trim junk case insensitive: "Starship Troopers"
     * </ul>
     */
    private static void MovieDefaultMatcher(String input) {
        // TODO test 3rd party denoise pattern
        // denoise filter Default = @"(([\(\{\[]|\b)((576|720|1080)[pi]|dir(ectors )?cut|dvd([r59]|rip|scr(eener)?)|(avc)?hd|wmv|ntsc|pal|mpeg|dsr|r[1-5]|bd[59]|dts|ac3|blu(-)?ray|[hp]dtv|stv|hddvd|xvid|divx|x264|dxva|(?-i)FEST[Ii]VAL|L[iI]M[iI]TED|[WF]S|PROPER|REPACK|RER[Ii]P|REAL|RETA[Ii]L|EXTENDED|REMASTERED|UNRATED|CHRONO|THEATR[Ii]CAL|DC|SE|UNCUT|[Ii]NTERNAL|[DS]UBBED)([\]\)\}]|\b)(-[^\s]+$)?)")]
        String name = input;
        if (DBG) println("+++++ input            : " + name);
        name = getFileNameWithoutExtension(name);
        if (DBG) println("+++++ fileNoExt        : " + name);
        // extract the last year from the string
        String year = null;
        // matches "[space or punctuation/brackets etc]year", year is group 1
        Matcher matcher = YEAR_PATTERN.matcher(name);
        int start = 0;
        int stop = 0;
        boolean found = false;
        while (matcher.find()) {
            found = true;
            start = matcher.start(1);
            stop = matcher.end(1);
        }
        // get the last match and extract it from the string
        if (found) {
            year = name.substring(start, stop);
            name = name.substring(0, start) + name.substring(stop);
        }
        if (DBG) println("+++++ release year     : %s year:%s", name, year);

        // Strip out starting numbering for collections
        name = removeNumbering(name);

        // Strip out everything else in brackets <[{( .. )})>, most of the time teams names, etc
        name = replaceAll(name, "", BRACKETS);
        if (DBG) println("+++++ brackets         : " + name);

        // strip away known case sensitive garbage
        name = cutOffBeforeFirstMatch(name, GARBAGE_CASESENSITIVE_PATTERNS);

        // replace all remaining whitespace & punctuation with a single space
        name = removeInnerAndOutterSeparatorJunk(name);

        // append a " " to aid next step
        // > "Foo bar 1080p AC3 " to find e.g. " AC3 "
        name = name + " ";

        // try to remove more garbage, this time " garbage " syntax
        // method will compare with lowercase name automatically
        name = cutOffBeforeFirstMatch(name, GARBAGE_LOWERCASE);

        name = name.trim();
        println("==== RESULT           : %s year:%s", name, year);
    }

    // Most of the common garbage in movies name we want to strip out
    // (they can be part of the name or correspond to extensions as well).
    private static final String[] GARBAGE_LOWERCASE = {
            " dvdrip ", " dvd rip ", "dvdscreener ", " dvdscr ", " dvd scr ",
            " brrip ", " br rip ", " bdrip", " bd rip ", " blu ray ", " bluray ",
            " hddvd ", " hd dvd ", " hdrip ", " hd rip ", " hdlight ", " minibdrip ",
            " webrip ", " web rip ",
            " 720p ", " 1080p ", " 1080i ", " 720 ", " 1080 ", " 480i ", " 2160p ", " 4k ", " 480p ", " 576p ", " 576i ", " 240p ", " 360p ", " 4320p ", " 8k ",
            " hdtv ", " sdtv ", " m hd ", " ultrahd ", " mhd ",
            " h264 ", " x264 ", " aac ", " ac3 ", " ogm ", " dts ", " hevc ", " x265 ", " av1 ",
            " avi ", " mkv ", " xvid ", " divx ", " wmv ", " mpg ", " mpeg ", " flv ", " f4v ",
            " asf ", " vob ", " mp4 ", " mov ",
            " directors cut ", " dircut ", " readnfo ", " read nfo ", " repack ", " rerip ", " multi ", " remastered ",
            " truefrench ", " srt ", " extended cut ",
            " sbs ", " hsbs ", " side by side ", " sidebyside ", /* Side-By-Side 3d stuff */
            " 3d ", " h sbs ", " h tb ", " tb ", " htb ", " top bot ", " topbot ", " top bottom ", " topbottom ", " tab ", " htab ", /* Top-Bottom 3d stuff */
            " anaglyph ", " anaglyphe ", /* Anaglyph 3d stuff */
            " truehd ", " atmos ", " uhd ", " hdr10+ ", " hdr10 ", " hdr ", " dolby ", " dts-x ", " dts-hd.ma ",
            " hfr ",
    };
    // stuff that could be present in real names is matched with tight case sensitive syntax
    // strings here will only match if separated by any of " .-_"
    private static final String[] GARBAGE_CASESENSITIVE = {
            "FRENCH", "TRUEFRENCH", "DUAL", "MULTISUBS", "MULTI", "MULTi", "SUBFORCED", "SUBFORCES", "UNRATED", "UNRATED[ ._-]DC", "EXTENDED", "IMAX",
            "COMPLETE", "PROPER", "iNTERNAL", "INTERNAL",
            "SUBBED", "ANiME", "LIMITED", "REMUX", "DCPRip",
            "TS", "TC", "REAL", "HD", "DDR", "WEB",
            "EN", "ENG", "FR", "ES", "IT", "NL", "VFQ", "VF", "VO", "VOSTFR", "Eng",
            "VOST", "VFF", "VF2", "VFI", "VFSTFR",
    };

    private static final Pattern YEAR_PATTERN = Pattern.compile("[\\s\\p{Punct}]((?:19|20)\\d{2})(?!\\d)");

    // ParseUtils.java

    // Strip out starting numbering for collections
    // Matches "1. ", "1) ", "1 - ", "1.-.", "1._"... but not "1.Foo" or "1-Foo" ..
    // i.e. "13.Years.Of.School"
    private static final Pattern LEADING_NUMBERING = Pattern.compile("^(\\d+([.)][\\s\\p{Punct}]+|\\s+\\p{Punct}[\\p{Punct}\\s]*))*");

    // Strip out everything else in brackets <[{( .. )})>, most of the time teams names, etc
    private static final Pattern BRACKETS = Pattern.compile("[<({\\[].+?[>)}\\]]");
    private static final Pattern BRACKETS2 = Pattern.compile("[<(\\[{].+?[>)\\]}]");

    // replaces alternative apostrophes with a simple '
    // besides the plain ' there is the typographic ’ and ‘ which is actually not an apostrophe
    private static final char[] ALTERNATE_APOSTROPHES = new char[]{'’', '‘'};

    // Matches dots in between Uppercase letters e.g. in "E.T.", "S.H.I.E.L.D." but not a "a.b.c."
    // replaces "S.H.I.E.L.D." with "SHIELD", only uppercase letters
    // Last dot is kept "a.F.O.O.is.foo" => "a.FOO.is.foo"
    private static final Pattern ACRONYM_DOTS = Pattern.compile("(?<=(\\b|[._])\\p{Lu})[.](?=\\p{Lu}([.]|$))");

    // ( whitespace | punctuation)+, matches dots, spaces, brackets etc
    private static final Pattern MULTI_NON_CHARACTER_PATTERN = Pattern.compile("[\\s\\p{Punct}&&[^']]+");

    // Removes leading numbering like "1. A Movie" => "A Movie",
    // does not replace numbers if they are not separated like in
    // "13.Years.Of.School"
    public static String removeNumbering(String input) {
        String result = replaceAll(input, "", LEADING_NUMBERING);
        if (DBG) println("+++++ remove numbering : " + result);
        return result;
    }

    /** replaces "S.H.I.E.L.D." with "SHIELD", only uppercase letters */
    public static String replaceAcronyms(String input) {
        String result = replaceAll(input, "", ACRONYM_DOTS);
        if (DBG) println("+++++ acronyms         : " + result);
        return result;
    }

    /** replaces alternative apostrophes with a simple ' */
    public static String unifyApostrophes(String input) {
        String result = replaceAllChars(input, ALTERNATE_APOSTROPHES, '\'');
        if (DBG) println("+++++ apostrophes      : " + result);
        return result;
    }

    /** removes all punctuation characters besides ' Also does apostrophe and Acronym replacement */
    public static String removeInnerAndOutterSeparatorJunk(String input) {
        // replace ’ and ‘ by ' - both could be used as apostrophes
        String result = unifyApostrophes(input);
        result = replaceAcronyms(result);
        result = replaceAll(result, " ", MULTI_NON_CHARACTER_PATTERN).trim();
        if (DBG) println("+++++ separators       : " + result);
        return result;
    }

    private static final Pattern[] GARBAGE_CASESENSITIVE_PATTERNS = new Pattern[GARBAGE_CASESENSITIVE.length];

    static {
        for (int i = 0; i < GARBAGE_CASESENSITIVE.length; i++) {
            // case sensitive string wrapped in "space or . or _ or -", in the end either separator or end of line
            // end of line is important since .foo.bar. could be stripped to .foo and that would no longer match .foo.
            GARBAGE_CASESENSITIVE_PATTERNS[i] = Pattern.compile("[ ._-]" + GARBAGE_CASESENSITIVE[i] + "(?:[ ._-]|$)");
        }
    }

    // ( whitespace | punctuation), matches dots, spaces, brackets etc
    private static final String NON_CHARACTER = "[\\s\\p{Punct}]";

    // matches "19XX and 20XX" - capture group
    private static final String YEAR_GROUP = "((?:19|20)\\d{2})";

    /**
     * assumes title is always first
     * @return substring from start to first finding of any garbage pattern
     */
    private static String cutOffBeforeFirstMatch(String input, Pattern[] patterns) {
        String remaining = input;
        for (Pattern pattern : patterns) {
            if (remaining.isEmpty()) return "";

            Matcher matcher = pattern.matcher(remaining);
            if (matcher.find()) {
                remaining = remaining.substring(0, matcher.start());
            }
        }
        if (DBG) println("+++++ CaSe junk        : " + remaining);
        return remaining;
    }

    /**
     * assumes title is always first
     * @param garbageStrings lower case strings
     * @return substring from start to first finding of any garbage string
     */
    public static String cutOffBeforeFirstMatch(String input, String[] garbageStrings) {
        // lower case input to test against lowercase strings
        String inputLowerCased = input.toLowerCase(Locale.US);
        int firstGarbage = input.length();
        for (String garbage : garbageStrings) {
            int garbageIndex = inputLowerCased.indexOf(garbage);
            // if found, shrink to 0..index
            if (garbageIndex > -1 && garbageIndex < firstGarbage)
                firstGarbage = garbageIndex;
        }
        // return substring from input -> keep case
        String result = input.substring(0, firstGarbage);
        if (DBG) println("+++++ lowercase junk   : " + result);
        return result;
    }

    public static String replaceAllChars(String input, char[] badChars, char newChar) {
        if (badChars == null || badChars.length == 0)
            return input;
        int inputLength = input.length();
        int replacementLenght = badChars.length;
        boolean modified = false;
        char[] buffer = new char[inputLength];
        input.getChars(0, inputLength, buffer, 0);
        for (int inputIdx = 0; inputIdx < inputLength; inputIdx++) {
            char current = buffer[inputIdx];
            for (int replacementIdx = 0; replacementIdx < replacementLenght; replacementIdx++) {
                if (current == badChars[replacementIdx]) {
                    buffer[inputIdx] = newChar;
                    modified = true;
                    break;
                }
            }
        }
        return modified ? new String(buffer) : input;
    }

    public static String replaceAll(String input, String replacement, Pattern pattern) {
        return pattern.matcher(input).replaceAll(replacement);
    }

    private static void println() {
        System.out.println();
    }

    private static void println(String in) {
        System.out.println(in);
    }

    private static void println(String in, Object... args) {
        System.out.println(String.format(in, args));
    }

    private static String getFileNameWithoutExtension(String input) {
        File file = new File(input);
        String name = file.getName();
        if (name != null) {
            int dotPos = name.lastIndexOf('.');
            if (dotPos > 0) {
                name = name.substring(0, dotPos);
            }
        }
        return name;
    }

    // StringUtils.java

    /** tries to parse String > int, errorValue if something prevents parsing (null String / wrong format) */
    public static int parseInt(String string, int errorValue) {
        if (string != null) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException e) {
                // error
            }
        }
        return errorValue;
    }

    // FileUtils.java

    final static String SEPARATOR = "/";

    public static String getName(String file) {
        return file.substring(file.lastIndexOf(SEPARATOR) + 1);
    }

    public static String removeLastSegment(String file){
        int index;
        if (file.endsWith(SEPARATOR))
            index = file.lastIndexOf(SEPARATOR, file.length()-2);
        else index = file.lastIndexOf(SEPARATOR);
        if (index<=0) return null;
        return file.substring(0, index + 1);
    }
}
