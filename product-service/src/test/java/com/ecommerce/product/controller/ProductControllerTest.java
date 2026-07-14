package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.enums.ProductStatus;
import com.ecommerce.product.exception.ProductAlreadyExistsException;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

        when(productService.getAllProducts())
                .thenReturn(List.of(
                        ProductResponse.builder()
                                .id(1L)
                                .name("Laptop")
                                .build()));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Laptop"));
    }

    @Test
    @WithMockUser
    void getProductById() throws Exception {

        when(productService.getProductById(1L))
                .thenReturn(ProductResponse.builder()
                        .id(1L)
                        .name("Laptop")
                        .build());

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    @WithMockUser
    void searchProducts() throws Exception {

        when(productService.searchByKeyword("lap"))
                .thenReturn(List.of(
                        ProductResponse.builder()
                                .id(1L)
                                .name("Laptop")
                                .build()));

        mockMvc.perform(get("/api/products/search")
                        .param("keyword", "lap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Laptop"));
    }

    @Test
    @WithMockUser
    void searchWithFilters() throws Exception {

        when(productService.searchWithFilters("Laptop", "Electronics"))
                .thenReturn(List.of(
                        ProductResponse.builder()
                                .id(1L)
                                .name("Laptop")
                                .category("Electronics")
                                .build()));

        mockMvc.perform(get("/api/products/search/filters")
                        .param("name", "Laptop")
                        .param("category", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Electronics"));
    }

    @Test
    @WithMockUser
    void getCategories() throws Exception {

        when(productService.getCategories())
                .thenReturn(List.of("Electronics"));

        mockMvc.perform(get("/api/products/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Electronics"));
    }

    @Test
    @WithMockUser
    void getProductsByCategory() throws Exception {

        when(productService.getProductsByCategory("Electronics"))
                .thenReturn(List.of(
                        ProductResponse.builder()
                                .id(1L)
                                .name("Laptop")
                                .category("Electronics")
                                .build()));

        mockMvc.perform(get("/api/products/category/Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Electronics"));
    }

    @Test
    @WithMockUser
    void getProductsByStatus() throws Exception {

        when(productService.getProductsByStatus(ProductStatus.ACTIVE))
                .thenReturn(List.of(
                        ProductResponse.builder()
                                .id(1L)
                                .name("Laptop")
                                .status(ProductStatus.ACTIVE)
                                .build()));

        mockMvc.perform(get("/api/products/status/ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct() throws Exception {

        ProductRequest request = new ProductRequest(
                "Laptop",
                "Gaming Laptop",
                85000.0,
                10,
                "Electronics",
                "LAP-100",
                ProductStatus.ACTIVE);

        ProductResponse response = ProductResponse.builder()
                .id(1L)
                .name("Laptop")
                .build();

        when(productService.createProduct(any(ProductRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/product")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduct() throws Exception {

        ProductRequest request = new ProductRequest(
                "Laptop Updated",
                "Gaming Laptop",
                90000.0,
                20,
                "Electronics",
                "LAP-100",
                ProductStatus.ACTIVE);

        ProductResponse response = ProductResponse.builder()
                .id(1L)
                .name("Laptop Updated")
                .build();

        when(productService.updateProduct(eq(1L), any(ProductRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/products/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop Updated"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct() throws Exception {

        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void productNotFound() throws Exception {

        when(productService.getProductById(100L))
                .thenThrow(new ProductNotFoundException(
                        "Product not found with id : 100"));

        mockMvc.perform(get("/api/products/100"))
                .andExpect(status().isNotFound());
    }

//    @Test
//    @WithMockUser(roles = "ADMIN")
//    void duplicateProduct() throws Exception {
//
//        ProductRequest request = new ProductRequest(
//                "Laptop",
//                "Gaming Laptop",
//                85000.0,
//                10,
//                "Electronics",
//                "LAP-100",
//                ProductStatus.ACTIVE);
//
//        when(productService.createProduct(any(ProductRequest.class)))
//                .thenThrow(new ProductAlreadyExistsException(
//                        "Product already exists"));
//
//        mockMvc.perform(post("/api/product")
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isConflict());
//    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void validationTest() throws Exception {

        ProductRequest request = new ProductRequest(
                "",
                "",
                -1.0,
                -5,
                "",
                "",
                ProductStatus.ACTIVE);

        mockMvc.perform(post("/api/product")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

//    @Test
//    @WithMockUser(roles = "USER")
//    void userCannotCreateProduct() throws Exception {
//
//        ProductRequest request = new ProductRequest(
//                "Laptop",
//                "Gaming Laptop",
//                85000.0,
//                10,
//                "Electronics",
//                "LAP-100",
//                ProductStatus.ACTIVE);
//
//        mockMvc.perform(post("/api/product")
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isForbidden());
//    }
}