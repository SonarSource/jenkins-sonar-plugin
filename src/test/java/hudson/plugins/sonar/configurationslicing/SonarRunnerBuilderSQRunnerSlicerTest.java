/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2024 SonarSource SA
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
package hudson.plugins.sonar.configurationslicing;

import hudson.model.FreeStyleProject;
import hudson.plugins.sonar.SonarRunnerBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarRunnerBuilderSQRunnerSlicerTest {

  @Rule
  public JenkinsRule j = new JenkinsRule();

  @Test
  public void availableProjectsWithSonarBuildStep() throws IOException {
    final FreeStyleProject project = j.createFreeStyleProject();
    assertThat(new SonarRunnerBuilderSQRunnerSlicer().getWorkDomain().size()).isZero();
    project.getBuildersList().add(new SonarRunnerBuilder());
    assertThat(new SonarRunnerBuilderSQRunnerSlicer().getWorkDomain().size()).isEqualTo(1);
  }

  @Test
  public void changeJobAdditionalProperties() throws Exception {
    final FreeStyleProject project = j.createFreeStyleProject();
    final SonarRunnerBuilder mySonar = new SonarRunnerBuilder();
    mySonar.setInstallationName("MySonar");
    mySonar.setSonarScannerName("SQ Runner 2.3");
    project.getBuildersList().add(mySonar);

    final SonarRunnerBuilderSQRunnerSlicer.SonarRunnerBuilderSQRunnerSlicerSpec spec = new SonarRunnerBuilderSQRunnerSlicer.SonarRunnerBuilderSQRunnerSlicerSpec();
    final List<String> values = spec.getValues(project);
    assertThat(values.get(0)).isEqualTo("SQ Runner 2.3");
    final List<String> newValues = new ArrayList<>();
    newValues.add("SQ Runner 2.4");
    spec.setValues(project, newValues);

    assertThat(mySonar.getSonarScannerName()).isEqualTo("SQ Runner 2.4");
  }

}
