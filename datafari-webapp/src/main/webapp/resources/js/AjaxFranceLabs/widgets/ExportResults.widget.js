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
AjaxFranceLabs.ExportResultsWidget = AjaxFranceLabs.AbstractWidget.extend({

	//Variables
	// field list
	fl : "",
	type : "excel",
	maxResultsToExport : 1000,
	 
	
	//Methods
	buildWidget : function() {
		self = this;
		this.clean();	
	},
	
	resultsExport : function() {
		var self = this;
		this.elm.addClass("export-prompt");
		
		// Create label
		var label = $("<label>" + window.i18n.msgStore['nbResultsToExport-label'] + " (max " + self.maxResultsToExport + "): </label>");
		
		// Create input
		var input = $("<input id='nb-results' value=500 type='number' min=1 max=" + self.maxResultsToExport + "></input>");
		
		// Create confirm button
		var confirmButton = $("<button></button>");
		confirmButton.html(window.i18n.msgStore['confirm']);
		confirmButton.click(function() {
			if(parseInt($("#nb-results").val()) < self.maxResultsToExport && parseInt($("#nb-results").val()) > 1) {
			
				// Get the number of results to export
				var nbResults = $("#nb-results").val();
				// Get the query
				var query = self.manager.store.get('q').val();
				// Get the filter queries
				var fq = self.manager.store.values("fq");
				// Get the sort option
				var sort = self.manager.store.get("sort").val();
				// Get the facet queries
				var facetQuery = self.manager.store.values("facet.query");
				// Get the facet fields
				var facetField = self.manager.store.values("facet.field");
				
				// Waiting text
				self.elm.removeClass("export-prompt");
				self.elm.addClass("blink-message");
				self.elm.html("<span id='blink-message'>" + window.i18n.msgStore['exportingResults'] + "</span>");
				function blinker() {
				    $('#blink-message').fadeOut(500);
				    $('#blink-message').fadeIn(500);
				}
				var blinkId = setInterval(blinker, 1000);
				
				// Ask to the server to create an excel export
				$.get("./ExportResults", {query:query, facetQuery:facetQuery, facetField:facetField, fq:fq, sort:sort, fl:self.fl, nbResults:nbResults, type:self.type}, function(data) {
					//Remove blink effect
					clearInterval(blinkId);
					self.elm.removeClass("blink-message");
					
					// Recreate export button
					self.clean();
					
					// if the export is successful, the server returns the file path of the generated Excel file
					if(data != null && data != undefined && data != "") {
						// Force the web browser to download the file
						document.location.href = "./DownloadFile?filePath=" + data + "&delete=true";
					}
				});
			}
		});
		
		//Create cancel button
		var cancelButton = $("<button>" + window.i18n.msgStore['cancel'] + "</button>");
		cancelButton.click(function() {self.clean();});
		
		// Clean elm then append each created element
		this.elm.html("");
		this.elm.append(label);
		this.elm.append(input);
		this.elm.append(confirmButton);
		this.elm.append(cancelButton);
	},
	
	clean : function() {
		var self = this;
		this.elm.html("<button id='export-button' class='button'>" + window.i18n.msgStore['exportResults-label'] + "</button>");
		this.elm.removeClass("export-prompt");
		$("#export-button").click(function() {self.resultsExport();});
	}
});
