package hudson.plugins.jfr.result;

import com.jrockit.mc.flightrecorder.spi.IField;

/**
 * @author wilson.wu
 */
public class JFREventField
{
	private final String testId;
	private final IField field;

	/**
	 * @param testId
	 * @param field
	 */
	public JFREventField(String testId, IField field)
	{
		super();
		this.testId = testId;
		this.field = field;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
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
		JFREventField other = (JFREventField) obj;
		if (field == null) {
			if (other.field != null) {
				return false;
			}
		}
		else if (!field.equals(other.field)) {
			return false;
		}
		if (testId == null) {
			if (other.testId != null) {
				return false;
			}
		}
		else if (!testId.equals(other.testId)) {
			return false;
		}
		return true;
	}

	/**
	 * @return the field
	 */
	public IField getField()
	{
		return field;
	}

	/**
	 * @return the testId
	 */
	public String getTestId()
	{
		return testId;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (field == null ? 0 : field.hashCode());
		result = prime * result + (testId == null ? 0 : testId.hashCode());
		return result;
	}

}
