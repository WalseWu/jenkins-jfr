package hudson.plugins.jfr.helper;

import hudson.plugins.jfr.JFREventSetting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jrockit.mc.flightrecorder.FlightRecording;
import com.jrockit.mc.flightrecorder.spi.IEvent;
import com.jrockit.mc.flightrecorder.spi.IEventType;
import com.jrockit.mc.flightrecorder.spi.IField;

/**
 * @author wilson.wu
 */
public final class JFREventHelper
{
	// os/cpu_load:user,system,total; vm/prof/memory_usage:total:init,total:used
	public static final String EVENT_PATTERN_STR = "([\\w/_]+)\\s*:\\s*([^(\\s|,|;)]+(\\s*,\\s*[^(\\s|,|;)]+)*)";

	public static final String EVENTS_SEPERATOR = "(\\s*;\\s*)+";

	public static final String EVENT_FIELDS_SEPERATOR = "(\\s*,\\s*)+";

	public static final Pattern EVENT_PATTERN = Pattern.compile(EVENT_PATTERN_STR);

	public static final String EVENT_FIELD_NUMBER_TYPE = "NUMERIC";

	public static final String EVENT_FIELD_PERCENTAGE_CONTENT = "percentage";

	public static final String EVENT_FIELD_MEMOERY = "memory";

	public static final String MEMORY_LABLE = "(M)";

	public static final String PERCENTAGE_LABLE = "(%)";

	public static String getEventFieldExtraLabel(IField field)
	{
		if (EVENT_FIELD_PERCENTAGE_CONTENT.equals(field.getContentType())) {
			return PERCENTAGE_LABLE;
		}
		else if (EVENT_FIELD_MEMOERY.equals(field.getContentType())) {
			return MEMORY_LABLE;
		}
		return null;
	}

	/**
	 * Get the field value of the event
	 * 
	 * @param event
	 * @param field
	 * @return
	 */
	public static Object getEventFieldValue(IEvent event, IField field)
	{
		Double value = JFREventHelper.getDoubleValue(field.getValue(event));
		if (value == null) {
			return value;
		}
		if (EVENT_FIELD_PERCENTAGE_CONTENT.equals(field.getContentType())) {
			value *= 100;
		}
		else if (EVENT_FIELD_MEMOERY.equals(field.getContentType())) {
			value = value / 1024 / 1024;
		}
		return value;
	}

	/**
	 * Parse a set of JFREventSettingS
	 * 
	 * @param jfrRecording
	 * @param eventSettings
	 * @return
	 */
	public static Map<IEventType, IField[]> parseJFREventSettingObj(FlightRecording jfrRecording, JFREventSetting[] eventSettings)
	{
		Map<IEventType, IField[]> events = new HashMap<IEventType, IField[]>();
		for (JFREventSetting eventSetting : eventSettings) {
			inner: for (IEventType type : jfrRecording.getEventTypes()) {
				if (type.getPath().equals(eventSetting.getEventPath())) {
					IField[] fields = new IField[eventSetting.getFieldIds().length];
					for (int i = 0; i < fields.length; ++i) {
						fields[i] = JFREventHelper.getFieldByIdentifier(type, eventSetting.getFieldIds()[i]);
					}
					events.put(type, fields);
					break inner;
				}
			}
		}
		return events;
	}

	/**
	 * Analyze the JFREventSettings from setting string
	 * 
	 * @return
	 */
	public static JFREventSetting[] parseJFREventSettingStr(String jfrEventSettingStr)
	{
		Matcher m = EVENT_PATTERN.matcher(jfrEventSettingStr);
		Set<JFREventSetting> esSet = new HashSet<JFREventSetting>();
		while (m.find()) {
			JFREventSetting es = new JFREventSetting(m.group(1), m.group(2).split(EVENT_FIELDS_SEPERATOR));
			esSet.add(es);
		}
		JFREventSetting[] result = new JFREventSetting[esSet.size()];
		esSet.toArray(result);
		return result;
	}

	/**
	 * Split the event setting string by {@link #EVENTS_SEPERATOR}
	 * 
	 * @param jfrEventSettingStr
	 * @return String[]
	 */
	public static String[] splitJfrEvents(String jfrEventSettingStr)
	{
		return jfrEventSettingStr.split(EVENTS_SEPERATOR);
	}

	private static double getDoubleValue(Object value)
	{
		Double result = null;
		if (value instanceof Number) {
			result = ((Number) value).doubleValue();
		}

		return result;
	}

	/**
	 * Get IField from the given IEventType by identifier
	 * 
	 * @param eventType
	 * @param identifier
	 * @return null is no valid IField existed in the given IEventType
	 */
	private static IField getFieldByIdentifier(IEventType eventType, String identifier)
	{
		for (IField field : eventType.getFields()) {
			if (identifier.equals(field.getIdentifier())) {
				return field;
			}
		}
		return null;
	}
}
