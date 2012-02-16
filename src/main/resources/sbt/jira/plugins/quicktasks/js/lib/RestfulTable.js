/**
 * A table who's entries/rows are can be retrieved, added and updated via rest (CRUD).
 * It uses backbone.js to sync the tables state back to the server and vice versa, avoiding page refreshes.
 *
 * For complete documentation and usage guide see:
 * https://extranet.atlassian.com/display/JIRADEV/JIRA.RestfulTable
 *
 * @namespace JIRA
 * @class RestfulTable
 */
JIRA.RestfulTable = Backbone.View.extend({

    // STATIC const

    NO_ENTRIES_CLASS: "jira-restfultable-no-entires",
    RESTFUL_TABLE_CLASS: "jira-restfultable",
    ALLOW_HOVER_CLASS: "jira-restfultable-allowhover",
    ROW_VIEW_DATA_KEY: "RestfulTable_Row_View",

    /**
     * @constructor
     * @param {Object} options
     */
    initialize: function (options) {

        var instance = this;

        // combine default and user options
        this.options = jQuery.extend(this._getDefaultOptions(options), options);

        // shortcuts to popular elements
        this.$table = jQuery(options.el)
                .addClass(this.RESTFUL_TABLE_CLASS)
                .addClass(this.ALLOW_HOVER_CLASS);

        this.$tbody = this.$table.find("tbody");
        this.$thead = this.$table.find("thead");

        // create a new Backbone collection to represent rows (http://documentcloud.github.com/backbone/#Collection)
        this._models = new this.options.Collection([], {
            comparator: function (row) { // sort models in colleciton based on dom ordering
                var index;
                jQuery.each(instance.getRows(), function (i) {
                    if (this.model.id === row.id) {
                        index = i;
                        return false;
                    }
                });

                return index;
            }
        });

        // shortcut to the class we use to create rows
        this._rowClass = this.options.views.row;

        if (this.options.editable) {

            this.editRows = []; // keep track of rows that are being edited concurrently

            if (!this.options.createDisabled) {

                // Create row responsible for adding new entries ...
                this._createRow = new this.options.views.editRow({
                        model: this.options.model.extend({
                            url: function () {
                                return instance.options.url;
                            }
                        }),
                        reorderable: this.options.reorderable
                    })
                    .bind("created", function (values) {
                        instance.addRow(values, 0);
                    })
                    .bind("validationError", function () {
                        this.trigger("focus");
                    })
                    .render({
                        errors: {},
                        values: {}
                    });

                // ... and appends it as the first row
                jQuery('<tbody class="jira-restfultable-create">').append(this._createRow.el).insertBefore(this.$tbody);

                this._applyFocusCoordinator(this._createRow);

                // focus create row
                this._createRow.trigger("focus");
            }

            this.$table.closest("form").submit(function (e) {
                e.preventDefault();
                if (instance.focusedRow) {
                    instance.focusedRow.trigger("save");
                }
            });

            if (this.options.reorderable) {
                this.$tbody.sortable({
                    handle: ".jira-restfultable-draghandle",
                    start: function (event, ui) {

                        var $ths = instance._createRow.$el.find("td");

                        ui.item.children().each(function (i) {
                            jQuery(this).width($ths.eq(i).width());
                        });
                        ui.item.addClass("jira-restfultable-movable");
                        // Add a <td> to the placeholder <tr> to inherit CSS styles.
                        ui.placeholder.html('<td colspan="' + instance.getColumnCount() + '">&nbsp;</td>');
                        ui.placeholder.css("visibility", "visible");
                        instance.getRowFromElement(ui.item[0]).trigger("modal");
                    },
                    stop: function (event, ui) {
                        ui.item.children().attr("style", "");
                        ui.item.removeClass("jira-restfultable-movable");
                        ui.placeholder.removeClass("jira-restfultable-row");
                        instance.getRowFromElement(ui.item[0]).trigger("modeless");
                    },
                    update: function (event, ui) {
                        var row = instance.getRowFromElement(ui.item[0]);
                        if (row) {

                            var data = {};
                            var nextRow = ui.item.next()[0];
                            if (nextRow) {
                                // Note that "after" actually means "before" here, since versions are displayed in reverse order.
                                data.after = instance.getRowFromElement(nextRow).model.url();
                            } else {
                                data.position = "First";
                            }
                            JIRA.SmartAjax.makeRequest({
                                url: row.model.url() + "/move",
                                type: "POST",
                                dataType: "json",
                                contentType: "application/json",
                                data: JSON.stringify(data),
                                complete: function (xhr, status, smartAjaxResult) {

                                    row.hideLoading();

                                    if (!smartAjaxResult.successful) {
                                        // The REST service provides nice and informative error messages, so let's use them,
                                        // *except* for some cases where JIRA.SmartAjax.buildSimpleErrorContent() provides better
                                        // error messages. Lovely.
                                        // TODO: JIRA.SmartAjax.buildSimpleErrorContent() should really handle this logic for us.
                                        var errorContent = (smartAjaxResult.status !== 401 || smartAjaxResult.statusText === JIRA.SmartAjax.SmartAjaxResult.TIMEOUT)
                                            ? JSON.parse(smartAjaxResult.data).errorMessages
                                            : JIRA.SmartAjax.buildSimpleErrorContent(smartAjaxResult);
                                        jQuery(document).trigger(JIRA.SERVER_ERROR_EVENT, errorContent);
                                    }
                                    jQuery(document).find("td.quick-tasks-completed input").each(function() {
                                    	var descriptionTd = jQuery(this).parent().parent().next();
                                    	if(jQuery(this).is(':checked'))
                                    		jQuery(descriptionTd).css("text-decoration", "line-through");
                                    	else
                                    		jQuery(descriptionTd).css("text-decoration", "none");
                                    });
                                }
                            });
                            row.showLoading();
                        }
                    },
                    axis: "y",
                    delay: 0,
                    containment: "document",
                    cursor: "move",
                    scroll: true,
                    zIndex: 8000
                });

                // Prevent text selection while reordering.
                this.$tbody.bind("selectstart mousedown", function (event) {
                    return !jQuery(event.target).is(".jira-restfultable-draghandle");
                });
            }
        }

        // when a model is removed from the collection, remove it from the viewport also
        this._models.bind("remove", function (model) {
            jQuery.each(instance.getRows(), function (i, row) {
                if (row.model === model) {
                    if (row.hasFocus() && instance._createRow) {
                        instance._createRow.trigger("focus");
                    }
                    instance._removeRow(row);
                }
            });
        });

        if (this.options.entries) {
            // Empty the models collection
            this._models.refresh([], { silent: true });
            this.renderRows(this.options.entries);
            // show message to user if we have no entries
            if (this.isEmpty()) {
                this.showNoEntriesMsg();
            }
        }

        this.$table.removeClass("loading").trigger("initialized", [this]);
    },

    /**
     * Adds row to collection and renders it
     *
     * @param {Object} values
     * @param {number} index
     * @return {JIRA.RestfulTable}
     */
    addRow: function (values, index) {

        var model;

        if (!values.id) {
            throw new Error("JIRA.RestfulTable.addRow: to add a row values object must contain an id. "
                    + "Maybe you are not returning it from your restend point?"
                    + "Recieved:" + JSON.stringify(values));
        }

        model = new this.options.model(values);

        this._renderRow(model, index);

        this._models.add(model);

        this.removeNoEntriesMsg();

        this.$table.trigger("addRow", [this]);

        return this;
    },

    /**
     * Provided a view, removes it from display and backbone collection
     *
     * @param {JIRA.RestfulTable.Row}
     */
    _removeRow: function (row) {
        this._models.remove(row.model);
        row.remove();
        if (this.isEmpty()) {
            this.showNoEntriesMsg();
        }

        this.$table.trigger("removeRow", [this]);
    },

    /**
     * Is there any entries in the table
     *
     * @return {Boolean}
     */
    isEmpty: function () {
        return this._models.length === 0;
    },


    /**
     * Gets all models
     *
     * @return {Backbone.Collection}
     */
    getModels: function () {
        return this._models;
    },

    /**
     * Gets table body
     *
     * @return {jQuery}
     */
    getTable: function () {
        return this.$table;
    },

    /**
     * Gets table body
     *
     * @return {jQuery}
     */
    getTableBody: function () {
        return this.$tbody;
    },

    /**
     * Gets create Row
     *
     * @return {B
     */
    getCreateRow: function () {
        return this._createRow;
    },

    /**
     * Gets the number of table colums
     *
     * @return {Integer}
     */
    getColumnCount: function () {
        return this.$table.find("thead:first th").length;
    },

    /**
     * Get the JIRA.RestfulTable.Row that corresponds to the given <tr> element.
     *
     * @param {HTMLElement} tr
     * @return {?JIRA.RestfulTable.Row}
     */
    getRowFromElement: function (tr) {
        return jQuery(tr).data(this.ROW_VIEW_DATA_KEY);
    },

    /**
     * Shows message {options.noEntriesMsg} to the user if there are no entries
     *
     * @return {JIRA.RestfulTable}
     */
    showNoEntriesMsg: function () {

        if (this.$noEntries) {
            this.$noEntries.remove();
        }

        this.$noEntries = jQuery("<tr>")
                .addClass(this.NO_ENTRIES_CLASS)
                .append(jQuery("<td>")
                    .attr("colspan", this.getColumnCount())
                    .text(this.options.noEntriesMsg)
                )
                .appendTo(this.$tbody);

        return this;
    },

    /**
     * Removes message {options.noEntriesMsg} to the user if there ARE entries
     *
     * @return {JIRA.RestfulTable}
     */
    removeNoEntriesMsg: function () {
        
        if (this.$noEntries && this._models.length > 0) {
            this.$noEntries.remove();
        }

        return this;
    },

    getRows: function () {

        var instance = this,
            views = [];

        this.$tbody.find("tr.jira-restfultable-readonly").each(function (i) {
            
            var $row = jQuery(this),
                view = $row.data(instance.ROW_VIEW_DATA_KEY);

            if (view) {
                views.push(view);
            }
        });

        return views;
    },

    /**
     * Appends entry to end or specified index of table
     *
     * @param {JIRA.RestfulTable.EntryModel} model
     * @param index
     * @return {jQuery}
     */
    _renderRow: function (model, index) {

        var instance = this,
            $rows = this.$tbody.find("tr.jira-restfultable-readonly"),
            $row,
            view;

        view = new this._rowClass({
            model: model,
            reorderable: this.options.reorderable
        });

        this.removeNoEntriesMsg();

        view.bind(JIRA.EDIT_EVENT, function (field) {
            instance.edit(this, field);
        });

        $row = view.render().$el;

        if (index !== -1) {

            if (typeof index === "number" && $rows.length !== 0) {
                $row.insertBefore($rows[index]);
            } else {
                this.$tbody.append($row);
            }
        }

        $row.data(this.ROW_VIEW_DATA_KEY, view);

        // deactivate all rows - used in the cases, such as opening a dropdown where you do not want the table editable
        // or any interactions
        view.bind("modal", function () {
            instance.$table.removeClass(instance.ALLOW_HOVER_CLASS);
            instance.$tbody.sortable("disable");
            jQuery.each(instance.getRows(), function () {
                if (!instance.isRowBeingEdited(this)) {
                    this.delegateEvents({}); // clear all events
                }
            });
        });

        // activate all rows - used in the cases, such as opening a dropdown where you do not want the table editable
        // or any interactions
        view.bind("modeless", function () {
            instance.$table.addClass(instance.ALLOW_HOVER_CLASS);
            instance.$tbody.sortable("enable");
            jQuery.each(instance.getRows(), function () {
                if (!instance.isRowBeingEdited(this)) {
                    this.delegateEvents(); // rebind all events
                }
            });
        });

        // ensure that when this row is focused no other are
        this._applyFocusCoordinator(view);

        return $row;
    },

    /**
     * Returns if the row is edit mode or note
     *
     * @param {JIRA.RestfulTable.Row} - read onyl row to check if being edited
     * @return {Boolean}
     */
    isRowBeingEdited: function (row) {

        var isBeingEdited = false;

        jQuery.each(this.editRows, function () {
            if (this.el === row.el) {
                isBeingEdited = true;
                return false;
            }
        });

        return isBeingEdited;
    },

    /**
     * Ensures that when supplied view is focused no others are
     *
     * @param {Backbone.View} view
     * @return {JIRA.RestfulTable}
     */
    _applyFocusCoordinator: function (view) {

        var instance = this;

        if (!view.hasFocusBound) {

            view.hasFocusBound = true;

            view.bind("focus", function () {

                if (instance.focusedRow && instance.focusedRow !== view) {
                    instance.focusedRow.trigger("blur");
                }

                instance.focusedRow = view;

                if (view instanceof JIRA.RestfulTable.Row && instance._createRow) {
                    instance._createRow.enable();
                }
            });
        }

        return this;
    },

    /**
     * Remove specificed row from collection holding rows being concurrently edited
     *
     * @param {JIRA.RestfulTable.EditRow} editView
     * @return {JIRA.RestfulTable}
     */
    _removeEditRow: function (editView) {
        var index = jQuery.inArray(editView, this.editRows);
        this.editRows.splice(index, 1);
        return this;
    },

    /**
     * Focuses last row still being edited or create row (if it exists)
     *
     * @return {JIRA.RestfulTable}
     */
    _shiftFocusAfterEdit: function () {

        if (this.editRows.length > 0) {
            this.editRows[this.editRows.length-1].trigger("focus");
        } else if (this._createRow) {
            this._createRow.trigger("focus");
        }

        return this;
    },

    /**
     * Evaluate if we save row when we blur. We can only do this when there is one row being edited at a time, otherwise
     * it causes an infinate loop JRADEV-5325
     *
     * @return {boolean}
     */
    _saveEditRowOnBlur: function () {
         return this.editRows.length <= 1;
    },

    /**
     * Dismisses rows being edited concurrently that have no changes
     */
    dismissEditRows: function () {
        jQuery.each(this.editRows, function () {
            if (!this.hasUpdates()) {
                this.trigger("finishedEditing");
            }
        });
    },

    /**
     * Converts readonly row to editable view
     *
     * @param {Backbone.View} row
     * @param {String} field - field name to focus
     * @return {Backbone.View} editRow
     */
    edit: function (row, field) {

        var instance = this,

            editRow = new this.options.views.editRow({
                el: row.el,
                isUpdateMode: true,
                reorderable: this.options.reorderable,
                model: row.model
            })
            .render({
                errors: {},
                update: true,
                values: row.model.toJSON()
            })
            .bind("updated", function (model, focusUpdated) {
                instance._removeEditRow (this);
                this.unbind();
                row.render().delegateEvents(); // render and rebind events
                row.trigger("updated"); // trigger blur fade out
                if (focusUpdated !== false) {
                    instance._shiftFocusAfterEdit();
                }
                if (jQuery.fn.removeDirtyWarning) {
                    row.$el.closest("form").removeDirtyWarning();
                }
            })
            .bind("validationError", function () {
                this.trigger("focus");
            })
            .bind("finishedEditing", function () {
                instance._removeEditRow(this);
                row.render().delegateEvents();
                this.unbind();  // avoid any other updating, blurring, finished editing, cancel events being fired
                if (jQuery.fn.removeDirtyWarning) {
                    row.$el.closest("form").removeDirtyWarning();
                }
            })
            .bind("cancel", function () {
                instance._removeEditRow(this);
                this.unbind();  // avoid any other updating, blurring, finished editing, cancel events being fired
                row.render().delegateEvents(); // render and rebind events
                instance._shiftFocusAfterEdit();
                if (jQuery.fn.removeDirtyWarning) {
                    row.$el.closest("form").removeDirtyWarning();
                }
            })
            .bind("blur", function () {
                instance.dismissEditRows(); // dismiss edit rows that have no changes
                if (instance._saveEditRowOnBlur()) {
                    this.trigger("save", false);  // save row, which if successful will call the updated event above
                }
            });

        // Ensure that if focus is pulled to another row, we blur the edit row
        this._applyFocusCoordinator(editRow);

        if (jQuery.fn.removeDirtyWarning) {
            row.$el.closest("form").removeDirtyWarning();
        }


        // focus edit row, which has the flow on effect of blurring current focused row
        editRow.trigger("focus", field);

        // disables form fields
        if (instance._createRow) {
            instance._createRow.disable();
        }

        this.editRows.push(editRow);

        return editRow;
    },


    /**
     * Renders all specified rows
     *
     * @param {Array} array of objects describing Backbone.Model's to render
     * @return {JIRA.RestfulTable}
     */
    renderRows: function (rows) {

        var model,
            $els = jQuery();

        // Insert prepopulated entries
        for (var i = 0; i < rows.length; i++) {
            model = new this.options.model(rows[i]);
            $els = $els.add(this._renderRow(model, -1));
            this._models.add(model)
        }



        this.removeNoEntriesMsg();

        this.$tbody.append($els);

        return this;
    },

    /**
     * Gets default options
     *
     * @param {Object} options
     */
    _getDefaultOptions: function (options) {
        return {
            model: options.model || JIRA.RestfulTable.EntryModel,
            views: {
                editRow: JIRA.RestfulTable.EditRow,
                row: JIRA.RestfulTable.Row
            },
            Collection: Backbone.Collection.extend({
                url: options.url,
                model: options.model || JIRA.RestfulTable.EntryModel
            }),
            reorderable: false
        }
    }

});

/**
 * A class provided to fill some gaps with the out of the box Backbone.Model class. Most notiably the inability
 * t  o send ONLY modified attributes back to the server.
 *
 * @class EntryModel
 * @namespace JIRA.RestfulTable
 */
JIRA.RestfulTable.EntryModel = Backbone.Model.extend({

    /**
     * Overrides default save handler to only save (send to server) attributes that have changed.
     * Also provides some default error handling.
     *
     * @override
     * @param attributes
     * @param options
     */
    save: function (attributes, options) {


        options = options || {};

        var instance = this,
            Model,
            syncModel,
            error = options.error, // we override, so store original
            success = options.success;


        // override error handler to provide some defaults
        options.error = function (model, xhr) {

            var smartAjaxResponse = AJS.convertXHRToSmartAjaxResult(xhr);

            instance._serverErrorHandler(smartAjaxResponse);

            // call original error handler
            if (error) {
                error.call(instance, instance, xhr, AJS.convertXHRToSmartAjaxResult(xhr));
            }
        };

        // if it is a new model, we don't have to worry about updating only changed attributes because they are all new
        if (this.isNew()) {

            // call super
            Backbone.Model.prototype.save.call(this, attributes, options);

        // only go to server if something has changed
        } else if (attributes) {

            // create temporary model
            Model = Backbone.Model.extend({
                url: this.url()
            });

            syncModel = new Model({
                id: this.id
            });

            options.success = function (model, xhr) {

                // update original model with saved attributes
                instance.clear().set(model.toJSON());

                // call original success handler
                if (success) {
                    success.call(instance, instance, xhr);
                }
            };

            // update temporary model with the changed attributes
            syncModel.save(attributes, options);
        }
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
     * @return JIRA.RestfulTable.EntryModel
     */
    destroy: function (options) {

        var instance = this,
            url = this.url(),
            data = jQuery.param(options.data);

        if (data !== "") {

            // we need to add to the url as the data param does not work for jQuery DELETE requests
            url = url + "?" + data;
        }

        JIRA.SmartAjax.makeRequest({
            url: url,
            type: "DELETE",
            dataType: "json",
            complete: function (xhr, status, smartAjaxResponse) {
                if (smartAjaxResponse.successful) {
                    if(instance.collection){
                        instance.collection.remove(instance);
                    }
                    if (options.success) {
                        options.success.call(instance, smartAjaxResponse.data);
                    }
                } else {
                    instance._serverErrorHandler(smartAjaxResponse);
                    if (options.error) {
                        options.error.call(instance, smartAjaxResponse.data);
                    }
                }
            }
        });

        return this;
    },


    /**
     * A more complex lookup for changed attributes then default backbone one.
     *
     * @param attributes
     */
    changedAttributes: function (attributes) {

        var changed = {},
            current = this.toJSON();

        jQuery.each(attributes, function (name, value) {

            if (!current[name]) {
                if (typeof value === "string") {
                    if (jQuery.trim(value) !== "") {
                        changed[name] = value;
                    }
                } else if (jQuery.isArray(value)) {
                    if (value.length !== 0) {
                        changed[name] = value;
                    }
                } else {
                    changed[name] = value;
                }
            } else if (current[name] && current[name] !== value) {

                if (typeof value === "object") {
                    if (!_.isEqual(value, current[name])) {
                        changed[name] = value;
                    }
                } else {
                    changed[name] = value;
                }
            }
        });

        if (!_.isEmpty(changed)) {
            this.addExpand(changed);
            return changed;
        }
    },

    /**
     * Useful point to override if youalways want to add an expand to your rest calls.
     *
     * @param changed attributes that have already changed
     */
    addExpand: function (changed){

    },

    /**
     * Throws a server error event unless user input validation error (status 400)
     *
     * @param smartAjaxResponse
     */
    _serverErrorHandler: function (smartAjaxResponse) {

        var errorMessage = smartAjaxResponse.data && smartAjaxResponse.data.errorMessages && smartAjaxResponse.data.errorMessages[0];

        if (!smartAjaxResponse.validationError) {

            if (errorMessage) {
                jQuery(document).trigger(JIRA.SERVER_ERROR_EVENT, errorMessage);
            } else {
                jQuery(document).trigger(JIRA.SERVER_ERROR_EVENT,
                             [JIRA.SmartAjax.buildSimpleErrorContent(smartAjaxResponse)]);
            }
        }
    },

    /**
     * Fetches values, with some generic error handling
     *
     * @override
     * @param options
     */
    fetch: function (options) {

        options = options || {};

        var instance = this,
            error = options.error;

        this.clear(); // clear the model, so we do not merge the old with the new

        options.error = function (model, xhr) {

            var smartAjaxResponse = AJS.convertXHRToSmartAjaxResult(xhr);

            instance._serverErrorHandler(smartAjaxResponse);

            if (error) {
                error.apply(this, arguments);
            }
        };

        // call super
        Backbone.Model.prototype.fetch.call(this, options);
    }
});

/**
 * An abstract class that gives the required behaviour for RestfulTable rows.
 * Extend this class and pass it as the {views.row} property of the options passed to JIRA.RestfulTable in construction.
 *
 * @class Row
 * @namespace JIRA.RestfulTable
 */
JIRA.RestfulTable.Row = Backbone.View.extend({

    // Static Const

    FOCUSED_CLASS: "jira-restfultable-focused",

    tagName: "tr",

    // delegate events
    events: {
        "click .jira-restfultable-editable" : "edit"
    },

    /**
     * @constructor
     * @param {object} options
     */
    initialize: function (options) {

        options = options || {};

        var realRender = this.render;

        if (!this.events["click .jira-restfultable-editable"]) {
            throw new Error("It appears you have overridden the events property. To add events you will need to use"
                    + "a work around. https://github.com/documentcloud/backbone/issues/244")
        }

        this.render = function () {
            this.$el.attr("className", ""); // clear all previous state classes
            realRender.apply(this, arguments);
            if (this.reorderable) {
                this.addDragHandle();
            }
            this.$el.addClass("jira-restfultable-row jira-restfultable-readonly");
            this.trigger("render", this.$el);
            this.$el.trigger("contentRefresh", [this.$el]);
            return this;
        };

        this.index = options.index || 0;
        this.reorderable = options.reorderable;

        // whenever we make a change to a version, re-render it
        this.$el = jQuery(this.el);

        this.bind("cancel", function () {
            this.disabled = true;
        })
        .bind("focus", function (field) {
            this.focus(field);
        })
        .bind("blur", function () {
            this.unfocus();
        })
        .bind("updated", function () {
            this._showUpdated();
        })
        .bind("modal", function () {
            this.$el.addClass("jira-restfultable-active");
        })
        .bind("modeless", function () {
            this.$el.removeClass("jira-restfultable-active")
        });
    },

    /**
     * Fades row from blue to transparent
     */
    _showUpdated: function () {

        var instance = this,
            cells = this.$el
                    .addClass("jira-restfultable-animate")
                    .find("td")
                    .css("backgroundColor","#ebf1fd");

        instance.delegateEvents({});

        setTimeout(function () {
            cells.animate({
                backgroundColor: "white"
            }, function () {
                cells.css("backgroundColor", "");
                jQuery(document).one("mousemove", function () {
                    instance.delegateEvents();
                    instance.$el.removeClass("jira-restfultable-animate");
                });
            });
        }, 500)
    },

    /**
     * Creates and shows a JIRA.FormDialog. The default submit handler is overridden to serialize the form values and
     * update the associated model with them. Updating of model triggers a change event which re-renders the table row.
     *
     * @private
     * @param {Backbone.View} View - dialog contents
     */
    _openDialog: function (View, id) {

        var instance = this,

            dialogForm = new View({
                model: this.model
            }),

            dialog = new JIRA.FormDialog({
                id: id,

                content: function (callback) {
                    dialogForm.render(function (el) {
                        callback(el);
                    });
                },

                submitHandler: function (e) {

                    dialogForm.submit(this.$form.serializeToObject(), instance, function () {
                        dialog.hide();
                    });

                    e.preventDefault();
                }
            });

         dialog.show();
    },

    /**
     * Save changed attributes back to server and re-render
     *
     * @param attr
     * @return {JIRA.RestfulTable.Row}
     */
    sync: function (attr) {

        this.model.addExpand(attr);

        var instance = this;

        this.showLoading();

        this.model.save(attr, {
            success: function () {
                instance.hideLoading().render();
                instance.trigger("updated")
            },
            error: function () {
                instance.hideLoading();
            }
        });

        return this;
    },

    /**
     * Get model from server and re-render
     *
     * @return {JIRA.RestfulTable.Row}
     */
    refresh: function (success, error) {

        var instance = this;

        this.showLoading();

        this.model.fetch({
            success: function () {
                instance.hideLoading().render();
                if (success) {
                    success.apply(this, arguments);
                }
            },
            error: function () {
                instance.hideLoading();
                if (error) {
                    error.apply(this, arguments);
                }
            }
        });

        return this;
    },

    /**
     * Adds drag handle
     * @return JIRA.RestfulTable.EditRow
     */
    addDragHandle: function () {
        if (this.$(".jira-restfultable-order").length === 0) {
            this.$el.prepend(JIRA.RestfulTable.Templates.dragHandle());
        }
        return this;
    },

    /**
     * Returns true if row has focused class
     *
     * @return Boolean
     */
    hasFocus: function () {
        return this.$el.hasClass(this.FOCUSED_CLASS);
    },

    /**
     * Adds focus class (Item has been recently updated)
     *
     * @return JIRA.RestfulTable.Row
     */
    focus: function () {
        jQuery(this.el).addClass(this.FOCUSED_CLASS);
        return this;
    },

    /**
     * Removes focus class
     *
     * @return JIRA.RestfulTable.Row
     */
    unfocus: function () {
        jQuery(this.el).removeClass(this.FOCUSED_CLASS);
        return this;

    },

    /**
     * Adds loading class (to show server activity)
     *
     * @return JIRA.RestfulTable.Row
     */
    showLoading: function () {
        this.$el.addClass(JIRA.LOADING_CLASS);
        return this;
    },

    /**
     * Hides loading class (to show server activity)
     *
     * @return JIRA.RestfulTable.Row
     */
    hideLoading: function () {
        this.$el.removeClass(JIRA.LOADING_CLASS);
        return this;
    },

    /**
     * Switches row into edit mode
     *
     * @param e
     */
    edit: function (e) {
        var editableContainer = jQuery(e.target).closest(".jira-restfultable-editable");
        this.trigger(JIRA.EDIT_EVENT, editableContainer.attr("data-field-name"));
        return this;
    },



    render: function  () {

        var renderData = this.model.toJSON(),
            html = "";

        jQuery.each(renderData, function (name, value) {
            if (!/id|self/.test(name)) {
                html += "<td><span class=\"jira-restfultable-editable\" data-field-name=\"" + name + "\">" + value + "</span></td>";
            }
        });

        html += "<td class='jira-restfultable-operations'></td>"

        this.$el.html(html);

        return this;
    }
});

/**
 * An abstract class that gives the required behaviour for the creating and editing entries. Extend this class and pass
 * it as the {views.row} property of the options passed to JIRA.RestfulTable in construction.
 *
 * @class EditRow
 * @namespace JIRA.RestfulTable
 */
JIRA.RestfulTable.EditRow = Backbone.View.extend({

    // Static Const
    FOCUSED_CLASS: "jira-restfultable-focused",

    tagName: "tr",

    // delegate events
    events: {
        "focusin" : "_focus",
        "click" : "_focus",
        "click .aui-button-cancel" : "_cancel",
        "keyup" : "_handleKeyUpEvent"
    },

    /**
     * @constructor
     * @param {Object} options
     */
    initialize: function (options) {

        this.$el = jQuery(this.el);

        var realRender = this.render;

        this.render = function (data) {

            realRender.apply(this, arguments);

            if (this.reorderable) {
                this.addDragHandle();
            }

            this.$el.append(this.getOperationsHTML(data.update)); // add submit/cancel buttons
            this.$el.addClass("jira-restfultable-row jira-restfultable-editrow");
            this.trigger("render", this.$el);
            this.$el.trigger("contentRefreshed", [this.$el]);

            if (jQuery.fn.removeDirtyWarning) {
                this.$el.closest("form").removeDirtyWarning();
            }

            return this;
        };

        this.reorderable = options.reorderable;

        if (options.isUpdateMode) {
            this.isUpdateMode = true;
        } else {
            this._modelClass = options.model;
            this.model = new this._modelClass();
        }

        this.bind("cancel", function () {
            this.disabled = true;
        })
        .bind("save", function (focusUpdated) {
            if (!this.disabled) {
                this.submit(focusUpdated);
            }
        })
        .bind("focus", function (name) {
            this.focus(name);
        })
        .bind("blur", function () {
            this.unfocus();
        })
        .bind("submitStarted", function () {
            this._submitStarted();
        })
        .bind("submitFinished", function () {
            this._submitFinished();
        });
    },



    /**
     * Adds drag handle
     * @return JIRA.RestfulTable.EditRow
     */
    addDragHandle: function () {
        if (this.$(".jira-restfultable-order").length === 0) {
            this.$el.prepend(JIRA.RestfulTable.Templates.dragHandle());
        }
        return this;
    },

    /**
     * Executes cancel event if ESC is pressed
     *
     * @param {Event} e
     */
    _handleKeyUpEvent: function (e) {
        if (e.keyCode === 27) {
            this.trigger("cancel");
        }
    },

    /**
     * Fires cancel event
     *
     * @param {Event} e
     * @return JIRA.RestfulTable.EditRow
     */
    _cancel: function (e) {
        this.trigger("cancel");
         e.preventDefault();
        return this;
    },


    /**
     * Disables events/fields and adds safe gaurd against double submitting
     *
     * @return JIRA.RestfulTable.EditRow
     */
    _submitStarted: function () {
        this.submitting = true;
        this.showLoading()
            .disable()
            .delegateEvents({});

        return this;
    },

    /**
     * Enables events & fields
     *
     * @return JIRA.RestfulTable.EditRow
     */
    _submitFinished: function () {
        this.submitting = false;
        this.hideLoading()
            .enable()
            .delegateEvents(this.events);

        return this;
    },

    /**
     * Handles dom focus event, by only focusing row if it isn't already
     *
     * @param {Event} e
     * @return JIRA.RestfulTable.EditRow
     */
    _focus: function (e) {
        if (!this.hasFocus()) {
            this.trigger("focus", e.target.name);
        }
        return this;
    },

    /**
     * Returns true if row has focused class
     *
     * @return Boolean
     */
    hasFocus: function () {
        return this.$el.hasClass(this.FOCUSED_CLASS);
    },

    /**
     * Focus specified field (by name or id - first argument), first field with an error or first field (DOM order)
     *
     * @param name
     * @return JIRA.RestfulTable.EditRow
     */
    focus: function (name) {

        var $focus,
            $error;

        this.enable();

        if (name) {
            $focus = this.$el.find(":input[name=" + name + "], #" + name);
        } else {

            $error = this.$el.find(".error:first");

            if ($error.length === 0) {
                $focus = this.$el.find(":input:text:first");
            } else {
                $focus = $error.parent().find(":input");
            }
        }

        this.$el.addClass(this.FOCUSED_CLASS);

        if (this.$el.find(":input").isInView()) {
            $focus.focus().trigger("select");
        }

        return this;
    },

    /**
     * Unfocuses row and disables it's submit button
     *
     * @return JIRA.RestfulTable.EditRow
     */
    unfocus: function () {
        this.disable();
        this.$el.removeClass(this.FOCUSED_CLASS);
        return this;
    },

    /**
     * Disables all fields
     *
     * @return JIRA.RestfulTable.EditRow
     */
    disable: function () {

        var $replacementSumit,
            $submit;

        // firefox does not allow you to submit a form if there are 2 or more submit buttons in a form, even if all but
        // one is disabled. It also does not let you change the type="submit' to type="button". Therfore he lies the hack.
        if (jQuery.browser.mozilla) {
            
            $submit = this.$el.find(":submit");

            if ($submit.length) {

                $replacementSumit = jQuery("<input type=\"button\" class='jira-restfultable-submit' />")
                        .addClass($submit.attr("className"))
                        .val($submit.val())
                        .data("enabledSubmit", $submit);

                $submit.replaceWith($replacementSumit);
            }
        }

        this.$el.addClass("jira-resfultable-disabled")
                .find(":submit")
                .attr("disabled", "disabled");

        return this;
    },

     /**
     * Enables all fields
     *
     * @return JIRA.RestfulTable.EditRow
     */
    enable: function () {

        var $placeholderSubmit,
            $submit;

        // firefox does not allow you to submit a form if there are 2 or more submit buttons in a form, even if all but
        // one is disabled. It also does not let you change the type="submit' to type="button". Therfore he lies the hack.
        if (jQuery.browser.mozilla) {
            $placeholderSubmit = this.$el.find(".jira-restfultable-submit"),
            $submit = $placeholderSubmit.data("enabledSubmit");

            if ($submit && $placeholderSubmit.length) {
                $placeholderSubmit.replaceWith($submit);
            }
        }


        this.$el.removeClass("jira-resfultable-disabled")
                .find(":submit")
                .removeAttr("disabled");

        return this;
    },

    /**
     * Shows loading indicator
     * @return JIRA.RestfulTable.EditRow
     */
    showLoading: function () {
        this.$el.addClass(JIRA.LOADING_CLASS);
        return this;
    },

    /**
     * Hides loading indicator
     * @return JIRA.RestfulTable.EditRow
     */
    hideLoading: function () {
        this.$el.removeClass(JIRA.LOADING_CLASS);
        return this;
    },

    /**
     * If any of the fields have changed
     * @return {Boolean}
     */
    hasUpdates: function () {
        return !!this.mapSubmitParams(this.$el.serializeToObject());
    },

    mapSubmitParams: function (params) {
        return this.model.changedAttributes(params);
    },

    /**
     *
     * Handle submission of new entries and editing of old.
     *
     * @param {Boolean} focusUpdated - flag of whether to focus read-only view after succssful submission
     * @return JIRA.RestfulTable.EditRow
     */
    submit: function (focusUpdated) {


        var instance = this,
            values;

        // IE doesnt like it when the focused element is removed
        jQuery(document.activeElement).blur();

        if (this.isUpdateMode) {

            values = this.mapSubmitParams(this.$el.serializeToObject()); // serialize form fields into JSON

            if (!values) {
                return instance.trigger("cancel");
            }
        } else {

            this.model.clear();

            values = this.mapSubmitParams(this.$el.serializeToObject()); // serialize form fields into JSON
        }

        this.trigger("submitStarted");

         /* Attempt to add to server model. If fail delegate to createView to render errors etc. Otherwise,
           add a new model to this._models and render a row to represent it. */
        this.model.save(values, {

            success: function () {

                if (instance.isUpdateMode) {
                    instance.trigger("updated", instance.model, focusUpdated);
                } else {

                    instance.render({errors: {}, values: {}})
                            .trigger("created", instance.model.toJSON())
                            .trigger("focus");

                    instance.model = new instance._modelClass(); // reset
                }

                instance.trigger("submitFinished");
            },

            error: function (model, xhr, smartAjaxResponse) {

                if (smartAjaxResponse.validationError) {

                    instance.renderErrors(smartAjaxResponse.data.errors)
                            .trigger("validationError");
                }

                instance.trigger("submitFinished");
            },

            silent: true
        });

        return this;
    },
    /**
     * Render an error message
     * @param msg
     * @return {jQuery}
     */
    renderError: function (msg) {
        return jQuery("<div />").addClass("error").text(msg);
    },

    /**
     * Render and append error messages. The property name will be matched to the input name to determine which cell to
     * append the error message to. If this does not meet your needs please extend this method.
     *
     * @param errors
     */
    renderErrors: function (errors) {

        var instance = this;

        this.$(".error").remove(); // avoid duplicates

        if (errors) {
            jQuery.each(errors, function (name, msg) {
                instance.$el.find("[name='" + name + "']")
                        .closest("td")
                        .append(instance.renderError(msg));
            });
        }

        return this;
    },

    /**
     * Handles all the rendering of the create version row. This includes handling validation errors if there is any
     *
     * @param {Object} renderData
     * ... {Object} errors - Errors returned from the server on validation
     * ... {Object} vales - Values of fields
     */
    render: function  (renderData) {

        var html = "";

        // edit mode
        if (renderData.update) {

            jQuery.each(renderData.values, function (name, value) {
                if (!/id|self/.test(name)) {
                    html += "<td><input type\"text\" class=\"text\" name=\"" + name + "\" value=\"" + value + "\" />";
                    if (renderData.errors[name]) {
                        html += "<div class=\"error\">" + renderData.errors[name] + "</div>";
                    }

                    html += "</td>";
                }
            });

            html += "<td class='jira-restfultable-operations'>";
                html += "<input type='submit' value='Update' />";
                html += "<a href='#' class='cancel'>Cancel</a>";
            html += "</td>";
        }

        this.$el.html(html);

        return this;
    },

    /**
     *
     * Gets markup for add/update and cancel buttons
     *
     * @param {Boolean} update
     */
    getOperationsHTML: function (update) {
        return JIRA.RestfulTable.Templates.editOperations({
            update: !!update
        })
    }
});
