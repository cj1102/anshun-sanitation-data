package com.anshun.dms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Routes known Vue SPA paths to the frontend entry document when dms/dist is present. */
@Controller
public class SpaController {
    @GetMapping({
            "/", "/login", "/dashboard", "/positions", "/leases", "/analytics",
            "/knowledge", "/ai/evaluation", "/system/users", "/system/audit-logs"
    })
    public String index() {
        return "forward:/index.html";
    }
}
