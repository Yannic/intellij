/*
 * Copyright 2020 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.android.run.runner;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.android.ddmlib.IDevice;
import com.android.tools.idea.run.ConsolePrinter;
import com.google.common.collect.ImmutableList;
import com.google.idea.blaze.android.run.deployinfo.BlazeAndroidDeployInfo;
import com.google.testing.util.TestUtil;
import java.io.File;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/** Tests for {@link com.google.idea.blaze.android.run.runner.ApkDebuggabilityChecker} */
@RunWith(JUnit4.class)
public class ApkDebuggabilityCheckerTest {
  private static final String NON_DEBUG_APK_LOCATION =
      "google3/aswb/tests/integrationtests/com/google/idea/blaze/android/run/runner/nondebug.apk";
  private static final String DEBUG_APK_LOCATION =
      "google3/aswb/tests/integrationtests/com/google/idea/blaze/android/run/runner/debug.apk";

  @Test
  public void testParseNonDebugApk() throws IOException {
    File testApk = new File(TestUtil.getSrcDir(), NON_DEBUG_APK_LOCATION);
    assertThat(ApkDebuggabilityChecker.isApkDebuggable(testApk)).isFalse();
  }

  @Test
  public void testParseDebugApk() throws IOException {
    File testApk = new File(TestUtil.getSrcDir(), DEBUG_APK_LOCATION);
    assertThat(ApkDebuggabilityChecker.isApkDebuggable(testApk)).isTrue();
  }

  /** Check deploying one debuggable and one non-debuggable APK to non-debug device. */
  @Test
  public void testCheckNonDebugApk() {
    BlazeAndroidDeployInfo deployInfo =
        new BlazeAndroidDeployInfo(
            null,
            null,
            ImmutableList.of(
                new File(TestUtil.getSrcDir(), NON_DEBUG_APK_LOCATION),
                new File(TestUtil.getSrcDir(), DEBUG_APK_LOCATION)));
    IDevice mockNonDebuggableDevice = Mockito.mock(IDevice.class);
    when(mockNonDebuggableDevice.getProperty("ro.debuggable")).thenReturn("0");
    StubConsolePrinter stderrSink = new StubConsolePrinter();

    ApkDebuggabilityChecker.checkDebugAttribute(deployInfo, mockNonDebuggableDevice, stderrSink);
    assertThat(stderrSink.stderrBuffer).contains("nondebug.apk");
  }

  /** Check deploying one debuggable and one non-debuggable APK to debug device. */
  @Test
  public void testCheckNonDebugApk_deployingToDebugDevice() {
    BlazeAndroidDeployInfo deployInfo =
        new BlazeAndroidDeployInfo(
            null,
            null,
            ImmutableList.of(
                new File(TestUtil.getSrcDir(), NON_DEBUG_APK_LOCATION),
                new File(TestUtil.getSrcDir(), DEBUG_APK_LOCATION)));
    IDevice mockNonDebuggableDevice = Mockito.mock(IDevice.class);
    when(mockNonDebuggableDevice.getProperty("ro.debuggable")).thenReturn("1");
    StubConsolePrinter stderrSink = new StubConsolePrinter();

    ApkDebuggabilityChecker.checkDebugAttribute(deployInfo, mockNonDebuggableDevice, stderrSink);
    assertThat(stderrSink.stderrBuffer).isEmpty();
  }

  /** Check deploying one debuggable APK to non-debug device. */
  @Test
  public void testCheckDebugApk() {
    BlazeAndroidDeployInfo deployInfo =
        new BlazeAndroidDeployInfo(
            null, null, ImmutableList.of(new File(TestUtil.getSrcDir(), DEBUG_APK_LOCATION)));
    IDevice mockNonDebuggableDevice = Mockito.mock(IDevice.class);
    when(mockNonDebuggableDevice.getProperty("ro.debuggable")).thenReturn("0");
    StubConsolePrinter stderrSink = new StubConsolePrinter();

    ApkDebuggabilityChecker.checkDebugAttribute(deployInfo, mockNonDebuggableDevice, stderrSink);
    assertThat(stderrSink.stderrBuffer).isEmpty();
  }

  private static class StubConsolePrinter implements ConsolePrinter {
    String stderrBuffer = "";

    @Override
    public void stdout(@NotNull String s) {}

    @Override
    public void stderr(@NotNull String s) {
      stderrBuffer += s;
    }
  }
}
