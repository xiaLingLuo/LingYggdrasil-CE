package im.xz.cn.database;

import im.xz.cn.model.PlayerProfile;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProfileDao {
    private final DatabaseManager db;

    public ProfileDao(DatabaseManager db) {
        this.db = db;
    }

    public void insert(PlayerProfile profile) {
        db.executeUpdate(
            "INSERT INTO player_profiles (id, user_id, name, skin_url, cape_url, skin_model, yggdrasil_token, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            profile.getId(), profile.getUserId(), profile.getName(),
            profile.getSkinUrl(), profile.getCapeUrl(), profile.getSkinModel(),
            profile.getYggdrasilToken(), profile.getCreatedAt()
        );
    }

    public PlayerProfile findById(String id) {
        return querySingle("SELECT * FROM player_profiles WHERE id = ?", id);
    }

    public PlayerProfile findByName(String name) {
        return querySingle("SELECT * FROM player_profiles WHERE name = ?", name);
    }

    public List<PlayerProfile> findByUserId(String userId) {
        List<PlayerProfile> profiles = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_profiles WHERE user_id = ? ORDER BY created_at ASC")) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    profiles.add(PlayerProfile.fromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ProfileDao.findByUserId failed: " + e.getMessage());
        }
        return profiles;
    }

    public List<PlayerProfile> findByNames(List<String> names) {
        if (names == null || names.isEmpty()) return new ArrayList<>();
        List<PlayerProfile> profiles = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT * FROM player_profiles WHERE name IN (");
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("?");
        }
        sb.append(")");
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sb.toString())) {
            for (int i = 0; i < names.size(); i++) {
                ps.setString(i + 1, names.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    profiles.add(PlayerProfile.fromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ProfileDao.findByNames failed: " + e.getMessage());
        }
        return profiles;
    }

    public void update(PlayerProfile profile) {
        db.executeUpdate(
            "UPDATE player_profiles SET name = ?, skin_url = ?, cape_url = ?, skin_model = ?, yggdrasil_token = ? WHERE id = ?",
            profile.getName(), profile.getSkinUrl(), profile.getCapeUrl(),
            profile.getSkinModel(), profile.getYggdrasilToken(), profile.getId()
        );
    }

    public List<PlayerProfile> findAll() {
        List<PlayerProfile> profiles = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_profiles ORDER BY created_at ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                profiles.add(PlayerProfile.fromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("ProfileDao.findAll failed: " + e.getMessage());
        }
        return profiles;
    }

    public void updateName(String id, String newName) {
        db.executeUpdate("UPDATE player_profiles SET name = ? WHERE id = ?", newName, id);
    }

    public void clearTextures(String id) {
        db.executeUpdate("UPDATE player_profiles SET skin_url = NULL, cape_url = NULL, skin_model = ? WHERE id = ?", "default", id);
    }

    public void updateUserId(String id, String newUserId) {
        db.executeUpdate("UPDATE player_profiles SET user_id = ? WHERE id = ?", newUserId, id);
    }

    public void updateToken(String id, String newToken) {
        db.executeUpdate("UPDATE player_profiles SET yggdrasil_token = ? WHERE id = ?", newToken, id);
    }

    public PlayerProfile findByNameAndToken(String name, String token) {
        return querySingle("SELECT * FROM player_profiles WHERE name = ? AND yggdrasil_token = ?", name, token);
    }

    public void delete(String id) {
        db.executeUpdate("DELETE FROM player_profiles WHERE id = ?", id);
    }

    public int count() {
        var result = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM player_profiles");
        if (result != null && result.get("cnt") != null) {
            return ((Number) result.get("cnt")).intValue();
        }
        return 0;
    }

    public int countByUserId(String userId) {
        var result = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM player_profiles WHERE user_id = ?", userId);
        if (result != null && result.get("cnt") != null) {
            return ((Number) result.get("cnt")).intValue();
        }
        return 0;
    }

    public boolean existsByName(String name) {
        var result = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM player_profiles WHERE name = ?", name);
        if (result != null && result.get("cnt") != null) {
            return ((Number) result.get("cnt")).intValue() > 0;
        }
        return false;
    }

    private PlayerProfile querySingle(String sql, Object... params) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return PlayerProfile.fromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("ProfileDao query failed: " + e.getMessage());
        }
        return null;
    }
}
