package com.obsidiandynamics.indigo;

import java.lang.annotation.*;
import java.lang.reflect.*;
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
    final List<Runner> wrapped = new ArrayList<>(children.size() * matrix.length);
    for (Parameter[] params : matrix) {
      for (Runner child : children) {
        wrapRunner(child, params, wrapped);
      }
    }
    return wrapped;
  }
  
  private static void wrapRunner(Runner runner, Parameter[] params, List<Runner> wrapped) {
    if (runner instanceof BlockJUnit4ClassRunner) {
      wrapped.add(wrapBlockRunner((BlockJUnit4ClassRunner) runner, params));
    } else if (runner instanceof Suite) {
      wrapSuite((Suite) runner, params, wrapped);
    } else {
      throw new IllegalArgumentException("Cannot wrap runners of type " + runner.getClass().getName());
    }
  }
  
  private static void wrapSuite(Suite suite, Parameter[] params, List<Runner> wrapped) {
    try {
      final Method method = suite.getClass().getDeclaredMethod("getChildren");
      method.setAccessible(true);
      @SuppressWarnings("unchecked")
      final List<Runner> children = (List<Runner>) method.invoke(suite);
      for (Runner child : children) {
        wrapRunner(child, params, wrapped);
      }
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new IllegalStateException(e);
    }
  }
  
  private static ParametrisedRunner wrapBlockRunner(BlockJUnit4ClassRunner blockRunner, Parameter[] params) {
    try {
      final Runner r = new BlockJUnit4ClassRunner(blockRunner.getTestClass().getJavaClass()) {
        @Override protected Description describeChild(FrameworkMethod method) {
          final Description s = super.describeChild(method);
          return Description.createTestDescription(s.getClassName(), 
                                                   s.getMethodName() + paramsToString(params), 
                                                   s.getAnnotations().toArray(new Annotation[0]));
        }

        private String paramsToString(Parameter[] params) {
          return Arrays.asList(params).stream().map(p -> p.value).collect(Collectors.toList()).toString();
        }
      };
      return new ParametrisedRunner(r, params);
    } catch (InitializationError e) {
      throw new IllegalStateException("Failed to initialise runner", e);
    } 
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
