<!DOCTYPE html>
<html>
<body>
	<!--Start Breadcrumb-->
	<nav aria-label="breadcrumb" class="bg-dark">
    <a href="#" class="side-menu-control"> <i class="fas fa-angle-left"></i></a>
    <ol class="breadcrumb">
      <li class="breadcrumb-item indexAdminUIBreadcrumbLink" id="topbar1"></li>
      <li class="breadcrumb-item" id="topbar2"></li>
      <li class="breadcrumb-item active" aria-current="page" id="topbar3"></li>
    </ol>
  </nav>
	<div class="row-fluid" style="height: 840px;"> <!-- QUICK AND DIRTY, must be another way using configuration of bootstrap mixins-->
    <iframe src="proxy/solr/#/@MAINCOLLECTION@/schema" width="100%" height="100%" frameborder="0" scrolling="yes" padding="none"></iframe>
    </div>
</body>
<script> 
setupLanguage();

function setupLanguage(){
	$(document).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
	$("#topbar1").text(window.i18n.msgStore['home']);
	$("#topbar2").text(window.i18n.msgStore['adminUI-SearchEngineAdmin']);
	$("#topbar3").text(window.i18n.msgStore['adminUI-SchemaAdmin']);
}
</script>
</html>