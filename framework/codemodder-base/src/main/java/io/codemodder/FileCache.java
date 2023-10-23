package io.codemodder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A cache for file contents. We cache contents because generally memory is cheap and fast. We may
 * want to offer configuration here eventually for situations where the opposite is true.
 */
public interface FileCache {

  /** Get the string contents of a file. */
  String get(Path path) throws IOException;

  /** Put the string contents of a file into the cache. */
  void overrideEntry(Path path, String contents);

  /** Remove the string contents of a file from the cache if it exists. */
  void removeEntry(final Path resolve);

  static FileCache createDefault() {
    return createDefault(10_000);
  }

  static FileCache createDefault(final int maxSize) {
    return new FileCache() {
      private final Map<Path, String> fileCache = new ConcurrentHashMap<>();

      @Override
      public String get(final Path path) throws IOException {
        String contents = fileCache.get(path);
        if (contents == null) {
          contents = Files.readString(path);
          if (fileCache.size() < maxSize) {
            fileCache.put(path, contents);
          }
        }
        return contents;
      }

      @Override
      public void overrideEntry(final Path path, final String contents) {
        if (!fileCache.containsKey(path)) {
          throw new IllegalArgumentException("cache entry must be for an existing key");
        }
        fileCache.put(path, contents);
      }

      @Override
      public void removeEntry(final Path path) {
        fileCache.remove(path);
      }
    };
  }
}
