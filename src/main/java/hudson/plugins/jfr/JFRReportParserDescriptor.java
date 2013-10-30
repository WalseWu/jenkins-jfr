package hudson.plugins.jfr;

import hudson.model.Descriptor;
import hudson.model.Hudson;

import java.util.List;

/**
 * @author wilson.wu
 */
public abstract class JFRReportParserDescriptor extends Descriptor<JFRReportParser>
{

	/**
	 * Returns all the registered {@link JFRReportParserDescriptor}s.
	 */
	public static List<JFRReportParserDescriptor> all()
	{
		return Hudson.getInstance().<JFRReportParser, JFRReportParserDescriptor> getDescriptorList(JFRReportParser.class);
	}

	public static JFRReportParserDescriptor getById(String id)
	{
		for (JFRReportParserDescriptor d : JFRReportParserDescriptor.all()) {
			if (d.getId().equals(id)) {
				return d;
			}
		}
		return null;
	}

	/**
	 * Internal unique ID that distinguishes a parser.
	 */
	@Override
	public final String getId()
	{
		return getClass().getName();
	}
}
