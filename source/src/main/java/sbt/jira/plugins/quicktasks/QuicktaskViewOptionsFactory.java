package sbt.jira.plugins.quicktasks;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bean.SubTaskBean;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.SimpleLinkFactory;
import com.atlassian.jira.plugin.webfragment.descriptors.SimpleLinkFactoryModuleDescriptor;
import com.atlassian.jira.plugin.webfragment.model.SimpleLink;
import com.atlassian.jira.plugin.webfragment.model.SimpleLinkImpl;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.collect.CollectionBuilder;
import com.atlassian.jira.util.velocity.VelocityRequestContext;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.jira.util.velocity.VelocityRequestSession;
import com.atlassian.jira.web.SessionKeys;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

public class QuicktaskViewOptionsFactory implements SimpleLinkFactory
{
	public static final String QUICK_TASK_VIEW_ALL = "all";
	public static final String QUICK_TASK_VIEW_INCOMPLETE = "incomplete";
	public static final String QUICK_TASK_VIEW = "jira.user.quicktaskview";
	
    private final VelocityRequestContextFactory requestContextFactory;
    private final JiraAuthenticationContext authenticationContext;

    public QuicktaskViewOptionsFactory(VelocityRequestContextFactory requestContextFactory, JiraAuthenticationContext authenticationContext)
    {
        this.requestContextFactory = requestContextFactory;
        this.authenticationContext = authenticationContext;
    }

    @Override
    public void init(SimpleLinkFactoryModuleDescriptor descriptor)
    {
    }

    @Override
    public List<SimpleLink> getLinks(User user, Map<String, Object> params)
    {
        final VelocityRequestContext requestContext = requestContextFactory.getJiraVelocityRequestContext();
        final I18nHelper i18n = authenticationContext.getI18nHelper();
        final Issue issue = (Issue) params.get("issue");


        final VelocityRequestSession session = requestContext.getSession();
        final String baseUrl = requestContext.getBaseUrl();
        final String quickTaskView = (String) session.getAttribute(QUICK_TASK_VIEW);
        boolean showingAll = true;
        if (StringUtils.isNotBlank(quickTaskView))
        {
            showingAll = quickTaskView.equals(QUICK_TASK_VIEW_ALL);
        }


        final SimpleLink allLink = new SimpleLinkImpl("quicktasks-show-all", i18n.getText("quicktasks.tab.show.all.quicktasks"), i18n.getText("quicktasks.tab.show.all.quicktasks"),
                null, showingAll ? "aui-list-checked aui-checked quicktasks-all" : "aui-list-checked quicktasks-all", "#", null);
        final SimpleLink openLink = new SimpleLinkImpl("quicktasks-show-open", i18n.getText("quicktasks.tab.show.open.quicktasks"), i18n.getText("quicktasks.tab.show.open.quicktasks"),
                null, !showingAll ? "aui-list-checked aui-checked quicktasks-incomplete" : "aui-list-checked quicktasks-incomplete", "#", null);

        return CollectionBuilder.list(allLink, openLink);
    }
}