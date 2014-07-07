package com.francelabs.datafari.servlets;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.common.params.ModifiableSolrParams;

import com.francelabs.datafari.statistics.StatsPusher;

/**
 * Servlet implementation class URL
 */
@WebServlet("/URL")
public class URL extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public URL() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String redirectUrl = "/url.jsp";
		
		
		
		ModifiableSolrParams params = new ModifiableSolrParams(request.getParameterMap());
		StatsPusher.pushDocument(params);
		RequestDispatcher rd = request.getRequestDispatcher(redirectUrl);
		rd.forward(request, response);
	}


}
