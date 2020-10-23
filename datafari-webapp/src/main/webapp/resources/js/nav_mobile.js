$(document).ready(function(){
	var endOfAnimationEvent = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
	var animationIn = "animated slideInLeft";
	var animationOut = "animated slideOutLeft";
	var facetsDiv = $("#facets_mobile");
	var solrDiv = $("#solr");
	facetsDiv.hide();
	$("#results_nav_mobile #nav_facets_mobile").click(function(){
		facetsDiv.addClass(animationIn).css("display","block").one(endOfAnimationEvent,function(){
			facetsDiv.removeClass(animationIn);
			solrDiv.hide();
			});
	});
	$("#facets_mobile #nav_mobile a").click(function(){
		solrDiv.show();
		facetsDiv.addClass(animationOut).one(endOfAnimationEvent,function(){
			Waypoint.refreshAll();
			facetsDiv.hide();
			facetsDiv.removeClass(animationOut);
			});
	});
	$("#nav_mobile a").append(' '+window.i18n.msgStore['previous'].charAt(0).toUpperCase() + window.i18n.msgStore['previous'].substr(1));
	$("#nav_facets_mobile a").append(' '+window.i18n.msgStore['filter'].charAt(0).toUpperCase() + window.i18n.msgStore['filter'].substr(1));
	$("#number_results_mobile").append(' '+window.i18n.msgStore['results'].charAt(0).toUpperCase() + window.i18n.msgStore['results'].substr(1));
	// left: 37, up: 38, right: 39, down: 40,
	// spacebar: 32, pageup: 33, pagedown: 34, end: 35, home: 36

});