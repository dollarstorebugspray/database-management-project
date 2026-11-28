package com.grocerystore.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ProductImagesTests {

    @Test
    void findsImagesByProductId() {
        ProductImages images = new ProductImages(List.of("1.svg", "12.png", "notes.txt", "abc.svg"));

        assertThat(images.urlFor(1)).isEqualTo("/images/products/1.svg");
        assertThat(images.urlFor(12)).isEqualTo("/images/products/12.png");
        assertThat(images.urlFor(2)).isNull();
    }

    @Test
    void photoTakesOverTheDrawing() {
        assertThat(new ProductImages(List.of("5.svg", "5.jpg")).urlFor(5)).isEqualTo("/images/products/5.jpg");
        assertThat(new ProductImages(List.of("5.jpg", "5.svg")).urlFor(5)).isEqualTo("/images/products/5.jpg");
    }
}
