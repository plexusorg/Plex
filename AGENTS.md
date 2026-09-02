# Plex engineering rules

These rules apply to the whole Plex repository. Apply the same standards when a Plex change also touches a sibling
`Module-*` repository.

## Default approach

- Prefer the shortest direct implementation that clearly satisfies the current requirement.
- Do not change code merely to route it through a new API, helper, manager, service, facade, or scheduler.
- Every new abstraction, compatibility path, fallback, concurrency primitive, and validation layer must solve a
  concrete current problem. "Just in case" is not a requirement.
- Delete obsolete complexity instead of wrapping or preserving it. Plex 2.0 is unreleased; do not add compatibility
  shims for unreleased behavior.
- Before deleting or changing an API, search all callers, tests, and affected sibling modules. Do not assume repository-
  local searches find consumers of a published API.
- Preserve established behavior unless the task explicitly changes it. Do not mix unrelated redesigns into a fix.
- Do not edit generated output, build directories, bundled assets, or dependency code.
- Do not commit, push, deploy, or modify live services unless the user explicitly asks.

## Keep code simple

- Use direct API calls when they are already correct. Do not replace `sender.sendMessage(...)` or
  `player.sendMessage(...)` with a Plex wrapper that adds no behavior.
- A public API exposes a stable capability; it is not mandatory indirection for Plex's own code.
- Do not create one-use helpers unless they name a genuinely non-obvious boundary or remove meaningful duplication.
- Do not introduce an overload matrix without demonstrated callers for each shape.
- Prefer ordinary control flow over streams, nested futures, or callback chains when the ordinary form is clearer.
- Do not use an `AtomicReference`, concurrent collection, snapshot, lock, or `volatile` for state that is local or
  sequential. For shared state, identify the threads that share it before choosing synchronization.
- Avoid manual `CompletableFuture` bridges unless adapting a genuinely callback-only API. Future callbacks may execute
  on the thread that completes them; schedule only the Bukkit operation that actually requires an owner.
- Do not catch `Throwable`. Catch specific recoverable exceptions. Never leave an empty catch block.
- Do not silently turn programming, dependency, renderer, or lifecycle failures into degraded behavior. Surface and log
  the failure so it can be fixed.
- Null-check and validate at trust boundaries such as HTTP, JSON, configuration, files, networks, and databases. Do not
  defensively check values that a non-null typed API contract guarantees.
- Comments should explain a non-obvious contract, invariant, or workaround. Do not narrate the code. Avoid ceremonial
  Javadocs that merely restate a method name.
- Follow the existing Allman brace style and avoid unrelated formatting churn.

## Paper and Folia threading

Do not treat Folia as a reason to schedule every Bukkit call. Verify behavior against the exact resolved Paper/Folia
source or current official documentation rather than memory.

- A player command runs on that player's owning region. A single-entity event runs on that entity's owning region.
  Direct operations on that player, including messaging and ordinary self-inventory changes, are correct there.
- Adventure and `CommandSender` messaging does not need a scheduler wrapper.
- Use an entity scheduler only when code originates outside the entity's owning region (for example an HTTP handler,
  asynchronous database callback, global task, or another entity's region) and needs entity-owned state, or for delayed
  work that must follow the entity.
- Use a region scheduler for location, chunk, block, and other region-owned world state. Do not use it for entity work.
- Use the global-region scheduler only for global-region-owned state and global timers. It is not a generic "main
  thread" and must not wrap messages or ordinary plugin state.
- Console command execution is global-region work. Player command execution is player-region work. Cross-player
  mutations belong on the target player's entity scheduler.
- `AsyncChatEvent` may be asynchronous. Do not iterate `Bukkit.getOnlinePlayers()` from an asynchronous chat callback;
  use the event viewers/renderer or cross an ownership boundary only for the operation that requires it.
- `Bukkit.getOnlinePlayers()` is a live view. Do not iterate it asynchronously. Snapshot it only when a real asynchronous
  handoff requires a snapshot.
- Do not teleport during `PlayerJoinEvent`; delay it or choose the appropriate spawn-location event. Prefer
  `teleportAsync` where Paper requires asynchronous chunk loading.
- In `InventoryClickEvent`, defer operations Paper requires on the next tick, such as opening, closing, or replacing the
  current inventory view. Do not defer unrelated work.
- Any scheduler added "for Folia" must have a specific ownership or event-contract justification. Keep
  `folia-supported: true` honest.

## Plex API and module boundaries

- Design shared Plex capabilities API-first. Define the contract and domain types in `api`, implement that contract in
  `server`, and have server code use the same API instead of maintaining a separate server-only implementation model.
- Do not build a feature around server classes and later add API interfaces, mirror DTOs, adapters, or conversion layers
  that duplicate it. The API contract is the source of truth; the server is one implementation of it.
- Put a capability in the API when Plex modules or other consumers reasonably need it. Keep genuinely private
  implementation details in `server`; API-first does not mean exposing every helper or internal class.
- Keep the public API small and capability-oriented. Do not expose implementation plumbing for convenience.
- `PlexModule.scheduler()` exists for ownership crossings, delayed work, asynchronous work, and module task lifecycle.
  It is not the default route for every operation.
- Do not add `module.respond(...)`, `module.send(...)`, or similar wrappers around a one-line platform call.
- When changing the Plex API, search every sibling Plex module for the old contract and build all affected modules
  against the current local Plex checkout.
- Missing required plugins should fail module enablement with a clear exception. Let the module lifecycle log the error
  and disable/remove the module; do not log once and continue half-enabled.
- Use the module logger for module failures and include the throwable. Use Plex's logging facilities for core lifecycle
  failures. Do not use `printStackTrace`, `System.out`, or log only `exception.getMessage()` when the stack trace matters.

## Punishments and storage

- Migration `001` is the schema source of truth for the unreleased Plex 2.0 line. Update it directly rather than
  manufacturing compatibility migrations for unreleased schemas.
- Core and module migration runners discover ordered migration resources from the active dialect directory. Module code
  must not construct or pass a list of migration filenames.
- Do not reintroduce `customTime`. Punishment duration rules belong to `PunishmentType` and the punishment API.
- A standard `BAN` lasts exactly 24 hours. Use `PunishmentType.STANDARD_BAN_DURATION`/the fixed-duration contract rather
  than duplicating a literal or accepting a caller-supplied ban duration.
- Punishment effectiveness is not equivalent to `endDate > now`. Respect the authoritative active state and bypass/admin
  rules exposed by the API. Commands and HTTP clients must not reconstruct different activity rules.
- Keep SQL rows, repositories, API DTOs, commands, and HTTP clients aligned whenever punishment fields or semantics
  change. Search all of them as one change.
- Validate migrations against every supported dialect. Do not make live database changes as part of source work.

## HTTP modules

- Jetty servlet threads and executor threads are not Bukkit entity or region threads. Validate request input and bound
  blocking work with an appropriate timeout.
- Parse requests and construct responses on the HTTP thread. Schedule only the platform state access or mutation that
  requires Bukkit ownership.
- Concurrency protection is justified for state genuinely shared by Jetty, executor, scheduler, and region threads.
  Document that boundary; do not spread concurrent containers everywhere else.
- Frontend and backend request contracts must be changed together. Check parameter names, nullability, response fields,
  and error status handling end to end.
- Renderer/import/startup failures must be visible and logged. Do not silently fall back to plain text for a broken
  renderer and call that success.
- Target the project's resolved Paper version directly. Do not add reflection-based version compatibility when the
  compile-time API already provides the required value.

## Live-state safety gate

Documentation is historical context, not authority for mutable external state. For deployments, databases, IAM/RBAC,
roles, grants, ownership, migrations, secrets, infrastructure, and production services:

1. Inspect the relevant live state read-only before making changes.
2. Compare live state with repository documentation and configuration.
3. If live state and documentation disagree, stop immediately.
4. Report the exact discrepancy and ask the user to clarify.
5. Never reconcile live state to documentation without explicit approval.
6. A missing documented role, resource, permission, schema, or prerequisite is evidence that documentation may be stale;
   it is not authorization to recreate it.
7. A failed operation does not authorize repairing permissions, provisioning prerequisites, changing ownership, or
   altering infrastructure.
8. Authorization to deploy or rerun a pipeline applies only to that named deployment or pipeline.
9. Never create, alter, drop, grant, revoke, or reassign a database role or privilege unless the user explicitly requests
   that exact class of change in the current turn.
10. If a permission change appears necessary, show the environment, current live state, proposed exact changes, expected
    impact, and rollback plan, then wait for explicit approval.

## Verification

- Establish what passes before a broad cleanup. Do not attribute pre-existing failures to the current edit.
- Build Plex from the repository root with `./gradlew build` (`.\gradlew.bat build --console=plain` on Windows).
- Build an affected sibling module against the local Plex checkout with
  `.\gradlew.bat build --include-build C:\Users\telesphoreo\IdeaProjects\Plex --console=plain`.
- For the Module-HTTPD frontend, run `bun run check` in `Module-HTTPD/src/main/frontend`.
- Run `git diff --check` and inspect the final diff. Every changed line must trace to the requested fix or a verified
  cleanup required by it.
- Report what was changed, what was deliberately left alone, and the exact verification run. Never claim success from
  compilation alone when behavior needs a focused test.
