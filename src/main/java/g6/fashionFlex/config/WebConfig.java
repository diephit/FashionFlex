package g6.fashionFlex.config;

import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebConfig - Configures static resource serving in Spring
 * - Serves static resources (CSS, JS, images, fonts) from classpath/static/
 * - Serves uploaded files (products, customers) from external uploads/ directory
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Get upload directories from application.properties
    @Value("${app.upload.products-dir:uploads/products}")
    private String productsDir;

    @Value("${app.upload.customers-dir:uploads/customers}")
    private String customersDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        
        // ===== STATIC RESOURCES FROM CLASSPATH =====
        // These are part of your application JAR/build
        
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(3600); // Cache for 1 hour
        
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(3600);
        
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCachePeriod(3600);
        
        registry.addResourceHandler("/vendor/**")
                .addResourceLocations("classpath:/static/vendor/")
                .setCachePeriod(3600);
        
        registry.addResourceHandler("/fonts/**")
                .addResourceLocations("classpath:/static/fonts/")
                .setCachePeriod(3600);

        // ===== UPLOADED FILES FROM EXTERNAL DIRECTORY =====
        // These are files uploaded by users (product images, profile pictures, etc.)
        
        // Map /uploads/** URLs to the external uploads directory
        // Example: /uploads/customers/filename.jpg → file:uploads/customers/filename.jpg
        String uploadsBasePath = Paths.get("uploads").toAbsolutePath().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadsBasePath + "/")
                .setCachePeriod(0); // Don't cache uploaded files (they might change)
        
        // Alternative: Map specific directories
        // /uploads/products/** → products upload directory
        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations("file:" + Paths.get(productsDir).toAbsolutePath().toString() + "/")
                .setCachePeriod(0);
        
        // /uploads/customers/** → customers upload directory
        registry.addResourceHandler("/uploads/customers/**")
                .addResourceLocations("file:" + Paths.get(customersDir).toAbsolutePath().toString() + "/")
                .setCachePeriod(0);
    }
}