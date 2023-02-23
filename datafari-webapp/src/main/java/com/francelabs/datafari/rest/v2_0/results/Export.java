package com.francelabs.datafari.rest.v2_0.results;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.francelabs.datafari.servlets.ExportResults;

@RestController
public class Export {

  @GetMapping("/rest/v2.0/results/export")
  public void performExport(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
    ExportResults.performExport(request, response);
  }
}
