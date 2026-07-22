package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.enums.ProductStatus;
import com.ecommerce.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
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
    @WithMockUser(roles = "CUSTOMER")
    void getAllProducts_shouldReturnOk() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(
                ProductResponse.builder().id(1L).name("Phone").build()
        ));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Phone"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getProductById_shouldReturnOk() throws Exception {
        when(productService.getProductById(1L)).thenReturn(
                ProductResponse.builder().id(1L).name("Phone").build()
        );

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Phone"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void searchProducts_shouldReturnOk() throws Exception {
        when(productService.searchByKeyword("phone")).thenReturn(List.of(
                ProductResponse.builder().id(1L).name("Phone").build()
        ));

        mockMvc.perform(get("/api/products/search").param("keyword", "phone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Phone"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void searchWithFilters_shouldReturnOk() throws Exception {
        when(productService.searchWithFilters("Phone", "Electronics")).thenReturn(List.of(
                ProductResponse.builder().id(1L).name("Phone").build()
        ));

        mockMvc.perform(get("/api/products/search/filters")
                        .param("name", "Phone")
                        .param("category", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Phone"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getCategories_shouldReturnOk() throws Exception {
        when(productService.getCategories()).thenReturn(List.of("Electronics", "Clothing"));

        mockMvc.perform(get("/api/products/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Electronics"))
                .andExpect(jsonPath("$[1]").value("Clothing"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getProductsByCategory_shouldReturnOk() throws Exception {
        when(productService.getProductsByCategory("Electronics")).thenReturn(List.of(
                ProductResponse.builder().id(1L).name("Phone").build()
        ));

        mockMvc.perform(get("/api/products/category/Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Phone"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getProductsByStatus_shouldReturnOk() throws Exception {
        when(productService.getProductsByStatus(ProductStatus.ACTIVE)).thenReturn(List.of(
                ProductResponse.builder().id(1L).name("Phone").status(ProductStatus.ACTIVE).build()
        ));

        mockMvc.perform(get("/api/products/status/ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_shouldReturnCreated() throws Exception {
        ProductRequest request = new ProductRequest(
                "Phone", "Desc", 100.0, 5, "Electronics", "SKU-1", ProductStatus.ACTIVE
        );

        when(productService.createProduct(any(ProductRequest.class)))
                .thenReturn(ProductResponse.builder().id(1L).name("Phone").build());

        mockMvc.perform(post("/api/product")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Phone"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduct_shouldReturnOk() throws Exception {
        ProductRequest request = new ProductRequest(
                "Phone Updated", "Desc", 120.0, 7, "Electronics", "SKU-1", ProductStatus.ACTIVE
        );

        when(productService.updateProduct(eq(1L), any(ProductRequest.class)))
                .thenReturn(ProductResponse.builder().id(1L).name("Phone Updated").build());

        mockMvc.perform(put("/api/products/1")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Phone Updated"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_shouldReturnNoContent() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
    }
}