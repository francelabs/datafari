var lastTimeout = null;

$(document).ready(function() {
  /**
   * In order to be able to use the sessionTimeout variable, it must be initialized in the login.jsp view with the following line:
   * 
   * <script type="text/javascript">var sessionTimeout = ${pageContext.session.maxInactiveInterval};</script>
   * 
   * This line must be placed BEFORE the current script is declared !!!!!
   * 
   */

  lastTimeout = setTimeout(function() {
    window.location.reload();
  }, sessionTimeout * 1000);

});
