package com.worksure.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worksure.db.Db;
import com.worksure.util.RowMaps;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Order(1)
public class DatabaseSeeder implements CommandLineRunner {
    private static final String[] FIRST = {
            "Rahim", "Karim", "Fatima", "Rafiq", "Nusrat", "Hasan", "Ayesha", "Imran",
            "Sadia", "Tanvir", "Laila", "Mizan", "Sumaiya", "Farhan", "Anika", "Rubel",
            "Shuvo", "Nadia", "Omar", "Jamal", "Priya", "Arif", "Mehdi", "Hira",
            "Saiful", "Bashir", "Maya", "Tania", "Samira", "Fahim", "Rina", "Kabir",
            "Sharmin", "Nayeem", "Jannat", "Sohel", "Mahmud", "Rasel", "Yasmin", "Babul",
            "Farzana", "Kamal", "Shirin", "Nabil"
    };
    private static final String[] LAST = {
            "Rahman", "Hossain", "Khan", "Ahmed", "Islam", "Chowdhury", "Begum", "Ali",
            "Akter", "Uddin", "Miah", "Sarkar", "Das", "Khatun", "Haque", "Sultana"
    };
    private static final String[] CITIES = {
            "Dhaka", "Chittagong", "Sylhet", "Rajshahi", "Khulna", "Gazipur", "Narayanganj", "Rangpur"
    };
    private static final String[] ADDRESSES = {
            "House 12, Road 4, Dhanmondi", "Flat 3B, Agrabad", "Zindabazar, Sylhet",
            "Shaheb Bazar, Rajshahi", "Sonadanga, Khulna", "Tongi, Gazipur",
            "Chashara, Narayanganj", "Modern More, Rangpur"
    };

    private final Db db;
    private final PasswordEncoder encoder;
    private final boolean seedOnStart;
    private final ObjectMapper mapper = new ObjectMapper();

    public DatabaseSeeder(Db db, PasswordEncoder encoder, @Value("${app.seed-on-start:true}") boolean seedOnStart) {
        this.db = db;
        this.encoder = encoder;
        this.seedOnStart = seedOnStart;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!seedOnStart) {
            return;
        }
        seedCategories();
        seedAdmin();
        seedWorkers();
    }

    private void seedCategories() throws Exception {
        Map<String, Object> existing = db.queryOne("SELECT COUNT(*) AS c FROM categories");
        if (existing != null && RowMaps.asLong(existing.get("c")) > 0) {
            return;
        }
        JsonNode root = mapper.readTree(new ClassPathResource("service-catalog.json").getInputStream());
        int sortOrder = 0;
        for (JsonNode major : root.get("majors")) {
            long majorId = db.insert(
                    """
                    INSERT INTO categories (parent_id, name, slug, icon, description, image_url, sort_order)
                    VALUES (NULL, ?, ?, ?, ?, ?, ?)
                    """,
                    major.get("name").asText(),
                    major.get("slug").asText(),
                    major.path("icon").asText(null),
                    major.path("description").asText(null),
                    "/images/" + major.path("image").asText(""),
                    sortOrder++
            );
            int subOrder = 0;
            for (JsonNode sub : major.get("subfeatures")) {
                db.insert(
                        """
                        INSERT INTO categories (parent_id, name, slug, icon, description, image_url, sort_order)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                        majorId,
                        sub.get("name").asText(),
                        sub.get("slug").asText(),
                        major.path("icon").asText(null),
                        sub.path("description").asText(null),
                        "/images/" + sub.path("image").asText(""),
                        subOrder++
                );
            }
        }
        System.out.println("[WorkSure] Seeded service categories from catalog.");
    }

    private void seedAdmin() {
        if (db.queryOne("SELECT id FROM users WHERE email = ?", "admin@gmail.com") != null) {
            return;
        }
        String hash = encoder.encode("12345");
        db.insert(
                "INSERT INTO users (email, password_hash, role, full_name, phone, city, address) VALUES (?,?,?,?,?,?,?)",
                "admin@gmail.com", hash, "admin", "WorkSure Admin", "+8801710000001", "Dhaka", "WorkSure HQ, Gulshan, Dhaka"
        );
        System.out.println("[WorkSure] Seeded admin@gmail.com (password 12345).");
    }

    private void seedWorkers() {
        Map<String, Object> count = db.queryOne("SELECT COUNT(*) AS c FROM workers");
        if (count != null && RowMaps.asLong(count.get("c")) > 0) {
            return;
        }
        String hash = encoder.encode("12345");
        List<Map<String, Object>> subs = db.query(
                "SELECT id, slug, name FROM categories WHERE parent_id IS NOT NULL ORDER BY id"
        );
        int i = 0;
        for (Map<String, Object> sub : subs) {
            String first = FIRST[i % FIRST.length];
            String last = LAST[i % LAST.length];
            String fullName = first + " " + last;
            String email = first.toLowerCase() + "@gmail.com";
            if (db.queryOne("SELECT id FROM users WHERE email = ?", email) != null) {
                email = first.toLowerCase() + i + "@gmail.com";
            }
            String city = CITIES[i % CITIES.length];
            String address = ADDRESSES[i % ADDRESSES.length];
            String phone = "+88017" + String.format("%08d", 11000000 + i);
            int hourly = rateFor(String.valueOf(sub.get("slug")), i);
            double rating = 4.2 + (i % 8) * 0.1;
            int ratingCount = 6 + (i % 35);
            int verified = i % 5 == 0 ? 0 : 1;

            long userId = db.insert(
                    "INSERT INTO users (email, password_hash, role, full_name, phone, city, address, country) VALUES (?,?,?,?,?,?,?,?)",
                    email, hash, "worker", fullName, phone, city, address, "Bangladesh"
            );
            long workerId = db.insert(
                    """
                    INSERT INTO workers (user_id, headline, bio, hourly_rate, rating_avg, rating_count, is_verified, years_experience, availability, service_radius_km)
                    VALUES (?,?,?,?,?,?,?,?,?,?)
                    """,
                    userId,
                    sub.get("name") + " specialist — " + first,
                    "Professional " + String.valueOf(sub.get("name")).toLowerCase()
                            + " provider based in " + city + ". " + (3 + (i % 10))
                            + " years of experience serving households and businesses across Bangladesh.",
                    hourly,
                    String.format("%.1f", rating),
                    ratingCount,
                    verified,
                    2 + (i % 12),
                    "{\"mon\":true,\"tue\":true,\"wed\":true,\"thu\":true,\"fri\":true,\"sat\":" + (i % 2 == 0) + ",\"sun\":false}",
                    12 + (i % 10)
            );
            String slug = ("svc-" + sub.get("slug") + "-" + workerId + "-" + Integer.toHexString(i + 1000)).toLowerCase();
            db.insert(
                    """
                    INSERT INTO services (worker_id, category_id, title, slug, description, base_price, duration_minutes, is_active, tags)
                    VALUES (?,?,?,?,?,?,?,?,?)
                    """,
                    workerId,
                    sub.get("id"),
                    sub.get("name") + " — standard visit",
                    slug,
                    "On-demand " + String.valueOf(sub.get("name")).toLowerCase()
                            + " in " + city + ". Materials included where applicable. Reliable WorkSure professional.",
                    hourly * 2L,
                    90 + (i % 4) * 30,
                    1,
                    sub.get("slug") + ",bangladesh,verified,popular"
            );
            i++;
        }
        System.out.println("[WorkSure] Seeded " + i + " workers (password 12345) covering every sub-service.");
    }

    private static int rateFor(String slug, int i) {
        if (slug.contains("security") || slug.contains("bodyguard") || slug.contains("cctv") || slug.contains("biometric")) {
            return 700 + (i % 8) * 80;
        }
        if (slug.contains("catering") || slug.contains("wedding") || slug.contains("chef") || slug.contains("barbecue") || slug.contains("feast")) {
            return 900 + (i % 6) * 150;
        }
        if (slug.contains("electric") || slug.contains("wiring") || slug.contains("generator") || slug.contains("smart-home")) {
            return 550 + (i % 7) * 70;
        }
        if (slug.contains("nanny") || slug.contains("babysit") || slug.contains("infant") || slug.contains("child")) {
            return 350 + (i % 5) * 50;
        }
        if (slug.contains("pet") || slug.contains("dog") || slug.contains("puppy") || slug.contains("aquarium")) {
            return 400 + (i % 6) * 60;
        }
        return 400 + (i % 8) * 50;
    }
}
