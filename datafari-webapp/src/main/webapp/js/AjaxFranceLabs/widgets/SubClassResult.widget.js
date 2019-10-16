String.prototype.truncate = function(length, length_show) {
  var string = this.valueOf();

  if (string.length > length) {
    var between_txt = string.substring(length_show, string.length - length_show);

    return string.replace(between_txt, "...");
  } else {
    return string;
  }
}

AjaxFranceLabs.SubClassResultWidget = AjaxFranceLabs.ResultWidget.extend({
  elmSelector : '#results',
  id : 'documents',
  pagination : true,
  firstTimeWaypoint : true,
  isMobile : $(window).width() < 800,
  mutex_locked : false,
  availableImages : {},
  previewDisplayer : null,

  buildWidget : function() {
    var self = this;
    this.elm = $(this.elmSelector);
    this._super();

    this.previewDisplayer = new AjaxFranceLabs.PreviewDisplayer({
      id : 'previewDisplayer'
    })

    // Initialize the queryElevator module if possible (if not, that means that
    // the user is not an administrator and is not allowed to use it)
    if (typeof AjaxFranceLabs.QueryElevatorModule === 'function') {
      this.queryElevator = new AjaxFranceLabs.QueryElevatorModule();
      this.queryElevator.setParentWidget(this);
      this.manager.addModule(this.queryElevator);
    }

    if (typeof AjaxFranceLabs.RelevancyQueryModule === 'function') {
      this.relevancyQuery = new AjaxFranceLabs.RelevancyQueryModule();
      this.relevancyQuery.setParentWidget(this);
      this.manager.addModule(this.relevancyQuery);
      this.relevancyQuery.cleanGlobalLinks();
    }
  },

  beforeRequest : function() {
    var self = this;
    if (!self.pagerRequest || !self.isMobile) {
      self.elm.find('.doc_list').empty();
      self.elm.find('.doc_list').append('<div class="bar-loader" />');
    } else {
      self.pagerRequest = false;
    }
    if (typeof self.queryElevator !== 'undefined') {
      var query = $('.searchBar input[type=text]').val();
      if (query === "") {
        query = "*:*";
      }
      self.queryElevator.initElevatedDocs(query);
    }

    if (this.pagination) {
      this.pagination.beforeRequest();
    }
  },

  afterRequest : function() {
    var data = this.manager.response, elm = $(this.elm), self = this;

    elm.find('.doc_list .bar-loader').remove();
    if (data.response.numFound === 0) {
      elm.find('.doc_list').append('<div class="doc no"><div class="res no"><span class="title noResult">' + window.i18n.msgStore['noResult'] + '</span></div></div>');
    } else {
      var self = this;
      $.each(data.response.docs, function(i, doc) {
        if (doc.url != undefined) {
          var url = doc.url.replace("localhost", window.location.hostname);
          var positionString = Manager.store.get("start").value;
          var position = 1;

          if (positionString !== null) {
            position += parseInt(positionString);
          }

          position += i;
          var description = '';
          if (doc.emptied != null && doc.emptied != undefined && doc.emptied == true) {
            description = '<i class="fas fa-exclamation-triangle"></i> ' + window.i18n.msgStore['emptied_content'];
          } else {
            if (data.highlighting[doc.id]) {
              $.each(data.highlighting[doc.id], function(key, value) {
                description += value;
              });
            }
            if (description.trim() == '') {
              description = doc.preview_content.toString().substring(0, 200);
            }
          }
          /*
           * TO enable entity extraction part 1 var phone ="";
           * 
           * if (doc.entity_phone != undefined){ phone = doc.entity_phone; }
           */
          var solrPos = position - 1;
          elm.find('.doc_list').append('<div class="doc e-' + i + '" id="' + doc.id + '" pos="' + solrPos + '"></div>');
          elm.find('.doc:last').append('<div class="res"></div>');

          elm.find('.doc:last .res').append('<span class="icon"></span>');
          var extension = doc.extension;

          if (self.isMobile) {
            if (extension.toLowerCase() !== undefined && extension.toLowerCase() != "")
              elm.find('.doc:last .icon').append('<span>[' + extension.toUpperCase() + ']</span> ');
          } else {
            if (extension !== undefined && extension != "") {
              var icon = "";
              var path = "images/icons/";
              if (extension.toLowerCase() in self.availableImages) {
                icon = self.availableImages[extension.toLowerCase()].icon;
                path = self.availableImages[extension.toLowerCase()].path;
              } else {
                if (AjaxFranceLabs.imageExists("./images/icons/" + extension.toLowerCase() + "-icon-24x24.png")) {
                  icon = extension.toLowerCase() + '-icon-24x24.png';
                } else if (AjaxFranceLabs.imageExists("./customs/icons/" + extension.toLowerCase() + "-icon-24x24.png")) {
                  path = "customs/icons/";
                  icon = extension.toLowerCase() + '-icon-24x24.png';
                } else {
                  icon = "default-icon-24x24.png";
                }
                self.availableImages[extension.toLowerCase()] = {
                  icon : icon,
                  path : path
                };
              }
              elm.find('.doc:last .icon').append('<object data="' + path + icon + '"></object>&nbsp;');
            } else {
              elm.find('.doc:last .icon').append('<object data="images/icons/default-icon-24x24.png"></object>&nbsp;');
            }
          }
          var urlRedirect = 'URL?url=' + url + '&id=' + Manager.store.get("id").value + '&q=' + Manager.store.get("q").value + '&position=' + position;
          elm.find('.doc:last .res').append('<a class="title" target="_blank" href="' + urlRedirect + '"></a>');
          var title = "";
          if (Array.isArray(doc.title)) {
            try {
              title = decodeURIComponent(doc.title[0]);
            } catch (e) {
              title = doc.title[0];
            }
          } else if (doc.title != undefined && doc.title != null) {
            try {
              title = decodeURIComponent(doc.title);
            } catch (e) {
              title = doc.title;
            }
          }
          elm.find('.doc:last .title').append('<span title="' + title + '">' + title.truncate(50, 15) + '</span>');
          elm.find('.doc:last .res').append('<div class="doc-details"><div class="description"></div></div>');
          elm.find('.doc:last .description').append('<div class="snippet">' + description + '</div>');
          var address = decodeURIComponent(url);
          elm.find('.doc:last .description').append('<div id="urlMobile"><p class="address" title="' + address + '">');
          elm.find('.doc:last .address').append('<span>' + AjaxFranceLabs.tinyUrl(address) + '</span>');
          /*
           * To enable entity extraction part 2 elm.find('.doc:last .address').append('<br/><span>Phone : ' + phone+ '</span>');
           */

          // Add the elevator links if the user is allowed
          if (typeof self.queryElevator !== 'undefined') {
            self.queryElevator.addElevatorLinks(elm.find('.doc:last .res'), doc.id);
          }

          if (typeof self.relevancyQuery !== 'undefined') {
            self.relevancyQuery.addRelevancyLinks(elm.find('.doc:last .res'), doc.id);
          }
        }
      });

      AjaxFranceLabs.addMultiElementClasses(elm.find('.doc'));
      self.previewDisplayer.init(self.manager.store.string(), self.manager.store.get("id").value);
    }
    if (this.pagination) {
      this.pagination.afterRequest(data);
    }
    if (this.isMobile) {
      if ($("#results .doc_list").children().length < parseInt($("#results_nav_mobile #number_results_mobile span").text(), 10)) {
        if (data.response.docs.length != 0) {
          $("#results .doc_list_pagination").show();
          if (this.firstTimeWaypoint) {
            this.firstTimeWaypoint = false;
            var waypoin = $("#results .doc_list_pagination").waypoint(function(e) {
              if ($("#results .doc_list_pagination").is(":visible")) {
                this.destroy();
                self.firstTimeWaypoint = true;
                self.mutex_locked = true;
                self.pagination.pageSelected++;
                self.nextPage();
                self.mutex_locked = false;
                Waypoint.refreshAll();
              }
            }, {
              offset : 'bottom-in-view'
            });
            while (self.mutex_locked)
              sleep(1);
            // home made mutex used to stop the browser from executing multiple
            // times the lines above
            // without waiting the precedent execution (no native mutex are
            // enable in javascript)
          }
        }
      } else {
        $("#results .doc_list_pagination").hide();
        $("#spinner_mobile").hide();
      }
    }
  },

  requestError : function(status, error) {
    let elm = $(this.elm), self = this;
    if (!this.isMobile)
      elm.find('.doc_list').empty();
    else
      elm.find('.doc_list .bar-loader').remove();

    if (status == "timeout") {
      elm.find('.doc_list').append('<div class="doc"><span class="noResult description">' + window.i18n.msgStore['requestTimeout'] + '</span></div>');
    } else {
      elm.find('.doc_list').append('<div class="doc"><span class="noResult description">' + window.i18n.msgStore['requestError'] + '</span></div>');
    }
  }
});
