package hudson.plugins.jfr;

import hudson.model.ModelObject;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.plugins.jfr.helper.GraphHelper;
import hudson.plugins.jfr.result.JFRReport;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.jfree.chart.JFreeChart;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @author wilson.wu
 */
public class JFRReportMap implements ModelObject
{
	private static final Logger logger = Logger.getLogger(JFRReportMap.class.getName());

	public static String getJFRReportDirRelativePath()
	{
		return JFRReportMap.getRelativePath();
	}

	public static String getJFRReportFileRelativePath(String parserDisplayName, String reportFileName)
	{
		return JFRReportMap.getRelativePath(parserDisplayName, reportFileName);
	}

	private static String getRelativePath(String... suffixes)
	{
		StringBuilder sb = new StringBuilder(200);
		sb.append(JFR_REPORTS_DIRECTORY);
		for (String suffix : suffixes) {
			sb.append(File.separator).append(suffix);
		}
		return sb.toString();
	}

	/**
	 * The {@link JFRBuildAction} that this report belongs to.
	 */
	private transient JFRBuildAction buildAction;
	private String reportType;

	/**
	 * {@link JFRReport}s are keyed by {@link JFRReport#reportFileName} Test names are arbitrary human-readable and URL-safe
	 * string that identifies an individual report.
	 */
	private Map<String, JFRReport> jfrReportMap = new LinkedHashMap<String, JFRReport>();
	private static final String JFR_REPORTS_DIRECTORY = "JFR-reports";

	/**
	 * Parses the reports and build a {@link JFRReportMap}.
	 * 
	 * @throws IOException
	 *             If a report fails to parse.
	 */
	JFRReportMap(final JFRBuildAction buildAction, TaskListener listener) throws IOException
	{
		this.buildAction = buildAction;
		parseReports(getBuild(), listener, null);
	}

	public void doJfrEventTimeGraph(StaplerRequest request, StaplerResponse response) throws IOException
	{
		try {
			String parameter = request.getParameter("jfrReportPosition");
			JFreeChart chart = GraphHelper.createOverlappingJFREventChart(getJFRReport(parameter));
			GraphHelper.exportChartAsSVG(chart, getJFRReport(parameter).getWidth(), getJFRReport(parameter).getHeight(), request,
					response);
		}
		catch (Exception e) {
			String newline = System.getProperty("line.separator");
			//Set exception message back for the failed test.
			StringWriter sb = new StringWriter(2048);
			sb.append(e.getMessage());
			sb.append(newline);
			e.printStackTrace(new PrintWriter(sb, true));
			logger.info(sb.toString());
		}
	}

	public AbstractBuild<?, ?> getBuild()
	{
		return buildAction.getBuild();
	}

	public String getDisplayName()
	{
		return Messages.Report_DisplayName();
	}

	public List<JFRReport> getJFRListOrdered()
	{
		List<JFRReport> listJFR = new ArrayList<JFRReport>(getJFRReportMap().values());
		logger.info("Ordered JFR Reports:" + listJFR.size());
		Collections.sort(listJFR);
		return listJFR;
	}

	/**
	 * <p>
	 * Give the JFR report with the parameter for name in Bean
	 * </p>
	 * 
	 * @param JFRReportName
	 * @return
	 */
	public JFRReport getJFRReport(String JFRReportName)
	{
		return jfrReportMap.get(JFRReportName);
	}

	public Map<String, JFRReport> getJFRReportMap()
	{
		return jfrReportMap;
	}

	public String getReportType()
	{
		return reportType;
	}

	public String getUrlName()
	{
		return "JFRReportList";
	}

	/**
	 * <p>
	 * Verify if the JFRReport exist the JFRReportName must to be like it is in the build
	 * </p>
	 * 
	 * @param JFRReportName
	 * @return boolean
	 */
	public boolean isFailed(String JFRReportName)
	{
		return getJFRReport(JFRReportName) == null;
	}

	public void setJFRReportMap(Map<String, JFRReport> jfrReportMap)
	{
		this.jfrReportMap = jfrReportMap;
	}

	public void setReportType(String reportType)
	{
		this.reportType = reportType;
	}

	private void addAll(Collection<JFRReport> reports)
	{
		for (JFRReport r : reports) {
			r.setBuildAction(buildAction);
			jfrReportMap.put(r.getReportFileName(), r);
		}
	}

	private void parseReports(AbstractBuild<?, ?> build, TaskListener listener, final String filename) throws IOException
	{
		File repo = new File(build.getRootDir(), JFRReportMap.getJFRReportDirRelativePath());
		File[] dirs = repo.listFiles(new FileFilter() {
			public boolean accept(File f)
			{
				return f.isDirectory();
			}
		});
		// this may fail, if the build itself failed, we need to recover
		// gracefully
		if (dirs != null) {
			for (File dir : dirs) {
				JFRReportParser p = buildAction.getParserByDisplayName(dir.getName());
				if (p != null) {
					File[] listFiles = dir.listFiles(new FilenameFilter() {

						public boolean accept(File dir, String name)
						{
							if (filename == null) {
								return true;
							}
							if (name.equals(filename)) {
								return true;
							}
							return false;
						}
					});
					addAll(p.parse(build, Arrays.asList(listFiles), listener));
				}
			}
		}
	}

	JFRBuildAction getBuildAction()
	{
		return buildAction;
	}

	void setBuildAction(JFRBuildAction buildAction)
	{
		this.buildAction = buildAction;
	}
}
