$(function($) {
    Manager.addWidget(new AjaxFranceLabs.HeaderMenusWidget({
		elm : $('#header-menus'),
		id : 'headerMenus'
    }));
    
    Manager.init();
});


$(document).ready(function() {

    $("a#basicSearchLink").prop("href","/Datafari/Search");
});