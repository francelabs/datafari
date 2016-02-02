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
		var querySolr = getParamValue('query', decodeURIComponent(window.location.search));
		this._super();
		var self = this;
		var docs = self.manager.response.response.docs;

		if (docs.length!=0){ 
			$.each(docs,function(index,doc){
				// Add here the information that you want to add in the previsualize window
				$($('.doc_list .res')[index]).append('<div class="previsualizetemplate" style="width:500px">More information');
				// For example, we display here the id of the document
				$($('.doc_list .previsualizetemplate')[index]).append('<div id="previsualizeid">ID : '+docs[index].id+' </div>' );
			});


		}




	}
});
