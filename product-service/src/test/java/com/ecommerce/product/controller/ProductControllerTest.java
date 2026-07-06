package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.enums.ProductStatus;
import com.ecommerce.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @Test
    @WithMockUser
    void getAllProducts() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(ProductResponse.builder().id(1L).name("Laptop").build()));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Laptop"));
    }

    @Test
    @WithMockUser
    void getProductById() throws Exception {
        when(productService.getProductById(1L)).thenReturn(ProductResponse.builder().id(1L).name("Laptop").build());

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    @WithMockUser
    void searchProducts() throws Exception {
        when(productService.searchByKeyword("lap")).thenReturn(List.of(ProductResponse.builder().id(1L).name("Laptop").build()));

        mockMvc.perform(get("/api/products/search").param("keyword", "lap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Laptop"));
    }

    @Test
    @WithMockUser
    void searchWithFilters() throws Exception {
        when(productService.searchWithFilters("lap", "Electronics"))
                .thenReturn(List.of(ProductResponse.builder().id(1L).name("Laptop").build()));

        mockMvc.perform(get("/api/products/search/filters")
                        .param("name", "lap")
                        .param("category", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Laptop"));
    }

    @Test
    @WithMockUser
    void getCategories() throws Exception {
        when(productService.getCategories()).thenReturn(List.of("Electronics"));

        mockMvc.perform(get("/api/products/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Electronics"));
    }

    @Test
    @WithMockUser
    void getProductsByCategory() throws Exception {
        when(productService.getProductsByCategory("Electronics"))
                .thenReturn(List.of(ProductResponse.builder().id(1L).name("Laptop").build()));

        mockMvc.perform(get("/api/products/category/Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Laptop"));
    }

    @Test
    @WithMockUser
    void getProductsByStatus() throws Exception {
        when(productService.getProductsByStatus(ProductStatus.ACTIVE))
                .thenReturn(List.of(ProductResponse.builder().id(1L).name("Laptop").build()));

        mockMvc.perform(get("/api/products/status/ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Laptop"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct() throws Exception {
        ProductRequest request = new ProductRequest("Laptop", "Desc", 100.0, 2, "Electronics", "SKU1", ProductStatus.ACTIVE);
        when(productService.createProduct(any(ProductRequest.class)))
                .thenReturn(ProductResponse.builder().id(1L).name("Laptop").build());

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduct() throws Exception {
        ProductRequest request = new ProductRequest("Laptop", "Desc", 100.0, 2, "Electronics", "SKU1", ProductStatus.ACTIVE);
        when(productService.updateProduct(eq(1L), any(ProductRequest.class)))
                .thenReturn(ProductResponse.builder().id(1L).name("Laptop").build());

        mockMvc.perform(put("/api/products/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateQuantity() throws Exception {
        when(productService.updateQuantity(1L, 50))
                .thenReturn(ProductResponse.builder().id(1L).quantity(50).build());

        mockMvc.perform(put("/api/products/1/quantity")
                        .with(csrf())
                        .param("quantity", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(50));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct() throws Exception {
        mockMvc.perform(delete("/api/products/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

//    @Test
//    @WithMockUser(roles = "USER")
//    void adminEndpointsShouldForbidNonAdmin() throws Exception {
//        ProductRequest request = new ProductRequest("Laptop", "Desc", 100.0, 2, "Electronics", "SKU1", ProductStatus.ACTIVE);
//
//        mockMvc.perform(post("/api/products")
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isForbidden());
//    }
}