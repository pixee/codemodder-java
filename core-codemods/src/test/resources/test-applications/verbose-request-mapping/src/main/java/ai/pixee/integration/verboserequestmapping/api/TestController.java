package ai.pixee.integration.verboserequestmapping.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/test")
public class TestController {

    @ResponseBody
    @RequestMapping("/hello")
    public String helloWorld() {
        return "Hello World!";
    }

    @ResponseBody
    @RequestMapping("/welcome")
    public String welcomeGfgMessage() {
        return "Welcome";
    }

    @ResponseBody
    @RequestMapping(value = "/greet", method = RequestMethod.GET)
    public String greet() {
        return "Greetings!";
    }

    @ResponseBody
    @RequestMapping(value = "/update", method = RequestMethod.PUT)
    public String updateData() {
        return "Data Updated!";
    }

    @ResponseBody
    @RequestMapping(value = "/delete", method = RequestMethod.DELETE)
    public String deleteData() {
        return "Data Deleted!";
    }

    @ResponseBody
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public String createData() {
        return "Data Created!";
    }
}