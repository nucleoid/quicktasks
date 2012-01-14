/**
 * Serializes form fields within the given element to a JSON object
 *
 * {
 *    fieldName: "fieldValue"
 * }
 *
 * @returns {Object}
 */
jQuery.fn.serializeToObject = function () {

    var data = {};

    this.find(":input:not(:button):not(:submit):not(:radio):not(:checkbox):not('select[multiple]')").each(function () {

        if (this.name === "") {
            return;
        }

        if (this.value === null) {
            this.value = "";
        }

        data[this.name] = this.value.match(/^(tru|fals)e$/i) ?
                            this.value.toLowerCase() == "true" : this.value;
    });

    this.find("input:radio:checked").each(function(){
        data[this.name] = this.value;
    });
    
    //edit by Mitch Statz, was returning "on" no matter what otherwise for checkboxes
    this.find("input:checkbox").each(function(){
    	if(jQuery(this).is(':checked'))
    		data[this.name] = true;
    	else
    		data[this.name] = false;
    });

    this.find("select[multiple]").each(function(){

        var $select = jQuery(this),
            val = $select.val();

        if ($select.data("aui-ss")) {
            if (val) {
                data[this.name] = val[0];
            } else {
                data[this.name] = "";
            }
        } else {

            if (val !== null) {
                data[this.name] = val;
            } else {
                data[this.name] = [];
            }
        }
    });

    return data;
};