/*
 * LingYggdrasil - A modern Minecraft skin/cape hosting and Yggdrasil API system
 * Copyright (C) 2026 XIAZHIRUI HUANG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package im.xz.cn.mail;


import im.xz.cn.i18n.I18n;
import im.xz.cn.logging.logApi;
import im.xz.cn.config.MailConfig;
import im.xz.cn.config.SystemConfig;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;

public class MailService {
    private static final logApi log = logApi.getLogger(MailService.class);
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
        SystemConfig sc = SystemConfig.getInstance();
        sendHtmlEmail(toEmail, I18n.t("mail.subjectVerify"), renderTemplate(sc.getMailTemplateVerify(), code));
    }

    public void sendEmailChangeVerification(String toEmail, String code) {
        SystemConfig sc = SystemConfig.getInstance();
        sendHtmlEmail(toEmail, I18n.t("mail.subjectEmailChange"), renderTemplate(sc.getMailTemplateEmailChange(), code));
    }

    public void sendPasswordChangeVerification(String toEmail, String code) {
        SystemConfig sc = SystemConfig.getInstance();
        sendHtmlEmail(toEmail, I18n.t("mail.subjectPasswordChange"), renderTemplate(sc.getMailTemplatePasswordChange(), code));
    }

    private String renderTemplate(String template, String code) {
        if (template == null || template.isBlank()) {
            return code == null ? "" : code;
        }
        return template.replace("{code}", code == null ? "" : code);
    }

    public boolean sendTestEmail(String toEmail, String content) {
        return sendHtmlEmail(toEmail, I18n.t("mail.subjectTest"), content == null ? "" : content);
    }

    private boolean sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        if (!config.isEnabled()) {
            log.warn("Mail service is disabled. Cannot send verification email.");
            return false;
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

            log.info("Verification email sent to {}", maskedEmail);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email to {}: {} - {}", maskedEmail,
                    e.getClass().getSimpleName(), e.getMessage(), e);
            return false;
        }
    }
    

    private String detectTransportProtocol() {
        if (canConnectSmtps()) {
            log.debug("[MailService] Detected protocol: SMTPS (SSL)");
            return "smtps";
        }
        
        if (canConnectStarttls()) {
            log.debug("[MailService] Detected protocol: STARTTLS");
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

}
