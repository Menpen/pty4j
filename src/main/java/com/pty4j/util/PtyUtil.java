package com.pty4j.util;

import com.pty4j.windows.WinPty;
import com.sun.jna.Platform;

import java.io.File;
import java.net.URI;
import java.security.CodeSource;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * @author traff
 */
public class PtyUtil {
  public static final String OS_VERSION = System.getProperty("os.version").toLowerCase();

  public static String[] toStringArray(Map<String, String> environment) {
    if (environment == null) return new String[0];

    return environment
            .entrySet()
            .stream()
            .map(entry->entry.getKey() + "=" + entry.getValue())
            .toArray(size -> new String[size]);

  }

  /**
   * Returns the folder that contains a jar that contains the class
   *
   * @param aclass a class to find a jar
   * @return
   */
  public static String getJarContainingFolderPath(Class aclass) throws Exception {
    CodeSource codeSource = aclass.getProtectionDomain().getCodeSource();

    File jarFile;

    if (codeSource.getLocation() != null) {
      jarFile = new File(codeSource.getLocation().toURI());
    } else {
      String path = aclass.getResource(aclass.getSimpleName() + ".class").getPath();

      int startIndex = path.indexOf(":") + 1;
      int endIndex = path.indexOf("!");
      if (startIndex == -1 || endIndex == -1) {
        throw new IllegalStateException("Class " + aclass.getSimpleName() + " is located not within a jar: " + path);
      }
      String jarFilePath = path.substring(startIndex, endIndex);
      jarFilePath = new URI(jarFilePath).getPath();
      jarFile = new File(jarFilePath);
    }

    return jarFile.getParentFile().getAbsolutePath();
  }

  public static String getPtyLibFolderPath() throws Exception {

    String ptyFolderFromEnv = System.getenv("PTY_LIB_FOLDER");
    String ptyFolderFromProperties = System.getProperty("PTY_LIB_FOLDER");

    if (ptyFolderFromEnv != null) {
      return ptyFolderFromEnv;
    }

    if (ptyFolderFromProperties != null) {
      return ptyFolderFromProperties;
    }

    //Class aclass = WinPty.class.getClassLoader().loadClass("com.jediterm.pty.PtyMain");
    Class aclass = WinPty.class;

    return getJarContainingFolderPath(aclass);
  }

  public static File resolveNativeLibrary() throws Exception {
    String libFolderPath = getPtyLibFolderPath();

    if (libFolderPath != null) {

      File libFolder = new File(libFolderPath);
      File lib = resolveNativeLibrary(libFolder);

      lib = lib.exists() ? lib : resolveNativeLibrary(new File(libFolder, "libpty"));

      if (!lib.exists()) {
        throw new IllegalStateException(String.format("Couldn't find %s, jar folder %s", lib.getName(),
                libFolder.getAbsolutePath()));
      }

      return lib;
    } else {
      throw new IllegalStateException("Couldn't detect lib folder");
    }
  }

  public static File resolveNativeLibrary(File parent) {
    return resolveNativeFile(parent, getNativeLibraryName());
  }

  public static File resolveNativeFile(String fileName) throws Exception {
    File libFolder = new File(getPtyLibFolderPath());
    File file = resolveNativeFile(libFolder, fileName);
    return file.exists() ? file : resolveNativeFile(new File(libFolder, "libpty"), fileName);
  }

  public static File resolveNativeFile(File parent, String fileName) {
    final File path = new File(parent, getPlatformFolder());

    String arch = Platform.is64Bit() ? "x86_64" : "x86";
    String prefix = isWinXp() ? "xp" : arch;

    return new File(new File(path, prefix), fileName);
  }

  private static String getPlatformFolder() {
    String result;

    if (Platform.isMac()) {
      result = "macosx";
    } else if (Platform.isWindows()) {
      result = "win";
    } else if (Platform.isLinux()) {
      result = "linux";
    } else {
      throw new IllegalStateException("Platform " + Platform.getOSType() + " is not supported");
    }

    return result;
  }

  private static String getNativeLibraryName() {
    String result;

    if (Platform.isMac()) {
      result = "libpty.dylib";
    } else if (Platform.isWindows()) {
      result = "libwinpty.dll";
    } else if (Platform.isLinux()) {
      result = "libpty.so";
    } else {
      throw new IllegalStateException("Platform " + Platform.getOSType() + " is not supported");
    }

    return result;
  }

  public static boolean isWinXp() {
    return Platform.isWindows() && (OS_VERSION.equals("5.1") || OS_VERSION.equals("5.2"));
  }
}
