/*******************************************************************************
 * Copyright 2015 France Labs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
AjaxFranceLabs.QueryHighlightingWidget = AjaxFranceLabs.FacetCore.extend({

  // Variables
  elm : null,
  name : null,
  highlightedElms : {},
  currentHighlight : {
    word : "",
    current : 0
  },

  // Methods

  initContent : function(widgetContentDiv) {

  },

  refreshCurrentHighlight : function(docContentDiv) {
    var hlWord = this.currentHighlight.word;
    var current = this.currentHighlight.current;
    if (hlWord != "" && current != -1) {
      var hlElement = this.highlightedElms[hlWord].elements[current];
      docContentDiv.find(".current-hlmark").removeClass("current-hlmark").addClass("hlmark");
      hlElement.removeClass("hlmark");
      hlElement.addClass("current-hlmark");
    }
  },

  // Construct a list of jquery elements for each highlighted word
  updateHlElmsMaps : function(docContentDiv) {
    var self = this;
    for ( var hlWord in this.highlightedElms) {
      // First of all, (re)initialize the list of jquery elements for the
      // current hlWord
      this.highlightedElms[hlWord].elements = [];
      var cpt = 0;

      // Search highlighted elements and if they correspond to the current
      // hlWord then put them in the list
      docContentDiv.find(".hlmark").each(function() {
        if ($(this).html().toLowerCase() == hlWord) {
          self.highlightedElms[hlWord].elements[cpt] = $(this);
          cpt++;
        }
      });
    }
  },

  applyHighlights : function(text, hlWord) {
    var regex = new RegExp('\\b' + hlWord + '\\b', "ig");
    text = text.replace(regex, '<span class="hlmark">$&</span>');

    // Init jquery elements map for current hlWord
    this.highlightedElms[hlWord] = {
      elements : [],
      current : -1
    };

    // Clean current highlight if any
    text = text.replace("<span class=\"current-hlmark\">", "<span class=\"hlmark\">");

    return text;
  },

  removeHighlights : function(text, hlWord) {

    // Clean current highlight if any
    var currentHlRegex = new RegExp("<span class=\"current-hlmark\">\(" + hlWord + "\)</span>", "ig");

    // If the regex match this means that there is a current highlight on the
    // hlWord so it needs to be removed
    if (text.match(currentHlRegex)) {
      text = text.replace(currentHlRegex, '<span class="hlmark">$1</span>');
      // Reset currentHighlight
      this.currentHighlight.word = "";
      this.currentHighlight.current = 0;
    }

    // Clean standard highlights
    var regex = new RegExp('<span class="hlmark">\(' + hlWord + '\)</span>', "ig");
    text = text.replace(/\n$/g, '\n\n').replace(regex, '$1');

    // Remove indexes map
    delete this.highlightedElms[hlWord];

    // Clean current highlight if any (in case the current highlight is another
    // word)
    text = text.replace("<span class=\"current-hlmark\">", "<span class=\"hlmark\">");

    return text;
  },

  // Adjust the scroll position of the docContentDiv to be sure that the
  // hlElement is visible
  adjustScroll : function(hlElement, docContentDiv) {
    var lineHeight = parseInt(docContentDiv.css("line-height").replace("px", ""));
    // Offset of the docContentDiv
    var contentDivOffset = docContentDiv.offset().top;
    // Position of the hlElement, relative to the docContentDiv offset
    var hlPos = hlElement.position().top;
    // Current scrollTop
    var scrollTop = docContentDiv.scrollTop();
    // Current scrollBottom
    var scrollBottom = scrollTop + docContentDiv.height();
    // Ideal scrollTop to be sure that the hlElement is visible
    var targetScrollTop = scrollTop + hlPos - contentDivOffset - lineHeight;
    if (targetScrollTop < 0) {
      targetScrollTop = 0;
    }

    // If the targetScrollTop is out of the current limits then force the
    // scrollTop to the targetScrollTop
    if (targetScrollTop < scrollTop || targetScrollTop > scrollBottom) {
      docContentDiv.scrollTop(targetScrollTop);
    }
  },

  updateWidgetContent : function(docContentDiv, widgetDiv, widgetContentDiv, docId, docPos, params, data, qId) {
    var self = this;
    widgetContentDiv.html("");
    if (data != undefined && data != null) {
      // Retrieve the query highlighting if any
      var fileHighlighting = data.highlighting[docId];
      var termsCollection = [];
      // Fill the termsCollection with the terms found in the query highlighting
      for ( var highlightField in fileHighlighting) {
        if (highlightField.indexOf("content") != -1 || highlightField == "exactContent") {
          if (fileHighlighting[highlightField].length > 0) {
            var contentHighlight = fileHighlighting[highlightField][0];
            var termRegex = /<span class="em">(.*?)<\/span>*/gm;
            var match = termRegex.exec(contentHighlight);
            while (match != null) {
              var foundTerm = match[1].trim().toLowerCase();
              if (termsCollection.indexOf(foundTerm) == -1) {
                termsCollection.push(foundTerm);
              }
              match = termRegex.exec(contentHighlight);
            }
          }
        }
      }

      // For each term, create a term button
      if (termsCollection.length > 0) {
        for (var i = 0; i < termsCollection.length; i++) {
          var divTerm = $("<div class='highlight-term'><span class='highlight-term-prev' style='visibility:hidden'><i class='fa fa-chevron-Left'></i></span> <span class='highlight-term-value'>"
              + termsCollection[i] + "</span> <span class='highlight-term-next' style='visibility:hidden'><i class='fa fa-chevron-Right'></i></span></div>");
          widgetContentDiv.append(divTerm);
        }

        $(".highlight-term-value").click(function() {
          // Word to highlight
          var hlWord = $(this).html();
          // Text of the docContentDiv
          var text = docContentDiv.html();
          if ($(this).hasClass("hlmark")) {
            // The hlWord is already highlighted so "un-highlight" it
            $(this).removeClass("hlmark");
            text = self.removeHighlights(text, hlWord);
            docContentDiv.html(text);
            // docContentDiv has been updated so we need to update the
            // highlighted jquery elements list for each highlighted word
            self.updateHlElmsMaps(docContentDiv);
            // Re set the current highlight in case it was on an other word
            self.refreshCurrentHighlight(docContentDiv);

            // Hide the previous and next buttons
            var nextButton = $(this).parent().children(".highlight-term-next");
            var prevButton = $(this).parent().children(".highlight-term-prev");
            nextButton.css("visibility", "hidden");
            prevButton.css("visibility", "hidden");
          } else {
            // The hlWord is not highlighted so highlight it
            $(this).addClass("hlmark");
            var highlightedText = self.applyHighlights(text, hlWord);
            docContentDiv.html(highlightedText);
            // docContentDiv has been updated so we need to update the
            // highlighted jquery elements list for each highlighted word
            self.updateHlElmsMaps(docContentDiv);
            // Re set the current highlight in case it was on an other word
            self.refreshCurrentHighlight(docContentDiv);

            // Display the previous and next buttons
            var nextButton = $(this).parent().children(".highlight-term-next");
            var prevButton = $(this).parent().children(".highlight-term-prev");
            nextButton.css("visibility", "visible");
            prevButton.css("visibility", "visible");
          }
        });

        $(".highlight-term-next").click(function() {
          var termVal = $(this).parent().children(".highlight-term-value");
          var hlWord = termVal.html();
          // reset others words current
          for ( var word in self.highlightedElms) {
            if (word != hlWord) {
              self.highlightedElms[word].current = -1;
            }
          }

          // Calculate the current index of the jquery element to highlight
          var current = self.highlightedElms[hlWord].current;
          if (self.highlightedElms[hlWord].elements[current + 1] != undefined) {
            current++;
          } else {
            current = 0;
          }
          self.highlightedElms[hlWord].current = current;

          // refresh the current highlight and adjust scroll
          var hlElement = self.highlightedElms[hlWord].elements[current];
          self.currentHighlight.word = hlWord;
          self.currentHighlight.current = current;
          self.refreshCurrentHighlight(docContentDiv);
          self.adjustScroll(hlElement, docContentDiv);
        });

        $(".highlight-term-prev").click(function() {
          var termVal = $(this).parent().children(".highlight-term-value");
          var hlWord = termVal.html();

          // reset others words current
          for ( var word in self.highlightedElms) {
            if (word != hlWord) {
              self.highlightedElms[word].current = -1;
            }
          }

          // Calculate the current index of the jquery element to highlight
          var current = self.highlightedElms[hlWord].current;
          if (self.highlightedElms[hlWord].elements[current - 1] != undefined) {
            current--;
          } else {
            current = self.highlightedElms[hlWord].elements.length - 1;
          }
          self.highlightedElms[hlWord].current = current;

          // refresh the current highlight and adjust scroll
          var hlElement = self.highlightedElms[hlWord].elements[current];
          self.currentHighlight.word = hlWord;
          self.currentHighlight.current = current;
          self.refreshCurrentHighlight(docContentDiv);
          self.adjustScroll(hlElement, docContentDiv);
        });
      } else {
        widgetDiv.hide();
      }
    } else {
      widgetDiv.hide();
    }
  }
});
