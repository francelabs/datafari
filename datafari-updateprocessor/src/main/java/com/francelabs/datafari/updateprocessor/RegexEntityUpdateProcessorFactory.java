package com.francelabs.datafari.updateprocessor;

import com.francelabs.datafari.updateprocessor.regexentity.RegexEntitySpecification;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class RegexEntityUpdateProcessorFactory extends UpdateRequestProcessorFactory {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private SolrParams params = null;

  private static final String PARAM_REGEX_SPEC_FILE = "regexFile";

  private String regexSpecFile;
  private List<RegexEntitySpecification> lstSpecs;


  @Override
  public void init(final NamedList args) {
    if (args != null) {
      params = args.toSolrParams();
      regexSpecFile = "../solrcloud/FileShare/conf/" + params.get(PARAM_REGEX_SPEC_FILE);
    }
    //read regex specification file
    Path path = Paths.get(regexSpecFile);
    try {
      lstSpecs = readCsvToBean(path);
    } catch (IOException e) {
      String userDirectory = new File("").getAbsolutePath();
      log.warn("current directory : " + userDirectory);
      throw new RuntimeException(e);
    }

  }

  @Override
  public UpdateRequestProcessor getInstance(final SolrQueryRequest req, final SolrQueryResponse rsp, final UpdateRequestProcessor next) {
    return new RegexEntityUpdateProcessor(params, lstSpecs, next);
  }

  private List<RegexEntitySpecification> readCsvToBean(Path filePath) throws IOException {
    List<RegexEntitySpecification> lstRegexEntitySpec;
    try (Reader reader = Files.newBufferedReader(filePath)) {
      CsvToBean<RegexEntitySpecification> csvToBean = new CsvToBeanBuilder<RegexEntitySpecification>(reader)
          .withSkipLines(1)
          .withSeparator('|')
          // No escape character (\0 is the null character).
          // With pipe as separator and using double quotation mark to describe a plain text, we don't need escape character
          // To escape double quote, double the character: ""is a citation"", will appear: "is a citation"
          .withEscapeChar('\0')
          .withType(RegexEntitySpecification.class)
          .build();

      lstRegexEntitySpec = csvToBean.parse();
    } catch (IOException e){
      log.error("Error trying to parse regex file: {}", e.getMessage(), e);
      throw e;
    }
    return lstRegexEntitySpec;
  }

}