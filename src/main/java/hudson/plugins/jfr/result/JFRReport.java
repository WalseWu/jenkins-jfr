package hudson.plugins.jfr.result;

import hudson.model.AbstractBuild;
import hudson.plugins.jfr.JFRBuildAction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jfree.data.xy.XYDataset;

import com.jrockit.mc.flightrecorder.spi.IEventType;
import com.jrockit.mc.flightrecorder.spi.IField;

/**
 * @author wilson.wu
 */
public class JFRReport implements Comparable<JFRReport>
{

	public static class ReportDataSet
	{
		private final XYDataset dataset;
		private final IEventType type;

		public ReportDataSet(XYDataset dataset, IEventType type)
		{
			super();
			this.dataset = dataset;
			this.type = type;
		}

		public XYDataset getDataset()
		{
			return dataset;
		}

		public IEventType getType()
		{
			return type;
		}
	}

	private String matchedFrom;
	private String reportFileName;
	private JFRBuildAction buildAction;

	private final int width;
	private final int height;

	private final Map<IEventType, JFREventTypeSample> eventTypeMap = new HashMap<IEventType, JFREventTypeSample>();

	private boolean initialized = false;

	/**
	 * @param width
	 * @param height
	 */
	public JFRReport(int width, int height)
	{
		this.width = width;
		this.height = height;
	}

	public int compareTo(JFRReport jfrReport)
	{
		if (jfrReport == this) {
			return 0;
		}
		int globEq = getMatchedFrom().compareTo(jfrReport.getMatchedFrom());
		return globEq == 0 ? getReportFileName().compareTo(jfrReport.getReportFileName()) : globEq;
	}

	public AbstractBuild<?, ?> getBuild()
	{
		return buildAction.getBuild();
	}

	public Set<IEventType> getEventTypes()
	{
		return eventTypeMap.keySet();
	}

	public JFREventTypeSample getEventTypeSample(IEventType key)
	{
		return eventTypeMap.get(key);
	}

	/**
	 * @return the height
	 */
	public int getHeight()
	{
		return height;
	}

	public String getMatchedFrom()
	{
		return matchedFrom;
	}

	public String getReportFileName()
	{
		return reportFileName;
	}

	/**
	 * @return the width
	 */
	public int getWidth()
	{
		return width;
	}

	public void initializeMap(Map<IEventType, IField[]> events, Set<String> filenames)
	{
		Set<IEventType> eventTypeSet = events.keySet();
		for (IEventType eventType : eventTypeSet) {
			JFREventTypeSample eventTypeSample = eventTypeMap.get(eventType);
			if (eventTypeSample == null) {
				eventTypeSample = new JFREventTypeSample(events.get(eventType), filenames);
				eventTypeMap.put(eventType, eventTypeSample);
			}
		}
		initialized = true;
	}

	public boolean isInitialized()
	{
		return initialized;
	}

	public void setBuildAction(JFRBuildAction buildAction)
	{
		this.buildAction = buildAction;
	}

	public void setGlobalName(String glob, FileGroup fileGroup, String title)
	{
		matchedFrom = glob;
		int groupSize = fileGroup.size();
		if (groupSize == 1) {
			setReportFileName(fileGroup.getFiles()[0].getName());
		}
		else {
			setReportFileName(title);
		}
	}

	public int size()
	{
		return eventTypeMap.keySet().size();
	}

	private void setReportFileName(String reportFileName)
	{
		this.reportFileName = reportFileName;
	}

	JFRBuildAction getBuildAction()
	{
		return buildAction;
	}
}
