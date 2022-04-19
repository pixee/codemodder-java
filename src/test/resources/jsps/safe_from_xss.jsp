"Safe" JSP stuff

<%= HTMLEncoder.sanitize(request.getParameter("no spaces")) %>

<%= literallyAnything(request.getParameter("lots of spaces")) %>



