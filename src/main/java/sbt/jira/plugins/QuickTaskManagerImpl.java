package sbt.jira.plugins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.java.ao.Query;
import sbt.jira.plugins.entities.QuickTask;

import com.atlassian.activeobjects.external.ActiveObjects;


public class QuickTaskManagerImpl implements QuickTaskManager
{
	private final ActiveObjects ao;
	
    public QuickTaskManagerImpl(ActiveObjects ao) {
    	this.ao = ao;
    }

	@Override
	public QuickTask add(Long issueId, Long displaySequence,
			String description, boolean completed) {
		if(displaySequence == null)
			displaySequence = Long.parseLong(findByIssueId(issueId).size()+ "");
		final QuickTask quicktask = ao.create(QuickTask.class);
		quicktask.setIssueId(issueId);
		quicktask.setDisplaySequence(displaySequence);
		quicktask.setDescription(description);
		quicktask.setCompleted(completed);
		quicktask.save();
        return quicktask;
	}
	
	@Override
	public QuickTask update(int id, Long displaySequence,
			String description, Boolean completed) {
		final QuickTask quicktask = findById(id);
		if(displaySequence != null)
			quicktask.setDisplaySequence(displaySequence);
		if(description != null)
			quicktask.setDescription(description);
		if(completed != null)
			quicktask.setCompleted(completed);
		quicktask.save();
        return quicktask;
	}
	
	@Override
	public QuickTask update(QuickTask task) {
		task.save();
        return task;
	}

	@Override
	public QuickTask findById(int id) {
		return ao.get(QuickTask.class, id);
	}

	@Override
	public List<QuickTask> findByIssueId(Long issueId) {
		List<QuickTask> tasks = new ArrayList<QuickTask>(Arrays.asList(ao.find(QuickTask.class, Query.select().where("issue_id = ?", issueId))));
		Collections.sort(tasks, new Comparator<QuickTask>(){
            public int compare(QuickTask q1, QuickTask q2) {
            	if(q1.getDisplaySequence() == null)
            		return 1;
            	if(q2.getDisplaySequence() == null)
            		return -1;
            	
            	return q1.getDisplaySequence().compareTo(q2.getDisplaySequence());
            }
        });
		return tasks;
	}

	@Override
	public int countByIssueId(Long issueId) {
		return ao.count(QuickTask.class, Query.select().where("issue_id = ?", issueId));
	}

	@Override
	public void deleteQuickTask(QuickTask quicktask) {
		ao.delete(quicktask);
	}

	@Override
	public void deleteQuickTasks(List<QuickTask> quicktasks) {
		QuickTask[] quicktaskArr = new QuickTask[quicktasks.size()];
		quicktasks.toArray(quicktaskArr);
		ao.delete(quicktaskArr);
	}

	@Override
	public QuickTask move(QuickTask taskToMove, Integer afterTaskId) {
		if(afterTaskId == null){
			List<QuickTask> tasks = findByIssueId(taskToMove.getIssueId());
			Long counter = 1L;
			for(QuickTask task : tasks){
				if(task.getID() == taskToMove.getID())
					task.setDisplaySequence(0L);
				else
					task.setDisplaySequence(counter);
				counter++;
			}
		}
		else{
			List<QuickTask> tasks = findByIssueId(taskToMove.getIssueId());
			Long afterTaskSequence = 0L;
			for(QuickTask task : tasks){
				if(task.getID() == afterTaskId)
					afterTaskSequence = task.getDisplaySequence();
			}
			
			Long counter = 0L;
			for(QuickTask task : tasks){
				if(taskToMove.getID() != task.getID())
					task.setDisplaySequence(counter);
				else{
					task.setDisplaySequence(afterTaskSequence + 1);
				}
					
				counter++;
			}
		}
		return taskToMove;
	}
}
