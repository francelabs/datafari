package com.francelabs.datafari.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@Configuration
public class AuthenticationControllers {

  @Controller
  public class AuthAndRedirectController {
    @RequestMapping(value = "/auth/redirect", method = RequestMethod.GET)
    /**
     * Protected endpoint that triggers the authentication and then simply redirect to the url specified by the 'redirect' request parameter
     *
     * @param request  the request
     * @param response the response
     * @return a redirect instruction to the specified URL in the 'redirect' request parameter, or the main Datafari search page otherwise
     */
    public String authAndRedirect(final HttpServletRequest request, final HttpServletResponse response) {
      final String redirectPrefix = "redirect:";
      String urlRedirect = "/applyLang";
      final String mainPage = request.getContextPath();
      final String redirect = request.getParameter("redirect");

      if (redirect != null) {
        urlRedirect = redirect;
      } else {
        String langParam = null;
        // If the language parameter is defined take it, otherwise use the referrer in the message header
        final String lang = request.getParameter("lang");

        if (lang != null) {
          langParam = "?lang=" + lang;
        }

        // If the language param was defined in the source URL, append the language
        // selection to the adminUi page URL to be able to display it in the correct language
        if (langParam != null) {
          urlRedirect = urlRedirect + langParam + "&urlRedirect=" + mainPage + "/Search";
        } else {
          urlRedirect = urlRedirect + "?urlRedirect=" + mainPage + "/Search";
        }
      }

      return redirectPrefix + urlRedirect;
    }
  }

  @RestController
  public class StandardErrorController implements ErrorController {

    @RequestMapping("/error")
    public ModelAndView renderErrorPage(final HttpServletResponse httpResponse) {

      final ModelAndView errorPage = new ModelAndView("default-error");
      String errorMsg = "";
      final int httpErrorCode = httpResponse.getStatus();

      switch (httpErrorCode) {
      case 400: {
        errorPage.setViewName("400");
        errorMsg = "Http Error Code: 400. Bad Request";
        break;
      }
      case 401: {
        errorPage.setViewName("401");
        errorMsg = "Http Error Code: 401. Unauthorized";
        break;
      }
      case 403: {
        errorPage.setViewName("403");
        errorMsg = "Http Error Code: 403. Unauthorized";
        break;
      }
      case 404: {
        errorPage.setViewName("404");
        errorMsg = "Http Error Code: 404. Resource not found";
        break;
      }
      case 503: {
        errorPage.setViewName("503");
        errorMsg = "Http Error Code: 503. Internal Server Error";
        break;
      }
      case 504: {
        errorPage.setViewName("504");
        errorMsg = "Http Error Code: 504. Internal Server Error";
        break;
      }
      }
      errorPage.addObject("errorMsg", errorMsg);
      return errorPage;
    }

  }

  @Controller
  @ConditionalOnExpression("${saml.enabled:false}==false && ${keycloak.enabled:false}==false && ${cas.enabled:false}==false")
  public class StandardLoginController {
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String loginPage(@RequestParam(value = "error", required = false) final String error, @RequestParam(value = "logout", required = false) final String logout,
        @RequestParam(value = "timeout", required = false) final String timeout, final Model model) {
      String errorMessage = null;
      String errorType = null;
      if (error != null) {
        errorMessage = "Username or Password is incorrect !!";
        errorType = "credentials";
      }
      if (logout != null) {
        errorMessage = "You have been successfully logged out !!";
        errorType = "logout";
      }
      if (timeout != null) {
        errorMessage = "Your session has expired !!";
        errorType = "session";
      }
      model.addAttribute("errorMessage", errorMessage);
      model.addAttribute("errorType", errorType);
      return "login";
    }
  }

  @Controller
  @ConditionalOnExpression("${saml.enabled:false}==false && ${keycloak.enabled:false}==false && ${cas.enabled:false}==false")
  public class StandardLogoutController {

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logoutPage(final HttpServletRequest request, final HttpServletResponse response) {

      String langParam = "";

      // If the language parameter is defined take it, otherwise use the referrer in the message header
      final String lang = request.getParameter("lang");

      if (lang != null) {

        langParam = "&lang=" + lang;

      }

      final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null) {
        new SecurityContextLogoutHandler().logout(request, response, auth);
      }
      return "redirect:/login?logout=true" + langParam;
    }

  }

}
