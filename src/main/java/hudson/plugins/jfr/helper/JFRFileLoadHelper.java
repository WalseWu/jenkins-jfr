package hudson.plugins.jfr.helper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

import com.jrockit.mc.flightrecorder.FlightRecording;
import com.jrockit.mc.flightrecorder.FlightRecordingLoader;

/**
 * @author wilson.wu
 */
public final class JFRFileLoadHelper
{
	private static final int BUFF_SIZE = 1024 * 1024 * 2;

	/**
	 * Load a jfr file which is under gzip style, temp file will be used in the mothod, don't use this method if the jfr is not a
	 * gzip style
	 * 
	 * @param f
	 * @return
	 * @throws IOException
	 */
	public static FlightRecording loadJFRGzipFile(File f) throws IOException
	{
		GZIPInputStream gzipStream = null;
		try {
			gzipStream = new GZIPInputStream(new BufferedInputStream(new FileInputStream(f)));
		}
		catch (IOException e) {
			return FlightRecordingLoader.loadFile(f);
		}
		FlightRecording recording = null;
		File tempFile = File.createTempFile(f.getName().substring(0, f.getName().indexOf(".")), ".jfr");
		try {
			OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile));
			try {
				byte[] buf = new byte[BUFF_SIZE];
				int len = 0;
				while ((len = gzipStream.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				out.flush();
				recording = FlightRecordingLoader.loadFile(tempFile);
			}
			finally {
				out.close();
			}
		}
		finally {
			if (tempFile.exists() && !tempFile.delete()) {
				tempFile.deleteOnExit();
			}
			gzipStream.close();
		}
		return recording;
	}
}
