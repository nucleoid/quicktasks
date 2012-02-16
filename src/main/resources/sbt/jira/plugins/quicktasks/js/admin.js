AJS.$(function () {
	if(AJS.$("form#quicktasks-admin input#updated").val() == "true"){
		JIRA.Messages.showSuccessMsg(AJS.I18n.getText("quicktasks.admin.success"));
	}
});