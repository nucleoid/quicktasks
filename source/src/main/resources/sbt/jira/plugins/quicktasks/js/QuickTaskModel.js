JIRA.QuickTaskModel = JIRA.RestfulTable.EntryModel.extend({

	addExpand: function (changed) {
        changed.expand = "operations";
    },
    
    /**
     * Destroys the model on the server. We need to override the default method as it does not support sending of
     * query paramaters.
     *
     * @override
     * @param options
     * ... {function} success - Server success callback
     * ... {function} error - Server error callback
     * ... {object} data
     *
     * @return JIRA.VersionModel
     */
    destroy: function (options) {

        var instance = this,
            url = this.url();

        JIRA.SmartAjax.makeRequest({
            url: url,
            type: "DELETE",
            dataType: "json",
            complete: function (xhr, status, smartAjaxResponse) {

                var smartAjaxResponseData = smartAjaxResponse.data;

                if (typeof smartAjaxResponse.data === "string") {
                    smartAjaxResponseData = JSON.parse(smartAjaxResponse.data);
                }

                var isValidationError = !(xhr.status === 400 && smartAjaxResponseData && smartAjaxResponseData.errors);

                if (smartAjaxResponse.successful) {
                    instance.collection.remove(instance);
                    if (options.success) {
                        options.success.call(instance, smartAjaxResponseData);
                    }
                } else if(isValidationError) {
                    instance._serverErrorHandler(smartAjaxResponse);
                    if (options.error) {
                        options.error.call(instance, smartAjaxResponseData);
                    }
                }
                if (options.complete) {
                    options.complete.call(instance, xhr.status, smartAjaxResponseData);
                }
            }
        });

        return this;
    },

});
