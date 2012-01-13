package sbt.jira.plugins;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericValue;

import sbt.jira.plugins.bean.QuickTaskBean;
import sbt.jira.plugins.bean.QuickTaskBeanImpl;

import com.atlassian.core.util.collection.EasyList;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.exception.RemoveException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.util.IssueUpdateBean;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.CollectionReorderer;

public class QuickTaskManagerImpl implements QuickTaskManager
{
    private final IssueLinkManager issueLinkManager;
    private final PermissionManager permissionManager;
    private final ApplicationProperties applicationProperties;

    public QuickTaskManagerImpl(IssueLinkManager issueLinkManager, PermissionManager permissionManager,
            ApplicationProperties applicationProperties)
    {
        this.issueLinkManager = issueLinkManager;
        this.permissionManager = permissionManager;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Turn on sub-tasks by creating a sub-task issue link type
     * and a default sub-task issue type
     */
    @Override
    public void enableQuickTasks() throws CreateException
    {
        // Update the application property
        applicationProperties.setOption(JIRA_OPTION_ALLOWQUICKTASKS, true);
    }

    @Override
    public void disableQuickTasks()
    {
        applicationProperties.setOption(JIRA_OPTION_ALLOWQUICKTASKS, false);
    }

    @Override
    public boolean isQuickTasksEnabled()
    {
        return applicationProperties.getOption(JIRA_OPTION_ALLOWQUICKTASKS);
    }

    @Override
    public boolean isQuickTask(GenericValue issue)
    {
        return (getParentIssueId(issue) != null);
    }

    @Override
    public Long getParentIssueId(GenericValue issue)
    {
        ensureIssueNotNull(issue);

        // Check if we have any incoming sub-task issue links
        final List<IssueLink> inwardLinks = issueLinkManager.getInwardLinks(issue.getLong("id"));
        for (final IssueLink inwardLink : inwardLinks)
        {
            if (inwardLink.getIssueLinkType().isSubTaskLinkType())
            {
                return inwardLink.getLong("source");
            }
        }
        return null;
    }

    @Override
    public QuickTaskBean getQuickTaskBean(Issue issue, User remoteUser)
    {
        QuickTaskBeanImpl subTaskBean = new QuickTaskBeanImpl();

        final Collection<IssueLink> subTaskIssueLinks = getQuickTaskIssueLinks(issue.getLong("id"));
        for (final IssueLink subTaskIssueLink : subTaskIssueLinks)
        {
            Issue subTaskIssue = subTaskIssueLink.getDestinationObject();
            // Check that the remote user has the permissions to actually see the sub-task due to issue level security.
            // Even though we keep the issue security level the same on sub-tasks and parent issues
            // due to things like 'assignee' and 'reporter' permissions, it is possible to have
            // a situation where a user can see the parent issue but not its sub task, or vice versa
            if (permissionManager.hasPermission(Permissions.BROWSE, subTaskIssue, remoteUser))
            {
                subTaskBean.addSubTask(subTaskIssueLink.getSequence(), subTaskIssue, issue);
            }
        }

        return subTaskBean;
    }

    private void ensureIssueNotNull(GenericValue issue)
    {
        if (issue == null)
        {
            throw new IllegalArgumentException("Issue cannot be null.");
        }
        else if (!"Issue".equals(issue.getEntityName()))
        {
            throw new IllegalArgumentException("The argument must be an issue.");
        }
    }

    /**
     * Get an issue's subtasks.
     *
     * @return A collection of {@link Issue}s.
     */
    @Override
    public Collection<Issue> getQuickTaskObjects(Issue parentIssue)
    {
        Collection<Issue> subTaskIssues = new LinkedList<Issue>();
        for (final IssueLink issueLink : getQuickTaskIssueLinks(parentIssue.getId()))
        {
            subTaskIssues.add(issueLink.getDestinationObject());
        }

        return subTaskIssues;

    }

    @Override
    public IssueUpdateBean changeParent(Issue quickTask, Issue newParentIssue, User currentUser)
            throws RemoveException, CreateException
    {
    	Issue oldParentIssue = quickTask.getParentObject();
        Collection<IssueLink> inwardLinks = issueLinkManager.getInwardLinks(quickTask.getLong("id"));
        for (final IssueLink issueLink : inwardLinks)
        {
            if (issueLink.getIssueLinkType().isSubTaskLinkType())
            {
                issueLinkManager.removeIssueLink(issueLink, currentUser);
            }
        }

        // Create change item for subtask
        ChangeItemBean cibParent = new ChangeItemBean(ChangeItemBean.CUSTOM_FIELD, "Parent Issue",
                oldParentIssue.getString("key"), oldParentIssue.getString("key"),
                newParentIssue.getString("key"), newParentIssue.getString("key"));

        GenericValue newQuickTask = (GenericValue) quickTask.getGenericValue().clone();
        //JRA-10546 - the sub task must have the same security level as its parent
        //Note, the IssueUpdater takes care of generating the change history (by checking which fields
        //have changed from the old to the new issue)
        newQuickTask.set(IssueFieldConstants.SECURITY, newParentIssue.getLong(IssueFieldConstants.SECURITY));

        IssueUpdateBean issueUpdateBean = new IssueUpdateBean(newQuickTask, quickTask.getGenericValue(), EventType.ISSUE_UPDATED_ID, currentUser);
        issueUpdateBean.setChangeItems(EasyList.build(cibParent));
        return issueUpdateBean;
    }
}