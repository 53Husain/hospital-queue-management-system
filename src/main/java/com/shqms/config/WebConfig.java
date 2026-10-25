package com.shqms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Browser pages and API are served by the same Spring Boot origin. */
@Configuration
public class WebConfig implements WebMvcConfigurer {
}
