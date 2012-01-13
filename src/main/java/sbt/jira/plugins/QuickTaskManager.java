package sbt.jira.plugins;

import java.util.Collection;
import java.util.List;

import org.ofbiz.core.entity.GenericValue;

import sbt.jira.plugins.bean.QuickTaskBean;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.exception.RemoveException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.util.IssueUpdateBean;

public interface QuickTaskManager {
	
	public static final String QUICK_TASK_LINK_TYPE_NAME = "quickTaskLinkType";
	public static final String QUICK_TASK_LINK_TYPE_OUTWARD_NAME = "quickTaskLinkTypeOutward";
	public static final String QUICK_TASK_LINK_TYPE_INWARD_NAME = "quickTaskLinkTypeInward";
	public static final String QUICK_TASK_LINK_TYPE_STYLE = "quickTaskLinkTypeStyle";
	public static final String QUICK_TASK_ISSUE_TYPE_STYLE = "quickTaskIssueTypeStyle";
	
	public static final String JIRA_OPTION_ALLOWQUICKTASKS = "jira.option.allquicktasks";
	
	public void enableQuickTasks()  throws CreateException;
	public void disableQuickTasks();
	public boolean isQuickTasksEnabled();
	public boolean isQuickTask(GenericValue issue);
	public Long getParentIssueId(GenericValue issue);
	public QuickTaskBean getQuickTaskBean(Issue issue, User remoteUser);
	public Collection<Issue> getQuickTaskObjects(Issue parentIssue);
	public IssueUpdateBean changeParent(Issue quickTask, Issue parentIssue, User currentUser) throws RemoveException, CreateException;
}
