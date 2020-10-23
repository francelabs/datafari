AjaxFranceLabs.PrevisualizeResultWidget = AjaxFranceLabs.AbstractWidget.extend({
  elmSelector : '#previsualize',
  id : 'previ',

  buildWidget : function() {
    this.elm = $(this.elmSelector);
  },

  afterRequest : function() {
    var data = this.manager.response, elm = $(this.elm), self = this;

    var querySolr = getParamValue('query', decodeURIComponent(window.location.search));
    var self = this;
    var docs = self.manager.response.response.docs;
    var preview_content = "";
    var title = "";
    var last_modified = "";
    var crawl_date = "";
    var author = "";
    var file_size = "";

    var dummy1 = "Dummy text 1";
    var dummy2 = "Dummy text 2";
    var dummy3 = "Dummy text 3";
    var dummy4 = "Dummy text 4";

    if (docs.length != 0) {
      $.each(docs, function(index, doc) {
        if (docs[index].preview_content != undefined) {
          preview_content = docs[index].preview_content;
        }

        if (docs[index].title != undefined) {
          title = doc.title[0].truncate(25, 12);
        }

        if (docs[index].last_modified != undefined) {
          last_modified = formatDate(docs[index].last_modified,"en-US");
        }

        if (docs[index].crawl_date != undefined) {
          crawl_date = formatDate(docs[index].crawl_date,"en-US");
        }

        if (docs[index].author != undefined) {
          author = docs[index].author;
        }

        if (docs[index].original_file_size != undefined) {
          file_size = docs[index].original_file_size;
          file_size = nFormatter(file_size,1);
        }

        // Add here the information that you want to add in the previsualize
        // window
        var htmlContent = '<table border="0" style="margin:10px">';
        htmlContent += '<tr><td style="width:170px"><b style="font-weight: bold;">Last Modified</b></td><td><b style="font-weight: bold;">Crawl date:</b></td></tr>';
        htmlContent += '<tr><td style="width:170px">' + last_modified + '</td><td>' + crawl_date + '</td></tr>';
        htmlContent += '<tr><td></td><td></td></tr>';
        htmlContent += '<tr><td style="width:170px"><b style="font-weight: bold;">Author:</b></td><td><b style="font-weight: bold;">File Size :</b></td></tr>';
        htmlContent += '<tr><td style="width:170px">' + author + '</td><td>' + file_size + 'B</td></tr>';
        htmlContent += '<tr><td></td><td></td></tr>';
        /*
        htmlContent += '<tr><td style="width:170px"><b style="font-weight: bold;">Dummy1:</b></td><td><b style="font-weight: bold;">Dummy2:</b></td></tr>';
        htmlContent += '<tr><td style="width:170px">' + dummy1 + '</td><td>' + dummy2 + '</td></tr>';
        htmlContent += '<tr><td style="width:170px"><b style="font-weight: bold;">Dummy3:</b></td><td><b style="font-weight: bold;">Dummy4:</b></td></tr>';
        htmlContent += '<tr><td style="width:170px">' + dummy3 + '</td><td>' + dummy4 + '</td></tr>';
        */
        htmlContent += '</table>';
        htmlContent += '<textarea style="width:250px;height:200px;border-color: #ccc;">' + preview_content + '</textarea>';
        /*
        htmlContent += '<div style="padding: 10px;background-color: #ccc;color: #000;">';
        htmlContent += '<span style="margin-right:15px; margin-left:10px"><b style="font-weight: bold;"> SHARE </b></span>';
        htmlContent += '<span><b style="font-weight: bold;"> MORE LIKE THIS </b></span></div>';
        */
        $($('.doc_list .res')[index]).append('<div class="previsualizetemplate arrow_box" style="width:320px;box-shadow: #ccc 0px 5px 5px 1px;">');
        $($('.doc_list .previsualizetemplate')[index]).append('<div class="inner-side-content">');
        $($('.doc_list .inner-side-content')[index]).append('<div style="margin:10px;font-size:17px"><b>' + title + '</b></div>');
        $($('.doc_list .inner-side-content')[index]).append(htmlContent);
        $($('.doc_list .previsualizetemplate')[index]).append('</div>');
        $($('.doc_list .res')[index]).append('</div>');

      });

    }

  }
});
