# Plex engineering rules

These rules apply to Plex and to any sibling `Module-*` repository affected by a Plex change.

## Decision order

When concerns compete, decide in this order:

1. Preserve explicitly intended behavior and product policy.
2. Be correct at real trust, persistence, lifecycle, and thread-ownership boundaries.
3. Keep demonstrated hot paths efficient and resistant to resource amplification.
4. Put behavior beside the state or resource that owns it.
5. Within those constraints, use the fewest concepts, hops, states, and mechanisms.

Simplicity means the shortest interaction chain that satisfies the contract. It does not mean deleting useful caching,
blocking a region thread, or ignoring failures. Correctness does not mean speculative guards, wrappers, schedulers,
retries, or repeated validation.

If materially different choices affect behavior, permissions, compatibility, persistence, performance, or operational
output, show the evidence and ask the user which contract is intended before editing.

### Example: efficient ban admission

Bad:

```text
join -> permission check -> adapter -> manager -> another permission check
     -> database -> date check -> another activity check -> kick wrapper
```

Better:

```text
pre-login -> admission owner -> maintained in-memory UUID/IP decision -> allow or deny
```

Repeated attempts must not repeatedly query storage. If decisions are loaded on demand, negative results or in-flight
loads must be cached/coalesced too. The admission owner evaluates effectiveness and bypass policy once; callers consume
that answer.

## Investigate before changing

For a bug, trace the failing path and state the supported root cause before editing. Read the failing line, callers,
state owner, final side effect, and relevant resolved dependency or Paper source. For dependency failures, verify the
installed/enabled plugin and version, resolved artifact/package, generated descriptor, load order, and classpath before
changing code. Missing required dependencies are not permission to use reflection or fallbacks.

For organizational work, map the complete interaction chain first. Every hop must:

- own state or resource lifetime;
- enforce domain policy;
- transform a real boundary representation; or
- cross a real execution/ownership boundary.

A hop that only forwards, renames, reschedules, or relocates an operation is a removal candidate. When the user points
to a simpler intended design, stop extending the current approach and reconsider it from that design.

Preserve intentional contracts, not every accident in the implementation. If current paths disagree—such as cached and
database name lookup using different case rules—report the conflict and ask which behavior is intended.

Before changing a public API, search Plex, all sibling modules, and known external consumers. Plex 2.0 is unreleased;
delete obsolete unreleased behavior instead of adding compatibility shims.

`apiCompatibility` remains `1` throughout unreleased Plex 2.0 development. Breaking an in-development module API and
updating every sibling module does not justify incrementing it; change the compatibility number only for an actual
released API generation.

### Example: the gamerule regression

Changing an `if / else if / else` dispatch into independent `if` statements appeared simpler, but the Boolean branch
fell through to the unknown-type error. Compilation and a lower-looking branch structure did not preserve the complete
operation. Trace every outcome before rewriting control flow.

## Organize around ownership

Prefer:

```text
entry point -> actual behavior owner -> required external boundary
```

Avoid contexts, facades, `Default*Api` adapters, managers, and services unless every link has a distinct job.

- Context objects carry invocation state; they are not messaging, lookup, parsing, or scheduling facades.
- Let the actual owner implement a public capability when practical.
- Keep an adapter when it provides isolation, redaction, immutability, or real representation conversion.
- Core code may call its internal owner directly when it needs internal types. It must share public semantics, not the
  public facade's call path.
- A lifecycle object is justified when it owns resources created and released together.
- Prefer one readable sequential method over one decision tree scattered through one-use helpers.
- Extract only a cohesive responsibility that would make sense without a metric.

Ask whether an organizational change shortened the interaction chain or merely moved it.

### Example: API ownership

Audit a chain such as:

```text
ModulesApi -> DefaultModulesApi -> ModuleManager -> module state
```

If the manager owns the state and can return the public projection without exposing mutable internals, it may implement
the interface directly. If the adapter enforces a real boundary, retain it. The example is a question to prove, not a
mandate to create or remove a class.

### Example: module lifetime

Classloader, task scope, registrations, descriptor, and data directory genuinely share a loaded module's lifetime. One
internal owner for those resources is justified. A separate manager for each cleanup step is not.

## Directness, validation, and failures

Use a platform call directly when it is already correct. Do not wrap ordinary messaging, permission checks, logging,
configuration reads, or player-local operations merely for consistency.

Bad:

```text
command -> context.send -> scheduler -> module.respond -> player.sendMessage
```

Better:

```java
player.sendMessage(component);
```

Adventure messaging needs no scheduler. If recipient discovery is unsafe, fix discovery rather than inventing scheduled
message delivery.

Validate untrusted HTTP, JSON, configuration, file, network, and database input once at ingress. After conversion to a
valid internal value, rely on that invariant. Revalidate only at an independent trust boundary.

Expected optional absence may disable only that integration, with one clear warning and availability exposed where
needed. Broken required dependencies, malformed internal state, renderer failures, and lifecycle failures stay visible
and fail the affected component rather than leaving it half-enabled. A missing required plugin fails module enablement;
let the lifecycle disable/remove the module rather than continuing half-enabled. Use the module logger for module
failures and Plex logging for core lifecycle failures; include the throwable when its stack matters. Do not use
reflection to hide a declared dependency, catch `Throwable`, leave empty catches, use `printStackTrace`/`System.out`, or
log only a message when the stack matters.

Use primitive Boolean literals. Comments explain non-obvious contracts or workarounds, not code narration. Avoid
ceremonial Javadocs, preserve Allman braces, and avoid unrelated formatting churn.

Always import referenced types normally. Never use fully qualified class names inline in fields, signatures,
annotations, generic arguments, method bodies, or constructor calls as a shortcut around adding an import. Resolve
simple-name collisions deliberately with the narrowest readable exception rather than scattering qualified names
through the implementation.

## Performance-aware state

Efficiency is part of correctness on hot paths. Caches, indexes, batching, coalesced futures, and bounded concurrency are
warranted when they prevent demonstrated blocking, repeated I/O, duplicate work, or resource amplification. Keep the
mechanism beside the operation it accelerates.

For each cache or concurrency mechanism, identify what applies: the cost avoided; owner and sharing threads/processes;
authoritative source; key/cardinality; population and invalidation; expiry/bounds only where staleness or growth requires
them; and failure behavior. Do not add expiry, locks, retries, snapshots, or more cache layers merely to complete a
checklist. Natural bounds and mutation-driven invalidation can be sufficient.

### Example: punishment state

The relevant durable or configured source remains authoritative, but repeated joins should use an in-memory admission
view. Successful mutations update or invalidate affected UUID/IP decisions. Redis may propagate cross-server
invalidation but must not implement separate ban semantics or automatically make local punishment depend on Redis.

### Example: warranted HTTP complexity

Module-HTTPD inventory streaming has real subscription/backpressure state, entity-owned inventory capture, and a writer
executor. Organize those responsibilities by owner; do not replace them with one synchronous loop or split every branch
into a manager.

## Paper and Folia ownership

Identify the current execution context and accessed state. Schedule only the smallest operation whose owner differs.

- Player commands run on that player's region; console commands are global-region work.
- Many synchronous single-entity events run on the entity owner, but verify the exact event contract.
- Messaging is not an entity mutation and needs no scheduler.
- Cross-player mutations use the target player's entity scheduler.
- Location, chunk, and block state uses the relevant region scheduler.
- Global scheduling does not grant ownership of players or live collections.
- Never iterate the live `Bukkit.getOnlinePlayers()` view asynchronously.
- `AsyncChatEvent` may be asynchronous; use its viewers/renderer or cross only the required boundary.
- Do not teleport during `PlayerJoinEvent`; use the proper spawn event or delayed/async teleport contract.
- Defer inventory-view replacement from `InventoryClickEvent` when Paper requires the next tick.

Verify uncertainty against the resolved Paper/Folia source or current official documentation. Keep
`folia-supported: true` honest.

Use Paper's schedulers directly; Plex must not mirror them in its API. The scheduler is selected by the state being
accessed, not by a general desire to be "thread safe":

Do not reflexively schedule code. A scheduler hop is justified only by a proven execution-ownership crossing, an
explicit delay/repetition requirement, or blocking work that must leave a region thread. Before adding one, identify
the current thread/context, the exact state owner, and the smallest operation that must cross the boundary. If those
cannot be named, do not schedule it. Never wrap an entire command, listener, callback, message, log statement,
permission check, configuration read, immutable computation, or Redis debug output in a scheduler merely because a
scheduler exists. Remove redundant hops rather than moving them to a different scheduler API.

Authoritative references: [Paper Folia support](https://docs.papermc.io/paper/dev/folia-support/),
[EntityScheduler](https://jd.papermc.io/paper/io/papermc/paper/threadedregions/scheduler/EntityScheduler.html),
[RegionScheduler](https://jd.papermc.io/paper/io/papermc/paper/threadedregions/scheduler/RegionScheduler.html),
[GlobalRegionScheduler](https://jd.papermc.io/paper/io/papermc/paper/threadedregions/scheduler/GlobalRegionScheduler.html),
and [AsyncScheduler](https://jd.papermc.io/paper/io/papermc/paper/threadedregions/scheduler/AsyncScheduler.html).

```java
// Entity-owned mutation from another region or an async callback.
player.getScheduler().run(plugin, task -> player.getInventory().clear(), retiredCallback);

// Block/chunk-owned work.
Bukkit.getRegionScheduler().run(plugin, location, task -> location.getBlock().setType(material));

// Truly global Bukkit state, such as world-list or game-rule orchestration.
Bukkit.getGlobalRegionScheduler().run(plugin, task -> reloadGlobalState());

// Blocking work which touches no Bukkit state belongs to that component's executor.
CompletableFuture.supplyAsync(repository::load, repositoryExecutor);
```

Paper's entity scheduler follows a moving entity and runs on its owning region. Supply a retired callback when the
caller needs completion or cleanup if the entity disappears; also handle a `null` task result. Region scheduling owns
the supplied location or chunk, not arbitrary entities in that region. The global-region scheduler owns only global
state and is not a substitute for the old main thread. The async scheduler grants no Bukkit ownership and must not be
used as Plex's general I/O executor. A component that owns database, network, filesystem, or other blocking work also
owns (or is passed) the executor for that work. Paper's async scheduler remains appropriate for a Paper-owned delayed
or repeating timer that does not access region state; its callback should hand blocking work to the component owner.

Modules call the same native Paper APIs with `module.plugin()` as the task owner and pass every retained or delayed task
to `module.ownTask(...)` so module unload cancels it. `ownTask` is lifecycle resource registration, not a scheduling
facade. Do not schedule a message, permission check, immutable transformation, configuration read, logging call, or
already-owned operation merely so the module can call `ownTask`.

### Example: cross-player game mode

A self game-mode change is direct. For another player, schedule only the target-owned mutation. Messaging itself remains
direct, but success output must follow the intended completion/failure semantics, including a retired entity. Do not put
the entire command or its messages inside the target scheduler.

### Example: HTTP inventory mutation

A Jetty request parses and validates on its HTTP thread, then schedules only the `PlayerInventory` mutation on the
target entity. Response construction and logging remain HTTP work.

## API, async, and domain boundaries

Design public capabilities from stable behavior modules need, not current server layout. Keep one implementation and one
set of semantics. A public interface does not require a pass-through implementation; a public view that freezes or
redacts internal state is a real boundary. Do not create mirror DTOs or overloads without demonstrated consumers.

Removing a Paper mirror means removing a public subsystem facade that shadows Paper's own model, such as a scheduler or
listener-registration API. It does not mean deleting focused Plex conveniences that add product semantics, such as
configured MiniMessage rendering, recipient policy, or domain-specific broadcast behavior.

Build every affected sibling module against the local Plex checkout after an API change.

Blocking database/network work must not run on a player or region thread. Put the async boundary at the component that
owns the blocking operation; do not offer competing sync/async versions for callers to choose incorrectly. Future
callbacks run on their completing thread. Avoid redundant executor hops, nested completion chains, and manual future
bridges. Schedule only the resulting Bukkit operation that needs an owner.

For a punishment mutation, preserve the applicable ordering:

```text
validate once -> persist -> update/invalidate local state -> platform effect -> report success
```

Do not broadcast, kick, or report success before required persistence succeeds. Best-effort Redis publication does not
gate local effects unless the chosen cross-server contract explicitly says it must.

Use one authoritative admission decision for SQL punishment effectiveness. Do not reconstruct it in commands, HTTP,
listeners, and repositories. Current configured indefinite-ban checks and SQL admission have different bypass behavior;
do not merge or change that policy without an explicit user decision.

Stable contracts:

- A standard `BAN` lasts exactly 24 hours through `PunishmentType.STANDARD_BAN_DURATION`.
- Do not reintroduce `customTime`.
- Migration `001` is the schema source of truth for unreleased Plex 2.0; update it directly instead of adding
  compatibility migrations for unreleased schema changes.
- Migration runners discover ordered resources from the active dialect directory.
- Keep SQL rows, repositories, API types, commands, caches, and HTTP clients aligned with punishment semantics.
- Validate source migrations for every supported dialect; never alter a live database during source work.

For HTTP modules, bound blocking work with a timeout, change frontend/backend contracts together, and document state
shared among Jetty, executors, schedulers, and regions. Target the resolved Paper API instead of reflective compatibility.

## Complexity and cleanup

Checkstyle enforces cyclomatic complexity no greater than 15 in Plex and every module. It is a build guard, not a design
target or proof of equivalence. Plex's Checkstyle configuration counts a `switch` block as one dispatch decision;
decisions inside its cases still count. Use a switch only for genuine dispatch on one value; do not encode unrelated
conditions or state into a switch to lower the score. Review an entry point and its one-use callees as one operation.

Do not pass the metric by moving branches into helpers, adding facades, replacing direct control flow with streams or
callbacks, repackaging distinct fixed operations into a one-use collection/loop/compound expression/giant string,
deleting necessary performance state, or changing behavior. Normal loops remain appropriate for genuinely homogeneous
runtime collections. First look for behavior proven obsolete by caller/contract tracing, duplicate parsing, repeated
policy checks, and unnecessary states or branches. Extraction is justified only when the extracted code has a cohesive
responsibility, removes meaningful duplication, or owns policy, state, lifecycle, a boundary, or an algorithm
independently of the metric.

Readable sequential orchestration is not high decision complexity. In `PlexCMD`, ten explicit `service.reload()` calls
belong together when they are fixed, intentionally ordered command behavior and no authoritative service collection
already exists. Do not hide them in a one-use `reloadServices()` helper, a collection loop, or a fluent/string
construction merely to change a metric. The same applies to straightforward version and plugin information output: keep
the operation visible unless a real reusable owner exists.

If cohesive code still exceeds the limit and splitting it would damage readability or encapsulation, a narrow
`@SuppressWarnings("checkstyle:CyclomaticComplexity")` on that method is more honest than fake extraction. Before adding
one, record why the decisions are inherent, inspect the method and its one-use callees as one operation, and have an
independent reviewer inspect the complete method and suppression rationale. Never use `all`, file-wide suppression, or
class/type/package-level complexity suppression; suppress only the individual method or constructor. Do not suppress to
avoid removing accidental complexity. Ask: what decisions were removed, what hops/types/helpers were added, and would
each extraction still exist if Checkstyle were disabled?

Example: a typed Brigadier tree followed by reconstructed `String[]` and a second parser contains two representations.
Moving the second parser into helpers does not simplify it; one authoritative parse should pass typed values to behavior.

Do not combine feature work, policy changes, and structural cleanup. Correctness issues found during an organizational
audit should be separate, attributable changes.

## Review and verification

Keep changes narrow. Do not edit generated output, build directories, bundled assets, or dependencies. Do not commit,
push, deploy, or modify live services unless explicitly requested.

Before behavior-affecting organizational, async, persistence, or ownership work, record relevant inputs, identity,
policy decisions, output, logging, ordering, side effects, and thread ownership. A small fix needs only its affected path.
Do not add tests unless the user explicitly requests them; run existing relevant tests where present.

For non-small changes:

1. Delegate independent scopes when useful, with one integration owner.
2. Have independent reviewers inspect the complete diff for regressions, unnecessary hops, metric gaming, lost
   performance, API mistakes, and ownership errors.
3. Validate findings against callers and contracts; reject speculative changes.
4. After justified fixes, perform one fresh complete-diff review. Repeat only after material behavior/architecture edits.
5. Run required builds, practical runtime smoke checks, `git diff --check`, and final diff inspection.

Establish a baseline before broad cleanup. Every changed line must trace to the request or required cleanup. Report what
changed, what remained, and exact verification commands/results. Compilation, Checkstyle, lower LOC, fewer methods, or
reviewer approval alone do not prove equivalence.

Verification commands:

- Plex: `.\gradlew.bat build --console=plain`
- Module: `.\gradlew.bat build --include-build C:\Users\telesphoreo\IdeaProjects\Plex --console=plain`
- Module-HTTPD frontend: `bun run check` in `Module-HTTPD/src/main/frontend`

## Live-state safety

Documentation is not authority for mutable deployments, databases, roles/grants, ownership, secrets, infrastructure, or
production services. Before an authorized operational change, inspect live state read-only and compare it with repository
assumptions. If they disagree, stop and report the exact discrepancy. Never reconcile live state to documentation
without explicit approval.

A missing documented resource is evidence documentation may be stale, not permission to recreate it. A failed operation
does not authorize provisioning or repairs. Deployment/pipeline authorization does not authorize incidental IAM, role,
grant, schema, secret, ownership, or infrastructure changes. Never create, alter, drop, grant, revoke, or reassign a
database role or privilege unless that exact class of change was explicitly requested in the current turn. If permission
changes appear necessary, show the environment, current state, exact changes, impact, and rollback, then wait for
approval.
