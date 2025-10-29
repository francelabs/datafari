$(document).ready(function () {
  // Internationalize content
  $("#topbar1").text(window.i18n.msgStore['home']);
  $("#topbar2").text(window.i18n.msgStore['adminUI-solrVectorSearch']);
  $("#topbar3").text(window.i18n.msgStore['adminUI-vectorSearchMenu-chunkingConfiguration']);
  $("#save-conf").text(window.i18n.msgStore['save']);
  $("#minChunkLengthLabel").html(window.i18n.msgStore['solrVectorSearch-minChunkLengthLabel']);
  $("#minAlphaNumRatioLabel").html(window.i18n.msgStore['solrVectorSearch-minAlphaNumRatioLabel']);
  $("#maxOverlapLabel").html(window.i18n.msgStore['solrVectorSearch-maxOverlapLabel']);
  $("#chunkSizeLabel").html(window.i18n.msgStore['solrVectorSearch-chunkSizeLabel']);
  $("#splitterLabel").html(window.i18n.msgStore['solrVectorSearch-splitterLabel']);


  $('#enableVectorSearch').bootstrapToggle();


  function loadFormData() {
    $.get("../rest/v2.0/management/chunking", function (data) {

        // Check or uncheck "Enable Vector Search"
        $("#enableVectorSearch").prop("checked", data.enableVectorSearch === true).change();

        // Set existing values
        $("#minChunkLength").val(data.minChunkLength);
        $("#minAlphaNumRatio").val(data.minAlphaNumRatio);
        $("#maxOverlap").val(data.maxoverlap || 0);
        $("#chunkSize").val(data.chunksize || 300);
        $("#splitter").val(data.splitter || "recursiveSplitter");

      }, "json");
  }

  // Loading data from backend
  loadFormData();



  // Form validation
  $('#chunking-form').on('submit', function (event) {
    event.preventDefault();

    if (!this.checkValidity()) {
      event.stopPropagation();
      $(this).addClass('was-validated');
      return;
    }

    // Build JSON payload
    const payload = {
      enableVectorSearch: $('#enableVectorSearch').is(':checked'),
      minChunkLength: parseInt($('#minChunkLength').val(), 10),
      minAlphaNumRatio: parseFloat($('#minAlphaNumRatio').val()),
      maxoverlap: parseInt($('#maxOverlap').val(), 10),
      chunksize: parseInt($('#chunkSize').val(), 10),
      splitter: $('#splitter').val()
    };

    // Loading spinner
    $('#save-conf').prop('disabled', true);
    $('#loadingIndicator').show();

    $.ajax({
        url: '../rest/v2.0/management/chunking',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(payload),
        success: function (response) {
          $('#save-conf').prop('disabled', false);
          $('#loadingIndicator').hide();
          $('#message')
            .removeClass('alert-danger')
            .addClass('alert-success')
            .text("Configuration saved successfully")
            .show();

          // Reload form data
          loadFormData();
        },
        error: function (xhr) {
          $('#save-conf').prop('disabled', false);
          $('#loadingIndicator').hide();
          $('#message')
            .removeClass('alert-success')
            .addClass('alert-danger')
            .text("An error occurred while saving configuration.")
            .show();
        }
    });
    setTimeout(() => {
      $('#message').fadeOut();
    }, 4000);

  });
});