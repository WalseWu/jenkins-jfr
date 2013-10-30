package hudson.plugins.jfr.result;

import java.util.Set;
import java.util.TreeSet;

/**
 * A JFEEventSample represents a series in the report graph
 * 
 * @author wilson.wu
 */
public class JFREventFieldSample
{
	private final Comparable<?> type;
	private final Set<JFREventFieldValue> values;

	public JFREventFieldSample(Comparable<?> type)
	{
		this.type = type;
		values = new TreeSet<JFREventFieldValue>();
	}

	public void addFieldValue(JFREventFieldValue value)
	{
		values.add(value);
	}

	public Comparable<?> getType()
	{
		return type;
	}

	public Set<JFREventFieldValue> getValues()
	{
		return values;
	}
}
