/**
 * 
 * Copyright France Labs Licensed under the Apache License, V2.0 | http://www.francelabs.com/en/AjaxFranceLabs/licence
 * 
 * @class AjaxFranceLabs.TableMobileWidget
 * @extends AjaxFranceLabs.AbstractFacetWidget
 * 
 * @documentation http://www.francelabs.com/en/AjaxFranceLabs/widget-module-documentation#table_widget
 * 
 */
AjaxFranceLabs.TableMobileWidget = AjaxFranceLabs.AbstractFacetWidget.extend({

  // Variables

  name : null,

  pagination : false,

  nbElmToDisplay : 10,

  sort : 'occurences',

  maxDisplay : 40,

  checkedOnTop : true,

  type : 'table',

  'facet.field' : true,

  // Methods

  buildWidget : function() {
    // The animations are written in animate.min.css. The javascript is used only to toggle the classes
    var endAnimationEvents = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
    var animation = 'animated rotateIn';
    var self = this, elm = $(this.elm);
    elm.hide();
    elm.addClass('facet').addClass('tableWidget').addClass('widget').attr('widgetId', this.id).append('<ul class="ultest"">');
    if (this.name != null) {
      elm.prepend('<div class="facetName">').find('.facetName').append('<i class="fas fa-chevron-down">').append('<span class="label la">').find('.label.la').append(this.name);
      elm.find('.facetName').click(function() {
        $('.facetSort, ul, .pagerModule.show', $(this).parents('.tableWidget')).toggle();
        var chevron = elm.find(".facetName i");
        if (chevron.hasClass("fa-chevron-down")) {
          chevron.removeClass('fa-chevron-down').addClass('fa-chevron-up ' + animation).on(endAnimationEvents, function() {
            $(this).removeClass(animation);
          });
        } else {
          chevron.removeClass('fa-chevron-up').addClass(animation + ' fa-chevron-down').on(endAnimationEvents, function() {
            $(this).removeClass(animation);
          });
        }
      });

      elm.find('.hide_show').click(function() {
        $('.facetSort, ul, .pagerModule', $(this).parents('.tableWidget')).toggle();
        $(this).toggleClass('close');
      });

    }
    elm.find('.facetSort').append('<label></label>').find('label').append(window.i18n.msgStore['sortFacet']).append('<select></select>').find('select').append('<option><span>A/Z</span></option>')
        .append('<option><span>Z/A</span></option>').append('<option><span>Occurences</span></option>').change(function(event) {
          switch ($('option:selected', this).index()) {
          case 0:
            self.sort = 'AtoZ';
            break;
          case 1:
            self.sort = 'ZtoA';
            break;
          case 2:
            self.sort = 'occurences';
            break;
          }
          self.sortBy(self.sort);
        });
    switch (this.sort) {
    case 'AtoZ':
      $(this.elm).find('.facetSort option:eq(0)').attr('selected', 'selected');
      break;
    case 'ZtoA':
      $(this.elm).find('.facetSort option:eq(1)').attr('selected', 'selected');
      break;
    case 'occurences':
      $(this.elm).find('.facetSort option:eq(2)').attr('selected', 'selected');
      break;
    }
    if (this.pagination === true) {
      this.pagination = new AjaxFranceLabs.PagerModule({
        elm : this.elm,
        updateList : function() {
          if (this.nbPage > 1) {
            $(this.source).children().css('display', 'none').slice(this.pageSelected * this.nbElmToDisplay, (this.pageSelected + 1) * this.nbElmToDisplay).css('display', this.display);
            AjaxFranceLabs.clearMultiElementClasses($('li', this.source));
            AjaxFranceLabs.addMultiElementClasses($('li:visible', this.source));
          } else {
            $(this).hide();
          }
        }
      });
      this.pagination.manager = this.manager;
    }
    if (this.pagination)
      this.pagination.init();
  },

  update : function() {
    $("#spinner_mobile").hide();
    var self = this, data = this.assocTags(this.manager.response.facet_counts.facet_fields[this.field]), max = (data.length > this.maxDisplay) ? this.maxDisplay : data.length, elm = $(this.elm);
    if (data.length == 0) {
      elm.hide();
    } else {
      elm.show();
      elm.find('ul').empty();
      for (var i = 0; i < max; i++) {
        var encodedName = encodeURI(data[i].name);
        if (data[i].name !== '') {
          elm.find('ul').append('<li></li>');
          elm.find('ul li:last').append('<label></label>');

          elm.find('ul li:last label').append('<div class="filterFacetCheck"></div>').append('<div class="filterFacetLabel"></div>');
          elm.find('ul li:last .filterFacetCheck').append('<input type="checkbox" value="' + data[i].name + '"/>');
          elm.find('ul li:last .filterFacetCheck input').attr('id', "mobile-" + self.id + "-" + encodedName);
          if (self.selectionType != 'OR' && this.manager.store.find('fq', self.field + ':' + AjaxFranceLabs.Parameter.escapeValue(data[i].name))) {
            elm.find('ul li:last .filterFacetCheck input').attr('checked', 'checked').parents('li').addClass('selected');
          } else if (this.manager.store.find('fq', new RegExp(self.field + ':'
              + AjaxFranceLabs.Parameter.escapeValue(data[i].name.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\").replace(/\\/g, "\\\\")) + '[ )]'))) {
            elm.find('ul li:last .filterFacetCheck input').attr('checked', 'checked').parents('li').addClass('selected');
          }
          elm.find('ul li:last .filterFacetCheck input').change(function() {
            if ($(this).is(':checked')) {
              if (self.selectionType === 'ONE') {
                elm.find('ul li .filterFacetCheck input:checked').not(this).each(function() {
                  self.remove($(this).val());
                });
              }
              self.clickHandler();
              self.selectHandler($(this).val().replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\"));
            } else {
              self.clickHandler();
              self.unselectHandler($(this).val().replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\"));
            }
          });
          elm.find('ul li:last .filterFacetCheck').append('<label></label>');
          if (elm.find('ul li:last .filterFacetCheck input').is(':checked'))
            elm.find('ul li:last .filterFacetCheck label').attr('for', "mobile-" + self.id + "-" + encodedName).append(
                '<span class="checkboxIcon far fa-check-square">&nbsp;</span>' + '<span class="filterFacetLinkValue">' + AjaxFranceLabs.tinyString(data[i].name, 19) + '</span>').append(
                '&nbsp;<span class="filterFacetLinkCount">(<span>' + data[i].nb + '</span>)</span>');
          else
            elm.find('ul li:last .filterFacetCheck label').attr('for', "mobile-" + self.id + "-" + encodedName).append(
                '<span class="checkboxIcon far fa-square">&nbsp;</span>' + '<span class="filterFacetLinkValue">' + AjaxFranceLabs.tinyString(data[i].name, 19) + '</span>').append(
                '&nbsp;<span class="filterFacetLinkCount">(<span>' + data[i].nb + '</span>)</span>');
        }
      }
      if (this.pagination) {
        this.pagination.source = $('ul', this.elm);
        this.pagination.updatePages();
      }
      this.sortBy(this.sort);
    }
  },

  assocTags : function(data) {
    var tags = [];
    for (var i = 0; i < data.length - 1; i++) {
      tags.push({
        name : data[i],
        nb : data[i + 1]
      });
      i++;
    }
    return tags;
  },

  sortBy : function(sort) {
    var elm = $(this.elm);
    switch (sort) {
    case 'AtoZ':
      if (this.checkedOnTop === true) {
        elm.find('ul').prepend($(this.elm).find('ul li .filterFacetCheck input:checked').parents('li'));
        elm.find('ul li .filterFacetCheck input:checked').parents('li').each(
            function() {
              var $this = this;
              $(this).nextAll().each(
                  function() {
                    if ($('.filterFacetCheck .filterFacetLinkValue', $this).text().toLowerCase() > $('.filterFacetCheck .filterFacetLinkValue', this).text().toLowerCase()
                        && $('.filterFacetCheck input', this).is(':checked'))
                      $(this).after($($this).detach());
                  });
            });
        elm.find('ul li .filterFacetCheck input:not(:checked)').parents('li').each(function() {
          var $this = this;
          $(this).nextAll().each(function() {
            if ($('.filterFacetCheck .filterFacetLinkValue', $this).text().toLowerCase() > $('.filterFacetCheck .filterFacetLinkValue', this).text().toLowerCase())
              $(this).after($($this).detach());
          });
        });
      } else {
        elm.find('ul li').each(function() {
          var $this = this;
          $(this).nextAll().each(function() {
            if ($('.filterFacetCheck .filterFacetLinkValue', $this).text().toLowerCase() > $('.filterFacetCheck .filterFacetLinkValue', this).text().toLowerCase())
              $(this).after($($this).detach());
          });
        });
      }
      break;
    case 'ZtoA':
      /*
       * elm.find('ul').prepend($('ul li .filterFacetCheck input:checked', elm).parents('li')); elm.find('ul li .filterFacetCheck
       * input:checked').parents('li').each(function() { var $this = this; $(this).nextAll().each(function() { if ($('.filterFacetCheck
       * .filterFacetLinkValue', $this).text().toLowerCase() < $('.filterFacetCheck .filterFacetLinkValue', this).text().toLowerCase() &&
       * $('.filterFacetCheck input', this).is(':checked')) $(this).after($($this).detach()); }); }); elm.find('ul li .filterFacetCheck
       * input:not(:checked)').parents('li').each(function() { var $this = this; $(this).nextAll().each(function() { if
       * ($('.filterFacetCheck .filterFacetLinkValue', $this).text().toLowerCase() < $('.filterFacetCheck .filterFacetLinkValue',
       * this).text().toLowerCase()) $(this).after($($this).detach()); }); });
       */

      elm.find('ul li').each(function() {
        var $this = this;
        $(this).nextAll().each(function() {
          if ($('.filterFacetCheck .filterFacetLinkValue', $this).text().toLowerCase() < $('.filterFacetCheck .filterFacetLinkValue', this).text().toLowerCase())
            $(this).after($($this).detach());
        });
      });

      break;
    case 'occurences':
      elm.find('ul li').each(function() {
        var $this = this;
        $(this).nextAll().each(function() {
          if (parseInt($('.filterFacetCheck .filterFacetLinkCount span', $this).text()) < parseInt($('.filterFacetCheck .filterFacetLinkCount span', this).text()))
            $(this).after($($this).detach());
        });
      });
      break;
    }
    if (this.pagination)
      this.pagination.updateList();
    AjaxFranceLabs.clearMultiElementClasses($(this.elm).find('ul li'));
    AjaxFranceLabs.addMultiElementClasses($(this.elm).find('ul li:visible'));
  },

  clickHandler : function() {
    $("#spinner_mobile").show();
    $("#results .doc_list").empty();
  },

  afterRequest : function() {
    this.update();
  }
});
