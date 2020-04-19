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
  
If you experience any issue with NOVA scraper, please use this simple program to propose enhancements via pull request not breaking the current examples.
