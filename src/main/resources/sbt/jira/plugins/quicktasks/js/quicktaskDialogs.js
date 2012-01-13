jQuery.namespace("sbt.jira.plugins.quicktasks");

/**
 * Renders and handles submission of delete form used in dialog
 */
sbt.jira.plugins.quicktasks.DeleteForm = Backbone.View.extend({

    /**
     * Destroys model on server
     *
     * @param {Object} values
     * @param complete
     * @return {sbt.jira.plugins.quicktasks.DeleteForm}
     */
    submit: function (values, row, complete) {

        this.$(".throbber").addClass("loading");

        this.model.destroy({
            data: values,
            success: function () {
                complete();
            },
            error: function () {
                complete();
            }
        });

        return this;
    },

    /**
     *
     * Renders delete form. This differs from standard render methods, as it requires async request/s to the server.
     * As a result when this method is calle the first argument is a function that is called when the content has been
     * rendered.
     *
     * @param {function} ready - callback to declare content is ready
     * @return {sbt.jira.plugins.quicktasks.DeleteForm}
     */
    render: function (ready) {
        var instance = this;
        return this;
    }
});
