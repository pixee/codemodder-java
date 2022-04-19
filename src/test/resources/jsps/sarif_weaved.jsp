This file tests several SARIF-driven fixing concepts.

# This section is all vulnerable code that we should be able to patch.
<%=io.pixee.security.XSS.htmlEncode(String.valueOf(vulnerableVariable))%>
<%=io.pixee.security.XSS.htmlEncode(String.valueOf(vulnerableVariable))%>
<%=io.pixee.security.XSS.htmlEncode(String.valueOf(vulnerableVariable))%>
<%=io.pixee.security.XSS.htmlEncode(String.valueOf(vulnerableFunction()))%>
<%=io.pixee.security.XSS.htmlEncode(String.valueOf(SomeType.vulnerableFunction()))%>
<%=io.pixee.security.XSS.htmlEncode(String.valueOf("" + vulnerable_Variable + SomeType.vulnerableFunction()))%>
<%=io.pixee.security.XSS.htmlEncode(String.valueOf(vulnerable_Variable + SomeType.vulnerableFunction()))%>
This line has text before <%=io.pixee.security.XSS.htmlEncode(String.valueOf(vulnerableVariable))%> and text after.
This line has a vulnerable EL ${fn:escapeXml(foo)}
${fn:escapeXml(foo)} with vulnerable EL at the beginning
Vulnerable EL ${fn:escapeXml(foo)} in the middle

# This section is all vulnerable code that we cant patch today.
This line starts here <%=
"and ends here" %>
This line has two vulnerable writes <%= vulnerableVariable %><%= vulnerableVariable %>
This line has two writes and only one vulnerable <%= vulnerableVariable %><%= safeVariable %>
This line has two vulnerable writes with text between <%= vulnerableVariable %> and <%= vulnerableVariable %>
This line has JSP expression and JSP action <%= vulnerableVariable %> and <c:out name="vulnerableVariable"/>
This line has JSP expression and EL <%= vulnerableVariable %> and ${vulnerableVariable}
This line has JSP expression, JSP action, and EL <%= vulnerableVariable %> and <c:out vulnerableVariable/> and ${vulnerableVariable}
This line has JSP expression language, but its empty ${} whoops

# This section contains one test -- a line that we can fix, but since theres no corresponding vulnerability, we dont.
<%= safeVariable %>

<%@ taglib uri = "http://java.sun.com/jsp/jstl/functions" prefix = "fn" %>
