/**
 * <strong>Workflow</strong> — the BPMN workflow-engine core (milestone M0, Issue #5). Wraps Flowable
 * behind a small, domain-shaped port so the rest of the ERP starts and decides approval processes
 * without depending on the engine. The first capability is a generic <em>approval</em> process
 * (request → review → approved/rejected); other modules trigger it for whatever they need signed off.
 * Hexagonal within the module: the domain is pure, Flowable lives only in the infrastructure adapter.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Workflow")
package de.volantic.erp.workflow;
