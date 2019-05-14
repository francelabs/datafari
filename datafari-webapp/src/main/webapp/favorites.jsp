<%@page contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>

<link rel="stylesheet" type="text/css" href="css/favorites.css" media="screen">
<script type="text/javascript" src="js/favorites.js"></script>

<div class="head-title"><span id="favorites-label"></span></div>
<div id="fav-content">
	<div class="loading"><i class="fas fa-circle-o-notch fa-spin"></i> Loading ...</div>
	<div id="Message" style="display:none;">No Document Saved Yet</div>
	<table id="favoritesTable" class="table table-bordered table-striped">
			<thead>
				<tr>
					<th id="doc-name">Document Name</th>
					<th id="doc-source">Full Path</th>
					<th id="doc-delete">Delete</th>
				</tr>
			</thead>
			<tbody>
				
			</tbody>
	</table>
	<span id="previous"><i  class="fas fa-chevron-left"></i></span>
	<span id="next"><i  class="fas fa-chevron-right"></i></span>

</div>
