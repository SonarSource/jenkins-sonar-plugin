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

import hudson.model.TaskListener;

/**
 * @author Julien HENRY
 * @since 2.0
 */
public final class Logger {

  public static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger("hudson.plugins.sonar");

  private Logger() {
    // Utility class
  }

  public static void printFailureMessage(TaskListener listener) {
    listener.getLogger().println("------------------------------------------------------------------------");
    listener.getLogger().println("SONAR ANALYSIS FAILED");
    listener.getLogger().println("------------------------------------------------------------------------");
  }
}
