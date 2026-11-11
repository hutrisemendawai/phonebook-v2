package com.pb.phonebook.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GuideController {

    @GetMapping("/panduan")
    public String guidePage() {
        return "guide/index";
    }

}
