package sbt.jira.plugins.quicktasks.webwork;

import java.util.Collection;
import java.util.List;

import org.springframework.util.CollectionUtils;

import com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;


public class QuickTaskAdminWebworkModuleAction   extends JiraWebActionSupport 
{
	public static String ANY_ISSUE_TYPE = "-1";
	
	private final PluginSettingsFactory pluginSettingsFactory;
	private final ProjectManager projectManager;
	private final IssueTypeManager issueTypeManager;
	
	private String[] issueTypes;
	private boolean global;
	private String[] projects;
	private boolean updated;
	
	public QuickTaskAdminWebworkModuleAction(PluginSettingsFactory pluginSettingsFactory, ProjectManager projectManager, 
			IssueTypeManager issueTypeManager){
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.projectManager = projectManager;
		this.issueTypeManager = issueTypeManager;
	}
	
	@Override
	protected void doValidation()
    {
		super.doValidation();
    }
	
	@Override
	@RequiresXsrfCheck
	public String doExecute() throws Exception
	{
		PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
		pluginSettings.put(QuickTaskAdminWebworkModuleAction.class.getName()  +".issueTypes", StringUtils.join(getIssueTypes(), ",") );
        pluginSettings.put(QuickTaskAdminWebworkModuleAction.class.getName()  +".projects", StringUtils.join(getProjects(), ","));
        pluginSettings.put(QuickTaskAdminWebworkModuleAction.class.getName()  +".globalProjectContext", Boolean.toString(isGlobal()));
        setUpdated(true);
    	return returnComplete();
	}
	
	@Override
	public String doDefault() throws Exception
    {
		if(!isSystemAdministrator())
			return LOGIN;

        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        String issueTypes = (String) settings.get(QuickTaskAdminWebworkModuleAction.class.getName() + ".issueTypes");
        String projects = (String) settings.get(QuickTaskAdminWebworkModuleAction.class.getName() + ".projects");
        String globalProjectContext = (String) settings.get(QuickTaskAdminWebworkModuleAction.class.getName() + ".globalProjectContext");
        if(issueTypes != null)
        	setIssueTypes(StringUtils.split(issueTypes, ","));
        if(projects != null)
        	setProjects(StringUtils.split(projects, ","));
        if (globalProjectContext != null)
            setGlobal(Boolean.parseBoolean(globalProjectContext));
        setUpdated(false);
		return INPUT;
    }
	
	public String getBaseUrl(){
		return getApplicationProperties().getString(APKeys.JIRA_BASEURL);
	}
	
    public String[] getIssueTypes() {
		return issueTypes;
	}

	public void setIssueTypes(String[] issueTypes) {
		this.issueTypes = issueTypes;
	}

	public boolean isGlobal() {
		return global;
	}

	public void setGlobal(boolean global) {
		this.global = global;
	}

	public String[] getProjects() {
		return projects;
	}

	public void setProjects(String[] projects) {
		this.projects = projects;
	}

	public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }
    
    public List<Project> getAllProjects(){
    	return projectManager.getProjectObjects();
    }
    
    public Collection<IssueType> getAllIssueTypes(){
    	return issueTypeManager.getIssueTypes();
    }
    
    public String issueTypeSelected(String id){
    	if(getIssueTypes() != null && id != null && checkListForValue(CollectionUtils.arrayToList(getIssueTypes()), id))
    		return "selected=\"selected\"";
    	return "";
    }
    
    public String projectSelected(Project project){
    	if(!isGlobal() && getProjects() != null && project != null && checkListForValue(CollectionUtils.arrayToList(getProjects()), project.getId().toString()))
    		return "selected=\"selected\"";
    	return "";
    }
    
    public String globalChecked(String value){
    	if(Boolean.valueOf(value) == isGlobal())
    		return "checked=\"checked\"";
    	return "";
    }
    
    private boolean checkListForValue(List list, Object object){
    	for(Object obj : list){
    		if(obj.equals(object))
    			return true;
    	}
    	return false;
    }
}
