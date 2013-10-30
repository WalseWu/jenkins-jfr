package hudson.plugins.jfr.result;

/**
 * @author wilson.wu
 */
public class JFREventFieldValue implements Comparable<JFREventFieldValue>
{
	private final long startTimestamp;
	private final long endTimestamp;
	private final long duration;
	private final Object value;

	public JFREventFieldValue(long startTimestamp, long endTimestamp, long duration, Object value)
	{
		this.startTimestamp = startTimestamp;
		this.endTimestamp = endTimestamp;
		this.duration = duration;
		this.value = value;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(JFREventFieldValue obj)
	{
		return (int) (getStartTimestamp() - obj.getStartTimestamp());
	}

	public long getDuration()
	{
		return duration;
	}

	public long getEndTimestamp()
	{
		return endTimestamp;
	}

	public long getStartTimestamp()
	{
		return startTimestamp;
	}

	public double getValue()
	{
		if (value instanceof String) {
			Double.valueOf((String) value);
		}
		if (value instanceof Float) {
			return ((Float) value).doubleValue();
		}
		if (value instanceof Long) {
			return ((Long) value).doubleValue();
		}
		if (value instanceof Integer) {
			return ((Integer) value).doubleValue();
		}

		return (Double) value;
	}
}
