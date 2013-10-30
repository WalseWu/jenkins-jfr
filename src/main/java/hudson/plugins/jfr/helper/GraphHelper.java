package hudson.plugins.jfr.helper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import hudson.plugins.jfr.Messages;
import hudson.plugins.jfr.result.JFREventField;
import hudson.plugins.jfr.result.JFREventFieldSample;
import hudson.plugins.jfr.result.JFREventFieldValue;
import hudson.plugins.jfr.result.JFRReport;
import hudson.plugins.jfr.result.JFRReport.ReportDataSet;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import com.jrockit.mc.flightrecorder.spi.IEventType;

/**
 * @author wilson.wu
 */
public final class GraphHelper
{
	private static class JFRTimeSeriesCollection extends TimeSeriesCollection
	{
		private static final long serialVersionUID = 1L;
		private String lable;

		public JFRTimeSeriesCollection()
		{
		}

		public String getLable()
		{
			return lable;
		}

		public void setLable(String lable)
		{
			this.lable = lable;
		}

	}

	public static JFRTimeSeriesCollection createJfrEventDataSet(Collection<JFREventFieldSample> samples)
	{
		final JFRTimeSeriesCollection dataset = new JFRTimeSeriesCollection();
		TimeSeries[] series = new TimeSeries[samples.size()];
		int i = 0;
		for (JFREventFieldSample sample : samples) {
			series[i] = new TimeSeries(sample.getType(), Millisecond.class);
			Set<JFREventFieldValue> values = sample.getValues();
			for (JFREventFieldValue v : values) {
				//				Day time = new Day(new Date(v.getStartTimestamp()));
				Millisecond time = new Millisecond(new Date(v.getStartTimestamp()));
				series[i].add(time, v.getValue());
			}
			dataset.addSeries(series[i]);
			i++;
		}
		return dataset;
	}

	/**
	 * Create a JFreeChart for the given JFRReport
	 * 
	 * @param r
	 * @return
	 */
	public static JFreeChart createOverlappingJFREventChart(JFRReport r)
	{
		Set<IEventType> eventTypes = r.getEventTypes();
		ReportDataSet[] dataSets = new ReportDataSet[eventTypes.size()];
		int index = 0;
		for (IEventType type : eventTypes) {
			JFRTimeSeriesCollection ds = GraphHelper.createJfrEventDataSet(r.getEventTypeSample(type).getSampleCollection());
			GraphHelper.setJFRTimeSeriesCollectionlabel(ds, r.getEventTypeSample(type).getEventFieldSet());
			dataSets[index++] = new ReportDataSet(ds, type);
		}
		if (dataSets.length == 0) {
			XYDataset ds = new TimeSeriesCollection();
			return ChartFactory.createTimeSeriesChart(Messages.Report_GraphLabel(), null, "No Value", ds, true, true, false);
		}

		//Primary Y-Axis
		JFreeChart localJFreeChart = ChartFactory.createTimeSeriesChart(Messages.Report_GraphLabel(), null, dataSets[0].getType()
				.getName() + ((JFRTimeSeriesCollection) dataSets[0].getDataset()).getLable(), dataSets[0].getDataset(), true,
				true, false);
		XYPlot localXYPlot = (XYPlot) localJFreeChart.getPlot();
		localXYPlot.setOrientation(PlotOrientation.VERTICAL);
		//		localXYPlot..setDomainPannable(true);
		//		localXYPlot.setRangePannable(true);
		localXYPlot.getRenderer().setSeriesPaint(0, Color.black);

		//Add y-axis
		for (int i = 1; i < dataSets.length; ++i) {
			NumberAxis localNumberAxis = new NumberAxis(dataSets[i].getType().getName()
					+ ((JFRTimeSeriesCollection) dataSets[i].getDataset()).getLable());
			localNumberAxis.setAutoRangeIncludesZero(true);
			localXYPlot.setRangeAxis(i, localNumberAxis);

			localXYPlot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_LEFT);

			localXYPlot.setDataset(i, dataSets[i].getDataset());
			localXYPlot.mapDatasetToRangeAxis(i, i);

			StandardXYItemRenderer localStandardXYItemRenderer = new StandardXYItemRenderer();
			localXYPlot.setRenderer(i, localStandardXYItemRenderer);
			localStandardXYItemRenderer.setSeriesPaint(i, GraphHelper.getColor(i, dataSets.length));
			localNumberAxis.setLabelPaint(GraphHelper.getColor(i, dataSets.length));
			localNumberAxis.setTickLabelPaint(GraphHelper.getColor(i, dataSets.length));
		}
		localXYPlot.setOrientation(PlotOrientation.VERTICAL);
		//		localXYPlot.setDomainPannable(true);
		//		localXYPlot.setRangePannable(true);
		localXYPlot.setBackgroundPaint(Color.WHITE);

		localXYPlot.setOutlinePaint(null);

		localXYPlot.getDomainAxis().setLowerMargin(0.0);
		localXYPlot.getDomainAxis().setUpperMargin(0.0);

		localXYPlot.getRangeAxis().setAutoTickUnitSelection(true);

		localXYPlot.getRenderer().setBaseStroke(new BasicStroke(4.0f));

		localJFreeChart.setBackgroundPaint(Color.white);

		localJFreeChart.getLegend().setPosition(RectangleEdge.BOTTOM);
		return localJFreeChart;
	}

	/**
	 * Exports a JFreeChart to a SVG file.
	 * 
	 * @param chart
	 *            JFreeChart to export
	 * @param bounds
	 *            the dimensions of the viewport
	 * @param svgFile
	 *            the output file.
	 * @throws IOException
	 *             if writing the svgFile fails.
	 */
	public static void exportChartAsSVG(JFreeChart chart, int width, int height, StaplerRequest req, StaplerResponse rsp)
			throws IOException
	{
		int w = width, h = height;

		String widthStr = req.getParameter("width");
		try {
			w = Integer.valueOf(widthStr);
		}
		catch (Exception e) {
			w = width;
		}
		String heightStr = req.getParameter("height");
		try {
			h = Integer.valueOf(heightStr);
		}
		catch (Exception e) {
			h = height;
		}

		h = h <= 0 ? Toolkit.getDefaultToolkit().getScreenSize().height - 260 : h;
		w = w <= 0 ? Toolkit.getDefaultToolkit().getScreenSize().width - 280 : w;

		Rectangle bounds = new Rectangle(w, h);
		// Get a DOMImplementation and create an XML document
		DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
		Document document = domImpl.createDocument(null, "svg", null);

		// Create an instance of the SVG Generator
		SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

		// draw the chart in the SVG generator
		chart.draw(svgGenerator, bounds);

		// Write svg file
		OutputStream outputStream = rsp.getOutputStream();
		Writer out = new OutputStreamWriter(outputStream, "UTF-8");
		svgGenerator.stream(out, true);
		outputStream.flush();
		out.flush();
	}

	/**
	 * Get one value of {@value #COLORS} by index
	 * 
	 * @param index
	 *            should >= 0
	 * @return
	 */
	public static Color getColor(int seriesIndex, int totalSeries)
	{
		float hue = (float) seriesIndex / (float) totalSeries;
		int steps = 4;
		float saturation = seriesIndex % steps / (2f * steps) + 0.5f;
		float brightness = (seriesIndex + (steps + 1) / 2f) % (steps + 1f) / (2f * (steps + 1f)) + 0.5f;
		return Color.getHSBColor(hue, saturation, brightness);
	}

	/**
	 * @param eventFieldSet
	 */
	private static void setJFRTimeSeriesCollectionlabel(JFRTimeSeriesCollection ds, Set<JFREventField> eventFieldSet)
	{
		for (JFREventField field : eventFieldSet) {
			String label = JFREventHelper.getEventFieldExtraLabel(field.getField());
			if (label != null) {
				ds.setLable(label);
				return;
			}
			ds.setLable("");
		}
	}
}
