package sbt.jira.plugins;

import java.util.Map;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.CacheableContextProvider;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.plugin.PluginParseException;

public class QuickTasksContextProvider implements CacheableContextProvider
{
	public static final String Quick_TASK_VIEW = "jira.user.quicktaskview";
	
    private final JiraAuthenticationContext authenticationContext;
    private final ApplicationProperties applicationProperties;

    public QuickTasksContextProvider(JiraAuthenticationContext authenticationContext, ApplicationProperties applicationProperties)
    {
        this.authenticationContext = authenticationContext;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void init(Map<String, String> params) throws PluginParseException
    {
    }


    @Override
    public String getUniqueContextKey(Map<String, Object> context)
    {
        final Issue issue = (Issue) context.get("issue");
        final User user = authenticationContext.getLoggedInUser();

        return issue.getId() + "/" + (user == null ? "" : user.getName());
    }

    @Override
    public Map<String, Object> getContextMap(Map<String, Object> context)
    {
        final MapBuilder<String, Object> paramsBuilder = MapBuilder.newBuilder(context);

        final Issue issue = (Issue) context.get("issue");

        paramsBuilder.add("issueKey", issue.getKey());
        paramsBuilder.add("issueId", issue.getId());
        paramsBuilder.add("applicationProperties", applicationProperties);
        return paramsBuilder.toMap();
    }
}
