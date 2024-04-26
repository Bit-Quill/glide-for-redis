package glide.ffi.resolvers;

import lombok.SneakyThrows;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

public class NativeLibLoader {

  @SneakyThrows
  public static void load() {
    String libName = "libglide_rs";
    String osName = System.getProperty("os.name").toLowerCase();
    if (osName.contains("linux")) {
      libName += ".so";
    } else if (osName.contains("mac")) {
      libName += ".dylib";
    } else {
      throw new UnsupportedOperationException("OS not supported: " + osName);
    }

    URL url = NativeLibLoader.class.getResource("/" + libName);
    File tmpDir = Files.createTempDirectory("glide").toFile();
    tmpDir.deleteOnExit();
    File nativeLibTmpFile = new File(tmpDir, libName);
    nativeLibTmpFile.deleteOnExit();
    try (InputStream in = url.openStream()) {
      Files.copy(in, nativeLibTmpFile.toPath());
    }
    System.load(nativeLibTmpFile.getAbsolutePath());
  }
}
