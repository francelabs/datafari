AjaxFranceLabs.PrevisualizeResultWidget = AjaxFranceLabs.ResultWidget.extend({
	elmSelector : '#previsualize',
	id : 'previ',
	pagination : false,
	firstTimeWaypoint : true,
	isMobile : $(window).width()<800,
	mutex_locked:false,

	buildWidget : function () {
		this.elm = $(this.elmSelector);
		this._super();
	},

	afterRequest : function() {
		var data = this.manager.response, elm = $(this.elm),self=this;

    if (!this.isMobile)
			elm.find('.doc_list').empty();
		else
			elm.find('.doc_list .bar-loader').remove();

		var querySolr = getParamValue('query', decodeURIComponent(window.location.search));
		this._super();
		var self = this;
		var docs = self.manager.response.response.docs;
		var preview_content = "";


		if (docs.length!=0){
			$.each(docs,function(index,doc){
				if (docs[index].preview_content != undefined){
					preview_content = docs[index].preview_content
				}
				else {
					preview_content = "";
				}
				// Add here the information that you want to add in the previsualize window
				$($('.doc_list .res')[index]).append('<div class="previsualizetemplate" style="width:500px">'+window.i18n.msgStore['preview_moreInformation']);
				// For example, we display here the id of the document
				$($('.doc_list .previsualizetemplate')[index]).append('<div id="previsualizeid">'+window.i18n.msgStore['preview_content']+': '+preview_content+' </div>' );
			});


		}




	}
});
