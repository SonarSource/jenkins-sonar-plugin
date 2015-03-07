/*
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package hudson.plugins.sonar.configurationslicing;

import hudson.maven.MavenModuleSet;
import hudson.plugins.sonar.SonarPublisher;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author drautureau
 */
public class JobAdditionalPropertiesSlicerTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void availableMavenProjectsWithSonarPublisher() throws IOException {
        final MavenModuleSet project = j.createMavenProject();
        assertThat(new JobAdditionalPropertiesSlicer().getWorkDomain().size()).isZero();
        project.getPublishersList().add(new SonarPublisher("MySonar", "-Dsonar.verbose", null));
        assertThat(new JobAdditionalPropertiesSlicer().getWorkDomain().size()).isEqualTo(1);
    }

    @Test
    public void changeJobAdditionalProperties() throws IOException {
        final MavenModuleSet project = j.createMavenProject();
        project.getPublishersList().add(new SonarPublisher("MySonar", "-Dsonar.verbose", null));
        final JobAdditionalPropertiesSlicer.JobAdditionalPropertiesSlicerSpec propertiesSpec = new JobAdditionalPropertiesSlicer.JobAdditionalPropertiesSlicerSpec();
        final List<String> values = propertiesSpec.getValues(project);
        assertThat(values.get(0)).isEqualTo("-Dsonar.verbose");

        final List<String> newValues = new ArrayList<String>();
        newValues.add("-Dsonar.showSql");
        propertiesSpec.setValues(project, newValues);
        final SonarPublisher publisher = project.getPublishersList().get(SonarPublisher.class);
        assertThat(publisher.getJobAdditionalProperties()).isEqualTo("-Dsonar.showSql");
    }

}
