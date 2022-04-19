Dangerous JSP stuff

<%=io.pixee.security.XSS.htmlEncode(request.getParameter("no spaces"))%>

<%=io.pixee.security.XSS.htmlEncode(   request.getParameter("lots of spaces")
)%>

<%=io.pixee.security.XSS.htmlEncode( request.getHeader ("something_here") )%>

<%=io.pixee.security.XSS.htmlEncode( request.getQueryString() )%>