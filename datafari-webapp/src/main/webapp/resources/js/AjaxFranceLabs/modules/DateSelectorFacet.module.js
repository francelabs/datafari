/*******************************************************************************
 * Copyright 2015 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
AjaxFranceLabs.DateSelectorFacetModule = AjaxFranceLabs.AbstractModule.extend({

	//Variables
	isMobile : $(window).width()<800,
	field : null,
	manager : null,
	id : null,
	save : null,
	hideGo : false,
	displayError : false,
	oldFilter : null,
	fromInitValue : null,
	toInitValue : null,
	yearsBeforeCurrent: 50,
	yearsAfterCurrent: 0,

	 
	
	//Methods
	execFacet : function() {
		
		// Create the filter
		var filter = this.getFilter();
		
		// Save the module before the widgets are refreshed
		this.selfSave();
		
		// Remove old filter in the store to replace it by the new one
		if(this.oldFilter != null && this.oldFilter != undefined && this.oldFilter != "") {
			this.manager.store.removeByValue("fq", this.oldFilter);
		}
		this.manager.store.addByValue("fq", filter);
		this.oldFilter = filter;
		
		// Make the request
		this.manager.makeRequest();
	},
	
	selfSave : function() {
		if($("#" + this.id + "-date-selector") != undefined  && $("#" + this.id + "-date-selector") != null && $("#" + this.id + "-date-selector").length) {
			this.save = $("#" + this.id + "-date-selector").detach();
		}
	},
	
	cleanValues : function() {
		var fqVals = this.manager.store.values("fq");
		for(var i=0; i<fqVals.length; i++) {
			if(fqVals[i] == this.getFilter()) {
				return;
			}
		}
		$("#" + this.id + "-fromInput").val("");
		$("#" + this.id + "-toInput").val("");
		$("#" + this.id + "-altFromInput").val("");
		$("#" + this.id + "-altToInput").val("");
	},
	
	getFromVal : function() {
		// Retrieve value
		var fromVal = $("#" + this.id + "-altFromInput").val();
		
		//  Replace value by wildcard if value is empty
		if(fromVal == undefined || fromVal == null || fromVal == "" || $("#" + this.id + "-fromInput").val() == "") {
			fromVal = "*";
		}
		
		return fromVal;
	},
	
	getToVal : function() {
		// Retrieve value
		var toVal = $("#" + this.id + "-altToInput").val();
		
		//  Replace value by wildcard if value is empty
		if(toVal == undefined || toVal == null || toVal == "" || $("#" + this.id + "-toInput").val() == "") {
			toVal = "*";
		}
		
		return toVal;
	},
	
	getFilter : function() {
		// Retrieve values
		var fromVal = this.getFromVal();
		var toVal = this.getToVal();
		
		return this.field + ":[" + fromVal + " TO " + toVal + "]";
	},
	
	hideGoButton : function() {
		$("#" + this.id + "-date-selector").find(".go").hide();
	}, 
	
	showGoButton : function() {
		$("#" + this.id + "-date-selector").find(".go").show();
	},
	
	// Validates that the input string is a valid date formatted as "dd/mm/yyyy"
	isValidDate : function(dateString)
	{
	    // First check for the pattern
	    if(!/^\d{1,2}\/\d{1,2}\/\d{4}$/.test(dateString)) {
	    	return false;
	    }

	    // Parse the date parts to integers
	    var parts = dateString.split("/");
	    var day = parseInt(parts[0], 10);
	    var month = parseInt(parts[1], 10);
	    var year = parseInt(parts[2], 10);

	    // Check the ranges of month and year
	    if(year < 1000 || year > 3000 || month == 0 || month > 12) {
	        return false;
	    }

	    var monthLength = [ 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 ];

	    // Adjust for leap years
	    if(year % 400 == 0 || (year % 100 != 0 && year % 4 == 0)) {
	        monthLength[1] = 29;
	    }

	    // Check the range of the day
	    var returnValue = day > 0 && day <= monthLength[month - 1];
	    return returnValue
	},
	
	checkAndUpdateFromInputs : function(dateText, inst) {
		if(dateText === "" || !this.isValidDate(dateText)) {
			$("#" + this.id + "-altFromInput").val(""); //Empty the altFromInput val as the fromInput val is empty
			if(dateText != "" && !this.isValidDate(dateText) && this.displayError) {
				$("#" + this.id + "-Message").show();
			} else if($("#" + this.id + "-toInput").val() === "" || this.isValidDate($("#" + this.id + "-toInput").val())) {
				// The toInput is also empty or valid, so we hide the error message in case it was displayed
				$("#" + this.id + "-Message").hide();
			}
		} else {
			if($("#" + this.id + "-toInput").val() === "" || this.isValidDate($("#" + this.id + "-toInput").val())) {
				// The toInput is also empty or valid, so we hide the error message in case it was displayed
				$("#" + this.id + "-Message").hide();
			}
		}
	},
	
	checkAndUpdateToInputs : function(dateText, inst) {
		if(dateText === "" || !this.isValidDate(dateText)) {
			// Date is empty or invalid, need to empty the altToInput value
			$("#" + this.id + "-altToInput").val(""); //Empty the altToInput val as the fromInput val is empty
			if(dateText != "" && !this.isValidDate(dateText) && this.displayError) {
				// Date is invalid, display an error message
				$("#" + this.id + "-Message").show();
			} else if($("#" + this.id + "-fromInput").val() === "" || this.isValidDate($("#" + this.id + "-fromInput").val())) {
				// The fromInput is also empty or valid, so we hide the error message in case it was displayed
				$("#" + this.id + "-Message").hide();
			}
		} else {
			if($("#" + this.id + "-fromInput").val() === "" || this.isValidDate($("#" + this.id + "-fromInput").val())) {
				// The fromInput is also empty or valid, so we hide the error message in case it was displayed
				$("#" + this.id + "-Message").hide();
			}
		}
	},

	createDateSelectorDiv : function() {

		// If in mobile mode deactivate this module
		if(!this.isMobile) {
			if(this.save != null && this.save != undefined) {
				// A save of the module is available ==> restore it
				this.save.appendTo(this.elm);
				this.cleanValues();
			} else {
				// It is the first time the module is called ==> generate it
				var self = this;
				this.elm.append("<div id='" + this.id + "-date-selector'></div>");
				var dateSelector = $("#" + this.id + "-date-selector");
				dateSelector.append("<span id='" + this.id + "-from-label'>" + window.i18n.msgStore["fromDate"] + "</span> <input type='text' id='" + this.id + "-fromInput' class='fromInput date-input'></input><input type='hidden' id='" + this.id + "-altFromInput'></input> <span id='" + this.id + "-to-label'>" + window.i18n.msgStore["toDate"] + "</span> <input type='text' id='" + this.id + "-toInput' class='toInput date-input'></input><input type='hidden' id='" + this.id + "-altToInput'></input> <a class='go'>Go</a> <span class='date-format-error' id='" + this.id + "-Message'>" + window.i18n.msgStore['wrong-date-format'] + "</span>");
				dateSelector.find(".fromInput").datepicker({
					dateFormat: "dd/mm/yy", 
					altFormat: "yy-mm-ddT00:00:00Z", 
					changeMonth: true, 
					changeYear: true, 
					onClose: function(dateText, inst){
						self.checkAndUpdateFromInputs(dateText, inst)
					}, 
					onSelect: function(dateText, inst) {
						// Used to keep the focus and being able to trigger the search by pressing Enter
						$("#" + self.id + "-fromInput").focus()
					}
				});
				dateSelector.find(".fromInput").datepicker( "option", "altField", "#" + this.id + "-altFromInput" );
				dateSelector.find(".fromInput").datepicker( "option", "yearRange", "c-"+ self.yearsBeforeCurrent + ":c+" +  self.yearsAfterCurrent);
				dateSelector.find(".toInput").datepicker({
					dateFormat: "dd/mm/yy", 
					altFormat: "yy-mm-ddT00:00:00Z", 
					changeMonth: true, 
					changeYear: true, 
					onClose: function(dateText, inst){
						self.checkAndUpdateToInputs(dateText, inst)
					}, 
					onSelect: function(dateText, inst) {
						// Used to keep the focus and being able to trigger the search by pressing Enter
						$("#" + self.id + "-toInput").focus()
					}
				});
				dateSelector.find(".toInput").datepicker( "option", "altField", "#" + this.id + "-altToInput" );
				dateSelector.find(".toInput").datepicker( "option", "yearRange", "c-"+ self.yearsBeforeCurrent + ":c+" +  self.yearsAfterCurrent );
				
				// Init values if possible
				if(self.fromInitValue != null) {
					var fromDate = self.convertAltDateFormatToDateFormat(self.fromInitValue);
					dateSelector.find(".fromInput").val(fromDate);
					dateSelector.find(".fromInput").datepicker("setDate", fromDate);
				}
				if(self.toInitValue != null) {
					var toDate = self.convertAltDateFormatToDateFormat(self.toInitValue);
					dateSelector.find(".toInput").val(toDate);
					dateSelector.find(".toInput").datepicker("setDate", toDate);
				}
				
				// Hide the Go button if it has been specified in the constructor
				if(this.hideGo === true) {
					this.hideGoButton();
				}
				
				// Set the onClick function of the Go button
				dateSelector.find(".go").click(function() {
					self.execFacet();
				});
				
				// When user press enter when focus is on the text inputs
				$("#" + this.id + "-fromInput").keypress(function(e) {
				    if(e.which == 13) {
				    	self.execFacet();
				    }
				});
				$("#" + this.id + "-toInput").keypress(function(e) {
				    if(e.which == 13) {
				    	self.execFacet();
				    }
				});
			}
		} 
	},
	
	// converts a string date formated like "YYYY-MM-DDT00:00:00Z" into a string formated like "DD/MM/YYYY"
	convertAltDateFormatToDateFormat : function(altFormatDate) {
		if(altFormatDate == "") {
			return "";
		} else {
			var dateRegex = /[0-9]+-[0-9]+-[0-9]+/g;
			var altDate = altFormatDate.match(dateRegex);
			if(altDate != null && altDate != undefined && altDate.length > 0) {
				var splittedDate = altDate[0].split("-");
				var year = splittedDate[0];
				var month = splittedDate[1];
				var day = splittedDate[2];
				var formatedDate = day + "/" + month + "/" + year;
				return formatedDate;
			} else {
				return "";
			}
		}
	},
	
	beforeRequest : function() {
		this.selfSave();
	},
	
	afterRequest : function() {
		this.createDateSelectorDiv();
	}
});