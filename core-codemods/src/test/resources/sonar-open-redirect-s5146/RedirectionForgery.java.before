import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RedirectionForgery{

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
	  String location = req.getParameter("url");
	  resp.sendRedirect(location);
	}

}
