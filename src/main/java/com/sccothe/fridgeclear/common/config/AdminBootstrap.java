package com.sccothe.fridgeclear.common.config;

import com.sccothe.fridgeclear.ai.domain.AiProtocol;
import com.sccothe.fridgeclear.ai.domain.SystemAiConfig;
import com.sccothe.fridgeclear.ai.repository.SystemAiConfigRepository;
import com.sccothe.fridgeclear.ai.service.AesGcmCrypto;
import com.sccothe.fridgeclear.auth.domain.UserAccount;
import com.sccothe.fridgeclear.auth.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

/**
 * 首次启动引导：
 * 1. 若 .env 配置了 AI_PLATFORM_* 且 system_ai_config 为空，写入全局 AI 配置（之后由管理端页面维护）；
 * 2. 若配置了 ADMIN_BOOTSTRAP_EMAIL，将该邮箱用户提升为 ADMIN；不存在则创建。
 */
@Component
public class AdminBootstrap implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final SystemAiConfigRepository systemConfigRepository;
    private final AesGcmCrypto crypto;
    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${fridgeclear.ai.platform.provider-name:}") private String platformProviderName;
    @Value("${fridgeclear.ai.platform.protocol:OPENAI_CHAT}") private String platformProtocol;
    @Value("${fridgeclear.ai.platform.base-url:}") private String platformBaseUrl;
    @Value("${fridgeclear.ai.platform.model-name:}") private String platformModelName;
    @Value("${fridgeclear.ai.platform.api-key:}") private String platformApiKey;
    @Value("${fridgeclear.admin.bootstrap-email:}") private String adminEmail;
    @Value("${fridgeclear.admin.bootstrap-password:}") private String adminPassword;

    public AdminBootstrap(SystemAiConfigRepository systemConfigRepository, AesGcmCrypto crypto,
                          UserAccountRepository userRepository, PasswordEncoder passwordEncoder) {
        this.systemConfigRepository = systemConfigRepository;
        this.crypto = crypto;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedSystemAiConfig();
        bootstrapAdmin();
    }

    private void seedSystemAiConfig() {
        if (platformApiKey == null || platformApiKey.isBlank()) return;
        if (systemConfigRepository.count() > 0) return;
        AiProtocol protocol;
        try {
            protocol = AiProtocol.valueOf(platformProtocol.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            log.warn("AI_PLATFORM_PROTOCOL 不是有效的协议值（{}），跳过全局 AI 配置种子", platformProtocol);
            return;
        }
        if (platformBaseUrl == null || platformBaseUrl.isBlank()) {
            log.warn("AI_PLATFORM_BASE_URL 未配置，跳过全局 AI 配置种子");
            return;
        }
        SystemAiConfig config = new SystemAiConfig();
        config.setProviderName(platformProviderName == null || platformProviderName.isBlank() ? "平台默认" : platformProviderName.trim());
        config.setProtocol(protocol);
        config.setBaseUrl(platformBaseUrl.trim());
        config.setModelName(platformModelName == null ? "" : platformModelName.trim());
        config.setApiKeyCiphertext(crypto.encrypt(platformApiKey));
        config.setEnabled(true);
        systemConfigRepository.save(config);
        log.info("已从环境变量写入全局 AI 配置（system_ai_config）");
    }

    private void bootstrapAdmin() {
        if (adminEmail == null || adminEmail.isBlank()) return;
        String email = adminEmail.trim().toLowerCase(Locale.ROOT);
        Optional<UserAccount> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            UserAccount user = existing.get();
            if (user.getRole() != UserAccount.UserRole.ADMIN) {
                user.setRole(UserAccount.UserRole.ADMIN);
                userRepository.save(user);
                log.info("已将 {} 提升为 ADMIN", email);
            }
            return;
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("ADMIN_BOOTSTRAP_PASSWORD 未配置，跳过创建管理员账号 {}", email);
            return;
        }
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(adminPassword));
        user.setNickname("管理员");
        user.setRole(UserAccount.UserRole.ADMIN);
        user.setStatus(UserAccount.UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("已创建管理员账号 {}", email);
    }
}
