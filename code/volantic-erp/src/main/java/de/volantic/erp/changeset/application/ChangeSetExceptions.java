package de.volantic.erp.changeset.application;

import de.volantic.erp.changeset.domain.model.ChangeSetId;

/** Application-level exceptions for the change-set use cases (mapped to RFC-7807 problems in the api layer). */
public final class ChangeSetExceptions {

    private ChangeSetExceptions() {
    }

    /** No session exists for the given id (→ 404). */
    public static final class ChangeSetNotFoundException extends RuntimeException {
        public ChangeSetNotFoundException(ChangeSetId id) {
            super("change set not found: " + id.value());
        }
    }

    /** The current actor is not the owner of the session they tried to operate on (→ 403). */
    public static final class ChangeSetAccessDeniedException extends RuntimeException {
        public ChangeSetAccessDeniedException(ChangeSetId id) {
            super("change set is owned by another actor: " + id.value());
        }
    }

    /** No handler is registered for the requested resource type (→ 400/422). */
    public static final class UnknownResourceTypeException extends RuntimeException {
        public UnknownResourceTypeException(String resourceType) {
            super("no handler registered for resource type: " + resourceType);
        }
    }

    /** A requested field is not editable in bulk for the resource (→ 422). */
    public static final class FieldNotEditableException extends RuntimeException {
        public FieldNotEditableException(String resourceType, String field) {
            super("field '" + field + "' is not editable in bulk for " + resourceType);
        }
    }

    /** A requested filter field cannot be used to select resources of this type (→ 422). */
    public static final class FieldNotFilterableException extends RuntimeException {
        public FieldNotFilterableException(String resourceType, String field) {
            super("field '" + field + "' cannot be used as a selection filter for " + resourceType);
        }
    }
}
