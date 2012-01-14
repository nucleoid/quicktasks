package sbt.jira.plugins;

import java.util.List;

import com.atlassian.activeobjects.tx.Transactional;

import sbt.jira.plugins.entities.QuickTask;

@Transactional
public interface QuickTaskManager {
	
	QuickTask add(Long issueId, Long displaySequence, String description, boolean completed);
	QuickTask update(int id, Long displaySequence, String description, Boolean completed);
	QuickTask update(QuickTask task);
	QuickTask move(QuickTask taskToMove, Integer afterTaskId);
	QuickTask findById(int id);
	List<QuickTask> findByIssueId(Long issueId);
	int countByIssueId(Long issueId);
	void deleteQuickTask(QuickTask quicktask);
	void deleteQuickTasks(List<QuickTask> quicktasks);
}
