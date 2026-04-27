package com.labcompare.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards all non-API, non-asset requests to index.html
 * so Angular router handles client-side navigation.
 */
@Controller
public class SpaController {

    @RequestMapping(value = {"/", "/search", "/book", "/my-bookings", "/admin"})
    public String forward() {
        return "forward:/index.html";
    }
}
