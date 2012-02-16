package sbt.jira.plugins.quicktasks.conditions;

import java.util.List;

import org.springframework.util.CollectionUtils;

import sbt.jira.plugins.quicktasks.webwork.QuickTaskAdminWebworkModuleAction;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractIssueCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.project.Project;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class HasQuickTasksPermission extends AbstractIssueCondition 
{
	private final PluginSettingsFactory pluginSettingsFactory;
	
	public HasQuickTasksPermission(PluginSettingsFactory pluginSettingsFactory)
	{
		this.pluginSettingsFactory = pluginSettingsFactory;
	}

	@Override
	public boolean shouldDisplay(User user, Issue issue, JiraHelper helper) {
		PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
		String issueTypes = (String) settings.get(QuickTaskAdminWebworkModuleAction.class.getName() + ".issueTypes");
        String projects = (String) settings.get(QuickTaskAdminWebworkModuleAction.class.getName() + ".projects");
        String globalProjectContext = (String) settings.get(QuickTaskAdminWebworkModuleAction.class.getName() + ".globalProjectContext"); 

		IssueType issueType = issue.getIssueTypeObject();
		Project project = issue.getProjectObject();
		
		List issueTypeList = issueTypes != null ? CollectionUtils.arrayToList(StringUtils.split(issueTypes, ",")) : null;
		List projectList = projects != null ? CollectionUtils.arrayToList(StringUtils.split(projects, ",")) : null;
        boolean global = globalProjectContext != null ? Boolean.parseBoolean(globalProjectContext) : true;
        
        if(isIssueTypeAllowed(issueType, issueTypeList) && isProjectAllowed(global, projectList, project))
        	return true;
		return false;
	}

	private boolean isIssueTypeAllowed(IssueType issueType, List issueTypeList) {
		return issueTypeList == null ||  checkListForValue(issueTypeList, QuickTaskAdminWebworkModuleAction.ANY_ISSUE_TYPE) || checkListForValue(issueTypeList, issueType.getId());
	}
	
	private boolean isProjectAllowed(boolean global, List projectList, Project project){
		return global || (projectList != null && checkListForValue(projectList, project.getId().toString()));
	}
	
	private boolean checkListForValue(List list, Object object){
    	for(Object obj : list){
    		if(obj.equals(object))
    			return true;
    	}
    	return false;
    }

}
