package hudson.plugins.jfr;

import hudson.model.Action;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.util.StreamTaskListener;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.StaplerProxy;

/**
 * @author wilson.wu
 */
public class JFRBuildAction implements Action, StaplerProxy
{

	private final AbstractBuild<?, ?> build;
	private final List<JFRReportParser> parsers;

	private transient final PrintStream hudsonConsoleWriter;

	private transient WeakReference<JFRReportMap> jfrReportMap;

	private static final Logger logger = Logger.getLogger(JFRBuildAction.class.getName());

	public JFRBuildAction(AbstractBuild<?, ?> pBuild, PrintStream logger, List<JFRReportParser> parsers)
	{
		build = pBuild;
		hudsonConsoleWriter = logger;
		this.parsers = parsers;
	}

	public AbstractBuild<?, ?> getBuild()
	{
		return build;
	}

	public String getDisplayName()
	{
		return Messages.BuildAction_DisplayName();
	}

	public PrintStream getHudsonConsoleWriter()
	{
		return hudsonConsoleWriter;
	}

	public String getIconFileName()
	{
		return "graph.gif";
	}

	public JFRReportParser getParserByDisplayName(String displayName)
	{
		if (parsers != null) {
			for (JFRReportParser parser : parsers) {
				if (parser.getDisplayName().equals(displayName)) {
					return parser;
				}
			}
		}
		return null;
	}

	public List<JFRReportParser> getParsers()
	{
		return parsers;
	}

	public Object getTarget()
	{
		return getJfrReportMap();
	}

	public String getUrlName()
	{
		return "JFRReport";
	}

	public void setJfrReportMap(WeakReference<JFRReportMap> jfrReportMap)
	{
		this.jfrReportMap = jfrReportMap;
	}

	private JFRReportMap getJfrReportMap()
	{
		JFRReportMap reportMap = null;
		WeakReference<JFRReportMap> wr = jfrReportMap;
		if (wr != null) {
			reportMap = wr.get();
			if (reportMap != null) {
				return reportMap;
			}
		}
		try {
			reportMap = new JFRReportMap(this, new StreamTaskListener(System.err, Computer.currentComputer().getDefaultCharset()));
		}
		catch (IOException e) {
			logger.log(Level.SEVERE, "Error creating new JFRReportMap()", e);
		}
		jfrReportMap = new WeakReference<JFRReportMap>(reportMap);
		return reportMap;
	}
}
