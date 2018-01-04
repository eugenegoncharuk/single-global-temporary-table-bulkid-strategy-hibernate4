# Single Table Bulk Id Strategy for Hibernate 4

There are following implementations of strategies in Hibernate 4:
- PersistentTableBulkIdStrategy - this is used in cases, when database doesn't support Temporary Tables (Oracle)
- TemporaryTableBulkIdStrategy - this is used in cases, when database supports Temporary Tables

If you are using Oracle and you don't have previleges for Tables creation in runtime, then it is right place for you.

This implementation has been looked from another repo with the same problem for Hibernate 5+ and adopted for Hibernate 4. https://github.com/grimsa/hibernate-single-table-bulk-id-strategy

## How to use

You need to add new property to override default strategy:

```
<property name="hibernate.hql.bulk_id_strategy" value="com.swissre.ini.hibernate.SingleGlobalTemporaryTableBulkIdStrategy"/>
```

You need to create new Table in your database:

```
CREATE TABLE HT_GLOBAL_TEMP_TABLE_IDS (ID CHAR(36), ENTITY_NAME VARCHAR(100));
```
