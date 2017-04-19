package com.obsidiandynamics.indigo;

import java.lang.annotation.*;
import java.util.*;
import java.util.stream.*;

import org.junit.runner.*;
import org.junit.runner.notification.*;
import org.junit.runners.*;
import org.junit.runners.model.*;

public final class CycleSuite extends Suite {
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @Inherited
  public @interface ParameterMatrix {
    public String[] keys();
    public ParameterValues[] values();
  }
  
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.ANNOTATION_TYPE)
  @Inherited
  public @interface ParameterValues {
    public String[] value();
  }
  
  private static final class Parameter {
    private final String key;
    private final String value;
    
    Parameter(String key, String value) {
      this.key = key;
      this.value = value;
    }

    void install() {
      System.setProperty(key, value);
    }
  }
  
  private static final class ParametrisedRunner extends Runner {
    private final Runner backing;
    final Parameter[] params;
    
    ParametrisedRunner(Runner backing, Parameter[] params) {
      this.backing = backing;
      this.params = params;
    }

    @Override
    public Description getDescription() {
      return backing.getDescription();
    }

    @Override
    public void run(RunNotifier notifier) {
      backing.run(notifier);
    }
  }
  
  private final Parameter[][] matrix;
  
  public CycleSuite(Class<?> klass, RunnerBuilder builder) throws InitializationError {
    super(klass, builder);
    final ParameterMatrix annotation = klass.getAnnotation(ParameterMatrix.class);
    if (annotation == null) throw new IllegalArgumentException("Missing " + ParameterMatrix.class.getSimpleName() + " annotation");
    matrix = extractParams(annotation);
  }
  
  private static Parameter[][] extractParams(ParameterMatrix annotation) {
    final Parameter[][] matrix = new Parameter[annotation.values().length][annotation.keys().length];
    int i = 0;
    for (ParameterValues values : annotation.values()) {
      int j = 0;
      for (String key : annotation.keys()) {
        matrix[i][j] = new Parameter(key, values.value()[j]);
        j++;
      }
      i++;
    }
    return matrix;
  }

  @Override
  protected List<Runner> getChildren() {
    final List<Runner> children = super.getChildren();
    final List<Runner> runners = new ArrayList<>(children.size() * matrix.length);
    for (Parameter[] params : matrix) {
      for (Runner child : children) {
        try {
          final Runner r = new BlockJUnit4ClassRunner(((BlockJUnit4ClassRunner) child).getTestClass().getJavaClass()) {
            private final Map<FrameworkMethod, Description> descriptionCache = new IdentityHashMap<>();
            
            @Override protected Description describeChild(FrameworkMethod method) {
              final Description cached = descriptionCache.get(method);
              if (cached == null) {
                final Description s = super.describeChild(method);
                final Description derived = Description.createTestDescription(s.getClassName(), 
                                                                              s.getMethodName() + paramsToString(params), 
                                                                              s.getAnnotations().toArray(new Annotation[0]));
                descriptionCache.put(method, derived);
                return derived;
              } else {
                return cached;
              }
            }

            private String paramsToString(Parameter[] params) {
              return Arrays.asList(params).stream().map(p -> p.value).collect(Collectors.toList()).toString();
            }
          };
          runners.add(new ParametrisedRunner(r, params));
        } catch (InitializationError e) {
          e.printStackTrace();
        } 
      }
    }
    return runners;
  }
  
  @Override
  protected void runChild(Runner runner, final RunNotifier notifier) {
    final ParametrisedRunner cycleRunner = (ParametrisedRunner) runner; 
    for (Parameter param : cycleRunner.params) {
      param.install();
    }
    runner.run(notifier);
  }
}
