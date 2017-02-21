/*
 * Jenkins Plugin for SonarQube, open source software quality management tool.
 * mailto:contact AT sonarsource DOT com
 *
 * Jenkins Plugin for SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Jenkins Plugin for SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.scanner.jenkins.pipeline;

import hudson.Extension;
import hudson.model.RootAction;
import hudson.model.UnprotectedRootAction;
import hudson.plugins.sonar.client.WsClient.CETask;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import jenkins.model.Jenkins;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.interceptor.RespondSuccess;

@Extension
public class SonarQubeWebHook implements UnprotectedRootAction {
  private static final Logger LOGGER = Logger.getLogger(SonarQubeWebHook.class.getName());
  public static final String URLNAME = "sonarqube-webhook";

  private List<Listener> listeners = new CopyOnWriteArrayList<>();

  @Override
  public String getIconFileName() {
    return null;
  }

  @Override
  public String getDisplayName() {
    return null;
  }

  @Override
  public String getUrlName() {
    return URLNAME;
  }

  @RequirePOST
  @RespondSuccess
  public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException {
    String payload = IOUtils.toString(req.getReader());

    LOGGER.info("Received POST from " + req.getRemoteHost());
    try {
      JSONObject o = JSONObject.fromObject(payload);

      LOGGER.fine(() -> "Full details of the POST was " + o.toString());
      String taskId = o.getString("taskId");
      String status = o.getString("status");
      String qgStatus = null;
      if (CETask.STATUS_SUCCESS.equals(status)) {
        qgStatus = o.getJSONObject("qualityGate").getString("status");
      }
      for (Listener listener : listeners) {
        listener.onTaskCompleted(taskId, status, qgStatus);
      }
    } catch (JSONException e) {
      LOGGER.warning(() -> "Invalid payload " + payload);
    }
  }

  public static SonarQubeWebHook get() {
    return Jenkins.getInstance().getExtensionList(RootAction.class).get(SonarQubeWebHook.class);
  }

  public void addListener(Listener l) {
    listeners.add(l);
  }

  public void removeListener(Listener l) {
    listeners.remove(l);
  }

  @FunctionalInterface
  public static interface Listener {

    void onTaskCompleted(String taskId, String taskStatus, @Nullable String qgStatus);

  }

}
