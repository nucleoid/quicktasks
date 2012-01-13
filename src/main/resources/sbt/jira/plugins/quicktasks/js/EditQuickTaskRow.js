jQuery.namespace("sbt.jira.plugins.quicktasks.editRow");

/**
 * Renders and assigns controls to table row responsible for creating versions
 *
 */
sbt.jira.plugins.quicktasks.EditQuickTaskRow = JIRA.RestfulTable.EditRow.extend({

    /**
     * Renders errors with special handling for userReleaseDate as the property name that comes back from the servers
     * error collection does not match that of the input.
     *
     * @param errors
     */
    renderErrors: function (errors) {

        JIRA.RestfulTable.EditRow.prototype.renderErrors.apply(this, arguments); // call super

        return this;
    },

    /**
     * Handles all the rendering of the create version row.
     *
     * @param {Object} renderData
     * ... {Object} values - Values of fields
     *
     */
    render: function (data) {

        data.issue = jQuery("form#quicktasks-add input[name=issueKey]").val();

        this.el.className = "quick-tasks-add-fields";

        this.$el.html(sbt.jira.plugins.quicktasks.editQuicktaskRow(data));

        return this;
    }
});
