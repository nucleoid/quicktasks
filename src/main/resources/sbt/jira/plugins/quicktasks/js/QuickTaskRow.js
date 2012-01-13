jQuery.namespace("sbt.jira.plugins.quicktasks.QuickTaskRow");

/**
 * Handles rendering, interaction and updating (delegating to model) of a single version
 */
sbt.jira.plugins.quicktasks.QuickTaskRow = JIRA.RestfulTable.Row.extend({

    /**
     * Resets and renders version row in table. This should be called whenever the model changes.
     */
    render: function () {

        var instance = this,
            id = this.model.get("id"),
            $el = jQuery(this.el);

        $el.attr("className", "quick-task"); // reset all classNames

        $el.attr("id", "quick-task-" + id + "-row").attr("data-id", id);

        $el.html(sbt.jira.plugins.quicktasks.quicktaskRow({
            quicktask: this.model.toJSON()
        }));

        var dropdown = new AJS.Dropdown({
            trigger: $el.find(".quick-tasks-operations-trigger"),
            content: $el.find(".quick-tasks-operations-list")
        });

        jQuery(dropdown).bind("showLayer", function () {
            instance.$el.addClass("jira-restfultable-active");
            instance.trigger("modal");
        })
        .bind("hideLayer", function () {
            instance.trigger("modeless");
        });

        this._assignDropdownEvents();

        return this;
    },

    _assignDropdownEvents: function () {

        var instance = this;

        this.$(".quick-tasks-operations-delete").click(function (e) {
            instance.openDeleteDialog();
            e.preventDefault();
        });
    },

    /**
     * Opens Delete Dialog
     */
    openDeleteDialog: function () {
        this._openDialog(sbt.jira.plugins.quicktasks.DeleteForm, "quicktask-" + this.model.get("id") + "-delete-dialog");
    }

});
