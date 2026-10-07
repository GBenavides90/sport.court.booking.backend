package com.sport.court.booking.service;

import com.sport.court.booking.domain.Category;
import com.sport.court.booking.exception.ConflictException;
import com.sport.court.booking.exception.NotFoundException;
import com.sport.court.booking.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** HU21 — Agregar categoría (título, descripción e imagen representativa). */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> listAll() {
        return categoryRepository.findAll();
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("La categoría no existe"));
    }

    public Category create(String title, String description, String imageUrl) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("El título es obligatorio");
        if (description == null || description.isBlank()) throw new IllegalArgumentException("La descripción es obligatoria");
        if (categoryRepository.existsByTitleIgnoreCase(title.trim())) {
            throw new ConflictException("Ya existe una categoría con ese título");
        }
        Category c = new Category();
        c.setTitle(title.trim());
        c.setDescription(description.trim());
        c.setImageUrl(imageUrl);
        return categoryRepository.save(c);
    }
}
