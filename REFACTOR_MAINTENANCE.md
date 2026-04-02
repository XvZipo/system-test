# Refactor maintenance guide (Phases 1–6)

This repository was refactored in phases to upgrade the build toolchain and remove repeated test
client setup code (HTTP and gRPC) without changing test logic.

## Build requirements

- **Run Gradle with JDK 11+ (recommended: JDK 17).**
  - JDK 8 is not supported by the current Gradle/plugins stack (e.g., Sonar Gradle plugin requires
    Java 11+).

## What changed in tests

### gRPC base classes (TestNG)

Located in `testcase/src/test/java/stest/tron/wallet/common/client/`:

- `AbstractGrpcFullSolidityTest`
  - Full node: `fullnode.ip.list[0]`
  - Solidity node: `solidityNode.ip.list[0]`
  - Exposes `blockingStubFull`, `blockingStubSolidity` and channels.

- `AbstractGrpcDualFullAndSolidityTest`
  - Full nodes: `fullnode.ip.list[0]` and `[1]`
  - Solidity node: `solidityNode.ip.list[0]`

- `AbstractGrpcFullSolidityExtensionTest` / `AbstractGrpcDualFullAndSolidityExtensionTest`
  - Adds `WalletExtensionGrpc.WalletExtensionBlockingStub` initialization on top of the bases above.

### HTTP endpoint base classes

Located in `testcase/src/test/java/stest/tron/wallet/common/client/`:

- Node-only:
  - `AbstractHttpNode0` → `httpnode.ip.list[0]`
  - `AbstractHttpNode1` → `httpnode.ip.list[1]`
  - `AbstractHttpNodes01` → indices `[0]` and `[1]`

- Common triples:
  - `AbstractHttpEndpoints024` → indices `[0, 2, 4]` with `httpSoliditynode`
  - `AbstractHttpEndpoints124` → indices `[1, 2, 4]` with `httpSoliditynode`
  - `AbstractHttpEndpointsZen024` / `AbstractHttpEndpointsZen124` → same indices but uses
    `httpSolidityNode` naming for Zen/shield-style tests

- Specialized:
  - `AbstractHttpSmartContractNodes023` → indices `[0, 2, 3]`
  - `AbstractHttpEndpoints0243` → indices `[0, 2, 3, 4]` (includes fullnode solidity port)
  - `AbstractHttpEstimateEnergyNodes156` → indices `[1, 5, 6]`

### Shared config access helpers

- `HttpNodeList` provides cached access to `httpnode.ip.list` from `testng.conf`.
- `GrpcNodeList` provides cached access to `fullnode.ip.list` and `solidityNode.ip.list` from
  `testng.conf`.

These helpers are intended to keep base classes small and to avoid repeating long config lookup
expressions.

## Repository hygiene rules

- `docs/` is treated as a local-only directory and is ignored by Git.
- The legacy ignore rule for the output directory is **`/Wallet/`** (anchored). Avoid adding broad
  patterns like `Wallet` that would accidentally ignore Java package paths containing `wallet`.

