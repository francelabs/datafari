package com.francelabs.datafari.updateprocessor;

import com.francelabs.datafari.updateprocessor.regexentity.RegexEntitySpecification;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexEntityUpdateProcessor extends UpdateRequestProcessor {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private List<RegexEntitySpecification> lstSpecs;

  public RegexEntityUpdateProcessor(final SolrParams params, final List<RegexEntitySpecification> lstSpecs, final UpdateRequestProcessor next) {
    super(next);
    if (params != null) {
    }
    this.lstSpecs = lstSpecs;
  }

  public void processAdd(final AddUpdateCommand cmd) throws IOException {
    final SolrInputDocument doc = cmd.getSolrInputDocument();

    applyRegexListOnDocument(doc, lstSpecs);

    super.processAdd(cmd);
  }

  private void applyRegexListOnDocument (SolrInputDocument doc, List<RegexEntitySpecification> lstSpecs){
    String sourceField;
    String docSourceFieldContent;
    String regex;
    String destinationField;
    String valueIfTrue;
    String valueIfFalse;
    Matcher matcher;
    boolean matchOK;
    // List of destination fields that have already been matched. We keep one matchOK
    List<String> destinationFieldMatched = new ArrayList<>();


    // Dès qu'il y a eu un matchOK on passe à la spec suivante: donc pas de keepOnlyOne
    for (RegexEntitySpecification regexSpec : lstSpecs) {
      destinationField = regexSpec.getDestinationMetadata();
      if (destinationFieldMatched.contains(destinationField))
        continue;

      sourceField = regexSpec.getSourceMetadata();
      docSourceFieldContent = (String) doc.getFieldValue(sourceField);
      regex = regexSpec.getRegexValue();
      valueIfTrue = regexSpec.getValueIfTrue();
      valueIfFalse = regexSpec.getValueIfFalse();

      if (StringUtils.isEmpty(regex) && StringUtils.isNotEmpty(valueIfTrue)) {
        doc.setField(destinationField, valueIfTrue);

      } else {
        matcher = Pattern.compile(regex).matcher(docSourceFieldContent);

        if (regexSpec.isRegexActive()){
          matchOK = matcher.find();
        } else {
          matchOK = matcher.matches();
        }
        if (matchOK) {
          destinationFieldMatched.add(destinationField);
          if (StringUtils.isNotEmpty(valueIfTrue)) {
            doc.setField(destinationField, valueIfTrue);
          } else {
            doc.setField(destinationField, matcher.group());
          }
        } else if (StringUtils.isNotEmpty(valueIfFalse)) {
          destinationFieldMatched.add(destinationField);
          doc.setField(destinationField, valueIfFalse);
        }
      }

    }
  }

}
