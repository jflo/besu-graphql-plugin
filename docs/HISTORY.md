# Source history

This repository was extracted from [besu-eth/besu](https://github.com/besu-eth/besu) with
`git filter-repo`, from branch `graphql-plugin-module` at commit
[`df4f0c6b1d`](https://github.com/besu-eth/besu/commit/df4f0c6b1dbae1fbb48feba6c0278b64ac8c8f2a)
(PR besu-eth/besu#11745). The imported history contains every Besu commit that touched:

- the in-tree plugin module `plugins/graphql/`, and
- the legacy core GraphQL sources it was derived from, including their earlier Pantheon locations
  (`ethereum/graphqlrpc/`, `ethereum/graphql/`, `ethereum/api/.../tech/pegasys/pantheon/...`,
  `ethereum/api/.../org/hyperledger/besu/ethereum/api/graphql/`, `ethereum/api/src/main/resources/schema.graphqls`,
  and `GraphQlOptions` in `besu/` and `app/`).

That is 191 commits, going back to the original EIP-1767 implementation in 2019. Each imported
commit message ends with an `Imported-From: besu-eth/besu@<sha>` trailer that names the
original commit. Bare PR references were qualified, as `besu-eth/besu#N`, or as
`PegaSysEng/pantheon#N` for commits made before 2019-09-16, so that they do not link to this
repository's own pull requests.

After the import, the plugin module was moved to the repository root (a pure rename) and the
legacy core files were deleted from the working tree. They are still in history.

## Following a file back into Besu core

The plugin's files were created by the import commit as new files, alongside the legacy core
files they were adapted from. Git therefore does not record them as renames, and
`git log --follow` stops at that commit. To go further back:

```sh
# history of the legacy counterpart (paths in the table below)
git log -- ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/GraphQLDataFetchers.java
git log --follow -- ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/GraphQLDataFetchers.java

# line-level blame across the copy into the plugin package
git blame -C -C -C src/main/java/org/hyperledger/besu/plugins/graphql/GraphQLDataFetchers.java
```

## Legacy file to plugin file

| Plugin file (`src/...`) | Legacy Besu core file |
| --- | --- |
| `src/main/java/org/hyperledger/besu/plugins/graphql/GraphQlOptions.java` | `app/src/main/java/org/hyperledger/besu/cli/options/GraphQlOptions.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/GraphQlOptionsTest.java` | `app/src/test/java/org/hyperledger/besu/cli/options/GraphQlOptionsTest.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/GraphQLConfiguration.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/GraphQLConfiguration.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/GraphQLContextType.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/GraphQLContextType.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/GraphQLDataFetchers.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/GraphQLDataFetchers.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/GraphQLException.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/GraphQLException.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/GraphQLHttpService.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/GraphQLHttpService.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/GraphQLProvider.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/GraphQLProvider.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/GraphQLServiceException.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/GraphQLServiceException.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/internal/Scalars.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/internal/Scalars.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/internal/pojoadapter/AccessListEntryAdapter.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/internal/pojoadapter/AccessListEntryAdapter.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/internal/pojoadapter/AccountAdapter.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/internal/pojoadapter/AccountAdapter.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/internal/pojoadapter/AdapterBase.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/internal/pojoadapter/AdapterBase.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/internal/pojoadapter/BlockAdapterBase.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/internal/pojoadapter/BlockAdapterBase.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/internal/pojoadapter/CallResult.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/internal/pojoadapter/CallResult.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/internal/pojoadapter/EmptyAccountAdapter.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/internal/pojoadapter/EmptyAccountAdapter.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/internal/pojoadapter/LogAdapter.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/internal/pojoadapter/LogAdapter.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/internal/pojoadapter/NormalBlockAdapter.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/internal/pojoadapter/NormalBlockAdapter.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/internal/pojoadapter/PendingStateAdapter.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/internal/pojoadapter/PendingStateAdapter.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/internal/pojoadapter/SyncStateAdapter.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/internal/pojoadapter/SyncStateAdapter.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/internal/pojoadapter/TransactionAdapter.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/internal/pojoadapter/TransactionAdapter.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/internal/pojoadapter/UncleBlockAdapter.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/internal/pojoadapter/UncleBlockAdapter.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/internal/pojoadapter/WithdrawalAdapter.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/internal/pojoadapter/WithdrawalAdapter.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/internal/response/GraphQLError.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/internal/response/GraphQLError.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/internal/response/GraphQLErrorResponse.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/internal/response/GraphQLErrorResponse.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/internal/response/GraphQLJsonRequest.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/internal/response/GraphQLJsonRequest.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/internal/response/GraphQLNoResponse.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/internal/response/GraphQLNoResponse.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/internal/response/GraphQLResponse.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/internal/response/GraphQLResponse.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/internal/response/GraphQLResponseType.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/internal/response/GraphQLResponseType.java` |
| `src/main/java/org/hyperledger/besu/plugins/graphql/internal/response/GraphQLSuccessResponse.java` | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/internal/response/GraphQLSuccessResponse.java` |
| `src/main/resources/org/hyperledger/besu/plugins/graphql/schema.graphqls` | `ethereum/api/src/main/resources/schema.graphqls` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/AbstractDataFetcherTest.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/AbstractDataFetcherTest.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/AbstractEthGraphQLHttpServiceTest.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/AbstractEthGraphQLHttpServiceTest.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/BlockDataFetcherTest.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/BlockDataFetcherTest.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/EthGraphQLHttpBySpecTest.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/EthGraphQLHttpBySpecTest.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/GraphQLConfigurationTest.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/GraphQLConfigurationTest.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/GraphQLHttpServiceCorsTest.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/GraphQLHttpServiceCorsTest.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/GraphQLHttpServiceHostWhitelistTest.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/GraphQLHttpServiceHostWhitelistTest.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/GraphQLHttpServiceTest.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/GraphQLHttpServiceTest.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/GraphQLHttpsServiceTest.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/GraphQLHttpsServiceTest.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/GraphQLTestHelper.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/GraphQLTestHelper.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/LogsAddressCapDataFetcherTest.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/LogsAddressCapDataFetcherTest.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/RangeBlockDataFetcherTest.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/RangeBlockDataFetcherTest.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/RangeLogsDataFetcherTest.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/RangeLogsDataFetcherTest.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/TransactionDataFetcherTest.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/TransactionDataFetcherTest.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/scalar/AddressScalarTest.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/scalar/AddressScalarTest.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/scalar/BigIntScalarTest.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/scalar/BigIntScalarTest.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/scalar/Bytes32ScalarTest.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/scalar/Bytes32ScalarTest.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/scalar/BytesScalarTest.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/scalar/BytesScalarTest.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/scalar/HashScalarTest.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/scalar/HashScalarTest.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/scalar/LogTopicScalarTest.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/scalar/LogTopicScalarTest.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/scalar/LongScalarTest.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/scalar/LongScalarTest.java` |
| `src/test/java/org/hyperledger/besu/plugins/graphql/scalar/VersionedHashScalarTest.java` | `ethereum/api/src/test/java/org/hyperledger/besu/ethereum/api/graphql/scalar/VersionedHashScalarTest.java` |
| _(none; replaced by plugin services)_ | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/GraphQLDataFetcherContext.java` |
| _(none; replaced by plugin services)_ | `ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/graphql/GraphQLDataFetcherContextImpl.java` |
| `src/test/resources/org/hyperledger/besu/plugins/graphql/*.json` (spec fixtures, byte-identical) | `ethereum/api/src/test/resources/org/hyperledger/besu/ethereum/api/graphql/*.json` |
