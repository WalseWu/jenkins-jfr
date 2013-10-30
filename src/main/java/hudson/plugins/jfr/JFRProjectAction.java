package hudson.plugins.jfr;

import hudson.model.Action;
import hudson.model.AbstractProject;
import hudson.model.Job;

import java.io.IOException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @author wilson.wu
 */
public class JFRProjectAction implements Action
{

	private static final String PLUGIN_NAME = "jfr-plugin";

	public final AbstractProject<?, ?> project;

	public JFRProjectAction(AbstractProject<?, ?> project)
	{
		this.project = project;
	}

	public void doRespondingTimeGraph(StaplerRequest request, StaplerResponse response) throws IOException
	{
	}

	public String getDisplayName()
	{
		return "Project Action";
	}

	public String getIconFileName()
	{
		return "graph.gif";
	}

	public Job<?, ?> getProject()
	{
		return project;
	}

	public String getUrlName()
	{
		return PLUGIN_NAME;
	}

}
