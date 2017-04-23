package com.obsidiandynamics.indigo.util;

@FunctionalInterface
public interface JvmVersionProvider {
  public static final class JvmVersion {
    public final int major;
    public final int minor;
    public final int update;
    public final int build;

    public JvmVersion(int major, int minor, int update, int build) {
      this.major = major;
      this.minor = minor;
      this.update = update;
      this.build = build;
    }

    @Override
    public String toString() {
      return "JvmVersion [major=" + major + ", minor=" + minor + ", update=" + update + ", build=" + build + "]";
    }
  }

  public static final class DefaultProvider implements JvmVersionProvider {
    private final String versionStr;
    
    public DefaultProvider() {
      this(System.getProperty("java.version"));
    }
    
    public DefaultProvider(String versionStr) {
      this.versionStr = versionStr;
    }
    
    @Override
    public JvmVersion get() {
      try {
        return parse(versionStr);
      } catch (Exception e) {
        final String fallbackStr = "1.8.0_1";
        System.err.format("WARNING: cannot parse version string '%s'; falling back to '%s'\n", 
                          versionStr, fallbackStr);
        return parse(fallbackStr);
      }
    }
    
    private static JvmVersion parse(String versionStr) {
      final String[] versionFrags = versionStr.split("\\.|_");
      final int major = Integer.parseInt(versionFrags[0]);
      final int minor = Integer.parseInt(versionFrags[1]);
      final int update = Integer.parseInt(versionFrags[2]);
      final int build = Integer.parseInt(versionFrags[3]);
      return new JvmVersion(major, minor, update, build);
    }
  }

  JvmVersion get();
}
