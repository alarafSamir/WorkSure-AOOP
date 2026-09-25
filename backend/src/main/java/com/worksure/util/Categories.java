package com.worksure.util;

import com.worksure.db.Db;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class Categories {
    private final Db db;

    public Categories(Db db) {
        this.db = db;
    }

    public List<Long> resolveCategoryIds(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        Map<String, Object> row = db.queryOne("SELECT id, parent_id FROM categories WHERE slug = ?", slug);
        if (row == null) {
            return null;
        }
        if (row.get("parent_id") != null) {
            return List.of(RowMaps.asLong(row.get("id")));
        }
        List<Map<String, Object>> children = db.query("SELECT id FROM categories WHERE parent_id = ?", row.get("id"));
        if (children.isEmpty()) {
            return List.of(RowMaps.asLong(row.get("id")));
        }
        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> c : children) {
            ids.add(RowMaps.asLong(c.get("id")));
        }
        return ids;
    }

    public String inPlaceholders(int n) {
        return String.join(", ", java.util.Collections.nCopies(n, "?"));
    }
}
