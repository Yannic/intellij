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

import com.android.ddmlib.IDevice;
import com.android.tools.idea.run.ConsolePrinter;
import com.google.common.annotations.VisibleForTesting;
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceFile;
import com.google.devrel.gmscore.tools.apk.arsc.Chunk;
import com.google.devrel.gmscore.tools.apk.arsc.XmlAttribute;
import com.google.devrel.gmscore.tools.apk.arsc.XmlChunk;
import com.google.devrel.gmscore.tools.apk.arsc.XmlStartElementChunk;
import com.google.idea.blaze.android.run.deployinfo.BlazeAndroidDeployInfo;
import com.intellij.openapi.diagnostic.Logger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

/** Checks APKs to see if they are deployable and warn the user if they aren't. */
public class ApkDebuggabilityChecker {
  /**
   * Checks to see if APKs to deploy are all debuggable and output an error message if they aren't.
   * This check is a no-op if the device is a debug device (e.g. userdebug build).
   *
   * <p>TODO Merge this with 3.6 and 4.0 versions of ApkDebugAttributeCheckingTask when 3.5 is
   * paved.
   */
  public static void checkDebugAttribute(
      BlazeAndroidDeployInfo deployInfo, IDevice idevice, ConsolePrinter consolePrinter) {
    // A device with ro.debuggable=1 is a userdebug device, which is always debuggable.
    String roDebuggable = idevice.getProperty("ro.debuggable");
    if (roDebuggable != null && roDebuggable.equals("1")) {
      return;
    }

    ArrayList<String> nonDebuggableApkNames = new ArrayList<>();
    for (File apk : deployInfo.getApksToDeploy()) {
      try {
        if (!isApkDebuggable(apk)) {
          nonDebuggableApkNames.add(apk.getName());
        }
      } catch (IOException e) {
        Logger.getInstance(ApkDebuggabilityChecker.class).error(e);
      }
    }

    if (nonDebuggableApkNames.isEmpty()) {
      return;
    }

    // Use "and" as delimiter because there won't be more than 2 APKs, so "and" makes more sense.
    String message =
        "The \"android:debuggable\" attribute is not set to \"true\" in "
            + String.join(" and ", nonDebuggableApkNames)
            + ". Debugger may not attach properly or attach at all."
            + " Please ensure \"android:debuggable\" attribute is set to true or"
            + " overridden to true via manifest overrides.";
    consolePrinter.stderr(message);
  }

  @VisibleForTesting
  public static boolean isApkDebuggable(File apk) throws IOException {
    try (ZipFile zipFile = new ZipFile(apk);
        InputStream stream = zipFile.getInputStream(zipFile.getEntry("AndroidManifest.xml"))) {
      BinaryResourceFile file = BinaryResourceFile.fromInputStream(stream);
      List<Chunk> chunks = file.getChunks();

      if (chunks.isEmpty()) {
        throw new IllegalArgumentException("Invalid APK, empty manifest");
      }

      if (!(chunks.get(0) instanceof XmlChunk)) {
        throw new IllegalArgumentException("APK manifest chunk[0] != XmlChunk");
      }

      XmlChunk xmlChunk = (XmlChunk) chunks.get(0);
      for (Chunk chunk : xmlChunk.getChunks().values()) {
        if (!(chunk instanceof XmlStartElementChunk)) {
          continue;
        }

        XmlStartElementChunk startChunk = (XmlStartElementChunk) chunk;
        if (startChunk.getName().equals("application")) {
          for (XmlAttribute attribute : startChunk.getAttributes()) {
            if (attribute.name().equals("debuggable")) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }
}
