package de.volantic.erp.security.api;

import de.volantic.erp.security.domain.model.OrgUnit;

/** Response body representing an org unit (REST v1). No JPA entity ever leaves the api layer. */
public record OrgUnitResponse(String id, long version, String parentId, String code, String name) {

    static OrgUnitResponse from(OrgUnit orgUnit) {
        return new OrgUnitResponse(
                orgUnit.id().value().toString(),
                orgUnit.version() == null ? 0L : orgUnit.version(),
                orgUnit.parentId() == null ? null : orgUnit.parentId().value().toString(),
                orgUnit.code(),
                orgUnit.name());
    }
}
