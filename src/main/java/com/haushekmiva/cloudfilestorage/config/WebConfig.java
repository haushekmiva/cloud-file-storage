package com.haushekmiva.cloudfilestorage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        // Если файл существует (картинка, js, css) или это сам index.html — отдаем его
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }

                        // Если в пути есть точка (запрос файла, которого нет) — отдаем null (будет 404)
                        if (resourcePath.contains(".")) {
                            return null;
                        }

                        // Во всех остальных случаях (любые SPA пути вроде /files/) возвращаем index.html
                        return location.createRelative("index.html");
                    }
                });
    }
}
