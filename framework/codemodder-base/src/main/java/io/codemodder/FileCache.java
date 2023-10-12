package io.codemodder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface FileCache {

  String get(Path path) throws IOException;

  void put(Path path, String contents);

  static FileCache createDefault() {
    return new FileCache() {
      private final Map<Path, String> fileCache = new ConcurrentHashMap<>();

      @Override
      public String get(final Path path) throws IOException {
        String contents = fileCache.get(path);
        if (contents == null) {
          contents = Files.readString(path);
          fileCache.put(path, contents);
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
