package au.edu.rmit.tzar.parser.beans;

/**
 * Specifies different levels of caching aggressiveness for library downloads.
 */
public enum DownloadMode {
  /**
   * Download every time, but respect http cache headers, and cache locally
   */
  CACHE,
  /**
   * Always download, regardless of cache headers
   */
  FORCE,
  /**
   * Check if the library exists locally, and if it does, don't download
   */
  ONCE
}
