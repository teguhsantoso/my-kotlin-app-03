# Extract Table Statements into Entity Classes

This plan extracts the hardcoded `Table` definitions from `AppSchema.kt` into dedicated Kotlin data classes. This improves organization, provides type safety for database records, and makes the schema definition cleaner.

## Proposed Changes

### PowerSync Entities

I will create two new data classes to represent the database tables. Each class will include a companion object containing its PowerSync `Table` definition.

#### [NEW] [TaskEntity.kt](file:///C:/Users/teguh/AndroidStudioProjects/MyApplication/my-kotlin-app-03/app/src/main/java/com/example/myapplication/powersync/TaskEntity.kt)
- Represents a task in the local database.
- Includes the `Table` definition for the `tasks` table.

#### [NEW] [PendingOperation.kt](file:///C:/Users/teguh/AndroidStudioProjects/MyApplication/my-kotlin-app-03/app/src/main/java/com/example/myapplication/powersync/PendingOperation.kt)
- Represents an offline operation waiting to be synced.
- Includes the `Table` definition for the `pending_operations` table.
- **Fixes Typo**: Corrects `oepration` to `operation`.

---

### Schema Configuration

#### [MODIFY] [AppSchema.kt](file:///C:/Users/teguh/AndroidStudioProjects/MyApplication/my-kotlin-app-03/app/src/main/java/com/example/myapplication/powersync/AppSchema.kt)
- Updates `AppSchema` to reference `TaskEntity.table` and `PendingOperation.table` instead of defining them inline.

## Verification Plan

### Automated Tests
- I will run a build to ensure the new classes are correctly referenced and there are no syntax errors.

### Manual Verification
- Verify that the `AppSchema` object still contains the same table structure (names and columns) as before.
