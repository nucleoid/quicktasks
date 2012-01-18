JIRA.SERVER_ERROR_EVENT = "serverError";
JIRA.REQUEST_FINISHED_EVENT = "requestFinished";
JIRA.REQUEST_STARTED_EVENT = "requestStarted";
JIRA.SUBMIT_EVENT = "submit";
JIRA.EDIT_EVENT = "edit";
JIRA.UPDATE_EVENT = "change";
JIRA.LOADING_CLASS = "loading";

/**
 * Displays generic server errors, such as login and comms error
 *
 * @param {Event} e
 * @param {Text/HTML} errorMessage - In most cases this should be build using [JIRA.SmartAjax.buildSimpleErrorContent]
 */
AJS.bindDefaultCustomEvent("serverError", function (e, errorMessage) {
    var serverErrorConsole = jQuery("#project-config-error-console");

    serverErrorConsole.empty();

    new JIRA.FormDialog({
        id: "server-error-dialog",
        content: function (callback) {
            callback(JIRA.Templates.Common.serverErrorDialog({
                message: errorMessage
            }));
        }
    }).show();


    // clear all dirty forms (isDirty.js)
    jQuery("form").data("AJS_DirtyForms_cleanValue", null);
});