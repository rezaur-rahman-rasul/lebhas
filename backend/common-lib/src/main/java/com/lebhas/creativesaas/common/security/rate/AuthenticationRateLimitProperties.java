package com.lebhas.creativesaas.common.security.rate;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "platform.security.rate-limit")
public class AuthenticationRateLimitProperties {

    private final Login login = new Login();
    private final Refresh refresh = new Refresh();

    public Login getLogin() {
        return login;
    }

    public Refresh getRefresh() {
        return refresh;
    }

    public static class Login {
        private int maxAttemptsPerIp = 20;
        private int maxAttemptsPerIdentity = 8;
        private int lockoutThreshold = 5;
        private Duration window = Duration.ofMinutes(15);
        private Duration lockoutDuration = Duration.ofMinutes(15);

        public int getMaxAttemptsPerIp() {
            return maxAttemptsPerIp;
        }

        public void setMaxAttemptsPerIp(int maxAttemptsPerIp) {
            this.maxAttemptsPerIp = maxAttemptsPerIp;
        }

        public int getMaxAttemptsPerIdentity() {
            return maxAttemptsPerIdentity;
        }

        public void setMaxAttemptsPerIdentity(int maxAttemptsPerIdentity) {
            this.maxAttemptsPerIdentity = maxAttemptsPerIdentity;
        }

        public int getLockoutThreshold() {
            return lockoutThreshold;
        }

        public void setLockoutThreshold(int lockoutThreshold) {
            this.lockoutThreshold = lockoutThreshold;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }

        public Duration getLockoutDuration() {
            return lockoutDuration;
        }

        public void setLockoutDuration(Duration lockoutDuration) {
            this.lockoutDuration = lockoutDuration;
        }
    }

    public static class Refresh {
        private int maxAttemptsPerIp = 40;
        private int maxAttemptsPerToken = 6;
        private Duration window = Duration.ofMinutes(15);

        public int getMaxAttemptsPerIp() {
            return maxAttemptsPerIp;
        }

        public void setMaxAttemptsPerIp(int maxAttemptsPerIp) {
            this.maxAttemptsPerIp = maxAttemptsPerIp;
        }

        public int getMaxAttemptsPerToken() {
            return maxAttemptsPerToken;
        }

        public void setMaxAttemptsPerToken(int maxAttemptsPerToken) {
            this.maxAttemptsPerToken = maxAttemptsPerToken;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }
    }
}
