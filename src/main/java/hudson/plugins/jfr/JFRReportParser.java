package hudson.plugins.jfr;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.plugins.jfr.helper.JFREventHelper;
import hudson.plugins.jfr.helper.JFRFileLoadHelper;
import hudson.plugins.jfr.result.FileGroup;
import hudson.plugins.jfr.result.JFREventField;
import hudson.plugins.jfr.result.JFREventFieldValue;
import hudson.plugins.jfr.result.JFRReport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.stapler.DataBoundConstructor;

import com.jrockit.mc.flightrecorder.FlightRecording;
import com.jrockit.mc.flightrecorder.spi.EventOrder;
import com.jrockit.mc.flightrecorder.spi.IEvent;
import com.jrockit.mc.flightrecorder.spi.IEventType;
import com.jrockit.mc.flightrecorder.spi.IField;
import com.jrockit.mc.flightrecorder.spi.IView;

/**
 * @author wilson.wu
 */
public class JFRReportParser implements Describable<JFRReportParser>, ExtensionPoint
{
	@Extension
	public static class DescriptorImpl extends JFRReportParserDescriptor
	{
		@Override
		public String getDisplayName()
		{
			return "JFRReport";
		}
	}

	private static final Logger logger = Logger.getLogger(JFRReportParser.class.getName());

	public static final String GLOB_PATTERN_STR = "(?://*[^//]+//)*([^//]+)(?:.jfr)$";

	private static final Pattern GLOB_PATTERN = Pattern.compile(GLOB_PATTERN_STR);

	private static final int DEFAULT_WIDTH = 600;
	private static final int DEFAULT_HEIGHT = 400;

	/**
	 * All registered implementations.
	 */
	public static ExtensionList<JFRReportParser> all()
	{
		return Hudson.getInstance().getExtensionList(JFRReportParser.class);
	}

	/**
	 * GLOB patterns that specify the jfr report.
	 */
	public final String glob;

	public final String title;

	public final int width;

	public final int height;

	/**
	 * JRockit Flight Recording events values which will be show on the graphs.
	 */
	public final String jfrEventSettingStr;

	private String displayName;

	/**
	 * Always accessing by {@link #getJfrEventSettings()}
	 */
	private JFREventSetting[] jfrEventSettings;

	@DataBoundConstructor
	public JFRReportParser(String glob, String jfrEventSettingStr, String title, int width, int height)
	{
		this.glob = glob == null || glob.length() == 0 ? getDefaultGlobPattern() : glob;
		this.jfrEventSettingStr = jfrEventSettingStr == null || jfrEventSettingStr.length() == 0 ? getDefaultJFREventsPattern()
				: jfrEventSettingStr.trim();
		resetDisplayName();
		this.title = title == null || title.length() == 0 ? "" : title;
		this.width = width <= 0 ? DEFAULT_WIDTH : width;
		this.height = height <= 0 ? DEFAULT_HEIGHT : height;
	}

	public String getDefaultGlobPattern()
	{
		return "**/*.jfr";
	}

	public String getDefaultJFREventsPattern()
	{
		return "os/cpu_load:user,system,total; vm/prof/memory_usage:total:init, total:used;";
	}

	@SuppressWarnings("unchecked")
	public Descriptor<JFRReportParser> getDescriptor()
	{
		return Hudson.getInstance().getDescriptorOrDie(getClass());
	}

	public String getDisplayName()
	{
		logger.info("glob: " + glob + ", displayName: " + displayName);
		if (displayName == null) {
			resetDisplayName();
		}
		return displayName;
	}

	public String getGlobFileName()
	{
		Matcher m = GLOB_PATTERN.matcher(glob);

		if (m.find()) {
			return m.group(0);
		}
		return null;
	}

	/**
	 * @return the height
	 */
	public int getHeight()
	{
		return height;
	}

	public JFREventSetting[] getJfrEventSettings()
	{
		if (jfrEventSettings == null || jfrEventSettings.length == 0) {
			jfrEventSettings = JFREventHelper.parseJFREventSettingStr(getJfrEventSettingStr());
		}
		return jfrEventSettings;
	}

	public String getJfrEventSettingStr()
	{
		return jfrEventSettingStr;
	}

	public String getReportName()
	{
		return this.getClass().getName().replaceAll("^.*\\.(\\w+)Parser.*$", "$1");
	}

	/**
	 * @return the title
	 */
	public String getTitle()
	{
		return title;
	}

	/**
	 * @return the width
	 */
	public int getWidth()
	{
		return width;
	}

	/**
	 * Split the jfr file collection to several groups according the configured {@link #glob}, files in the same group will be
	 * merged in the graph
	 * 
	 * @param reports
	 * @return
	 */
	public Set<FileGroup> groupFiles(Collection<File> reports)
	{
		//aaa/bbb/*/ccc/A.B.C.XXX_*.jfr
		//aaa/bbb/*/ccc/blah.blah.AllTests-*.jfr
		// -> ^blah.blah.AllTests-(.*).jfr$
		Set<FileGroup> result = new TreeSet<FileGroup>();
		String globFileName = getGlobFileName();
		if (globFileName.matches("(\\*)+.jfr")) {
			//only one group
			File[] files = new File[reports.size()];
			reports.toArray(files);
			result.add(new FileGroup(globFileName, files, 0));
		}
		else {
			//blah.blah.AllTests-*.jfr  ==> blah.blah.AllTests-(.*).jfr
			String rGlobFileName = globFileName.replaceAll("[\\*]+", "(.*)");
			List<File> matchedFiles = new ArrayList<File>();
			int commonStrLen = Math.max(0, rGlobFileName.indexOf("(.*)"));
			for (File report : reports) {
				if (report.getName().matches(rGlobFileName)) {
					matchedFiles.add(report);
				}
				else {
					result.add(new FileGroup(report.getName(), new File[] { report }, 0));
				}
			}
			File[] fs = new File[matchedFiles.size()];
			matchedFiles.toArray(fs);
			result.add(new FileGroup(globFileName, fs, commonStrLen));
		}
		return result;
	}

	/**
	 * Match the JFR Event Setting string from configuration page
	 * 
	 * @return true if the setting can be matched by the pattern, otherwise return false
	 */
	public boolean isEventPatternMatched()
	{
		String[] events = JFREventHelper.splitJfrEvents(jfrEventSettingStr);
		boolean result = false;
		for (String event : events) {
			result = event.matches(JFREventHelper.EVENT_PATTERN_STR);
			if (!result) {
				break;
			}
		}
		return result;
	}

	/**
	 * Parse a set of Report files.
	 * 
	 * @param build
	 * @param reports
	 *            a set of *.jfr files
	 * @param listener
	 * @return
	 * @throws IOException
	 */
	public Collection<JFRReport> parse(AbstractBuild<?, ?> build, Collection<File> reports, TaskListener listener)
			throws IOException
	{
		Set<FileGroup> groupReports = groupFiles(reports);
		List<JFRReport> result = new LinkedList<JFRReport>();

		for (FileGroup fg : groupReports) {
			JFRReport r = new JFRReport(width, height);
			r.setGlobalName(glob, fg, getTitle());
			parseEventsReport(fg, r, getJfrEventSettings());
			result.add(r);
		}
		Collections.sort(result);
		return result;
	}

	public void setJfrEventSettings(JFREventSetting[] jfrEventSettings)
	{
		this.jfrEventSettings = jfrEventSettings;
	}

	public boolean validateGlobPattern()
	{
		return glob.endsWith(".jfr");
	}

	private void parseEventsReport(FileGroup fg, final JFRReport r, JFREventSetting[] eventSettings) throws IOException
	{
		if (fg == null || fg.getFiles().length == 0) {
			return;
		}

		Map<String, FlightRecording> map = new HashMap<String, FlightRecording>();
		for (File f : fg.getFiles()) {
			//get rid of the duplicated sub string of the file name
			String suffixName = f.getName().substring(fg.getCommonStrLen())
					.replace(getGlobFileName().replaceAll("[\\*]+", ""), "").replaceAll(".jfr", "");
			map.put(suffixName, JFRFileLoadHelper.loadJFRGzipFile(f));
		}
		Set<String> filenames = map.keySet();
		Map<IEventType, IField[]> events = null;
		for (String name : filenames) {
			FlightRecording recording = map.get(name);
			if (!r.isInitialized()) {
				events = JFREventHelper.parseJFREventSettingObj(recording, eventSettings);
				r.initializeMap(events, filenames);
			}
			IView view = recording.createView();
			view.setEventTypes(events.keySet());
			view.setOrder(EventOrder.ASCENDING);

			for (IEvent event : view) {
				long startTimestamp = event.getStartTimestamp();
				long endTimestamp = event.getEndTimestamp();
				long duration = event.getDuration();
				IEventType eventType = event.getEventType();
				IField[] fields = events.get(eventType);
				for (IField field : fields) {
					if (field != null) {
						if (JFREventHelper.getEventFieldValue(event, field) == null) {
							logger.warning("the jfr event field " + field.getIdentifier()
									+ " is null, you may config the wrong field for the graph.");
						}
						else {
							JFREventFieldValue efv = new JFREventFieldValue(startTimestamp / 1000000, endTimestamp / 1000000,
									duration / 1000000, JFREventHelper.getEventFieldValue(event, field));
							r.getEventTypeSample(eventType).getJFREventSample(new JFREventField(name, field)).addFieldValue(efv);
						}
					}
				}
			}
		}
	}

	/**
	 * The display name is used for the folder name where report files placed.
	 */
	private void resetDisplayName()
	{
		if (glob.indexOf("/") < 0) {
			displayName = getGlobFileName().replaceAll("[\\*]+", "hqia").replaceAll(".jfr", "");
			return;
		}
		String globFileName = getGlobFileName();
		int gl = globFileName.length();
		String folder = glob.substring(0, glob.length() - gl - 1).replaceAll("[\\*]+", "hpia");
		String prefix = folder.substring(folder.lastIndexOf("/") + 1).concat(".").concat(globFileName.substring(0, gl - 4));
		displayName = prefix.replaceAll("[/\\\\]", ".").replaceAll("[\\*]+", "hqia");
	}
}
