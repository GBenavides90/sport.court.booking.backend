package com.sport.court.booking.config;

import com.sport.court.booking.domain.Category;
import com.sport.court.booking.domain.Feature;
import com.sport.court.booking.domain.Role;
import com.sport.court.booking.domain.User;
import com.sport.court.booking.repository.CategoryRepository;
import com.sport.court.booking.repository.CourtRepository;
import com.sport.court.booking.repository.FeatureRepository;
import com.sport.court.booking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Datos iniciales del Sprint 2:
 *  - Categorías base y características base (si las tablas están vacías).
 *  - Migración de las canchas existentes del Sprint 1 (columna de texto `category`) a la entidad Category.
 *  - Administrador inicial (HU16) configurable con ADMIN_EMAIL / ADMIN_PASSWORD.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final Map<String, String> LEGACY_TITLES = Map.of(
            "futbol", "Fútbol", "tenis", "Tenis", "padel", "Pádel", "squash", "Squash", "basquet", "Básquet");

    private final CategoryRepository categoryRepository;
    private final FeatureRepository featureRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;
    private final CourtRepository courtRepository;
    private final com.sport.court.booking.repository.BookingRepository bookingRepository;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(CategoryRepository categoryRepository, FeatureRepository featureRepository,
                           UserRepository userRepository, CourtRepository courtRepository,
                           com.sport.court.booking.repository.BookingRepository bookingRepository,
                           PasswordEncoder passwordEncoder, JdbcTemplate jdbc,
                           @Value("${app.admin.email:admin@sportcourt.com}") String adminEmail,
                           @Value("${app.admin.password:Admin#2026}") String adminPassword) {
        this.categoryRepository = categoryRepository;
        this.featureRepository = featureRepository;
        this.userRepository = userRepository;
        this.courtRepository = courtRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedCategories();
        seedFeatures();
        migrateLegacyCourtCategories();
        seedAdmin();
        ensureCourtImagesAndSampleBookings();
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) return;
        categoryRepository.saveAll(List.of(
                category("Fútbol", "Canchas de fútbol 5, 7 y 11 con césped sintético o natural."),
                category("Tenis", "Canchas de tenis en arcilla y superficie dura."),
                category("Pádel", "Canchas de pádel con paredes de cristal."),
                category("Squash", "Canchas de squash cerradas y climatizadas."),
                category("Básquet", "Canchas de básquet techadas y al aire libre.")));
    }

    private void seedFeatures() {
        if (featureRepository.count() > 0) return;
        featureRepository.saveAll(List.of(
                new Feature(null, "Iluminación nocturna", "lighting"),
                new Feature(null, "Vestuarios", "lockers"),
                new Feature(null, "Duchas", "shower"),
                new Feature(null, "Estacionamiento", "parking"),
                new Feature(null, "Wi-Fi", "wifi"),
                new Feature(null, "Cafetería", "cafe")));
    }

    private void migrateLegacyCourtCategories() {
        try {
            // La columna de texto heredada ya no se usa: se permite NULL para que las canchas nuevas puedan guardarse.
            jdbc.execute("ALTER TABLE courts MODIFY category VARCHAR(255) NULL");
        } catch (DataAccessException e) {
            log.debug("Columna heredada courts.category no modificable (base nueva o dialecto distinto): {}", e.getMessage());
        }
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT id, category FROM courts WHERE category_id IS NULL AND category IS NOT NULL");
            for (Map<String, Object> row : rows) {
                String legacy = String.valueOf(row.get("category")).trim();
                String title = LEGACY_TITLES.getOrDefault(legacy.toLowerCase(),
                        legacy.isEmpty() ? "General" : Character.toUpperCase(legacy.charAt(0)) + legacy.substring(1));
                Category cat = categoryRepository.findByTitleIgnoreCase(title)
                        .orElseGet(() -> categoryRepository.save(category(title, "Canchas de " + title.toLowerCase() + ".")));
                jdbc.update("UPDATE courts SET category_id = ? WHERE id = ?", cat.getId(), row.get("id"));
            }
            if (!rows.isEmpty()) log.info("Migradas {} canchas existentes a la nueva entidad Categoría", rows.size());
        } catch (DataAccessException e) {
            // Base nueva: no existe la columna de texto heredada `category`. No hay nada que migrar.
            log.debug("Sin migración de categorías heredadas: {}", e.getMessage());
        }

        // Si todavía existen canchas con category_id NULL o inválido, asignar la primera categoría disponible
        try {
            Category defaultCat = categoryRepository.findAll().stream().findFirst().orElse(null);
            if (defaultCat != null) {
                int fixedNull = jdbc.update("UPDATE courts SET category_id = ? WHERE category_id IS NULL", defaultCat.getId());
                int fixedOrphan = jdbc.update(
                        "UPDATE courts c LEFT JOIN categories cat ON c.category_id = cat.id SET c.category_id = ? WHERE cat.id IS NULL",
                        defaultCat.getId());
                if (fixedNull + fixedOrphan > 0) {
                    log.info("Corregidas {} canchas huérfanas asociándolas a la categoría '{}'",
                            fixedNull + fixedOrphan, defaultCat.getTitle());
                }
            }
        } catch (DataAccessException e) {
            log.warn("Aviso al verificar canchas huérfanas: {}", e.getMessage());
        }
    }

    private void seedAdmin() {
        String email = adminEmail.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) return;
        User admin = new User();
        admin.setFirstName("Administrador");
        admin.setLastName("Sport Court");
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("Administrador inicial creado: {}", email);
    }

    private static final Map<String, List<String>> THEMATIC_GALLERIES = Map.of(
            "Fútbol", List.of(
                    "https://images.unsplash.com/photo-1574629810360-7efbbe195018?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1529900245534-47fbf866b35e?auto=format&fit=crop&w=800&q=80",
                    "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?auto=format&fit=crop&w=800&q=80",
                    "https://images.unsplash.com/photo-1518091043644-c1d4457512c6?auto=format&fit=crop&w=800&q=80",
                    "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?auto=format&fit=crop&w=800&q=80"
            ),
            "Tenis", List.of(
                    "https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1554068865-24cecd4e34b8?auto=format&fit=crop&w=800&q=80",
                    "https://images.unsplash.com/photo-1622279457486-62dcc4a431d6?auto=format&fit=crop&w=800&q=80",
                    "https://images.unsplash.com/photo-1560012057-4372e14c5085?auto=format&fit=crop&w=800&q=80",
                    "https://images.unsplash.com/photo-1531315630201-bb15abeb1653?auto=format&fit=crop&w=800&q=80"
            ),
            "Pádel", List.of(
                    "https://images.unsplash.com/photo-1592656094267-764a45160876?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1612872087720-bb876e2e67d1?auto=format&fit=crop&w=800&q=80",
                    "https://images.unsplash.com/photo-1626245566085-f3775dbd1b58?auto=format&fit=crop&w=800&q=80",
                    "https://images.unsplash.com/photo-1541252260730-0412e8e2108e?auto=format&fit=crop&w=800&q=80",
                    "https://images.unsplash.com/photo-1511067007798-446726aa44d5?auto=format&fit=crop&w=800&q=80"
            ),
            "General", List.of(
                    "https://images.unsplash.com/photo-1574629810360-7efbbe195018?auto=format&fit=crop&w=1200&q=80",
                    "https://images.unsplash.com/photo-1546519638-68e109498ffc?auto=format&fit=crop&w=800&q=80",
                    "https://images.unsplash.com/photo-1519766304817-4f37bda74a29?auto=format&fit=crop&w=800&q=80",
                    "https://images.unsplash.com/photo-1577223625816-7546f13df25d?auto=format&fit=crop&w=800&q=80",
                    "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=800&q=80"
            )
    );

    // Inicializar fotos de galería por defecto si no existen
    private void ensureCourtImagesAndSampleBookings() {
        try {
            List<com.sport.court.booking.domain.Court> courts = courtRepository.findAll();
            for (com.sport.court.booking.domain.Court c : courts) {
                if (c.getImages() == null || c.getImages().isEmpty()) {
                    String catTitle = c.getCategory() != null ? c.getCategory().getTitle() : "General";
                    List<String> gallery = THEMATIC_GALLERIES.getOrDefault(catTitle, THEMATIC_GALLERIES.get("General"));
                    c.setImages(new java.util.ArrayList<>(gallery));
                    if (c.getImageUrl() == null || c.getImageUrl().isBlank()) {
                        c.setImageUrl(gallery.get(0));
                    }
                    courtRepository.save(c);
                }
            }

            if (bookingRepository.count() == 0 && !courts.isEmpty()) {
                User admin = userRepository.findByEmailIgnoreCase(adminEmail.trim().toLowerCase()).orElse(null);
                if (admin != null) {
                    com.sport.court.booking.domain.Court firstCourt = courts.get(0);
                    java.time.LocalDate today = java.time.LocalDate.now();
                    com.sport.court.booking.domain.Booking b1 = com.sport.court.booking.domain.Booking.builder()
                            .bookingCode("SCB-SAMPLE-101")
                            .court(firstCourt)
                            .user(admin)
                            .startDate(today.plusDays(1))
                            .endDate(today.plusDays(2))
                            .startTime("10:00")
                            .endTime("12:00")
                            .notes("Reserva inicial de demostración")
                            .status("CONFIRMED")
                            .createdAt(java.time.LocalDateTime.now())
                            .build();
                    bookingRepository.save(b1);
                    log.info("Reserva inicial creada para demostración en cancha #{}", firstCourt.getId());
                }
            }
        } catch (Exception e) {
            log.warn("Aviso al inicializar imágenes y reservas de muestra: {}", e.getMessage());
        }
    }

    private Category category(String title, String description) {
        return new Category(null, title, description, null);
    }
}
