package im.xz.cn.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.mail.MailService;

import java.util.Map;


public final class EmailNotifications {

    private static final ObjectMapper JSON = new ObjectMapper();

    private EmailNotifications() {
    }

    public static void enqueue(DatabaseManager database, String type, String target,
                               String code, String locale) {
        try {
            String payload = JSON.writeValueAsString(Map.of("code", code == null ? "" : code));
            new NotificationJobRepository(database).enqueue(type, target, payload, locale,
                    type + ":" + target + ":" + code);
        } catch (Exception error) {
            throw new IllegalStateException("cannot enqueue notification", error);
        }
    }

    public static void dispatch(NotificationJob job, MailService mailService) {
        if (job == null) {
            throw new IllegalArgumentException("notification job is required");
        }
        if (mailService == null || !mailService.isEnabled()) {
            throw new IllegalStateException("mail service is unavailable");
        }
        String code = "";
        try {
            Map<?, ?> payload = JSON.readValue(job.payloadJson() == null ? "{}" : job.payloadJson(), Map.class);
            Object raw = payload.get("code");
            code = raw == null ? "" : String.valueOf(raw);
        } catch (Exception ignored) {
            
        }
        boolean sent = switch (job.type()) {
            case "EMAIL_VERIFY" -> mailService.sendVerificationCode(job.target(), code);
            case "EMAIL_CHANGE" -> mailService.sendEmailChangeVerification(job.target(), code);
            case "PASSWORD_CHANGE" -> mailService.sendPasswordChangeVerification(job.target(), code);
            default -> throw new IllegalArgumentException("unknown notification type: " + job.type());
        };
        if (!sent) {
            
            throw new IllegalStateException("mail delivery failed: " + job.type());
        }
    }
}
