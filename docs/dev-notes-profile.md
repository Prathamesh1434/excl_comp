# Profile Feature Developer Notes

## Profile JSON Schema

The profile data is stored in a JSON file with the following schema:

```json
{
  "profileId": "uuid",
  "profileName": "string",
  "createdAt": "timestamp",
  "updatedAt": "timestamp",
  "saveFilePaths": "boolean",
  "dataFilePath": "string",
  "dataSheetName": "string",
  "filterFilePath": "string",
  "filterSheetName": "string",
  "sourceHeaderRows": [1, 2],
  "sourceConcatenationMode": "CONCATENATE",
  "rootExpression": { ... },
  "profileVersion": 1
}
```

The `rootExpression` is a nested structure of groups and rules that represents the filter logic.

## Profile Versioning

The `profileVersion` field is used to handle backward and forward compatibility of profiles. When a profile is loaded, its version is checked against the current version in the application. If there is a mismatch, a warning is shown to the user.

In the future, a migration mechanism can be implemented to automatically upgrade older profile versions to the latest schema.
