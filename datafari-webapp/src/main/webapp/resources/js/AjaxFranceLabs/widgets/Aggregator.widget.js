/*******************************************************************************************************************************************
 * Copyright 2015 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 ******************************************************************************************************************************************/
AjaxFranceLabs.AggregatorWidget = AjaxFranceLabs.AbstractWidget.extend({
  // Variables

  name: null,

  pagination: true,

  nbElmToDisplay: 10,

  sort: "occurences",

  displayChars: 40,

  checkedOnTop: true,

  remoteList: [],

  // Methods

  buildWidget: function () {
    var endAnimationEvents =
      "webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend";
    var animation = "animated rotateIn";
    var self = this;
    var elm = $(this.elm);
    elm.hide();
    elm
      .addClass("facet")
      .addClass("tableWidget")
      .addClass("widget")
      .attr("widgetId", this.id)
      .append(
        "<div class='facet-actions facet-tooltip'><a class='button' id='aggregator-remove-all'>" +
          window.i18n.msgStore["facet_external_datafaris_clear_filter"] +
          "</a></div>"
      )
      .append("<ul></ul>")
      .append("<button>" + window.i18n.msgStore["validate"] + "</button>");

      elm.find(".facet-actions").append("<span class='facet-tooltip-text'>" + 
        window.i18n.msgStore["facet_external_datafaris_unchecks_all_options"] + 
        "</span>")

    elm.find("#aggregator-remove-all").click(self.removeAllHandler.bind(self));
    elm.find("button").click(self.validateClick.bind(self));
    if (this.name != null) {
      elm
        .prepend('<div class="facetName">')
        .find(".facetName")
        .append('<i class="fas fa-chevron-down"></i>')
        .append('<span class="label la"></span>')
        .find(".label.la")
        .append(this.name);
      elm.find(".facetName").click(function () {
        $(
          ".facetSort, ul, .pagerModule.show, div.facet-actions, button",
          $(this).parents(".tableWidget")
        ).toggle();
        var chevron = elm.find(".facetName i");
        if (chevron.hasClass("fa-chevron-down")) {
          chevron
            .removeClass("fa-chevron-down")
            .addClass("fa-chevron-up " + animation)
            .on(endAnimationEvents, function () {
              $(this).removeClass(animation);
            });
        } else {
          chevron
            .removeClass("fa-chevron-up")
            .addClass(animation + " fa-chevron-down")
            .on(endAnimationEvents, function () {
              $(this).removeClass(animation);
            });
        }
      });
    }
    elm
      .find(".facetSort")
      .append("<label></label>")
      .find("label")
      .append(window.i18n.msgStore["sortFacet"])
      .append("<select></select>")
      .find("select")
      .append("<option><span>A/Z</span></option>")
      .append("<option><span>Z/A</span></option>")
      .change(function (event) {
        switch ($("option:selected", this).index()) {
          case 0:
            self.sort = "AtoZ";
            break;
          case 1:
            self.sort = "ZtoA";
            break;
          default:
            self.sort = "AtoZ";
            break;
        }
        self.sortBy(self.sort);
      });
    switch (this.sort) {
      case "AtoZ":
        $(this.elm)
          .find(".facetSort option:eq(0)")
          .attr("selected", "selected");
        break;
      case "ZtoA":
        $(this.elm)
          .find(".facetSort option:eq(1)")
          .attr("selected", "selected");
        break;
      default:
        $(this.elm)
          .find(".facetSort option:eq(0)")
          .attr("selected", "selected");
        break;
    }
    if (this.pagination === true) {
      this.pagination = new AjaxFranceLabs.PagerModule({
        elm: this.elm,
        updateList: function () {
          if (this.nbPage > 1) {
            $(this.source)
              .children()
              .css("display", "none")
              .slice(
                this.pageSelected * this.nbElmToDisplay,
                (this.pageSelected + 1) * this.nbElmToDisplay
              )
              .css("display", this.display);
            AjaxFranceLabs.clearMultiElementClasses($("li", this.source));
            AjaxFranceLabs.addMultiElementClasses($("li:visible", this.source));
          }
        },
      });
      this.pagination.manager = this.manager;
    }
    if (this.pagination) this.pagination.init();
    this.populateRemoteList();
  },

  update: function () {
    var self = this;
    var data = this.remoteList;
    var elm = $(this.elm);
    if (data.length == 0) {
      elm.hide();
    } else {
      elm.show();
      elm.find("ul").empty();
      for (var i = 0; i < data.length; i++) {
        var decodedName = decodeURIComponent(data[i].label);
        if (decodedName !== "") {
          elm.find("ul").append("<li></li>");
          elm.find("ul li:last").append("<label></label>");

          elm
            .find("ul li:last label")
            .append('<div class="filterFacetCheck"></div>')
            .append('<div class="filterFacetLabel"></div>');
          elm
            .find("ul li:last .filterFacetCheck")
            .append(
              '<input type="checkbox" value="' +
                self.encodeForHtmlProperty(decodedName) +
                '"/>'
            );
          elm
            .find("ul li:last .filterFacetCheck input")
            .attr(
              "id",
              self.id + "-" + self.encodeForHtmlProperty(data[i].label)
            );
          // escapeValue is to put quotes around strings with special characters that would be
          // interpreted by Solr if not into quoted strings.
          var decodedEscapedValue = AjaxFranceLabs.Parameter.escapeValue(
            decodedName
          );
          if (data[i].selected) {
            elm
              .find("ul li:last .filterFacetCheck input")
              .attr("checked", "checked")
              .parents("li")
              .addClass("selected");
          }
          elm.find("ul li:last .filterFacetCheck input").change(function () {
            var name = $(this)
              .attr("id")
              .slice(self.id.length + 1);
            self.clickHandler(self, name, $(this).is(":checked"));
          });
          elm.find("ul li:last .filterFacetCheck").append("<label></label>");
          var label = decodedName;
          var checkboxClass = "fa-square";
          if (elm.find("ul li:last .filterFacetCheck input").is(":checked")) {
            checkboxClass = "fa-check-square";
          }
          elm
            .find("ul li:last .filterFacetCheck label")
            .attr(
              "for",
              self.id + "-" + self.encodeForHtmlProperty(data[i].label)
            )
            .append(
              '<span class="checkboxIcon far ' +
                checkboxClass +
                '">&nbsp;</span>' +
                '<span class="filterFacetLinkValue">' +
                AjaxFranceLabs.tinyString(label, self.displayChars) +
                "</span>"
            );
        }
      }

      if (this.pagination) {
        this.pagination.source = $("ul", this.elm);
        this.pagination.updatePages();
      }
      this.sortBy(this.sort);
    }
  },

  encodeForHtmlProperty: function (str) {
    return str.replace(/"/g, "&quot;");
  },

  sortBy: function (sort) {
    var elm = $(this.elm);
    switch (sort) {
      case "AtoZ":
        if (this.checkedOnTop === true) {
          elm
            .find("ul")
            .prepend(
              $(this.elm)
                .find("ul li .filterFacetCheck input:checked")
                .parents("li")
            );
          elm
            .find("ul li .filterFacetCheck input:checked")
            .parents("li")
            .each(function () {
              var $this = this;
              $(this)
                .nextAll()
                .each(function () {
                  if (
                    $(".filterFacetCheck .filterFacetLinkValue", $this)
                      .text()
                      .toLowerCase() >
                      $(".filterFacetCheck .filterFacetLinkValue", this)
                        .text()
                        .toLowerCase() &&
                    $(".filterFacetCheck input", this).is(":checked")
                  )
                    $(this).after($($this).detach());
                });
            });
          elm
            .find("ul li .filterFacetCheck input:not(:checked)")
            .parents("li")
            .each(function () {
              var $this = this;
              $(this)
                .nextAll()
                .each(function () {
                  if (
                    $(".filterFacetCheck .filterFacetLinkValue", $this)
                      .text()
                      .toLowerCase() >
                    $(".filterFacetCheck .filterFacetLinkValue", this)
                      .text()
                      .toLowerCase()
                  )
                    $(this).after($($this).detach());
                });
            });
        } else {
          elm.find("ul li").each(function () {
            var $this = this;
            $(this)
              .nextAll()
              .each(function () {
                if (
                  $(".filterFacetCheck .filterFacetLinkValue", $this)
                    .text()
                    .toLowerCase() >
                  $(".filterFacetCheck .filterFacetLinkValue", this)
                    .text()
                    .toLowerCase()
                )
                  $(this).after($($this).detach());
              });
          });
        }
        break;
      case "ZtoA":
        elm.find("ul li").each(function () {
          var $this = this;
          $(this)
            .nextAll()
            .each(function () {
              if (
                $(".filterFacetCheck .filterFacetLinkValue", $this)
                  .text()
                  .toLowerCase() <
                $(".filterFacetCheck .filterFacetLinkValue", this)
                  .text()
                  .toLowerCase()
              )
                $(this).after($($this).detach());
            });
        });

        break;
      case "occurences":
        elm.find("ul li").each(function () {
          var $this = this;
          $(this)
            .nextAll()
            .each(function () {
              if (
                parseInt(
                  $(
                    ".filterFacetCheck .filterFacetLinkCount span",
                    $this
                  ).text()
                ) <
                parseInt(
                  $(".filterFacetCheck .filterFacetLinkCount span", this).text()
                )
              )
                $(this).after($($this).detach());
            });
        });
        break;
    }
    if (this.pagination) this.pagination.updateList();
    AjaxFranceLabs.clearMultiElementClasses($(this.elm).find("ul li"));
    AjaxFranceLabs.addMultiElementClasses($(this.elm).find("ul li:visible"));
  },

  populateRemoteList: function () {
    var self = this;
    $.get("./aggregatorList").done(function (data) {
      if (Array.isArray(data) && data.length != 0) {
        self.remoteList = data;
      } else {
        self.remoteList = [];
      }
      self.update();
    });
  },

  clickHandler: function (self, label, checked) {
    for (var i = 0; i < self.remoteList.length; i++) {
      if (self.encodeForHtmlProperty(self.remoteList[i].label) == label) {
        self.remoteList[i].selected = checked;
        break;
      }
    }
    self.update();
    self.beforeRequest();
  },

  beforeRequest: function () {
    this.manager.store.remove("aggregator");
    if (this.remoteList && this.remoteList.length > 0) {
      var selectedStr = this.remoteList.reduce(function (acc, currentValue) {
        if (currentValue.selected) {
          if (acc) {
            acc += ",";
          }
          acc += currentValue.label;
        }
        return acc;
      }, "");
      this.manager.store.addByValue("aggregator", selectedStr);
    }
  },

  afterRequest: function () {
    if (this.manager.store.isParamDefined("aggregator")) {
      selectedStr = this.manager.store.get("aggregator").val();
      if (selectedStr != null && selectedStr != undefined) {
        var selectedList = selectedStr.split(",");
        this.updateRemoteStates(selectedList);
      }
    }
    this.update();
  },

  updateRemoteStates: function (selectedList) {
    var self = this;
    self.remoteList.map(function (element) {
      if (selectedList.indexOf(element.label) != -1) {
        element.selected = true;
      } else {
        element.selected = false;
      }
    });
  },

  validateClick: function () {
    this.manager.makeRequest();
  },

  addAllHandler: function () {
    this.remoteList.map(function (data) {
      data.selected = true;
    });
    this.update();
  },

  removeAllHandler: function () {
    this.remoteList.map(function (data) {
      data.selected = false;
    });
    this.update();
  },
});
