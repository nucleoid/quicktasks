JIRA.QuickTaskModel = JIRA.RestfulTable.EntryModel.extend({

	addExpand: function (changed) {
        changed.expand = "operations";
    }

});
