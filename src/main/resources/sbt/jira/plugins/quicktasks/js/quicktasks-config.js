// use this constant for rest version to ease update
JIRA.REST_VERSION = "2";
JIRA.REST_BASE_URL = contextPath + "/rest/quicktasks/" + JIRA.REST_VERSION;


JIRA.IssueConfig = function () {
    return {
        getKey: function () {
            return jQuery("form#quicktasks-add input[name=issueKey]").val();
        },
        getId: function () {
            return jQuery("form#quicktasks-add input[name=issueId]").val();
        }
    };
}();


AJS.convertXHRToSmartAjaxResult = function (xhr) {
    var textStatus = xhr.status >= 400 ? "error" : "success";
    return JIRA.SmartAjax.SmartAjaxResult(xhr, new Date().getTime(), textStatus, JSON.parse(xhr.responseText))
};

AJS.$(function () {

    var $operations = AJS.$(".operation-menu");

    AJS.$("a.quick-tasks-inlinedialog-trigger").each(function() {
        AJS.InlineDialog(
            AJS.$(this),
            "quick-tasks-inlinedialog-" + AJS.escapeHtml(parseUri(this.href).queryKey.fieldId),
            this.href,
            {
                width: 200
            }
        );
    });
    
    var dropdown = new AJS.Dropdown({
        trigger: $operations.find(".project-config-operations-trigger"),
        content: $operations.find(".aui-list"),
        alignment: AJS.RIGHT
    });

    var operationsMenuState = "closed";

    AJS.$(dropdown).bind({
        "showLayer": function() {
            operationsMenuState = "open";
        },
        "hideLayer": function() {
            operationsMenuState = "closed";
        }
    });

    dropdown.trigger().bind("mouseenter focus", function() {
        if (operationsMenuState === "willopen") {
            dropdown.show();
        }
    });
});
