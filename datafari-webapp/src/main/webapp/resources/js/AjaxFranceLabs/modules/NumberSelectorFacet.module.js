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
AjaxFranceLabs.NumberSelectorFacetModule = AjaxFranceLabs.AbstractModule.extend({

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
  multiplier : {},

	 
	
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
		if($("#" + this.id + "-number-selector") != undefined  && $("#" + this.id + "-number-selector") != null && $("#" + this.id + "-number-selector").length) {
			this.save = $("#" + this.id + "-number-selector").detach();
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
	},
	
	getFromVal : function() {
		// Retrieve value
		var fromVal = $("#" + this.id + "-fromInput").val();
		
		//  Replace value by wildcard if value is empty
		if(fromVal == undefined || fromVal == null || fromVal == "") {
			fromVal = "*";
		}
		
		return fromVal;
	},
	
	getToVal : function() {
		// Retrieve value
		var toVal = $("#" + this.id + "-toInput").val();
		
		//  Replace value by wildcard if value is empty
		if(toVal == undefined || toVal == null || toVal == "") {
			toVal = "*";
		}
		
		return toVal;
	},
  
  getFromMultiplicator : function() {
    // Retrieve value
    var fromMultiplicator = $("#" + this.id + "-fromSelect").val();
    
    //  Replace value by wildcard if value is empty
    if(fromMultiplicator == undefined || fromMultiplicator == null || fromMultiplicator == "") {
      fromMultiplicator = 1;
    } else {
      fromMultiplicator = parseInt(fromMultiplicator);
    }
    
    return fromMultiplicator;
  },
  
  getToMultiplicator : function() {
    // Retrieve value
    var toMultiplicator = $("#" + this.id + "-toSelect").val();
    
    //  Replace value by wildcard if value is empty
    if(toMultiplicator == undefined || toMultiplicator == null || toMultiplicator == "") {
      toMultiplicator = 1;
    } else {
      toMultiplicator = parseInt(toMultiplicator);
    }
    
    return toMultiplicator;
  },
	
	getFilter : function() {
		// Retrieve values
		var fromVal = this.getFromVal();
		var toVal = this.getToVal();
    
    var fromMultiplicator = this.getFromMultiplicator();
    var toMultiplicator = this.getToMultiplicator();
    
    var finalFromVal="";
    if(fromVal == "*") {
      finalFromVal = fromVal;
    } else {
      finalFromVal = (parseInt(fromVal) * fromMultiplicator);
    }
    
    var finalToVal="";
    if(toVal == "*") {
      finalToVal = toVal;
    } else {
      finalToVal = (parseInt(toVal) * toMultiplicator);
    }
		
		return this.field + ":[" + finalFromVal + " TO " + finalToVal + "]";
	},
	
	hideGoButton : function() {
		$("#" + this.id + "-number-selector").find(".go").hide();
	}, 
	
	showGoButton : function() {
		$("#" + this.id + "-number-selector").find(".go").show();
	},

	createNumberSelectorDiv : function() {

		// If in mobile mode deactivate this module
		if(!this.isMobile) {
			if(this.save != null && this.save != undefined) {
				// A save of the module is available ==> restore it
				this.save.appendTo(this.elm);
				this.cleanValues();
			} else {
				// It is the first time the module is called ==> generate it
				var self = this;
				this.elm.append("<div id='" + this.id + "-number-selector'></div>");
				var numberSelector = $("#" + this.id + "-number-selector");
        
        var options="";
        Object.entries(self.multiplier).forEach(
          ([label, multiplicator]) => options+="<option value='" + multiplicator + "'>" + label + "</option>");
        var hiddenMultiplicators="";
        if(options == "") {
          hiddenMultiplicators="style='display:none;'";
        }
        
				numberSelector.append("<span id='" + this.id + "-from-label' class='numSelLabel'>" + window.i18n.msgStore["fromLabel"] + "</span> <input type='number' id='" + this.id + "-fromInput' class='fromInput num-input'></input> <select id='" + this.id + "-fromSelect' " + hiddenMultiplicators + ">" + options + "</select></br><span id='" + this.id + "-to-label' class='numSelLabel'>" + window.i18n.msgStore["toLabel"] + "</span> <input type='number' id='" + this.id + "-toInput' class='toInput num-input'></input> <select id='" + this.id + "-toSelect'  " + hiddenMultiplicators + ">" + options + "</select> <a class='go'>Go</a>");
				
				// Init values if possible
				if(self.fromInitValue != null && !isNaN(self.fromInitValue)) {
					numberSelector.find(".fromInput").val(self.fromInitValue);
				}
				if(self.toInitValue != null && !isNaN(self.toInitValue)) {
					numberSelector.find(".toInput").val(self.toInitValue);
				}
				
				// Hide the Go button if it has been specified in the constructor
				if(this.hideGo === true) {
					this.hideGoButton();
				}
				
				// Set the onClick function of the Go button
				numberSelector.find(".go").click(function() {
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
	
	beforeRequest : function() {
		this.selfSave();
	},
	
	afterRequest : function() {
		this.createNumberSelectorDiv();
	}
});