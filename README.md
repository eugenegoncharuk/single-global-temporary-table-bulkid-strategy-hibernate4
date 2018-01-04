# Single Table Bulk Id Strategy for Hibernate 4

Bulk Id Strategy implementation for Hibernate 4 instead of every temporary Table creation per original Table.
This is very useful in cases when Database cannot provide Temporary Tables, for example Oracle.

## How to use

You need to add new property to override default strategies:

```
<property name="hibernate.hql.bulk_id_strategy" value="com.swissre.ini.hibernate.SingleGlobalTemporaryTableBulkIdStrategy"/>
```

You need to create new Table in your database:

```
CREATE TABLE HT_GLOBAL_TEMP_TABLE_IDS (ID CHAR(36), ENTITY_NAME VARCHAR(100));
```