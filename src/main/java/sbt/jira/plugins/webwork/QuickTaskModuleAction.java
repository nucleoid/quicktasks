package sbt.jira.plugins.webwork;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.web.action.issue.CreateIssue;
import com.atlassian.jira.web.action.issue.IssueCreationHelperBean;

public class QuickTaskModuleAction extends CreateIssue
{
    private final IssueManager issueManager;

    private Long parentIssueId;

    public QuickTaskModuleAction(IssueManager issueManager, IssueCreationHelperBean issueCreationHelperBean, IssueFactory issueFactory)
    {
        super(issueFactory, issueCreationHelperBean);
        this.issueManager = issueManager;
    }

    @Override
    protected void doValidation()
    {
        try
        {
            issueCreationHelperBean.validateProject(getIssueObject(), this, ActionContext.getParameters(), this, this);
            if (!invalidInput())
            {
                getIssueObject().setProject(getProject());
            }

            issueCreationHelperBean.validateIssueType(getIssueObject(), this, ActionContext.getParameters(), this, this);
            if (!invalidInput())
            {
                getIssueObject().setIssueTypeId(getIssuetype());
            }
        }
        catch (Exception e)
        {
            log.error(e, e);
            addErrorMessage("An exception occurred: " + e + ".");
        }
    }
    
    @Override
    protected String doExecute() throws Exception
    {
        // validate their licence just in case they url hacked or came in via the create issue drop down
        // this overrides any other errors that may be in effect
        issueCreationHelperBean.validateLicense(this, this);
        if (hasAnyErrors())
        {
            return "invalidlicense";
        }

        // NOTE: this is passing null because the issueGV is null at this point and we can't
        // resolve a fieldLayoutItem to pass. For these two fields we are fine, since they are not renderable
        ProjectSystemField projectField = (ProjectSystemField) getField(IssueFieldConstants.PROJECT);
        projectField.updateIssue(null, getIssueObject(), getFieldValuesHolder());

        IssueTypeSystemField issueTypeField = (IssueTypeSystemField) getField(IssueFieldConstants.ISSUE_TYPE);
        issueTypeField.updateIssue(null, getIssueObject(), getFieldValuesHolder());

        // Store last issue type, so it can be set as the default in the next issue the user files
        recordHistoryIssueType();

        // Store last project, so it can be set as the default in the next issue the user files
        setSelectedProjectId(getPid());

        //populate custom field values holder with default values
        populateFieldHolderWithDefaults(getIssueObject(), Collections.EMPTY_LIST);

        return SUCCESS;
    }
    
    @Override
    public String doDefault() throws Exception
    {
        issueCreationHelperBean.validateLicense(this, this);
        if (hasAnyErrors())
        {
            return "invalidlicense";
        }
        // set the project to the recently selected one (as a sensible default)
        // if they've been browsing issues in a project, makes sense they would want to add to same project
        GenericValue current = getSelectedProject();

        Long requestedPid = getPid();

        if (current != null && getAllowedProjects().contains(current))
        {
            if (pid == null)
            {
                pid = current.getLong("id");
            }
            getFieldValuesHolder().put(IssueFieldConstants.PROJECT, pid);
        }

        String requestedIssueType = getIssuetype();
        setHistoryIssuetype();

        if (prepareFieldsIfOneOption(requestedPid, requestedIssueType))
        {
            return getRedirectForCreateBypass();
        }

        return super.doDefault();
    }
    
    protected String getRedirectForCreateBypass()
    {
        return forceRedirect("CreateQuickTask.jspa?parentIssueId=" + getParentIssueId() + "&pid=" + getPid());
    }

    /**
     * Checks if there is only one sub-task issue type for the project of the parent issue.
     * If this is the case, the custom field values holder will be populated with those values.
     *
     * @return true if the field population occurred; false otherwise
     */
    boolean prepareFieldsIfOneOption()
    {
        final MutableIssue parent = issueManager.getIssueObject(parentIssueId);
        if (parent == null)
        {
            return false;
        }

        // need this here to check permissions which were previously only checked when running the JSP
        if (getAllowedProjects().isEmpty())
        {
            return false;
        }

        final Project project = parent.getProjectObject();
        setPid(project.getId());
        getFieldValuesHolder().put(IssueFieldConstants.PROJECT, project.getId());
        return true;
    }

    public Long getParentIssueId()
    {
        return parentIssueId;
    }

    public void setParentIssueId(Long parentIssueId)
    {
        this.parentIssueId = parentIssueId;
    }

    public MutableIssue getIssueObject()
    {
        if (getIssueObjectWithoutDatabaseRead() == null)
        {
            MutableIssue issue = super.getIssueObject();
            issue.setParentId(getParentIssueId());
            issue.setProject(getProject());
        }

        return getIssueObjectWithoutDatabaseRead();
    }

    public Long getPid()
    {
        final Long parentIssueId = getParentIssueId();
        final Issue issue = issueManager.getIssueObject(parentIssueId);
        if (issue != null)
        {
            return issue.getLong("project");
        }
        else
        {
            log.error("Issue with id '" + parentIssueId + "' does not exist or could not be retrieved.");
            return null;
        }
    }

    public String getParentIssueKey()
    {
        final Issue parentIssue = getParentIssue();
        if (parentIssue != null)
        {
            return parentIssue.getKey();
        }

        return null;
    }

    public String getParentIssuePath()
    {
        return "/browse/" + getParentIssueKey();
    }

    private Issue getParentIssue()
    {
        return getIssueManager().getIssueObject(getParentIssueId());
    }

    public Map<String, Object> getDisplayParams()
    {
        final Map<String, Object> displayParams = new HashMap<String, Object>();
        displayParams.put("theme", "aui");
        return displayParams;
    }
}