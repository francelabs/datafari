var lastTimeout = null;

$(document).ready(function() {
  lastTimeout = setTimeout(function() {
    alert('Session has expired!');
    window.location.href = "/Datafari/login.jsp";
  }, secondsBeforeExpire * 1000);

  $("a").click(function(e) {
    if (lastTimeout != null) {
      clearTimeout(lastTimeout);
    }

    lastTimeout = setTimeout(function() {
      alert(window.i18n.msgStore['session-expired']);
      window.location.href = "/Datafari/login.jsp";
    }, secondsBeforeExpire * 1000);
  });
});