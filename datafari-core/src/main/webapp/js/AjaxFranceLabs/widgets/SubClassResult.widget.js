AjaxFranceLabs.SubClassResultWidget = AjaxFranceLabs.ResultWidget.extend({
elmSelector : '#results',
id : 'documents',
pagination : true,
firstTimeWaypoint : true,
isMobile : $(window).width()<800,
mutex_locked:false,
availableImages: {},

buildWidget : function () {
	this.elm = $(this.elmSelector);
	this._super();


	// Initialize the queryElevator module if possible (if not, that means that the user is not an administrator and is not allowed to use it)
	if (typeof AjaxFranceLabs.QueryEvaluatorModule === 'function') {
		this.queryEvaluator = new AjaxFranceLabs.QueryEvaluatorModule();
		this.queryEvaluator.setParentWidget(this);
		this.manager.addModule(this.queryEvaluator);
	}
},

imageExists : function(image_url){

    var http = new XMLHttpRequest();

    http.open('HEAD', image_url, false);
    http.send();

    return http.status != 404;

},

afterRequest : function() {
	var data = this.manager.response, elm = $(this.elm),self=this;
	if (!this.isMobile)
		elm.find('.doc_list').empty();
	else
		elm.find('.doc_list .bar-loader').remove();
	if (data.response.numFound === 0) {
		elm
				.find('.doc_list')
				.append(
						'<div class="doc no"><div class="res no"><span class="title noResult">'+window.i18n.msgStore['noResult']+'</span></div></div>');
	} else {
		var self = this;
		$.each(data.response.docs,
						function(i, doc) {
							if (doc.url != undefined){
				                var url = doc.url.replace("localhost",window.location.hostname);
				                var positionString = Manager.store.get("start").value;
				                var position = 1;

				                if (positionString !== null){
				                	position += parseInt(positionString);
				                }

				                position += i;
								var description = '';
								if (data.highlighting[doc.id]) {
									$.each( data.highlighting[doc.id], function( key, value ) {
										description += value;
										});
								}
								if(description.trim() == '') {
									description = doc.preview_content.toString().substring(0,200);
								}
								
								elm.find('.doc_list').append(
												'<div class="doc e-'+ i + '" id="'+ doc.id+ '"></div>');
								elm.find('.doc:last').append(
												'<div class="res"></div>');

								elm.find('.doc:last .res').append('<span class="icon"></span>');
								var extension = doc.extension;

								if (self.isMobile){
									if (extension.toLowerCase()!==undefined && extension.toLowerCase()!="")
										elm.find('.doc:last .icon').append('<span>['+ extension.toUpperCase() +']</span> ');
								}
								else {
									if (extension !==undefined && extension !="") {
										var icon = "";
										if(extension.toLowerCase() in self.availableImages) {
											icon = self.availableImages[extension.toLowerCase()];
										} else {
											if(self.imageExists("./images/icons/" + extension.toLowerCase() + "-icon-24x24.png")) {
												icon = extension.toLowerCase() +'-icon-24x24.png';
												self.availableImages[extension.toLowerCase()] = extension.toLowerCase() +'-icon-24x24.png';
											} else {
												icon = "default-icon-24x24.png"
												self.availableImages[extension.toLowerCase()] = "default-icon-24x24.png";
											}
										}
										elm.find('.doc:last .icon').append('<object data="images/icons/'+ icon +'"></object>&nbsp;');
									} else {
										elm.find('.doc:last .icon').append('<object data="images/icons/default-icon-24x24.png"></object>&nbsp;');
									}
								}
				                var urlRedirect = 'URL?url='+ url + '&id='+Manager.store.get("id").value + '&q=' + Manager.store.get("q").value + '&position='+position;
								elm.find('.doc:last .res').append('<a class="title" target="_blank" href="'+urlRedirect+'"></a>');
								var title;
								// if the document is an html file, get the extracted title metadata
								if (doc.extension == "html"){
									title = doc.title;
								// for other files, get the filename from the url
								} else{
									title = doc.url.split('/');
									title = title[title.length-1];
								}
								elm.find('.doc:last .title').append('<span>' +decodeURIComponent(title) + '</span>');
								elm.find('.doc:last .res').append('<p class="description">');
								elm.find('.doc:last .description').append('<div id="snippet">'+ description+ '</div>');
								elm.find('.doc:last .description').append('<div id="urlMobile"><p class="address">');
								elm.find('.doc:last .address').append('<span>' + AjaxFranceLabs.tinyUrl(decodeURIComponent(url)) + '</span>');

								// Add the elevator links if the user is allowed
								if (typeof self.queryEvaluator !== 'undefined') {
									self.queryEvaluator.addEvaluatorLinks(elm.find('.doc:last .res'), doc.id);
								}
						}
						});
		if (typeof self.queryEvaluator !== 'undefined') {
			$.get("./SearchExpert/queryEvaluator", {
				"query" : self.manager.store.params.q.value,
				"docids" : data.response.docs.map(function(doc) {
					  return doc.id;
				})
			}, function(data) {
				if (data.code == 0) {
					$.each(data.docs,
							function(docID, rank) {
						var elem = $(document.getElementById(docID));

								if (rank == 1){
									elem.find(".evaluator-up-unclicked").show();
									elem.find(".evaluator-up-clicked").hide();
									elem.find(".evaluator-down-unclicked").hide();
									elem.find(".evaluator-down-clicked").show();

								} else {
									if (rank == 10){

										elem.find(".evaluator-up-unclicked").hide();
										elem.find(".evaluator-up-clicked").show();
										elem.find(".evaluator-down-unclicked").show();
										elem.find(".evaluator-down-clicked").hide();

									}
								}
					});
				}});
		}

		AjaxFranceLabs.addMultiElementClasses(elm
				.find('.doc'));
		if (this.pagination)
			this.pagination.afterRequest(data);
	}
	if (this.isMobile){
		if ( $(".doc_list").children().length<parseInt($("#number_results_mobile span").text(),10)){
			if (data.response.docs.length!=0){
				$("#results .doc_list_pagination").show();
				if (this.firstTimeWaypoint){
					this.firstTimeWaypoint = false;
					var waypoin = $(".doc_list_pagination").waypoint(function(e){
						this.destroy();
						self.firstTimeWaypoint = true;
						self.mutex_locked=true;
						self.pagination.pageSelected ++;
						self.nextPage();
						self.mutex_locked=false;
						Waypoint.refreshAll();
					},{ offset: 'bottom-in-view'});
					while(self.mutex_locked)
						sleep(1);
					//home made mutex  used to stop the browser from executing multiple times the lines above
					// without waiting the precedent execution (no native mutex are enable in javascript)
				}
			}
		}else{
			$("#results .doc_list_pagination").hide();
			$("#spinner_mobile").hide();
			}
		}
	},

	requestError: function(status, error) {
		let elm = $(this.elm),self=this;
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
