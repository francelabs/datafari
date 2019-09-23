$(function($) {
    Manager.addWidget(new AjaxFranceLabs.HeaderMenusWidget({
		elm : $('#header-menus'),
		id : 'headerMenus'
    }));
    
    Manager.init();

    // The basic search link from this page needs to get us back
    // to the search page, not just load the search widget as it 
    // is not part of the page.
    $("a#basicSearchLink").prop("href","/Datafari/Search");
    let basicSearchText = window.i18n.msgStore['search'];
    if (basicSearchText) {
      $("#basicSearchLink").html(basicSearchText);
    }

    let contentDiv = $(document).find("#solr");
    let titles = contentDiv.find("h2");
    for (let i = 0; i < titles.length - 1; i++) {
      let current = $(titles[0]);
      current.attr("data-toggle", "collapse");
      current.attr("data-target", ".section" + i);
      current.html(current.html() + " <span class=\"caret\"></span>");
      let currentContent = current.nextUntil("h2");
      currentContent.addClass("collapse section" + i);
    }
    let lastIndex = titles.length - 1;
    let last = $(titles[lastIndex]);
    last.nextUntil("div").addClass("collapse section" + lastIndex);
    last.attr("data-toggle", "collapse");
    last.attr("data-target", ".section" + lastIndex);
    last.html(last.html() + " <span class=\"caret\"></span>");

});