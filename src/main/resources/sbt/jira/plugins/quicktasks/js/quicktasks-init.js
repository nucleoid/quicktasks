// Initialisation of our quicktasks table
jQuery(function () {

    var quicktasksTable = jQuery("#quicktasks-table");

    if(!(quicktasksTable.length > 0)) {
        return;
    }

    function getResourceURL () {
        return JIRA.REST_BASE_URL + "/" + JIRA.IssueConfig.getId();
    }

    function getQuicktasks (callback) {
        JIRA.SmartAjax.makeRequest({
            url: getResourceURL(),
            data: {expand: "operations"},
            complete: function (xhr, status, response) {
                if (response.successful) {
                    callback(response.data.reverse())
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

    getQuicktasks(function (quicktasks) {


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
    })
});