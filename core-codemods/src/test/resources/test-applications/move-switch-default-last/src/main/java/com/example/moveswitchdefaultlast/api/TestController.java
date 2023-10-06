package com.example.moveswitchdefaultlast.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/")
    public String switchTest(@RequestParam final int day) {

        final String dayString = switch (day) {

            // Case
            case 1 -> "Monday";

            // Case
            case 2 -> "Tuesday";

            // Case
            case 3 -> "Wednesday";

            // Case
            case 4 -> "Thursday";
            // Default case
            default -> "Invalid day";

            // Case
            case 5 -> "Friday";

            // Case
            case 6 -> "Saturday";

            // Case
            case 7 -> "Sunday";
        };

      return dayString;
    }

}
