function clearActiveLinks() {
  $("#loginDatafariLinks").find(".active").removeClass("active");
}

function hideSearchView() {
  $(".search-view-ui").addClass('force-hide');
  $(".header-menu-ui").addClass('force-hide');
}

function displaySearchView() {
  $(".header-menu-ui").addClass('force-hide');
  $(".search-view-ui").removeClass('force-hide');
}