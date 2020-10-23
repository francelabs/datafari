$(document).ready(function() {

  $(".search-source").click(function() {
    var isMobile = $(window).width() < 800;
    $(".search-source").removeClass("active");
    $(this).addClass("active");
    $(".external-results-elm").hide();
    if ($(this).children().attr("id") === "datafari-search-link") {
      $(".results-elm").show();
      if (isMobile) {
        $("#status-div").hide();
      } else {
        $("#status-div").show();
      }
    } else {
      $(".results-elm").hide();
    }
  });

});
