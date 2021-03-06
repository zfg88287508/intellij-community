// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.diagnostic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

// non-sequential and repeated items
public enum ParallelActivity {
  PREPARE_APP_INIT("prepareAppInitActivity"), PRELOAD_ACTIVITY("preloadActivity"),
  APP_OPTIONS_TOP_HIT_PROVIDER("appOptionsTopHitProvider"), PROJECT_OPTIONS_TOP_HIT_PROVIDER("projectOptionsTopHitProvider"),
  COMPONENT("component"), SERVICE("service"), EXTENSION("extension")
  ;

  private static final long MEASURE_THRESHOLD = TimeUnit.MILLISECONDS.toNanos(10);

  private final String jsonName;

  ParallelActivity(@NotNull String jsonName) {
    this.jsonName = jsonName;
  }

  @NotNull
  public String getJsonName() {
    return jsonName;
  }

  @NotNull
  public Activity start(@NotNull String name) {
    return ActivityImpl.createParallelActivity(this, name);
  }

  public void record(long start, @NotNull Class<?> clazz) {
    record(start, clazz, null);
  }

  /**
   * Default threshold is applied.
   */
  public void record(long start, @NotNull Class<?> clazz, @Nullable StartUpMeasurer.Level level) {
    long end = System.nanoTime();
    if ((end - start) <= MEASURE_THRESHOLD) {
      return;
    }

    ActivityImpl item = new ActivityImpl(clazz.getName(), /* description = */ null, start, /* parent = */ null, level, this);
    item.setEnd(end);
    StartUpMeasurer.add(item);
  }
}
