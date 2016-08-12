var externalELK = false;

function fillExtendedFields(data){
		$("#externalELKLabel").hide();
		$("#ELKServerDiv").hide();
		$("#ELKScriptsDirDiv").hide();
		return externalELK;
}

function getExternalELK() {
	return externalELK;
}