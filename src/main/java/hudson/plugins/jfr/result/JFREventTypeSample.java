package hudson.plugins.jfr.result;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.jrockit.mc.flightrecorder.spi.IField;

/**
 * @author wilson.wu
 */
public class JFREventTypeSample
{
	private final Map<JFREventField, JFREventFieldSample> sampleMap = new HashMap<JFREventField, JFREventFieldSample>();

	public JFREventTypeSample(IField[] iFields, Set<String> filenames)
	{
		for (String name : filenames) {
			for (IField field : iFields) {
				if (field != null) {
					JFREventField jfrField = new JFREventField(name, field);
					JFREventFieldSample fieldSample = sampleMap.get(jfrField);
					if (fieldSample == null) {
						String suffix = "".equals(name) ? "" : " - " + name;
						fieldSample = new JFREventFieldSample(field.getName() + suffix);
						sampleMap.put(jfrField, fieldSample);
					}
				}
			}
		}
	}

	public Set<JFREventField> getEventFieldSet()
	{
		return sampleMap.keySet();
	}

	/**
	 * 
	 */
	public JFREventFieldSample getJFREventSample(JFREventField key)
	{
		return sampleMap.get(key);
	}

	public Collection<JFREventFieldSample> getSampleCollection()
	{
		return sampleMap.values();
	}
}
