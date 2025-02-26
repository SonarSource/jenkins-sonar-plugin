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
package hudson.plugins.sonar.utils;

import hudson.util.VersionNumber;
import jenkins.model.Jenkins;

/**
 * Utility class to encapsulate the details of routing information in the Jenkins web application.
 * Use this class to get Jenkins URLs and relative paths.
 */
public final class JenkinsRouter {

  public static final boolean BEFORE_V2 = Jenkins.getVersion().isOlderThan(new VersionNumber("2"));

  private JenkinsRouter() {
    throw new AssertionError("utility class, forbidden constructor");
  }

  public static String getGlobalToolConfigUrl() {
    return getRootUrl() + getGlobalToolConfigRelPath();
  }

  private static String getRootUrl() {
    return Jenkins.get().getRootUrl();
  }

  private static String getGlobalToolConfigRelPath() {
    return BEFORE_V2 ? "configure" : "configureTools";
  }

}
