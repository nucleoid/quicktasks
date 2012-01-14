package sbt.jira.plugins;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.atlassian.sal.api.user.UserManager;

@Path("/")
public class QuickTaskResource {
	private static final String STUPID_CHECKBOX_VALUE = "on";
	
	private final UserManager userManager;
    private final QuickTaskManager quickTaskManager;

    public QuickTaskResource(UserManager userManager, QuickTaskManager quickTaskManager)
    {
        this.userManager = userManager;
        this.quickTaskManager = quickTaskManager;
     }
    
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("id") String id, @Context HttpServletRequest request)
    {
        String username = userManager.getRemoteUsername(request);
        if (username != null && !userManager.isSystemAdmin(username))
            return Response.status(Status.UNAUTHORIZED).build();
        
        final Long issueId = Long.parseLong(id);
        List<sbt.jira.plugins.entities.QuickTask> tasks = quickTaskManager.findByIssueId(issueId);
        
        List<QuickTask> converted = new ArrayList<QuickTask>();
        
        for(sbt.jira.plugins.entities.QuickTask entityTask : tasks){
        	converted.add(new QuickTask(entityTask));
        }
        
        return Response.ok(converted).build();
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response post(final QuickTask quicktask, @Context HttpServletRequest request)
    {
        String username = userManager.getRemoteUsername(request);
        if (username != null && !userManager.isSystemAdmin(username))
            return Response.status(Status.UNAUTHORIZED).build();

        sbt.jira.plugins.entities.QuickTask taskCreated = 
        	quickTaskManager.add(quicktask.getIssueId(), quicktask.getDisplaySequence(), quicktask.getDescription(), quicktask.isCompleted());
        QuickTask converted = new QuickTask(taskCreated);
        
        return Response.ok(converted).build();
    }
    
    @POST
    @Path("{taskId}/move")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response post(@PathParam("taskId") int taskId, String position, @Context HttpServletRequest request)
    {
        String username = userManager.getRemoteUsername(request);
        if (username != null && !userManager.isSystemAdmin(username))
            return Response.status(Status.UNAUTHORIZED).build();

        sbt.jira.plugins.entities.QuickTask currentTask = quickTaskManager.findById(taskId);
        //position:   {"after":"/jira/rest/quicktasks/2/1"}
        Integer after = null;
        if(!position.equals("First"))
        	after = Integer.parseInt(position);
        currentTask = quickTaskManager.move(currentTask, after);
        QuickTask converted = new QuickTask(currentTask);
        
        return Response.ok(converted).build();
    }
    
    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response put(final QuickTask quicktask, @Context HttpServletRequest request)
    {
        String username = userManager.getRemoteUsername(request);
        if (username != null && !userManager.isSystemAdmin(username))
            return Response.status(Status.UNAUTHORIZED).build();

        sbt.jira.plugins.entities.QuickTask taskUpdated = 
        	quickTaskManager.update(quicktask.getId(), quicktask.getDisplaySequence(), quicktask.getDescription(), quicktask.isCompleted());
        QuickTask converted = new QuickTask(taskUpdated);
        
        return Response.ok(converted).build();
    }

    @DELETE
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") String id, @Context HttpServletRequest request)
    {
        String username = userManager.getRemoteUsername(request);
        if (username != null && !userManager.isSystemAdmin(username))
            return Response.status(Status.UNAUTHORIZED).build();

        final int taskId = Integer.parseInt(id);
        sbt.jira.plugins.entities.QuickTask task = quickTaskManager.findById(taskId);
        quickTaskManager.deleteQuickTask(task);
        
        return Response.noContent().build();
    }
    
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static final class QuickTask
    {
    	@XmlElement private int id;
        @XmlElement private Long issueId;
        @XmlElement private Long displaySequence;
        @XmlElement private String description;
        @XmlElement private Boolean completed;
        @XmlElement private String expand;
        @XmlElement private List<Operation> operations;
        
        public QuickTask(){
        	this.operations = new ArrayList<Operation>();
        	Operation delete = new Operation();
        	delete.setLabel("Delete");
        	delete.setStyleClass("aui-list-item-link quick-tasks-operations-delete");
        	this.operations.add(delete);
        }
        
        public QuickTask(sbt.jira.plugins.entities.QuickTask quickTask){
        	this();
        	this.id = quickTask.getID();
        	this.issueId = quickTask.getIssueId();
        	this.displaySequence = quickTask.getDisplaySequence();
        	this.description = quickTask.getDescription();
        	this.completed = quickTask.isCompleted();
        }
        
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public Long getIssueId() {
			return issueId;
		}
		public void setIssueId(Long issueId) {
			this.issueId = issueId;
		}
		public Long getDisplaySequence() {
			return displaySequence;
		}
		public void setDisplaySequence(Long displaySequence) {
			this.displaySequence = displaySequence;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public Boolean isCompleted() {
			return completed;
		}
		public void setCompleted(Boolean completed) {
			this.completed = completed;
		}

		public String getExpand() {
			return expand;
		}

		public void setExpand(String expand) {
			this.expand = expand;
		}

		public List<Operation> getOperations() {
			return operations;
		}

		public void setOperations(List<Operation> operations) {
			this.operations = operations;
		}
    }
    
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static final class Operation {
    	@XmlElement private String href;
    	@XmlElement private String styleClass;
    	@XmlElement private String label;
    	
		public String getHref() {
			return href;
		}
		public void setHref(String href) {
			this.href = href;
		}
		public String getStyleClass() {
			return styleClass;
		}
		public void setStyleClass(String styleClass) {
			this.styleClass = styleClass;
		}
		public String getLabel() {
			return label;
		}
		public void setLabel(String label) {
			this.label = label;
		}
    }
}