package io.github.azaev.questjournal.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Loud warnings for settings that are fine on a laptop but dangerous on a public server. */
@Component
class StartupChecks {

    private static final Logger log = LoggerFactory.getLogger(StartupChecks.class);

    private final AppProperties props;

    StartupChecks(AppProperties props) {
        this.props = props;
    }

    @EventListener(ApplicationReadyEvent.class)
    void warnAboutUnsafeDefaults() {
        if (props.jwt().secret().startsWith("dev-only-")) {
            log.warn("JWT_SECRET is not set: using the dev-only key. Anyone can forge tokens. Set JWT_SECRET in production!");
        }
        if (!props.registration().inviteRequired()) {
            log.warn("INVITE_CODE is not set: registration is open to anyone who can reach this server.");
        }
    }
}
