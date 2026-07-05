package im.xz.cn.mail;

import im.xz.cn.config.MailConfig;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;

public class MailService {
    private final MailConfig config;
    private final Session session;
    private final String transportProtocol;

    public MailService(MailConfig config) {
        this.config = config;
        
        if (config.isEnabled()) {
            this.transportProtocol = detectTransportProtocol();
            this.session = createSession(transportProtocol);
        } else {
            this.transportProtocol = "smtp";
            this.session = null;
        }
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public void sendVerificationCode(String toEmail, String code) {
        String subject = "泠 Yggdrasil - 邮箱验证码";
        String body = buildVerificationHtml(code, "邮箱验证");
        sendHtmlEmail(toEmail, subject, body);
    }

    public void sendEmailChangeVerification(String toEmail, String code) {
        String subject = "泠 Yggdrasil - 邮箱变更验证码";
        String body = buildVerificationHtml(code, "邮箱变更");
        sendHtmlEmail(toEmail, subject, body);
    }

    public void sendPasswordChangeVerification(String toEmail, String code) {
        String subject = "泠 Yggdrasil - 密码变更验证码";
        String body = buildVerificationHtml(code, "密码变更");
        sendHtmlEmail(toEmail, subject, body);
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        if (!config.isEnabled()) {
            System.err.println("Mail service is disabled. Cannot send verification email.");
            return;
        }

        String maskedEmail = toEmail.replaceAll("(?<=.{2}).(?=.*@)", "*");

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.getFrom()));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject(subject, "UTF-8");
            message.setContent(htmlBody, "text/html; charset=UTF-8");
            message.setSentDate(new java.util.Date());

            if ("smtps".equals(transportProtocol)) {
                try (Transport transport = session.getTransport("smtps")) {
                    transport.connect(config.getHost(), config.getUsername(), config.getPassword());
                    transport.sendMessage(message, message.getAllRecipients());
                }
            } else {
                Transport.send(message);
            }

            System.out.println("Verification email sent to " + maskedEmail);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + maskedEmail
                    + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
    

    private String detectTransportProtocol() {
        if (canConnectSmtps()) {
            System.out.println("[MailService] Detected protocol: SMTPS (SSL)");
            return "smtps";
        }
        
        if (canConnectStarttls()) {
            System.out.println("[MailService] Detected protocol: STARTTLS");
            return "smtp";
        }
        
        throw new SecurityException("无法建立加密连接，请检查邮箱服务器");
    }

    private Session createSession(String protocol) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", config.getHost());
        props.put("mail.smtp.port", String.valueOf(config.getPort()));
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        
        props.put("mail.smtp.ssl.checkserveridentity", "true");
        props.put("mail.smtp.ssl.trust", "");

        if ("smtps".equals(protocol)) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtps.auth", "true");
            props.put("mail.smtps.host", config.getHost());
            props.put("mail.smtps.port", String.valueOf(config.getPort()));
            props.put("mail.smtps.ssl.enable", "true");
            props.put("mail.smtps.connectiontimeout", "5000");
            props.put("mail.smtps.timeout", "5000");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getUsername(), config.getPassword());
            }
        });
    }

    private boolean canConnectSmtps() {
        try {
            Properties props = getProps();

            Session testSession = Session.getInstance(props);
            Transport transport = testSession.getTransport("smtps");
            try {
                transport.connect(config.getHost(), config.getPort(),
                        config.getUsername(), config.getPassword());
                return true;
            } finally {
                if (transport.isConnected()) transport.close();
            }
        } catch (Exception e) {
            return false;
        }
    }

    @NotNull
    private Properties getProps() {
        Properties props = new Properties();
        props.put("mail.smtps.auth", "true");
        props.put("mail.smtps.host", config.getHost());
        props.put("mail.smtps.port", String.valueOf(config.getPort()));
        props.put("mail.smtps.ssl.enable", "true");
        props.put("mail.smtp.ssl.checkserveridentity", "true");
        props.put("mail.smtp.ssl.trust", "");
        props.put("mail.smtps.connectiontimeout", "5000");
        props.put("mail.smtps.timeout", "5000");
        return props;
    }

    private boolean canConnectStarttls() {
        try {
            Properties props = getProperties();

            Session testSession = Session.getInstance(props);
            Transport transport = testSession.getTransport("smtp");
            try {
                transport.connect(config.getHost(), config.getPort(),
                        config.getUsername(), config.getPassword());
                return true;
            } finally {
                if (transport.isConnected()) transport.close();
            }
        } catch (Exception e) {
            return false;
        }
    }

    @NotNull
    private Properties getProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", config.getHost());
        props.put("mail.smtp.port", String.valueOf(config.getPort()));
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.checkserveridentity", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        return props;
    }

    private String buildVerificationHtml(String code, String action) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif; background: linear-gradient(135deg, #FFE4F0, #F0E6FF); margin: 0; padding: 20px; }
                    .container { max-width: 480px; margin: 40px auto; background: #FFFFFF; border-radius: 16px; padding: 40px; box-shadow: 0 8px 32px rgba(255,105,180,0.15); }
                    .header { text-align: center; margin-bottom: 30px; }
                    .header h1 { color: #FF69B4; font-size: 24px; margin: 0; }
                    .header p { color: #999; font-size: 14px; margin-top: 8px; }
                    .code-box { background: linear-gradient(135deg, #FF69B4, #FF8DC7); color: #FFFFFF; text-align: center; padding: 20px; border-radius: 12px; margin: 24px 0; }
                    .code-box .code { font-size: 36px; font-weight: bold; letter-spacing: 8px; }
                    .code-box .label { font-size: 14px; margin-bottom: 8px; opacity: 0.9; }
                    .info { color: #666; font-size: 14px; line-height: 1.8; text-align: center; }
                    .footer { text-align: center; margin-top: 30px; color: #BBB; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>✿ 泠 Yggdrasil ✿</h1>
                        <p>%s验证码</p>
                    </div>
                    <div class="code-box">
                        <div class="label">您的验证码是</div>
                        <div class="code">%s</div>
                    </div>
                    <div class="info">
                        <p>验证码有效期为 <strong>5 分钟</strong></p>
                        <p>请勿将验证码泄露给他人</p>
                    </div>
                    <div class="footer">
                        <p>此邮件由系统自动发送，请勿回复</p>
                        <p>© LingYggdrasil</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(action, code);
    }
}
