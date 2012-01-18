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

/**
 * Factory to return the options for the different views for subtask list (All, Unresolved)
 *
 * @since v4.4
 */
public class SubTaskViewOptionsFactory implements SimpleLinkFactory
{
    private final VelocityRequestContextFactory requestContextFactory;
    private final JiraAuthenticationContext authenticationContext;

    public SubTaskViewOptionsFactory(VelocityRequestContextFactory requestContextFactory, JiraAuthenticationContext authenticationContext)
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
        final String subTaskView = (String) session.getAttribute(SessionKeys.SUB_TASK_VIEW);
        boolean showingAll = SubTaskBean.SUB_TASK_VIEW_DEFAULT.equals(SubTaskBean.SUB_TASK_VIEW_ALL);
        if (StringUtils.isNotBlank(subTaskView))
        {
            showingAll = subTaskView.equals(SubTaskBean.SUB_TASK_VIEW_ALL);
        }


        final SimpleLink allLink = new SimpleLinkImpl("subtasks-show-all", i18n.getText("viewissue.subtasks.tab.show.all.subtasks"), i18n.getText("viewissue.subtasks.tab.show.all.subtasks"),
                null, showingAll ? "aui-list-checked aui-checked" : "aui-list-checked", baseUrl + "/browse/" + issue.getKey() + "?subTaskView=all#issuetable", null);
        final SimpleLink openLink = new SimpleLinkImpl("subtasks-show-open", i18n.getText("viewissue.subtasks.tab.show.open.subtasks"), i18n.getText("viewissue.subtasks.tab.show.open.subtasks"),
                null, !showingAll ? "aui-list-checked aui-checked" : "aui-list-checked", baseUrl + "/browse/" + issue.getKey() + "?subTaskView=unresolved#issuetable", null);

        return CollectionBuilder.list(allLink, openLink);
    }
}

