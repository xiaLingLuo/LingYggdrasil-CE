package im.xz.cn.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PlayerProfile {
    private String id;
    private String userId;
    private String name;
    private String skinUrl;
    private String capeUrl;
    private String skinModel;
    private String yggdrasilToken;
    private String createdAt;

    public PlayerProfile(String id, String userId, String name, String skinUrl, String capeUrl,
                         String skinModel, String yggdrasilToken, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.skinUrl = skinUrl;
        this.capeUrl = capeUrl;
        this.skinModel = skinModel;
        this.yggdrasilToken = yggdrasilToken;
        this.createdAt = createdAt;
    }

    public static PlayerProfile fromResultSet(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String userId = rs.getString("user_id");
        String name = rs.getString("name");
        String skinUrl = rs.getString("skin_url");
        String capeUrl = rs.getString("cape_url");
        String skinModel = rs.getString("skin_model");
        if (skinModel == null || skinModel.isEmpty()) skinModel = "default";
        String yggdrasilToken = rs.getString("yggdrasil_token");
        String createdAt = rs.getString("created_at");
        return new PlayerProfile(id, userId, name, skinUrl, capeUrl, skinModel, yggdrasilToken, createdAt);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSkinUrl() { return skinUrl; }
    public void setSkinUrl(String skinUrl) { this.skinUrl = skinUrl; }

    public String getCapeUrl() { return capeUrl; }
    public void setCapeUrl(String capeUrl) { this.capeUrl = capeUrl; }

    public String getSkinModel() { return skinModel; }
    public void setSkinModel(String skinModel) { this.skinModel = skinModel; }

    public String getYggdrasilToken() { return yggdrasilToken; }
    public void setYggdrasilToken(String yggdrasilToken) { this.yggdrasilToken = yggdrasilToken; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
