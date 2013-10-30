package hudson.plugins.jfr.result;

import java.io.File;

/**
 * @author wilson.wu
 */
public class FileGroup implements Comparable<FileGroup>
{
	private final String groupname;
	private final File[] files;
	private final int commonStrLen;

	/**
	 * @param groupname
	 * @param files
	 */
	public FileGroup(String groupname, File[] files, int commonStrLen)
	{
		super();
		this.groupname = groupname;
		this.files = files;
		this.commonStrLen = commonStrLen;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(FileGroup o)
	{
		return groupname.compareTo(o.getGroupname());
	}

	/**
	 * @return the commonStrLen
	 */
	public int getCommonStrLen()
	{
		return commonStrLen;
	}

	/**
	 * @return the files
	 */
	public File[] getFiles()
	{
		return files;
	}

	/**
	 * @return the groupname
	 */
	public String getGroupname()
	{
		return groupname;
	}

	public int size()
	{
		return files == null ? 0 : files.length;
	}
}
