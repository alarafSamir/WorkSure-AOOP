package com.worksure.web;

import com.worksure.db.Db;
import com.worksure.security.AuthUser;
import com.worksure.security.SecurityUtils;
import com.worksure.storage.VerificationDocuments;
import com.worksure.util.RowMaps;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;

@RestController
public class VerificationDocumentController {
    private final Db db;
    private final VerificationDocuments documents;

    public VerificationDocumentController(Db db, VerificationDocuments documents) {
        this.db = db;
        this.documents = documents;
    }

    @GetMapping("/api/verification-documents/{id}")
    public ResponseEntity<Resource> download(@PathVariable long id) {
        AuthUser user = SecurityUtils.currentUser();
        Map<String, Object> doc = db.queryOne("SELECT d.*, w.user_id FROM worker_documents d JOIN workers w ON w.id = d.worker_id WHERE d.id = ?", id);
        if (doc == null) throw new ApiException(404, "Document not found");
        if (!"admin".equals(user.role()) && !("worker".equals(user.role()) && user.id() == RowMaps.asLong(doc.get("user_id")))) {
            throw new ApiException(403, "Only the document owner or an admin may download this document");
        }
        Path file = documents.privatePath(String.valueOf(doc.get("file_url")));
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new ApiException(404, "Document file unavailable");
        String name = file.getFileName().toString();
        String extension = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
        // Download rather than rendering untrusted uploaded content in the application origin.
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"verification-" + id + extension + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(file));
    }
}
