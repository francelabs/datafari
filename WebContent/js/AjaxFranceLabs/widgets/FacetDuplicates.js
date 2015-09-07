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
AjaxFranceLabs.FacetDuplicates = AjaxFranceLabs.TableWidget.extend({

	/* 
	 * this method is used to build the HTML of the facet
	 */

	update : function() {
		var self = this, data = this.assocTags(this.manager.response.facet_counts.facet_fields[this.field]), max = (data.length > this.maxDisplay) ? this.maxDisplay : data.length, elm = $(this.elm), compteur = 0, names = [];
		// compteur is a counter used to iterate in all hashes that should be converted in names
		if (data.length!==0){
			//show the facet signature if we have a duplicated file
			$("#facet_signature").show();
			$.get(self.manager.serverUrl+'select?q=*%3A*&rows=1&fl=title&wt=json&indent=true&hl=off&_=1433765381298&fq=signature%3A%22'+data[compteur].name+'%22',function recursive(responseJSON){
				names[compteur]=responseJSON.response.docs[0].title[0]; // we save the response in array
				compteur++;
				if (compteur<data.length)
					$.get(self.manager.serverUrl+'select?q=*%3A*&rows=1&fl=title&wt=json&indent=true&hl=off&_=1433765381298&fq=signature%3A%22'+data[compteur].name+'%22',recursive);
				else{
					elm.find('ul').empty();
					for (var i = 0; i < max; i++) {
						if (names[i] !== ''){
							elm.find('ul').append('<li>');
							elm.find('ul li:last').append('<label>');
							
							
							elm.find('ul li:last label').append('<div class="filterFacetCheck">').append('<div class="filterFacetLabel">');
							elm.find('ul li:last .filterFacetCheck').append('<input type="checkbox" value="' + data[i].name + '"/>');
							elm.find('ul li:last .filterFacetCheck input').attr('id',data[i].name);
							if (self.manager.store.find('fq', new RegExp(self.field + ':' + AjaxFranceLabs.Parameter.escapeValue(data[i].name.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&").replace(/\\/g,"\\\\")) + '[ )]')))
								elm.find('ul li:last .filterFacetCheck input').attr('checked', 'checked').parents('li').addClass('selected');
							elm.find('ul li:last .filterFacetCheck input').change(function() {
								if ($(this).attr('checked') == 'checked') {
									if(self.selectionType === 'ONE' && elm.find('ul li .filterFacetCheck input:checked').not(self).length)
										self.remove(elm.find('ul li .filterFacetCheck input:checked').not(this).val());
									self.clickHandler();
									self.selectHandler($(this).val().replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&"));
								} else {
									self.clickHandler();
									self.unselectHandler($(this).val().replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&"));
								}
							});
							elm.find('ul li:last .filterFacetCheck').append('<label>');
							if (elm.find('ul li:last .filterFacetCheck input').attr('checked')== 'checked' )
								elm.find('ul li:last .filterFacetCheck label').attr('for', data[i].name).append('<span class="checkboxIcon fa fa-check-square-o">&nbsp;</span>'+'<span class="filterFacetLinkValue">'+AjaxFranceLabs.tinyString(names[i], 19)+'</span>').append('&nbsp;<span class="filterFacetLinkCount">(<span>' + data[i].nb + '</span>)</span>');
							else 
								elm.find('ul li:last .filterFacetCheck label').attr('for', data[i].name).append('<span class="checkboxIcon fa fa-square-o">&nbsp;</span>'+'<span class="filterFacetLinkValue">'+AjaxFranceLabs.tinyString(names[i], 19)+'</span>').append('&nbsp;<span class="filterFacetLinkCount">(<span>' + data[i].nb + '</span>)</span>');
						}
					}
					if (self.pagination) {
						self.pagination.source = $('ul', self.elm);
						self.pagination.updatePages();
					}
					self.sortBy(self.sort);
				}
			});
		}else{
			// if there's no duplicated file, hide the facet
			$("#facet_signature").hide();
		}
	},
		
});