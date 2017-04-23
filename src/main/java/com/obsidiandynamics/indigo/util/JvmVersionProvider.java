package com.obsidiandynamics.indigo.util;

public abstract class JvmVersionProvider {
  private static JvmVersionProvider PROVIDER = new DefaultJvmVersionProvider();

  public static final class JvmVersion {
    public final int major;
    public final int minor;
    public final int update;
    public final int build;

    private JvmVersion(int major, int minor, int update, int build) {
      this.major = major;
      this.minor = minor;
      this.update = update;
      this.build = build;
    }
    
    public static JvmVersion parse(String versionStr) {
      final String[] versionFrags = versionStr.split("\\.|_");
      final int major = Integer.parseInt(versionFrags[0]);
      final int minor = Integer.parseInt(versionFrags[1]);
      final int update = Integer.parseInt(versionFrags[2]);
      final int build = Integer.parseInt(versionFrags[3]);
      return new JvmVersion(major, minor, update, build);
    }

    @Override
    public String toString() {
      return "JvmVersion [major=" + major + ", minor=" + minor + ", update=" + update + ", build=" + build + "]";
    }
  }

  public static final class DefaultJvmVersionProvider extends JvmVersionProvider {
    @Override
    public JvmVersion get() {
      final String versionStr = System.getProperty("java.version");
      try {
        return JvmVersion.parse(versionStr);
      } catch (Exception e) {
        final String fallbackStr = "1.8.0_1";
        System.err.format("WARNING: cannot parse system property 'java.version' with value '%s'; falling back to '%s'\n", 
                          versionStr, fallbackStr);
        return JvmVersion.parse(fallbackStr);
      }
    }
  }

  public static final void setProvider(JvmVersionProvider provider) {
    PROVIDER = provider;
  }

  public static final JvmVersion getVersion() {
    return PROVIDER.get();
  }

  public abstract JvmVersion get();
}
