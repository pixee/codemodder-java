This file tests several SARIF-driven fixing concepts.

# This section is all vulnerable code that we should be able to patch.
<%= vulnerableVariable%>
<%=vulnerableVariable %>
<%= vulnerableVariable %>
<%= vulnerableFunction() %>
<%= SomeType.vulnerableFunction() %>
<%= "" + vulnerable_Variable + SomeType.vulnerableFunction() %>
<%= vulnerable_Variable + SomeType.vulnerableFunction() %>
This line has text before <%= vulnerableVariable %> and text after.
This line has a vulnerable EL ${foo}
${foo} with vulnerable EL at the beginning
Vulnerable EL ${foo} in the middle

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
