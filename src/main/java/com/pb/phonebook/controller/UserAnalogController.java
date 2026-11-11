package com.pb.phonebook.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserAnalogController {

    @GetMapping("/user-analog-ancol")
    public String userAnalogAncol() {
        return "users/analog-ancol";
    }
}

