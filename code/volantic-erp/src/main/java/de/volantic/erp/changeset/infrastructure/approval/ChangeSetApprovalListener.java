package de.volantic.erp.changeset.infrastructure.approval;

import de.volantic.erp.changeset.application.ChangeSetService;
import de.volantic.erp.changeset.application.port.out.ChangeSetStore;
import de.volantic.erp.changeset.domain.model.ChangeSet;
import de.volantic.erp.changeset.domain.model.ChangeSetId;
import de.volantic.erp.workflow.ApprovalDecided;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Applies the four-eyes approval outcome to a change-set session (ADR-0006 §7). Listens for the workflow
 * {@link ApprovalDecided} event; when it concerns a {@code changeset.session}, it approves (apply + commit)
 * or rejects (discard) the session.
 *
 * <p>{@link ApplicationModuleListener}: the publication is durable (Modulith outbox) and runs after the
 * decision commits, in its own transaction — so the transfer is not lost if delivery is retried.
 *
 * <p>The handler runs <strong>as the original requester</strong>: the buffered writes go through the
 * resource modules' {@code @PreAuthorize} domain services and must authorize against someone, and the
 * requester (not the async thread, which has no security context) is the actor whose permissions apply.
 * The reviewer's approval is the additional four-eyes authorization for performing the transfer at all.
 */
@Component
class ChangeSetApprovalListener {

    private final ChangeSetService changeSets;
    private final ChangeSetStore store;

    ChangeSetApprovalListener(ChangeSetService changeSets, ChangeSetStore store) {
        this.changeSets = changeSets;
        this.store = store;
    }

    @ApplicationModuleListener
    void on(ApprovalDecided event) {
        if (!ChangeSetService.APPROVAL_SUBJECT_TYPE.equals(event.subjectType())) {
            return;
        }
        ChangeSetId session = new ChangeSetId(UUID.fromString(event.subjectId()));
        ChangeSet changeSet = store.findById(session).orElse(null);
        if (changeSet == null) {
            return; // unknown session — nothing to do
        }
        runAs(changeSet.actor(), () -> changeSets.applyApprovalOutcome(session, event.approved()));
    }

    /**
     * Runs the action under a fresh security context authenticated as the given OIDC subject, then
     * restores the original context. A new context object is installed (not the shared one mutated), so
     * the impersonation cannot leak into another thread under any {@code SecurityContextHolder} strategy.
     */
    private static void runAs(String oidcSubject, Runnable action) {
        SecurityContext original = SecurityContextHolder.getContext();
        try {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(
                    new UsernamePasswordAuthenticationToken(oidcSubject, null, AuthorityUtils.NO_AUTHORITIES));
            SecurityContextHolder.setContext(context);
            action.run();
        } finally {
            SecurityContextHolder.setContext(original);
        }
    }
}
