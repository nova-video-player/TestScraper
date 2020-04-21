# TestScraper: a simple test program to debug NOVA Scraper
TestScraper is a simple test program that will take all the filenames in `input.txt` text file and detail step by step all the processing done on these filenames to issue a cleaner string to be sent to either themoviedb or thetvdb for identification.

The in NOVA the scrape process is launched by `Scraper.java` that relies on `SearchPreprocessor.java` to perform the matching in the following order:
- first priority is for TV shows
  - `TvShowMatcher.java` for filename without path matching
  - `TvShowFolderMatcher.java` for folder name based matching
  - `TvShowPathMatcher.java` for pathname based matching
- followed by movies
  - `MovieVerbatimMatcher.java` not addressed in this debug class
  - `MovieDVDMatcher.java` not addressed in this debug class
  - `MoviePathMatcher.java` for pathname based matching 
  - `MovieSceneMatcher.java` not addressed in this debug class
- then fallback to default that matches everything
  - `MovieDefaultMatcher.java` for filename based matching
  
In TestScraper all these classes and dependencies are merged in one file with debugs.

If you experience any issue with NOVA scraper, please use this simple program to propose enhancements via pull request not breaking the current examples.

The list the non scraped video URIs can be obtained by running the following script on the exported NOVA media library following these steps:
- extract the media library from the phone
  - enter advanced settings by going to NOVA -> Preferences->click/tap 8 times on "Force software decoding"
  - trigger the database export via the "Export media database" Preference entry
  - pull the database from the host via adb: `adb pull /sdcard/org.courville.nova-media.db .`
- run the following script (requires `sqlite3` to be installed)
```
echo "SELECT _data FROM video WHERE m_id IS NULL OR s_id IS NULL" | sqlite3 org.courville.nova-media.db
```
- use the output of the script as the `input.txt file to run TestScraper