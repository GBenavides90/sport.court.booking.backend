package com.sport.court.booking;

import com.sport.court.booking.domain.Category;
import com.sport.court.booking.domain.Court;
import com.sport.court.booking.domain.Feature;
import com.sport.court.booking.domain.Role;
import com.sport.court.booking.domain.User;
import com.sport.court.booking.repository.CategoryRepository;
import com.sport.court.booking.repository.CourtRepository;
import com.sport.court.booking.repository.FeatureRepository;
import com.sport.court.booking.repository.UserRepository;
import com.sport.court.booking.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

/** Base de las pruebas de integración: contexto completo con H2, MockMvc y rollback por prueba. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class AbstractIntegrationTest {

    public static final String PASSWORD = "Segura123";

    @Autowired protected MockMvc mvc;
    @Autowired protected UserRepository userRepository;
    @Autowired protected CategoryRepository categoryRepository;
    @Autowired protected FeatureRepository featureRepository;
    @Autowired protected CourtRepository courtRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected JwtService jwtService;

    protected User createUser(String email, Role role) {
        User u = new User();
        u.setFirstName("Ana");
        u.setLastName("Pérez");
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(PASSWORD));
        u.setRole(role);
        return userRepository.save(u);
    }

    protected String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    protected String adminToken() {
        return bearer(createUser("admin@test.com", Role.ADMIN));
    }

    protected String userToken() {
        return bearer(createUser("user@test.com", Role.USER));
    }

    protected Category category(String title) {
        return categoryRepository.save(new Category(null, title, "Descripción de " + title, null));
    }

    protected Feature feature(String name, String icon) {
        return featureRepository.save(new Feature(null, name, icon));
    }

    protected Court court(String name, Category category, Feature... features) {
        Court c = new Court();
        c.setName(name);
        c.setDescription("Cancha de prueba " + name);
        c.setCategory(category);
        c.setCapacity(10);
        Set<Feature> set = new LinkedHashSet<>(Set.of(features));
        c.setFeatures(set);
        return courtRepository.save(c);
    }
}
