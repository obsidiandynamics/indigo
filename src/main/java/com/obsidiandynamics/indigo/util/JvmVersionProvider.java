package com.obsidiandynamics.indigo.util;

@FunctionalInterface
public interface JvmVersionProvider {
  public static final class JvmVersion implements Comparable<JvmVersion> {
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
      return JvmVersion.class.getSimpleName() + " [major=" + major + ", minor=" + minor + ", update=" + update + ", build=" + build + "]";
    }

    @Override
    public int compareTo(JvmVersion o) {
      final int majorComp = Integer.compare(major, o.major);
      if (majorComp != 0) {
        return majorComp;
      } else {
        final int minorComp = Integer.compare(minor, o.minor);
        if (minorComp != 0) {
          return minorComp;
        } else {
          final int updateComp = Integer.compare(update, o.update);
          if (updateComp != 0) {
            return updateComp;
          } else {
            return Integer.compare(build, o.build);
          }
        }
      }
    }
    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + major;
      result = prime * result + minor;
      result = prime * result + update;
      result = prime * result + build;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if (obj instanceof JvmVersion) {
        final JvmVersion that = (JvmVersion) obj;
        return major == that.major && minor == that.minor && update == that.update && build == that.build;
      } else {
        return false;
      }
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
      final int minor = versionFrags.length > 1 ? Integer.parseInt(versionFrags[1]) : 0;
      final int update = versionFrags.length > 2 ? Integer.parseInt(versionFrags[2]) : 0;
      final int build = versionFrags.length > 3 ? Integer.parseInt(versionFrags[3]) : 0;
      return new JvmVersion(major, minor, update, build);
    }
  }

  JvmVersion get();
}
