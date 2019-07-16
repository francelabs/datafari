/*******************************************************************************
 * Copyright 2019 France Labs
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
AjaxFranceLabs.TagCloudWidget = AjaxFranceLabs.AbstractWidget.extend({

	//Variables
    name: null,
    pagination: false,
    nbElmToDisplay : 10,
    discardedValues : [],
    mappingValues : {},
    displayChars: 40,

	//Methods

	init : function() {
		if (!this.initialized) {
			this.initialized = true;
			this.buildWidget();
		}
	},
    
    buildWidget : function() {
        var endAnimationEvents = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
        var animation = 'animated rotateIn';
        var self = this, elm = $(this.elm);
        elm.hide();
        elm.addClass('facet').addClass('tableWidget').addClass('widget').attr('widgetId', this.id).append('<ul></ul>');
        if (this.name != null) {
            elm.prepend('<div class="facetName">').find('.facetName').append('<i class="fas fa-chevron-down"></i>').append('<span class="label la"></span>').find('.label.la').append(this.name);
            elm.find('.facetName').toggle(function() {
            $('.facetSort, ul, .pagerModule.show', $(this).parents('.tableWidget')).hide();
            elm.find(".facetName i").removeClass('fa-chevron-down')
                .addClass('fa-chevron-up ' + animation)
                .on(endAnimationEvents, function() {$(this).removeClass(animation);});
            }, function() {
            $('.facetSort, ul, .pagerModule.show', $(this).parents('.tableWidget')).show();
            elm.find(".facetName i").removeClass('fa-chevron-up')
                .addClass(animation + ' fa-chevron-down')
                .on(endAnimationEvents, function() {$(this).removeClass(animation);});
            });
        }
        
        if (this.pagination === true) {
            this.pagination = new AjaxFranceLabs.PagerModule({
            elm : this.elm,
            updateList : function() {
                if (this.nbPage > 1) {
                    $(this.source).children().css('display', 'none')
                        .slice(this.pageSelected * this.nbElmToDisplay, (this.pageSelected + 1) * this.nbElmToDisplay)
                        .css('display', this.display);
                    AjaxFranceLabs.clearMultiElementClasses($('li', this.source));
                    AjaxFranceLabs.addMultiElementClasses($('li:visible', this.source));
                }
            }
            });
            this.pagination.manager = this.manager;
        }
        if (this.pagination)
            this.pagination.init();
    },

    update : function() {
        var self = this;
        var data = this.assocTags(this.manager.response.clusters);
        var max = (data.length > this.maxDisplay) ? this.maxDisplay : data.length;
        var elm = $(this.elm);
        if (data.length == 0) {
            elm.hide();
        } else {
            elm.show();
            elm.find('ul').empty();
            let queryPrefix = "";
            let fqs = self.manager.store.values("fq");
            for(var i=0;i<fqs.length;i++) {
                queryPrefix += "fq=" + fqs[i] + "&";
            }
            for (var i = 0; i < max; i++) {
                var decodedName = decodeURIComponent(data[i].name);
                if (decodedName !== '' && $.inArray(decodedName, self.discardedValues) == -1) {
                    elm.find('ul').append('<li></li>');
                    elm.find('ul li:last').append('<label></label>');
            
                    elm.find('ul li:last label').append('<div class="filterFacetCheck"></div>').append('<div class="filterFacetLabel"></div>');
                    // escapeValue is to put quotes around strings with special characters that would be 
                    // interpreted by Solr if not into quoted strings. 
                    var decodedEscapedValue = AjaxFranceLabs.Parameter.escapeValue(decodedName);
                    
                    elm.find('ul li:last .filterFacetCheck').append('<label></label>');
                    var label = "";
                    if (!jQuery.isEmptyObject(this.mappingValues) && decodedName in this.mappingValues) {
                        label = this.mappingValues[decodedName];
                    } else {
                        label = decodedName;
                    }
                    
                    let query = queryPrefix + "q=" +self.manager.store.values("q")[0] + " " + decodedName;
                    elm.find('ul li:last .filterFacetCheck label')
                        .append('<span class="filterFacetLinkValue"><a href="/Datafari/Search?lang=' + window.i18n.language + '&request=' + encodeURIComponent(query) + '">' + AjaxFranceLabs.tinyString(label, self.displayChars) + '</a></span>')
                        .append('&nbsp;<span class="filterFacetLinkCount"></span>');
                }
            }   
        
            if (this.pagination) {
                this.pagination.source = $('ul', this.elm);
                this.pagination.updatePages();
            }
        }
    },

    assocTags : function(data) {
        var tags = [];
        for (var i = 0; i < data.length - 1; i++) {
          var encodedName = encodeURI(data[i].labels[0]);
          if (encodedName.trim() != "") {
            tags.push({
              name : encodedName,
              nb : data[i].docs.length,
              score: data[i].score
            });
          }
        }
        return tags;
    },
	
	beforeRequest : function() {
	},

	afterRequest : function() {
        this.update();
	}
});