<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/templates/feature-not-available.css" />" media="screen" />
<div class="unavailable-title">
  <h1>
    Feature not available in the community edition !
  </h1>
</div>
<div class="feature-description">${param.featureDesc} is not available in the community edition. You can either create it by yourself, or <a href='mailto:info@francelabs.com'>contact us</a> to get a free trial version of the Enterprise Edition.</div>
<div class="feature-image"><img src="<c:url value="${param.featureImg}" />" ></div>