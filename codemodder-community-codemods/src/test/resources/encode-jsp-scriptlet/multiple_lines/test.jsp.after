Dangerous JSP stuff

<%=org.owasp.encoder.Encode.forHtml(request.getParameter("no spaces"))%>

<%=org.owasp.encoder.Encode.forHtml(   request.getParameter("lots of spaces")
)%>

<%=org.owasp.encoder.Encode.forHtml( request.getHeader ("something_here") )%>

<%=org.owasp.encoder.Encode.forHtml( request.getQueryString() )%>