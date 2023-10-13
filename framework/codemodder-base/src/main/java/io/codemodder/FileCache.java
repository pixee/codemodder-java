package io.codemodder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A cache for file contents. This is useful for caching file contents when running codemods that
 */
public interface FileCache {

  /** Get the string contents of a file. */
  String get(Path path) throws IOException;

  /** Put the string contents of a file into the cache. */
  void put(Path path, String contents);

  static FileCache createDefault() {
    return new FileCache() {
      private final Map<Path, String> fileCache = new ConcurrentHashMap<>();

      @Override
      public String get(final Path path) throws IOException {
        String contents = fileCache.get(path);
        if (contents == null) {
          contents = Files.readString(path);
          if (fileCache.size() < 5000) {
            fileCache.put(path, contents);
          }
        }
        return contents;
      }

      @Override
      public void put(final Path path, final String contents) {
        fileCache.put(path, contents);
      }
    };
  }
}
