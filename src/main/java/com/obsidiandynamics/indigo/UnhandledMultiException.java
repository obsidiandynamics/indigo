package com.obsidiandynamics.indigo;

import java.util.*;

public final class UnhandledMultiException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final Throwable[] errors;
  
  UnhandledMultiException(Throwable[] errors) {
    super(Arrays.asList(errors).toString());
    this.errors = errors;
  }
  
  public Throwable[] getErrors() {
    return errors;
  }
}
