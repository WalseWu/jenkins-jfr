package hudson.plugins.jfr;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author wilson.wu
 */
public class JFRPublisher extends Recorder
{

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher>
	{
		@Override
		public String getDisplayName()
		{
			return Messages.Publisher_DisplayName();
		}

		@Override
		public String getHelpFile()
		{
			return "/plugin/hudson-jfr/help.html";
		}

		public List<JFRReportParserDescriptor> getParserDescriptors()
		{
			return JFRReportParserDescriptor.all();
		}

		@Override
		public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType)
		{
			return true;
		}
	}

	/**
	 * Configured report parseres.
	 */
	private List<JFRReportParser> parsers;

	@DataBoundConstructor
	public JFRPublisher(List<? extends JFRReportParser> parsers)
	{
		if (parsers == null) {
			parsers = Collections.emptyList();
		}
		this.parsers = new ArrayList<JFRReportParser>(parsers);
	}

	public List<JFRReportParser> getParsers()
	{
		return parsers;
	}

	@Override
	public Action getProjectAction(AbstractProject<?, ?> project)
	{
		// return JFRProjectAction for main page extension
		return null;
	}

	public BuildStepMonitor getRequiredMonitorService()
	{
		return BuildStepMonitor.BUILD;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException,
			IOException
	{
		PrintStream logger = listener.getLogger();

		// add the report to the build object.
		JFRBuildAction buildAction = new JFRBuildAction(build, logger, parsers);
		build.addAction(buildAction);

		for (JFRReportParser parser : parsers) {
			String glob = parser.glob;
			logger.println("JFR: Recording " + parser.getReportName() + " reports '" + glob + "'");

			List<FilePath> files = locateJFRReports(build.getWorkspace(), glob, logger);

			if (files.isEmpty()) {
				// build.setResult(Result.FAILURE);
				logger.println("JFR: no " + parser.getReportName() + " files matching '" + glob
						+ "' have been found. Has the report generated?. Setting Build to " + build.getResult());
				return true;
			}
			copyReportsToMaster(build, logger, files, parser.getDisplayName());
		}

		return true;
	}

	public void setParsers(List<JFRReportParser> parsers)
	{
		this.parsers = parsers;
	}

	private List<File> copyReportsToMaster(AbstractBuild<?, ?> build, PrintStream logger, List<FilePath> files, String displayName)
			throws IOException, InterruptedException
	{
		List<File> localReports = new ArrayList<File>();
		for (FilePath src : files) {
			logger.println("JFR: File " + src.getName() + ": " + displayName);
			final File localReport = getJFRReport(build, displayName, src.getName());
			if (src.isDirectory()) {
				logger.println("JFR: File '" + src.getName() + "' is a directory, not a JFR Report");
				continue;
			}
			src.copyTo(new FilePath(localReport));
			localReports.add(localReport);
		}
		return localReports;
	}

	private File getJFRReport(AbstractBuild<?, ?> build, String displayName, String jfrReportName)
	{
		String reportFilePath = JFRReportMap.getJFRReportFileRelativePath(displayName, jfrReportName);
		return new File(build.getRootDir(), reportFilePath);
	}

	private List<FilePath> locateJFRReports(FilePath workspace, String glob, PrintStream logger) throws IOException,
			InterruptedException
	{
		// First use ant-style pattern
		try {
			FilePath[] ret = workspace.list(glob);
			if (ret.length > 0) {
				return Arrays.asList(ret);
			}
		}
		catch (IOException e) {
		}

		// If it fails, do a legacy search
		ArrayList<FilePath> files = new ArrayList<FilePath>();
		String parts[] = glob.split("\\s*[;:,]+\\s*");
		logger.println("glob: " + glob);
		for (String part : parts) {
			logger.println("part: " + part);
		}
		for (String path : parts) {
			FilePath src = workspace.child(path);
			logger.println("workspace: " + src.getName());
			if (src.exists()) {
				logger.println("src: " + src.getBaseName());
				if (src.isDirectory()) {
					logger.println(src.list("**/*"));
					files.addAll(Arrays.asList(src.list("**/*")));
				}
				else {
					files.add(src);
				}
			}
		}
		return files;
	}

}
