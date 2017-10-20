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
package hudson.plugins.sonar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.annotations.VisibleForTesting;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.sonar.action.SonarAnalysisAction;
import hudson.plugins.sonar.action.SonarMarkerAction;
import hudson.plugins.sonar.utils.Logger;
import hudson.plugins.sonar.utils.MaskPasswordsOutputStream;
import hudson.plugins.sonar.utils.SQServerVersions;
import hudson.plugins.sonar.utils.SonarUtils;
import hudson.slaves.WorkspaceList;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildWrapper;

public class SonarBuildWrapper extends SimpleBuildWrapper {
  private static final String DEFAULT_SONAR = "sonar";
  private String installationName = null;

  @DataBoundConstructor
  public SonarBuildWrapper(@Nullable String installationName) {
    this.installationName = installationName;
  }

  @Override
  public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars initialEnvironment)
    throws IOException, InterruptedException {

    SonarInstallation.checkValid(getInstallationName());
    SonarInstallation installation = SonarInstallation.get(getInstallationName());

    String msg = Messages.SonarBuildWrapper_Injecting(installation.getName());
    Logger.LOG.info(msg);
    listener.getLogger().println(msg);

    
    // Setup temporary report dir
    //
    String now = new SimpleDateFormat("yyyyMMdd-HHmmss-S").format(new Date());
    FilePath reportDir = WorkspaceList.tempDir(workspace).child("sonar-" + now);
    reportDir.mkdirs();
    
    
    context.getEnv().putAll(createVars(installation, reportDir));

    context.setDisposer(new AddBuildInfo(installation, reportDir));

    build.addAction(new SonarMarkerAction());
  }

  @VisibleForTesting
  @Deprecated
  static Map<String, String> createVars(SonarInstallation inst) {
	  return createVars(inst, null);
  }
  
  @VisibleForTesting
  static Map<String, String> createVars(SonarInstallation inst, FilePath reportDir) {
    Map<String, String> map = new HashMap<>();

    map.put("SONAR_CONFIG_NAME", inst.getName());
    String hostUrl = getOrDefault(inst.getServerUrl(), "http://localhost:9000");
    map.put("SONAR_HOST_URL", hostUrl);
    String login = getOrDefault(inst.getSonarLogin(), "");
    map.put("SONAR_LOGIN", login);
    String password = getOrDefault(inst.getSonarPassword(), "");
    map.put("SONAR_PASSWORD", password);
    String token = getOrDefault(inst.getServerAuthenticationToken(), "");
    map.put("SONAR_AUTH_TOKEN", token);

    map.put("SONAR_JDBC_URL", getOrDefault(inst.getDatabaseUrl(), ""));

    String jdbcDefault = SQServerVersions.SQ_5_1_OR_LOWER.equals(inst.getServerVersion()) ? DEFAULT_SONAR : "";
    map.put("SONAR_JDBC_USERNAME", getOrDefault(inst.getDatabaseLogin(), jdbcDefault));
    map.put("SONAR_JDBC_PASSWORD", getOrDefault(inst.getDatabasePassword(), jdbcDefault));

    if (StringUtils.isEmpty(inst.getMojoVersion())) {
      map.put("SONAR_MAVEN_GOAL", "sonar:sonar");
    } else {
      map.put("SONAR_MAVEN_GOAL", SonarUtils.getMavenGoal(inst.getMojoVersion()));
    }

    map.put("SONAR_EXTRA_PROPS", getOrDefault(getAdditionalProps(inst), ""));

    String workingDir = reportDir.getRemote();
    map.put("SONAR_WORKING_DIRECTORY", workingDir);
    
    StringBuilder sb = new StringBuilder();
    sb.append("{ \"sonar.host.url\" : \"").append(StringEscapeUtils.escapeJson(hostUrl)).append("\"");
    if (!login.isEmpty() || !token.isEmpty()) {
      sb.append(", \"sonar.login\" : \"").append(StringEscapeUtils.escapeJson(token.isEmpty() ? login : token)).append("\"");
    }
    if (!password.isEmpty()) {
      sb.append(", \"sonar.password\" : \"").append(StringEscapeUtils.escapeJson(password)).append("\"");
    }
    if (reportDir!=null) {
    	sb.append(", \"sonar.working.directory\" : \"").append(StringEscapeUtils.escapeJson(workingDir)).append("\"");
    }
    sb.append("}");

    map.put("SONARQUBE_SCANNER_PARAMS", sb.toString());

    // resolve variables against each other
    EnvVars.resolve(map);

    return map;
  }

  private static String getAdditionalProps(SonarInstallation inst) {
    ArgumentListBuilder builder = new ArgumentListBuilder();
    // no need to tokenize since we need a String anyway
    builder.add(inst.getAdditionalAnalysisPropertiesUnix());
    builder.add(inst.getAdditionalProperties());

    return StringUtils.join(builder.toList(), ' ');
  }

  private static String getOrDefault(String value, String defaultValue) {
    return !StringUtils.isEmpty(value) ? value : defaultValue;
  }

  @Override
  public ConsoleLogFilter createLoggerDecorator(Run<?, ?> build) {
    SonarInstallation inst = SonarInstallation.get(getInstallationName());
    if (inst == null) {
      return null;
    }

    Logger.LOG.info(Messages.SonarBuildWrapper_MaskingPasswords());

    List<String> passwords = new ArrayList<>();

    if (!StringUtils.isBlank(inst.getDatabasePassword())) {
      passwords.add(inst.getDatabasePassword());
    }
    if (!StringUtils.isBlank(inst.getSonarPassword())) {
      passwords.add(inst.getSonarPassword());
    }
    if (!StringUtils.isBlank(inst.getServerAuthenticationToken())) {
      passwords.add(inst.getServerAuthenticationToken());
    }

    return new SonarQubePasswordLogFilter(passwords, build.getCharset().name());
  }

  private static final class AddBuildInfo extends Disposer {

    private static final long serialVersionUID = 1L;

    private final SonarInstallation installation;
    private final FilePath reportDir;
    
    public AddBuildInfo(SonarInstallation installation, FilePath reportDir) {
      this.installation = installation;
      this.reportDir = reportDir;
    }

    @Override
    public void tearDown(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
      // null result means success so far. If no logs are found, it's probably because it was simply skipped
      //SonarUtils.addBuildInfoTo(build, workspace, installation.getName(), build.getResult() == null);
    	
    	// Read the report file
    	//
    	Properties reportProperties = new Properties();
    	if (reportDir.exists()) {
	    	FilePath reportFile = reportDir.child(SonarUtils.REPORT_TASK_FILE_NAME);
	    	if( reportFile.exists() ) {
	    	    try(InputStream in=reportFile.read()) {
	    	    	reportProperties.load(in);
	    	    }
	    	}
    	}
    	
    	// Create the SonaryAnalysisAction and add it to the build
    	//
        SonarAnalysisAction buildInfo = new SonarAnalysisAction(installation.getName());
        buildInfo.setUrl(     reportProperties.getProperty(SonarUtils.DASHBOARD_URL_KEY));
        buildInfo.setCeTaskId(reportProperties.getProperty(SonarUtils.CE_TASK_ID_KEY));
        build.addAction(buildInfo);

        
        // Dispose the reportDir
        //
        if (reportDir.exists()) {
        	reportDir.deleteRecursive();
        }
    }
  }

  private static class SonarQubePasswordLogFilter extends ConsoleLogFilter implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<String> passwords;
    private final String consoleCharset;

    public SonarQubePasswordLogFilter(List<String> passwords, String consoleCharset) {
      this.passwords = passwords;
      this.consoleCharset = consoleCharset;
    }

    @Override
    public OutputStream decorateLogger(Run ignore, OutputStream logger) throws IOException, InterruptedException {
      return new MaskPasswordsOutputStream(logger, Charset.forName(consoleCharset), passwords);
    }

  }

  /**
   * @return name of {@link hudson.plugins.sonar.SonarInstallation}
   */
  @Nullable
  public String getInstallationName() {
    return installationName;
  }

  public void setInstallationName(@Nullable String installationName) {
    this.installationName = installationName;
  }

  @Symbol("withSonarQubeEnv")
  @Extension
  public static final class DescriptorImpl extends BuildWrapperDescriptor {
    @Override
    public String getDisplayName() {
      return Messages.SonarBuildWrapper_DisplayName();
    }

    @Override
    public boolean isApplicable(AbstractProject<?, ?> item) {
      return Jenkins.getInstance().getDescriptorByType(SonarGlobalConfiguration.class).isBuildWrapperEnabled();
    }

    /**
     * @return all configured {@link hudson.plugins.sonar.SonarInstallation}
     */
    public SonarInstallation[] getSonarInstallations() {
      return SonarInstallation.all();
    }

    @Override
    public String getHelpFile() {
      return "/plugin/sonar/help-buildWrapper.html";
    }
  }

}
