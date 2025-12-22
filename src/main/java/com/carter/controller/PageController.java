package com.carter.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Page controller for serving the SPA frontend.
 *
 * @author Carter
 * @since 1.0.0
 */
@Controller
public class PageController {

    /**
     * Serves the main SPA application.
     * All frontend routes are handled by index.html.
     */
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }
}

