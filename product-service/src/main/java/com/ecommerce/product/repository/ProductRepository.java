package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Product;
import com.ecommerce.product.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategory(String category);

    List<Product> findByStatus(ProductStatus status);

    List<Product> findByCategoryAndStatus(String category, ProductStatus status);

    List<Product> findByNameContaining(String name);

    boolean existsBySku(String sku);

    // Search by name OR category
    @Query("SELECT p FROM Product p WHERE " +
            "(LOWER(p.name) LIKE LOWER(:keyword) OR " +
            "LOWER(p.category) LIKE LOWER(:keyword)) AND " +
            "p.status = 'ACTIVE'")
    List<Product> searchByNameOrCategory(String keyword);

    // Get all unique categories
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL AND p.status = 'ACTIVE'")
    Set<String> findAllUniqueCategories();

    // Search with both name AND category filters
    @Query("SELECT p FROM Product p WHERE " +
            "(LOWER(p.name) LIKE LOWER(:name) OR :name IS NULL OR :name = '') AND " +
            "(LOWER(p.category) LIKE LOWER(:category) OR :category IS NULL OR :category = '') AND " +
            "p.status = 'ACTIVE'")
    List<Product> searchWithFilters(String name, String category);
}