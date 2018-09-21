/*
 * Jenkins :: Integration Tests
 * Copyright (C) 2013-2018 SonarSource SA
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

import com.sonar.it.jenkins.orchestrator.JenkinsOrchestrator;
import com.sonar.it.jenkins.orchestrator.JenkinsOrchestrator.FailedExecutionException;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SynchronousAnalyzer;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import java.io.File;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.PropertyUpdateQuery;

import static com.sonar.it.jenkins.JenkinsTestSuite.getProject;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

public class JenkinsWithoutMaven {

  // Jenkins should only talk to the URL actually configured in a SonarInstallation
  final static String SONAR_PUBLIC_BASE_URL = "http://sonar_public_domain/";

  @ClassRule
  public static Orchestrator orchestrator = JenkinsTestSuite.ORCHESTRATOR;

  @ClassRule
  public static JenkinsOrchestrator jenkins = JenkinsOrchestrator.builderEnv().build();

  private final File csharpFolder = new File("projects", "csharp");
  private final File consoleApp1Folder = new File(csharpFolder, "ConsoleApplication1");
  private final File consoleNetCoreFolder = new File(csharpFolder, "NetCoreConsoleApp");
  private final File jsFolder = new File("projects", "js");

  @BeforeClass
  public static void setUpSonar() {
    // Workaround for SONAR-4257
    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.core.serverBaseURL", SONAR_PUBLIC_BASE_URL));
  }

  @BeforeClass
  public static void setUpJenkins() {
    orchestrator.resetData();
    Location sqJenkinsPluginLocation = FileLocation.of("../target/sonar.hpi");
    jenkins
      .installPlugin("filesystem_scm")
      .installPlugin("form-element-path")
      .installPlugin("jquery")
      .installPlugin("msbuild")
      .installPlugin(sqJenkinsPluginLocation)
      .configureSQScannerInstallation("2.8", 0)
      .configureMsBuildSQScanner_installation("2.3.2.573", false, 0)
      .configureMsBuildSQScanner_installation("3.0.0.629", false, 1)
      .configureMsBuildSQScanner_installation("4.1.0.1148", true, 2)
      .configureSonarInstallation(orchestrator);
    if (SystemUtils.IS_OS_WINDOWS) {
      jenkins.configureMSBuildInstallation();
    }
    jenkins.checkSavedSonarInstallation(orchestrator);
    jenkins.configureDefaultQG(orchestrator);
  }

  @Before
  public void resetData() {
    orchestrator.resetData();
  }

  @Test
  public void testFreestyleJobWithSonarQubeScanner_use_sq_scanner_2_8() {
    String jobName = "js-runner-sq-2.8";
    String projectKey = "js-runner-2.8";
    assertThat(getProject(projectKey)).isNull();
    BuildResult result = jenkins
      .newFreestyleJobWithSQScanner(jobName, "-v", jsFolder, null,
        "sonar.projectKey", projectKey,
        "sonar.projectVersion", "1.0",
        "sonar.projectName", "Abacus",
        "sonar.sources", "src")
      .executeJob(jobName);

    if (JenkinsTestSuite.isWindows()) {
      assertThat(result.getLogs()).contains("sonar-scanner.bat");
    } else {
      assertThat(result.getLogs()).contains("sonar-scanner");
    }
    assertThat(result.getLogs()).contains("SonarQube Scanner 2.8");
  }

  @Test
  public void testFreestyleJobWithScannerForMsBuild() throws FailedExecutionException {
    assumeTrue(SystemUtils.IS_OS_WINDOWS);
    String jobName = "csharp";
    String projectKey = "csharp";
    assertThat(getProject(projectKey)).isNull();
    jenkins
      .newFreestyleJobWithScannerForMsBuild(jobName, null, consoleApp1Folder, projectKey, "CSharp", "1.0", "3.0.0.629", "ConsoleApplication1.sln", false)
      .executeJob(jobName);

    waitForComputationOnSQServer();
    assertThat(getProject(projectKey)).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
  }

  @Test
  public void testFreestyleJobWithScannerForMsBuild_NetCore() {
    String jobName = "csharp-core";
    String projectKey = "csharp-core";
    assertThat(getProject(projectKey)).isNull();
    jenkins
      .newFreestyleJobWithScannerForMsBuild(jobName, null, consoleNetCoreFolder, projectKey, "CSharp NetCore", "1.0", "4.1.0.1148", "NetCoreConsoleApp.sln", true)
      .executeJob(jobName);

    waitForComputationOnSQServer();
    assertThat(getProject(projectKey)).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
  }

  @Test
  public void testFreestyleJobWithScannerForMsBuild_3_0() {
    assumeTrue(SystemUtils.IS_OS_WINDOWS);
    File toolPath = new File(jenkins.getServer().getHome().getAbsolutePath() + File.separator + "tools" + File.separator + "hudson.plugins.sonar.MsBuildSQRunnerInstallation");
    String jobName = "msbuild-sq-runner-3_0";
    String projectKey = "msbuild-sq-runner-3_0";
    assertThat(getProject(projectKey)).isNull();
    BuildResult result = jenkins
      .newFreestyleJobWithScannerForMsBuild(jobName, null, jsFolder, projectKey, "JS with space", "1.0", "3.0.0.629", null, false)
      .executeJobQuietly(jobName);

    assertThat(result.getLogs())
      .contains(
        "tools" + File.separator + "hudson.plugins.sonar.MsBuildSQRunnerInstallation" + File.separator + "Scanner_for_MSBuild_3.0.0.629" + File.separator
          + "MSBuild.SonarQube.Runner.exe begin /k:" + projectKey + " \"/n:JS with space\" /v:1.0 /d:sonar.host.url="
          + orchestrator.getServer().getUrl());

    assertThat(toolPath).isDirectory();
  }

  @Test
  public void testFreestyleJobWithScannerForMsBuild_2_3_2() {
    assumeTrue(SystemUtils.IS_OS_WINDOWS);
    File toolPath = new File(jenkins.getServer().getHome().getAbsolutePath() + File.separator + "tools" + File.separator + "hudson.plugins.sonar.MsBuildSQRunnerInstallation");
    String jobName = "msbuild-sq-runner-2_3_2";
    String projectKey = "msbuild-sq-runner-2_3_2";
    assertThat(getProject(projectKey)).isNull();
    BuildResult result = jenkins
      .newFreestyleJobWithScannerForMsBuild(jobName, null, jsFolder, projectKey, "JS with space", "1.0", "2.3.2.573", null, false)
      .executeJobQuietly(jobName);

    assertThat(result.getLogs())
      .contains(
        "tools" + File.separator + "hudson.plugins.sonar.MsBuildSQRunnerInstallation" + File.separator + "Scanner_for_MSBuild_2.3.2.573" + File.separator
          + "MSBuild.SonarQube.Runner.exe begin /k:" + projectKey + " \"/n:JS with space\" /v:1.0 /d:sonar.host.url="
          + orchestrator.getServer().getUrl());

    assertThat(toolPath).isDirectory();
  }

  @Test
  public void testNoSonarPublisher() {
    // Maven plugin no more installed by default in version 2
    assumeTrue(jenkins.getServer().getVersion().isGreaterThanOrEquals(2, 0));
    String jobName = "no Sonar Publisher";
    jenkins.assertNoSonarPublisher(jobName, new File("projects", "noPublisher"));
  }

  private void assertSonarUrlOnJob(String jobName, String projectKey) {
    assertThat(jenkins.getSonarUrlOnJob(jobName)).startsWith(SONAR_PUBLIC_BASE_URL);
    assertThat(jenkins.getSonarUrlOnJob(jobName)).endsWith(projectKey);
  }

  private static void waitForComputationOnSQServer() {
    new SynchronousAnalyzer(orchestrator.getServer()).waitForDone();
  }

}
