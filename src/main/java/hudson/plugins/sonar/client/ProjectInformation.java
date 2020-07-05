/*
 * SonarQube Scanner for Jenkins
 * Copyright (C) 2007-2019 SonarSource SA
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
package hudson.plugins.sonar.client;

import hudson.model.InvisibleAction;
import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ProjectInformation extends InvisibleAction {
  private long created;
  private String[] errors;
  private String name;
  private String url;
  private String status;
  private String ceStatus;
  private String ceUrl;

  private final String ERROR_MESSAGE = "Failed";
  private final String OK_MESSAGE = "Passed";
  private final String WARN_MESSAGE = "Warning";
  private final String UNKOWN_MESSAGE = "N/A";

  public ProjectInformation() {
    this.created = System.currentTimeMillis();
  }

  public long created() {
    return created;
  }

  public String getCeUrl() {
    return ceUrl;
  }

  public void setCeUrl(String ceUrl) {
    this.ceUrl = ceUrl;
  }

  public String getCeStatus() {
    return ceStatus;
  }

  public void setCeStatus(@Nullable String ceStatus) {
    this.ceStatus = (ceStatus != null) ? ceStatus.toLowerCase(Locale.US) : null;
  }

  @CheckForNull
  public String getStatus() {
    return status;
  }

  public void setName(String name) {
    this.name = name;
  }

  @CheckForNull
  public String getUrl() {
    return url;
  }

  public void setUrl(@Nullable String url) {
    this.url = url;
  }

  public String getProjectName() {
    return name;
  }

  public boolean hasErrors() {
    return errors != null && errors.length > 0;
  }

  public String[] getErrors() {
    return errors;
  }

  public void setErrors(String[] errors) {
    this.errors = errors;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getBadgeStatus() {

    String badgeString = "";

    if (status == null) {
      return "N/A";
    }

    switch(status.toUpperCase()) {
      case "OK": 
        badgeString = OK_MESSAGE;
      case "WARN": 
        badgeString = WARN_MESSAGE;
      case "ERROR":
        badgeString = ERROR_MESSAGE;
      default:
        badgeString = UNKOWN_MESSAGE;
    }
    return badgeString;
  }

}
