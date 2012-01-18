package it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.DatabaseUpdater;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import sbt.jira.plugins.quicktasks.QuickTaskManagerImpl;
import sbt.jira.plugins.quicktasks.entities.QuickTask;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(QuickTaskManagerImplTest.QuickTaskManagerImplTestDatabaseUpdater.class)
public class QuickTaskManagerImplTest 
{
	private static final Long ISSUE_ID = 84l;
	private static final Long ISSUE_ID2 = 85l;
	private static final Long DISPLAY_SEQUENCE = 1L;
	private static final Long DISPLAY_SEQUENCE2 = 2L;
	private static final String DESCRIPTION = "This is a quicktask";
	private static final boolean COMPLETED = false;
	private static final boolean COMPLETED2 = true;
	
	private EntityManager entityManager;
	private ActiveObjects ao;
	private QuickTaskManagerImpl quicktaskManager;
	
	@Before
	public void setUp() throws Exception {
		assertNotNull(entityManager);
		ao = new TestActiveObjects(entityManager);
		quicktaskManager = new QuickTaskManagerImpl(ao);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAdd() {
		final String description = "kitty";
 
        assertEquals(3, ao.find(QuickTask.class).length);
 
        final QuickTask add = quicktaskManager.add(ISSUE_ID, DISPLAY_SEQUENCE2 + 2, description, false);
        assertFalse(add.getID() == 0);
 
        ao.flushAll();
 
        final QuickTask[] quicktasks = ao.find(QuickTask.class);
        assertEquals(4, quicktasks.length);
        QuickTask found = null;
        for(QuickTask quicktask : quicktasks){
        	if(quicktask.getDescription() == description)
        		found = quicktask;
        }
        Long first = 1L;
        assertNotNull(found);
        assertEquals(first, found.getDisplaySequence());
	}
	
	@Test
	public void testUpdateParams() {
		final String description = "kitty";
 
		QuickTask[] tasks = ao.find(QuickTask.class);
        assertEquals(3, tasks.length);
 
        final int existingId = tasks[0].getID();
        final QuickTask updated = quicktaskManager.update(existingId, DISPLAY_SEQUENCE2 + 2, description, false);
 
        ao.flushAll();
 
        final QuickTask[] quicktasks = ao.find(QuickTask.class);
        assertEquals(3, quicktasks.length);
        QuickTask found = null;
        for(QuickTask quicktask : quicktasks){
        	if(quicktask.getDescription() == description)
        		found = quicktask;
        }
        assertNotNull(found);
        assertEquals(existingId, found.getID());
	}
	
	@Test
	public void testUpdateParamsWithNulls() {
		final String description = "kitty";
 
		QuickTask[] tasks = ao.find(QuickTask.class);
        assertEquals(3, tasks.length);
 
        final int existingId = tasks[1].getID();
        final QuickTask updated = quicktaskManager.update(existingId, null, null, null);
 
        ao.flushAll();

        assertNotNull(updated);
        assertEquals(existingId, updated.getID());
        assertNotNull(updated.getDisplaySequence());
        assertNotNull(updated.getDescription());
        assertTrue(updated.isCompleted());
	}
	
	@Test
	public void testUpdateQuickTask() {
		final String description = "kitty";
 
		QuickTask[] tasks = ao.find(QuickTask.class);
        assertEquals(3, tasks.length);
 
        QuickTask tasked = tasks[0];
        tasked.setDisplaySequence(DISPLAY_SEQUENCE2 + 2);
        tasked.setDescription(description);
        final QuickTask updated = quicktaskManager.update(tasked);
 
        ao.flushAll();
 
        final QuickTask[] quicktasks = ao.find(QuickTask.class);
        assertEquals(3, quicktasks.length);
        QuickTask found = null;
        for(QuickTask quicktask : quicktasks){
        	if(quicktask.getDescription() == description)
        		found = quicktask;
        }
        assertNotNull(found);
        assertEquals(tasked.getID(), found.getID());
	}

	@Test
	public void testFindById() {
		QuickTask[] preQuicktasks = ao.find(QuickTask.class);
		assertEquals(3, preQuicktasks.length);
		int firstId = preQuicktasks[0].getID();
        ao.flushAll();
        
        final QuickTask quicktask = quicktaskManager.findById(firstId);
        assertNotNull(quicktask);
        assertEquals(firstId, quicktask.getID());
        assertEquals(DISPLAY_SEQUENCE, quicktask.getDisplaySequence());
	}
	
	@Test
	public void testFindByIssueId() {       
        assertEquals(3, ao.find(QuickTask.class).length);
 
        ao.flushAll();
        
        final List<QuickTask> quicktasks = quicktaskManager.findByIssueId(ISSUE_ID);
        assertEquals(2, quicktasks.size());
        assertEquals(ISSUE_ID, quicktasks.get(0).getIssueId());
        assertEquals(ISSUE_ID, quicktasks.get(1).getIssueId());
        assertEquals(DISPLAY_SEQUENCE, quicktasks.get(0).getDisplaySequence());
        assertEquals(DISPLAY_SEQUENCE2, quicktasks.get(1).getDisplaySequence());
	}
	
	@Test
	public void testFindByIssueIdSortsList() {       
        assertEquals(3, ao.find(QuickTask.class).length);
 
        final Long sequence = 1L;
        final QuickTask add = quicktaskManager.add(ISSUE_ID, sequence, DESCRIPTION, false);
        ao.flushAll();
        Long second = 2L;
        Long third = 3L;
        final List<QuickTask> quicktasks = quicktaskManager.findByIssueId(ISSUE_ID);
        assertEquals(3, quicktasks.size());
        assertEquals(ISSUE_ID, quicktasks.get(0).getIssueId());
        assertEquals(ISSUE_ID, quicktasks.get(1).getIssueId());
        assertEquals(sequence, quicktasks.get(0).getDisplaySequence());
        assertEquals(second, quicktasks.get(1).getDisplaySequence());
        assertEquals(third, quicktasks.get(2).getDisplaySequence());
	}

	@Test
	public void testFindByIssueIdIncomplete() {       
        assertEquals(3, ao.find(QuickTask.class).length);
        quicktaskManager.add(ISSUE_ID, DISPLAY_SEQUENCE2 + 2, "blah", false);
        ao.flushAll();
        
        final List<QuickTask> quicktasks = quicktaskManager.findByIssueIdIncomplete(ISSUE_ID);
        assertEquals(2, quicktasks.size());
        assertEquals(ISSUE_ID, quicktasks.get(0).getIssueId());
        assertEquals(ISSUE_ID, quicktasks.get(1).getIssueId());
        assertEquals(COMPLETED, quicktasks.get(0).isCompleted());
        assertEquals(COMPLETED, quicktasks.get(1).isCompleted());
	}
	
	@Test
	public void testCountByIssueId() {
        assertEquals(3, ao.find(QuickTask.class).length);

        ao.flushAll();
        
        final int quicktaskCount = quicktaskManager.countByIssueId(ISSUE_ID);
        assertEquals(2, quicktaskCount);
	}
	
	@Test
	public void testDeleteQuickTask() {
		List<QuickTask> allQuicktasks = new ArrayList<QuickTask>(Arrays.asList(ao.find(QuickTask.class))); 
		assertEquals(3, allQuicktasks.size());
		 
        ao.flushAll();
        
        quicktaskManager.deleteQuickTask(allQuicktasks.get(0));
        assertEquals(2, ao.find(QuickTask.class).length);
	}
	
	@Test
	public void testDeleteQuickTasks() {
		List<QuickTask> allQuicktasks = new ArrayList<QuickTask>(Arrays.asList(ao.find(QuickTask.class))); 
		assertEquals(3, allQuicktasks.size());
		 
        ao.flushAll();
        allQuicktasks.remove(0);
        
        quicktaskManager.deleteQuickTasks(allQuicktasks);
        assertEquals(1, ao.find(QuickTask.class).length);
	}
	
	@Test
	public void testMoveToFirst(){
		quicktaskManager.add(ISSUE_ID, DISPLAY_SEQUENCE2 + 1, "blah", false);
		assertEquals(4, ao.find(QuickTask.class).length);
		final List<QuickTask> preQuicktasks = quicktaskManager.findByIssueId(ISSUE_ID);
		final int thirdId = preQuicktasks.get(2).getID();
		ao.flushAll();

        final QuickTask moved = quicktaskManager.move(preQuicktasks.get(2), preQuicktasks.get(0).getID());
        ao.flushAll();
        
        Long one = 1L;
        Long newSequ = DISPLAY_SEQUENCE2 + 1L;
        final List<QuickTask> quicktasks = quicktaskManager.findByIssueId(ISSUE_ID);
        assertEquals(3, quicktasks.size());
        assertEquals(thirdId, quicktasks.get(0).getID());
        assertEquals(one, quicktasks.get(0).getDisplaySequence());
        assertEquals(newSequ, quicktasks.get(2).getDisplaySequence());
	}
	
	@Test
	public void testMoveMiddle(){
		quicktaskManager.add(ISSUE_ID, DISPLAY_SEQUENCE2 + 1, "blah", false);
		quicktaskManager.add(ISSUE_ID, DISPLAY_SEQUENCE2 + 2, "blahf", true);
		quicktaskManager.add(ISSUE_ID, DISPLAY_SEQUENCE2 + 3, "blahd", false);
		quicktaskManager.add(ISSUE_ID, DISPLAY_SEQUENCE2 + 4, "blahs", true);
		quicktaskManager.add(ISSUE_ID, DISPLAY_SEQUENCE2 + 5, "blaht", false);
		assertEquals(8, ao.find(QuickTask.class).length);
		final List<QuickTask> preQuicktasks = quicktaskManager.findByIssueId(ISSUE_ID);
		final int fourthId = preQuicktasks.get(3).getID();
		ao.flushAll();

        final QuickTask moved = quicktaskManager.move(preQuicktasks.get(3), preQuicktasks.get(5).getID());
        ao.flushAll();
        
        Long five = 5L;
        Long six = 6L;
        final List<QuickTask> quicktasks = quicktaskManager.findByIssueId(ISSUE_ID);
        assertEquals(7, quicktasks.size());
        assertEquals(fourthId, quicktasks.get(4).getID());
        assertEquals(five, quicktasks.get(4).getDisplaySequence());
        assertEquals(six, quicktasks.get(5).getDisplaySequence());
	}
	
	@Test
	public void testMoveLast(){
		assertEquals(3, ao.find(QuickTask.class).length);
		
		final List<QuickTask> preQuicktasks = quicktaskManager.findByIssueId(ISSUE_ID);
		final int firstId = preQuicktasks.get(0).getID();
		ao.flushAll();
		//move the first task to last (by Issue)
        final QuickTask moved = quicktaskManager.move(preQuicktasks.get(0), null);
        ao.flushAll();
        Long one = 1L;
        final List<QuickTask> quicktasks = quicktaskManager.findByIssueId(ISSUE_ID);
        assertEquals(2, quicktasks.size());
        assertNotSame(firstId, quicktasks.get(0).getID());
        assertEquals(firstId, quicktasks.get(1).getID());
        assertEquals(DISPLAY_SEQUENCE, quicktasks.get(0).getDisplaySequence());
        assertEquals(DISPLAY_SEQUENCE2, quicktasks.get(1).getDisplaySequence());
	}
	
	@Test
	public void testMoveFirst(){
		quicktaskManager.add(ISSUE_ID, DISPLAY_SEQUENCE2 + 1, "blah", false);
		assertEquals(4, ao.find(QuickTask.class).length);
		final List<QuickTask> preQuicktasks = quicktaskManager.findByIssueId(ISSUE_ID);
		final int thirdId = preQuicktasks.get(2).getID();
		ao.flushAll();

        quicktaskManager.moveFirst(preQuicktasks.get(2));
        ao.flushAll();
        
        Long one = 1L;
        final List<QuickTask> quicktasks = quicktaskManager.findByIssueId(ISSUE_ID);
        assertEquals(3, quicktasks.size());
        assertEquals(thirdId, quicktasks.get(0).getID());
        assertEquals(one, quicktasks.get(0).getDisplaySequence());
	}
	
	public static class QuickTaskManagerImplTestDatabaseUpdater implements DatabaseUpdater
    {
        @Override
        public void update(EntityManager em) throws Exception
        {
            em.migrate(QuickTask.class);
            
            final QuickTask quicktask = em.create(QuickTask.class);
            quicktask.setIssueId(ISSUE_ID);
    		quicktask.setDisplaySequence(DISPLAY_SEQUENCE);
    		quicktask.setDescription(DESCRIPTION);
    		quicktask.setCompleted(COMPLETED);
    		quicktask.save();
            
            final QuickTask quicktask2 = em.create(QuickTask.class);
            quicktask2.setIssueId(ISSUE_ID);
            quicktask2.setDisplaySequence(DISPLAY_SEQUENCE2);
            quicktask2.setDescription(DESCRIPTION);
            quicktask2.setCompleted(COMPLETED2);
            quicktask2.save();
            
            final QuickTask quicktask3 = em.create(QuickTask.class);
            quicktask3.setIssueId(ISSUE_ID2);
            quicktask3.setDisplaySequence(DISPLAY_SEQUENCE2 + 1);
            quicktask3.setDescription(DESCRIPTION);
            quicktask3.setCompleted(COMPLETED2);
            quicktask3.save();
        }
    }
}
