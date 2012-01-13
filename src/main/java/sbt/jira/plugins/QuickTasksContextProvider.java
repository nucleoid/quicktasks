package sbt.jira.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import sbt.jira.plugins.entities.QuickTask;
import sbt.jira.plugins.entities.QuickTaskBean;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.fields.FieldException;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.layout.column.ColumnLayoutItem;
import com.atlassian.jira.issue.fields.layout.column.ColumnLayoutStorageException;
import com.atlassian.jira.issue.util.AggregateTimeTrackingCalculatorFactory;
import com.atlassian.jira.plugin.webfragment.CacheableContextProvider;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.jira.util.velocity.VelocityRequestContext;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.jira.util.velocity.VelocityRequestSession;
import com.atlassian.jira.web.ExecutingHttpRequest;
import com.atlassian.jira.web.component.IssueTableLayoutBean;
import com.atlassian.jira.web.component.IssueTableWebComponent;
import com.atlassian.jira.web.component.TableLayoutUtils;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.query.order.SearchSort;
import com.atlassian.velocity.VelocityManager;

/**
 * Context Provider for the subtask section on view issue.  Is Cacheable.
 *
 * @since v4.4
 */
public class QuickTasksContextProvider implements CacheableContextProvider
{
	public static final String Quick_TASK_VIEW = "jira.user.quicktaskview";
	
    private final QuickTaskManager quickTaskManager;
    private final JiraAuthenticationContext authenticationContext;
    private final ApplicationProperties applicationProperties;
    private final FieldManager fieldManager;
    private final PermissionManager permissionManager;
    private final IssueManager issueManager;
    private final IssueFactory issueFactory;
    private final VelocityManager velocityManager;
    private final VelocityRequestContextFactory velocityRequestContextFactory;
    private final AggregateTimeTrackingCalculatorFactory aggregateTimeTrackingCalculatorFactory;
    private final TableLayoutUtils tableLayoutUtils;
    private final ColumnLayoutItemFactory columnLayoutItemFactory;

    public QuickTasksContextProvider(QuickTaskManager quickTaskManager, JiraAuthenticationContext authenticationContext,
            ApplicationProperties applicationProperties, FieldManager fieldManager, PermissionManager permissionManager,
            IssueManager issueManager, IssueFactory issueFactory, VelocityManager velocityManager,
            VelocityRequestContextFactory velocityRequestContextFactory, TableLayoutUtils tableLayoutUtils,
            AggregateTimeTrackingCalculatorFactory aggregateTimeTrackingCalculatorFactory, ColumnLayoutItemFactory columnLayoutItemFactory)
    {
        this.quickTaskManager = quickTaskManager;
        this.authenticationContext = authenticationContext;
        this.applicationProperties = applicationProperties;
        this.fieldManager = fieldManager;
        this.permissionManager = permissionManager;
        this.issueManager = issueManager;
        this.issueFactory = issueFactory;
        this.velocityManager = velocityManager;
        this.velocityRequestContextFactory = velocityRequestContextFactory;
        this.aggregateTimeTrackingCalculatorFactory = aggregateTimeTrackingCalculatorFactory;
        this.tableLayoutUtils = tableLayoutUtils;
        this.columnLayoutItemFactory = columnLayoutItemFactory;
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
        final User user = authenticationContext.getLoggedInUser();

        final VelocityRequestContext requestContext = velocityRequestContextFactory.getJiraVelocityRequestContext();
        final String selectedIssueId = requestContext.getRequestParameter("selectedIssueId");

        paramsBuilder.add("hasQuickTasks", !quicktaskBean.getQuickTasks(getQuickTaskView()).isEmpty());
        paramsBuilder.add("issueKey", issue.getKey());
        paramsBuilder.add("issueId", issue.getId());

        return paramsBuilder.toMap();
    }

    private boolean isEditable(Issue issue, User user)
    {
        return permissionManager.hasPermission(Permissions.EDIT_ISSUE, issue, user) && issueManager.isEditable(issue);
    }

    private QuickTaskBean getQuickTaskBean(Issue issue, Map<String, Object> context)
    {
        final HttpServletRequest request = getRequest(context);
        if (request != null)
        {
        	QuickTaskBean quickTaskBean = (QuickTaskBean) request.getAttribute("sbt.jira.plugin.quicktask.bean." + issue.getKey());
            if (quickTaskBean != null)
            {
                return quickTaskBean;
            }
            quickTaskBean = quickTaskManager.getQuickTaskBean(issue, authenticationContext.getLoggedInUser());
            request.setAttribute("sbt.jira.plugin.quicktask.bean." + issue.getKey(), quickTaskBean);
            return quickTaskBean;
        }

        return quickTaskManager.getQuickTaskBean(issue, authenticationContext.getLoggedInUser());
    }

    private String getQuickTaskView()
    {
        final VelocityRequestSession session = velocityRequestContextFactory.getJiraVelocityRequestContext().getSession();

        final String quickTaskView = (String) session.getAttribute(Quick_TASK_VIEW);
        return StringUtils.isNotBlank(quickTaskView) ? quickTaskView : QuickTaskBean.QUICK_TASK_VIEW_DEFAULT;

    }

    private String getTableHtml(Issue issue, User user, Map<String, Object> context)
    {
        QuickTaskBean quickTaskBean = getQuickTaskBean(issue, context);
        String quickTaskView = getQuickTaskView();
        Collection<QuickTask> issues = quickTaskBean.getQuickTasks(quickTaskView);
        List<Issue> issueObjects = new ArrayList<Issue>();

        for (QuickTask quickTask : issues)
        {
            Issue quickTaskIssue = quickTask.getQuickTask();
            issueObjects.add(quickTaskIssue);
        }

        IssueTableWebComponent issueTable = new IssueTableWebComponent();
        IssueTableLayoutBean layout = null;
        try
        {
            layout = getQuickTaskIssuesLayout(user, issue, quickTaskBean, quickTaskView);
        }
        catch (ColumnLayoutStorageException e)
        {
            throw new RuntimeException(e);
        }
        catch (FieldException e)
        {
            throw new RuntimeException(e);
        }

        return issueTable.getHtml(layout, issueObjects, null);
    }

    public class QuickTaskTableRenderer
    {
        private final Issue issue;
        private final User user;
        private final Map<String, Object> context;


        public QuickTaskTableRenderer(Issue issue, User user, Map<String, Object> context)
        {
            this.user = user;
            this.issue = issue;
            this.context = context;
        }

        public String getHtml()
        {
            return getTableHtml(issue, user, context);

        }
    }

    protected HttpServletRequest getRequest(Map<String, Object> context)
    {
        return ExecutingHttpRequest.get();
    }
    
    public IssueTableLayoutBean getQuickTaskIssuesLayout(User user, final Issue parentIssue, final QuickTaskBean quickTaskBean, final String quickTaskView) throws ColumnLayoutStorageException, FieldException
    {
        final ColumnLayoutItem displaySequence = columnLayoutItemFactory.getQuickTaskDisplaySequenceColumn();
        final ColumnLayoutItem simpleSummary = columnLayoutItemFactory.getQuickTaskSimpleSummaryColumn();
        final ColumnLayoutItem subTaskReorder = columnLayoutItemFactory.getQuickTaskReorderColumn(user, parentIssue, quickTaskBean, quickTaskView);

        final List<ColumnLayoutItem> columns = new ArrayList<ColumnLayoutItem>();
        columns.add(displaySequence);
        columns.add(simpleSummary);
        columns.add(subTaskReorder);

        final IssueTableLayoutBean layout = new IssueTableLayoutBean(columns, Collections.<SearchSort>emptyList());
        layout.setSortingEnabled(false);
        layout.setDisplayHeader(false);
        layout.setShowExteriorTable(false);
        layout.setTableCssClass(""); //override the grid CSS class
        layout.setShowActionColumn(true);
        return layout;
    }
}
