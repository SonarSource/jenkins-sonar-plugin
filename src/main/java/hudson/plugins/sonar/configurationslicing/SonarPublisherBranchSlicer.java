/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2025 SonarSource SA
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

import configurationslicing.UnorderedStringSlicer;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.plugins.sonar.SonarPublisher;

@Extension(optional = true)
public class SonarPublisherBranchSlicer extends UnorderedStringSlicer<AbstractProject<?, ?>> {

  public SonarPublisherBranchSlicer() {
    super(new SonarPublisherBranchSlicerSpec());
  }

  protected static class SonarPublisherBranchSlicerSpec extends AbstractSonarPublisherSlicerSpec {

    @Override
    public String getName() {
      return "SonarQube (Post Build) - Branch Slicer";
    }

    @Override
    public String getUrl() {
      return "sqPublisherBranch";
    }

    @Override
    protected String doGetValue(SonarPublisher publisher) {
      return defaultValueIfBlank(publisher.getBranch());
    }

    @Override
    protected void doSetValue(SonarPublisher publisher, String value) {
      publisher.setBranch(nullIfDefaultValue(value));
    }

    @Override
    protected String getDefaultValue() {
      return "(Empty)";
    }

  }
}
