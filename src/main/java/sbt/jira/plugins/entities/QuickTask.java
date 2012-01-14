package sbt.jira.plugins.entities;

import net.java.ao.Entity;
import net.java.ao.Preload;

@Preload
public interface QuickTask extends Entity
{
	Long getIssueId();
	
	void setIssueId(Long issueId);
	
	Long getDisplaySequence();
	
	void setDisplaySequence(Long sequence);

	String getDescription();
	
	void setDescription(String description);
	
	boolean isCompleted();
	
	void setCompleted(boolean completed);
}
