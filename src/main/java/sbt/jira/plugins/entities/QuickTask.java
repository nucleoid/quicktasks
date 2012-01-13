package sbt.jira.plugins.entities;

import com.atlassian.jira.issue.Issue;

public abstract interface QuickTask {
	public abstract Long getSequence();

	public abstract Long getDisplaySequence();

	public abstract Issue getParent();

	public abstract Issue getQuickTask();
}
