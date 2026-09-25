package com.worksure.storage;

import com.worksure.db.Db;
import com.worksure.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Verification files never use the public UploadService or its resource directory. */
@Service
@Order(2) // Schema and demo data must exist before migrating old documents.
public class VerificationDocuments implements CommandLineRunner {
    private final Db db;
    private final Path publicDir;
    private final Path privateDir;
    private volatile boolean ready;
    private static final Set<String> EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".pdf");

    public VerificationDocuments(Db db, @Value("${app.upload-dir:uploads}") String publicDir,
                                 @Value("${app.private-document-dir:private-documents}") String privateDir) throws IOException {
        this.db = db;
        Files.createDirectories(Path.of(publicDir));
        Files.createDirectories(Path.of(privateDir));
        this.publicDir = Path.of(publicDir).toRealPath();
        this.privateDir = Path.of(privateDir).toRealPath();
        if (this.privateDir.startsWith(this.publicDir)) {
            throw new IllegalArgumentException("Private documents must be outside the public upload directory");
        }
    }

    public boolean ready() { return ready; }

    @Override
    public void run(String... args) throws IOException {
        for (Map<String, Object> doc : db.query("SELECT id, file_url FROM worker_documents")) {
            String reference = String.valueOf(doc.get("file_url"));
            if (reference.startsWith("private:")) continue;
            String path = URI.create(reference).getPath();
            if (path == null || !path.startsWith("/uploads/")) {
                throw new IllegalStateException("Unrecognized legacy verification file for document " + doc.get("id"));
            }
            String name = path.substring("/uploads/".length());
            Path target = privatePath("private:" + name);
            Path source = publicDir.resolve(name);
            // Move first: a failed DB update must never leave a document publicly readable.
            // The same name makes a restart safe if the previous move already succeeded.
            if (Files.exists(source)) {
                if (Files.exists(target)) {
                    if (Files.mismatch(source, target) != -1) throw new IOException("Conflicting private document " + doc.get("id"));
                    Files.delete(source);
                } else {
                    Files.move(source, target);
                }
            }
            db.run("UPDATE worker_documents SET file_url = ? WHERE id = ?", "private:" + name, doc.get("id"));
        }
        // Bring legacy/demo flags under the same rule as future admin decisions.
        // This single SQL statement updates each flag and its timestamp atomically.
        db.run("""
                UPDATE workers w
                LEFT JOIN (SELECT worker_id, COUNT(*) AS approvals FROM worker_documents
                           WHERE status = 'approved' GROUP BY worker_id) d ON d.worker_id = w.id
                SET w.verified_at = CASE WHEN COALESCE(d.approvals, 0) > 0
                                        THEN COALESCE(w.verified_at, NOW()) ELSE NULL END,
                    w.is_verified = (COALESCE(d.approvals, 0) > 0)
                """);
        ready = true;
    }

    public String store(MultipartFile file) {
        if (!ready) throw new ApiException(503, "Document storage is starting. Please retry.");
        if (file == null || file.isEmpty()) throw new ApiException(400, "File required");
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String extension = dot < 0 ? "" : original.substring(dot).toLowerCase(Locale.ROOT);
        if (!EXTENSIONS.contains(extension)) throw new ApiException(400, "Only JPG, JPEG, PNG, WEBP and PDF files are allowed");
        String reference = "private:" + UUID.randomUUID() + extension;
        try {
            file.transferTo(privatePath(reference));
        } catch (IOException e) {
            throw new ApiException(500, "Could not save document");
        }
        return reference;
    }

    public Path privatePath(String reference) {
        if (!reference.startsWith("private:")) throw new ApiException(404, "Document file unavailable");
        String name = reference.substring("private:".length());
        if (!name.matches("[a-zA-Z0-9._-]+") || name.equals(".") || name.equals("..")) {
            throw new ApiException(404, "Document file unavailable");
        }
        return privateDir.resolve(name);
    }

    public List<Map<String, Object>> publicMetadata(List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            row.put("file_url", "/api/verification-documents/" + row.get("id"));
        }
        return rows;
    }
}
