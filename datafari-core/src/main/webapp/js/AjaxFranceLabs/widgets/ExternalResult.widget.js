AjaxFranceLabs.ExternalResultWidget = AjaxFranceLabs.ResultWidget.extend({
	elmSelector : '#external',
	id : 'external',
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

		var urldatasource = 'http://localhost:8080/Datafari/externaldata.json';

		$('.doc_list').append('<div class="externalresults">');
		$('.externalresults').append('<div class="externalmaintitle"><img src="css/images/sharepoint.png" />Results from external datasource : </div>');
		$.getJSON(urldatasource,querySolr, function(result){

			$.each(result.d.query.PrimaryQueryResult.RelevantResults.Table.Rows.results,
					function(j, docu) {
				var highlightingvalues = docu.Cells.results[10].Value;
				highlightingvalues = replaceAll(highlightingvalues,"<c0>","<span class='em'>");
				highlightingvalues = replaceAll(highlightingvalues,"</c0>","</span>");

				elm.find('.externalresults').append(
						'<div class="external sp'+ j +'"></div>');
				var extension = docu.Cells.results[17].Value;
				elm.find('.external:last').append(
						'<div class="externaltitle"><a class="externallinktitle" target="_blank" href="'+docu.Cells.results[6].Value+'">'+docu.Cells.results[3].Value+'</a>');
				elm.find('.external:last').append('<p class="externalhighlight">'+highlightingvalues);
				elm.find('.external:last').append('<p class="externalurl">'+docu.Cells.results[6].Value);

			})
		});
		elm.find('.doc_list').append('<div class="externalmore">See more results </div>');	
		elm.find('.doc_list').append('<div class="datafariresultstitle"><img src="css/images/logo_zebre_mini.png" />Rest of results : </div>');	


	}
});
