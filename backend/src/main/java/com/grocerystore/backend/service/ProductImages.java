package com.grocerystore.backend.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class ProductImages {

    private static final Logger log = LoggerFactory.getLogger(ProductImages.class);
    private static final Pattern FILE_NAME =
            Pattern.compile("(\\d+)\\.(svg|png|jpg|jpeg|webp)", Pattern.CASE_INSENSITIVE);

    private final Map<Integer, String> urls = new HashMap<>();

    public ProductImages() {
        try {
            Resource[] files = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:static/images/products/*");
            for (Resource file : files) {
                add(file.getFilename());
            }
            log.info("Found {} product images", urls.size());
        } catch (IOException exception) {
            log.warn("Could not read product images: {}", exception.getMessage());
        }
    }

    ProductImages(List<String> fileNames) {
        fileNames.forEach(this::add);
    }

    public String urlFor(int productId) {
        return urls.get(productId);
    }

    private void add(String fileName) {
        if (fileName == null) {
            return;
        }
        Matcher matcher = FILE_NAME.matcher(fileName);
        if (!matcher.matches()) {
            return;
        }
        int productId = Integer.parseInt(matcher.group(1));
        String url = "/images/products/" + fileName;
        String current = urls.get(productId);
        if (current == null || (current.endsWith(".svg") && !fileName.toLowerCase().endsWith(".svg"))) {
            urls.put(productId, url);
        }
    }
}
