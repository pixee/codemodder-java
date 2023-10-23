package ai.pixee.integration.moveswitchdefaultlast.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test")
    public String switchTest(@RequestParam final int day) {

        switch(day) {

            default: return "Invalid";

            case 1: {
                return "Monday";
            }

            case 2: {
                return "Tuesday";
            }

            case 3: {
                return "Wednesday";
            }

            case 4: {
                return "Thursday";
            }

            case 5: {
                return "Friday";
            }

            case 6: {
                return "Saturday";
            }

            case 7: {
                return "Sunday";
            }
        }
    }

}
