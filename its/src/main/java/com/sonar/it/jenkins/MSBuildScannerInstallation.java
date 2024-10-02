/*
 * Jenkins :: Integration Tests
 * Copyright (C) 2013-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.sonar.it.jenkins;

import org.jenkinsci.test.acceptance.po.Jenkins;
import org.jenkinsci.test.acceptance.po.ToolInstallation;
import org.jenkinsci.test.acceptance.po.ToolInstallationPageObject;

@ToolInstallationPageObject(installer = "hudson.plugins.sonar.MsBuildSonarQubeRunnerInstaller", name = "SonarScanner for MSBuild")
public class MSBuildScannerInstallation extends ToolInstallation {

  public MSBuildScannerInstallation(Jenkins jenkins, String path) {
    super(jenkins, path);
  }

  public static void install(final Jenkins jenkins, final String version, boolean isDotnetCore) {
    installTool(jenkins, MSBuildScannerInstallation.class, getInstallName(version, isDotnetCore), getVersion(version, isDotnetCore));
  }

  private static String getVersion(String version, boolean isDotnetCore) {
    String suffix;
    if (isEqualOrAboveVersion6(version)) {
      suffix = isDotnetCore ? "-net" : "-net-framework";
    } else {
      suffix = isDotnetCore ? "-netcore" : "";
    }
    return version + suffix;
  }


  public static String getInstallName(final String version, boolean isDotnetCore) {
    if (isEqualOrAboveVersion6(version)) {
      return "SonarScanner for " + (isDotnetCore ? " .NET" : " .NET Framework") + version;
    }
    return "SonarScanner for MSBuild" + version + (isDotnetCore ? " - .NET Core" : " - .NET Fwk");
  }

  private static boolean isEqualOrAboveVersion6(String version) {
    //version 6 and above have different package naming
    String[] numbers = version.split("\\.");
    return Integer.parseInt(numbers[0]) >= 6;
  }

}
