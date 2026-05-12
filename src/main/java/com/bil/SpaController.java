package com.bil;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards all non-API, non-static routes to the React SPA's index.html
 * so that client-side routing (React Router / single-page navigation) works
 * when the app is served directly from Spring Boot on port 8080.
 */
@Controller
public class SpaController {

    @RequestMapping(value = {
            "/",
            "/{path:[^\\.]*}",
            "/{path:(?!api|swagger|v3|h2|actuator).*}/**/{subPath:[^\\.]*}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}

