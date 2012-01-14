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
     * Renders delete form.
     * @return {sbt.jira.plugins.quicktasks.DeleteForm}
     */
    render: function (ready) {
    	var instance = this;

        instance.el.innerHTML = sbt.jira.plugins.soy.deleteFormView();
        ready.call(instance, instance.el);
        return this;
    }
});
