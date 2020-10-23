$(document).ready(function() {

  $("html").on('click', "#datafariAdminMenu, a.side-menu-control", function() {
    $("#sidebar").toggleClass("active");
    $("#content").toggleClass("active");

    if (!$("#sidebar").hasClass("active")) {
      $(".side-menu-control").html("<i class='fas fa-angle-left'></i>");
    } else {
      $(".side-menu-control").html("<i class='fas fa-bars'></i>");
    }
  });

});