package com.worksure.config;

import com.worksure.storage.VerificationDocuments;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final VerificationDocuments documents;

    public WebConfig(VerificationDocuments documents) {
        this.documents = documents;
    }
    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(path.toUri().toString())
                .resourceChain(false)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        // Fail closed until every legacy verification file has been moved.
                        if (!documents.ready()) return null;
                        Resource resource = super.getResource(resourcePath, location);
                        if (resource == null || !resource.getFile().toPath().toRealPath().startsWith(path.toRealPath())) return null;
                        return resource;
                    }
                });
    }
}
