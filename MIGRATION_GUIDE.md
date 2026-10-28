# Migration and data-preservation guide

## Important: do not uninstall the existing app yet

The original app stores its library in WebView `localStorage`. The native preview stores data separately in app-private SharedPreferences JSON. Android does not automatically expose the WebView database to the native repository. Uninstalling the old app or clearing its storage can delete the original library.

## Recommended safe transition

1. Keep the currently installed version until you have exported and verified a backup.
2. If the original UI exposes **Export backup**, export the JSON file to a user-accessible folder.
3. Preserve a second copy of the backup before installing a native build.
4. The native preview's backup importer now checks the original `knowledge-library-backup` format/version and validates package records before applying the merge. This has been matched to the original JavaScript source, but must still be tested with a real exported backup before relying on it.
5. Import the backup and verify package titles, nested concepts, favorites, and special characters before deleting the old installation.

## Compatibility limitations in this preview

- Backup compatibility is implemented from source inspection but has not been verified by an on-device round-trip with a real backup.
- SharedPreferences JSON is used as a temporary native persistence layer. A later migration can move it to Room after the data schema and migration tests are established.
- Keep application ID `com.knowledgelibrary` and the signing identity if updating an existing installation. The source ZIP does not contain a private signing key and should not.
- Never clear application data as a troubleshooting step until a verified backup exists.
