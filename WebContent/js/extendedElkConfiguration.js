var externalELK = false;

function fillExtendedFields(data){
		$("#externalELKLabel").hide();
		$("#ELKServerDiv").html('');
		$("#ELKScriptsDirDiv").html('');
		$("#ELKServerDiv").hide();
		$("#ELKScriptsDirDiv").hide();
		return externalELK;
}

function getExternalELK() {
	return externalELK;
}