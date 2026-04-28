package com.labcompare.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {
    @RequestMapping(value = {"/", "/search", "/book", "/my-bookings", "/admin", "/login", "/superadmin"})
    public String forward() {
        return "forward:/index.html";
    }
}