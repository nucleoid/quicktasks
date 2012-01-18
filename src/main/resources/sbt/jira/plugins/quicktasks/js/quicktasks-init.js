// Initialisation of our quicktasks table
jQuery(function () {

    var quicktasksTable = jQuery("#quicktasks-table");
    var allUrl = JIRA.REST_BASE_URL + "/" + JIRA.IssueConfig.getId();
    var incompleteUrl = JIRA.REST_BASE_URL + "/" + JIRA.IssueConfig.getId() + "/incomplete";
    if(!(quicktasksTable.length > 0)) {
        return;
    }
    var resourceURL = allUrl;
    var urlType = getUrlType();

    function getUrlType(){
    	var toReturn = "";
    	JIRA.SmartAjax.makeRequest({
            url: JIRA.REST_BASE_URL + "/" + JIRA.IssueConfig.getId() + '/urlType',
            complete: function (xhr, status, response) {
            	toReturn = xhr.responseText;
            	resourceURL = toReturn === "incomplete" ? incompleteUrl : allUrl
            	getQuicktasks(populateTable);
            }
        });
    	return toReturn;
    }

    function getResourceURL () {
        return resourceURL;
    }

    function getQuicktasks (callback) {
        JIRA.SmartAjax.makeRequest({
            url: getResourceURL(),
            data: {expand: "operations"},
            complete: function (xhr, status, response) {
                if (response.successful) {
                    callback(response.data)
                } else {
                	quicktasksTable.trigger("serverError",
                            [JIRA.SmartAjax.buildSimpleErrorContent(response)]);
                }
            }
        });
    }

    function focusFirstField () {
    	quicktasksTable.find(":input:text:first").focus(); // set focus to first field
    }

    var populateTable = function (quicktasks) {
    	sbt.jira.plugins.quicktasks.QuickTasksTable = new JIRA.RestfulTable({
            editable: true,
            reorderable: true,
            el: quicktasksTable,
            url: contextPath + "/rest/quicktasks/" + JIRA.REST_VERSION + "/",
            entries: quicktasks,
            model: JIRA.QuickTaskModel,
            noEntriesMsg: AJS.I18n.getText("quicktasks.none"),
            views: {
                editRow: sbt.jira.plugins.quicktasks.EditQuickTaskRow,
                row: sbt.jira.plugins.quicktasks.QuickTaskRow
            }
        });

        jQuery(".jira-restfultable-init").remove();

        jQuery('<li>')
            .appendTo("#project-config-panel-versions ul.operation-menu");

        focusFirstField();
    };
    
    jQuery('.quicktasks-all').click(function(link) {
    	link.preventDefault();
    	clearTable();
    	resourceURL = allUrl;
    	addTableRows();
    	jQuery(this).addClass('aui-checked');
    });
    
    jQuery('.quicktasks-incomplete').click(function(link) {
    	link.preventDefault();
    	clearTable();
    	resourceURL = incompleteUrl;
    	addTableRows();
    	jQuery(this).addClass('aui-checked');
    });
    
    function clearTable(){
    	jQuery('.aui-list-checked').each(function(){
    		jQuery(this).removeClass('aui-checked');
    	});
    	jQuery(sbt.jira.plugins.quicktasks.QuickTasksTable.getRows()).each(function(){
    		sbt.jira.plugins.quicktasks.QuickTasksTable._removeRow(this);
    	});
    }
    
    function addTableRows(){
    	getQuicktasks(function(models){
    		jQuery(models).each(function(){
    			sbt.jira.plugins.quicktasks.QuickTasksTable.addRow(this, null);
    		})
    	});
    }
    JIRA.SmartAjax.makeRequest.extend({
    	
    });
});