package io.codemodder.plugins.maven.operator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

public class FileUtils {

  private FileUtils() {}

  private static final String TEMP_SUBFOLDER = "mySecureDirectory";

  public static Path createTempDirectoryWithPermissions() throws IOException {
    // Get the system's temporary directory
    Path systemTempDir = Paths.get(System.getProperty("java.io.tmpdir"));

    // Create a dedicated sub-folder within the temporary directory
    Path tempDirectory = Files.createTempDirectory(systemTempDir, TEMP_SUBFOLDER);

    // Set permissions (read, write, execute for owner; no permissions for others)
    Set<PosixFilePermission> perms = new HashSet<>();
    perms.add(PosixFilePermission.OWNER_READ);
    perms.add(PosixFilePermission.OWNER_WRITE);
    perms.add(PosixFilePermission.OWNER_EXECUTE);

    Files.setPosixFilePermissions(tempDirectory, perms);

    return tempDirectory;
  }
}
