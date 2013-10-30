package hudson.plugins.jfr;

import java.util.Arrays;

/**
 * @author wilson.wu
 */
public final class JFREventSetting
{

	private final String eventPath;

	private final String[] fieldIds;

	public JFREventSetting(String eventPath, String[] fieldIds)
	{
		super();
		this.eventPath = eventPath;
		this.fieldIds = fieldIds;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		JFREventSetting other = (JFREventSetting) obj;
		if (eventPath == null) {
			if (other.eventPath != null) {
				return false;
			}
		}
		else if (!eventPath.equals(other.eventPath)) {
			return false;
		}
		return true;
	}

	public String getEventPath()
	{
		return eventPath;
	}

	public String[] getFieldIds()
	{
		return fieldIds;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (eventPath == null ? 0 : eventPath.hashCode());
		return result;
	}

	@Override
	public String toString()
	{
		return "JFREventSetting [eventPath=" + eventPath + ", fieldIds=" + Arrays.toString(fieldIds) + "]";
	}

}
