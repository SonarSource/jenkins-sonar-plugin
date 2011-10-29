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
package hudson.plugins.sonar.model;

import hudson.Util;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.plugins.sonar.Messages;
import hudson.triggers.SCMTrigger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Evgeny Mandrikov
 * @since 1.2
 */
public class TriggersConfig implements Serializable {

  private boolean skipScmCause;

  private boolean skipUpstreamCause;

  private boolean skipUserCause;

  /**
   * @since 1.7
   */
  private String envVar;

  public TriggersConfig() {
  }

  @DataBoundConstructor
  public TriggersConfig(boolean skipScmCause, boolean skipUpstreamCause, boolean skipUserCause, String envVar) {
    this.skipScmCause = skipScmCause;
    this.skipUpstreamCause = skipUpstreamCause;
    this.skipUserCause = skipUserCause;
    this.envVar = envVar;
  }

  public boolean isSkipScmCause() {
    return skipScmCause;
  }

  public void setSkipScmCause(boolean b) {
    this.skipScmCause = b;
  }

  public boolean isSkipUpstreamCause() {
    return skipUpstreamCause;
  }

  public void setSkipUpstreamCause(boolean b) {
    this.skipUpstreamCause = b;
  }

  public boolean isSkipUserCause() {
    return skipUserCause;
  }

  public void setSkipUserCause(boolean b) {
    this.skipUserCause = b;
  }

  public String getEnvVar() {
    return Util.fixEmptyAndTrim(envVar);
  }

  public void setEnvVar(String envVar) {
    this.envVar = envVar;
  }

  public String isSkipSonar(AbstractBuild<?, ?> build) {
    Result result = build.getResult();

    if (result != null) {
      // skip analysis if build failed
      // unstable means that build completed, but there were some test failures, which is not critical for analysis
      if (result.isWorseThan(Result.UNSTABLE)) {
        return Messages.SonarPublisher_BadBuildStatus(build.getResult().toString());
      }
    }

    // skip analysis by environment variable
    if (getEnvVar() != null) {
      String value = build.getBuildVariableResolver().resolve(getEnvVar());
      if ("true".equalsIgnoreCase(value)) {
        return Messages.Skipping_Sonar_analysis();
      }
    }

    // skip analysis, when all causes from blacklist
    List<Cause> causes = new ArrayList<Cause>(build.getCauses());
    Iterator<Cause> iter = causes.iterator();
    while (iter.hasNext()) {
      Cause cause = iter.next();
      if (SCMTrigger.SCMTriggerCause.class.isInstance(cause) && isSkipScmCause()) {
        iter.remove();
      } else if (Cause.UpstreamCause.class.isInstance(cause) && isSkipUpstreamCause()) {
        iter.remove();
      } else if (Cause.UserCause.class.isInstance(cause) && isSkipUserCause()) {
          iter.remove();
      }
    }
    return causes.isEmpty() ? Messages.Skipping_Sonar_analysis() : null;
  }

  /**
   * For internal use only.
   */
  public static class SonarCause extends Cause {
    @Override
    public String getShortDescription() {
      return null;
    }
  }
}
