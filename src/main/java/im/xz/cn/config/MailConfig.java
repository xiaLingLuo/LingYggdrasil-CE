package im.xz.cn.config;

public class MailConfig {
    private boolean enabled;
    private String host;
    private int port;
    private String username;
    private String password;
    private String from;
    private boolean tls = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public boolean isTls() { return tls; }
    public void setTls(boolean tls) { this.tls = tls; }
}
