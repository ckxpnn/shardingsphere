+++
title = "SQL Error Code"
weight = 1
chapter = true
+++

SQL error codes provide by standard `SQL State`, `Vendor Code` and `Reason`, which return to client when SQL execute error.

**the error codes are draft, still need to be adjusted.**

| SQL State | Vendor Code | Reason |
| --------- | ----------- | ------ |
| 01000     | 10000       | Circuit break open, the request has been ignored |
| 08000     | 10001       | The URL \`%s\` is not recognized, please refer to the pattern \`%s\` |
| 42000     | 10002       | Can not support 3-tier structure for actual data node \`%s\` with JDBC \`%s\` |
| 42000     | 10003       | Unsupported SQL node conversion for SQL statement \`%s\` |
| HY004     | 10004       | Unsupported conversion data type \`%s\` for value \`%s\` |
| HY004     | 10100       | Can not register driver, reason is: %s |
| 34000     | 10200       | Can not get cursor name from fetch statement |
| HY000     | 10300       | Could not support variable \`%s\` |
| HY004     | 10301       | Invalid value \`%s\` |
| 42000     | 11000       | You have an error in your SQL syntax: %s |
| 42000     | 11001       | configuration error |
| 42000     | 11002       | Resource does not exist |
| 42000     | 11003       | Rule does not exist |
| HY000     | 11004       | File access failed, reason is: %s |
| 42000     | 11200       | Can not support database \`%s\` in SQL translation |
| 42000     | 11201       | Translation error, SQL is: %s |
| 25000     | 11320       | Switch transaction type failed, please terminate the current transaction |
| 25000     | 11321       | JDBC does not support operations across multiple logical databases in transaction |
| 25000     | 11322       | Failed to create \`%s\` XA data source |
| 42S02     | 11400       | Can not get traffic execution unit |
| 42000     | 12000       | Unsupported command: %s |
| 44000     | 13000       | SQL check failed, error message: %s |
| HY000     | 14000       | The table \`%s\` of schema \`%s\` is locked |
| HY000     | 14001       | The table \`%s\` of schema \`%s\` lock wait timeout of %s ms exceeded |
| HY000     | 14010       | Can not find \`%s\` file for datetime initialize |
| HY000     | 14011       | Load datetime from database failed, reason: %s |
| HY000     | 15000       | Work ID assigned failed, which can not exceed 1024 |
| HY000     | 16000       | Can not find pipeline job \`%s\` |
| HY000     | 16001       | Failed to get DDL for table \`%s\` |
| 42S02     | 17000       | Single table \`%s\` does not exist |
| 42S02     | 17001       | Schema \`%s\` does not exist |
| 0A000     | 17002       | Can not drop schema \`%s\` because of contains tables |
| 0A000     | 17010       | DROP TABLE ... CASCADE is not supported |
| HY000     | 20000       | Sharding algorithm class \`%s\` should be implement \`%s\` |
| 44000     | 20001       | Can not get uniformed table structure for logic table \`%s\`, it has different meta data of actual tables are as follows: %s |
| 42S02     | 20002       | Can not get route result, please check your sharding rule configuration |
| 44000     | 20003       | Can not find table rule with logic tables \`%s\` |
| 44000     | 20010       | Sharding value can't be null in insert statement |
| HY004     | 20011       | Found different types for sharding value \`%s\` |
| 44000     | 20012       | Can not update sharding value for table \`%s\` |
| 0A000     | 20013       | Can not support operation \`%s\` with sharding table \`%s\` |
| 0A000     | 20014       | Can not support DML operation with multiple tables \`%s\` |
| 0A000     | 20015       | The CREATE VIEW statement contains unsupported query statement |
| 42000     | 20016       | %s ... LIMIT can not support route to multiple data nodes |
| 44000     | 20017       | PREPARE statement can not support sharding tables route to same data sources |
| 42000     | 20018       | INSERT INTO ... SELECT can not support applying key generator with absent generate key column |
| 44000     | 20019       | The table inserted and the table selected must be the same or bind tables |
| 44000     | 20020       | Inline sharding algorithms expression \`%s\` and sharding column \`%s\` do not match |
| 42S01     | 20030       | Index \`%s\` already exists |
| 42S02     | 20031       | Index \`%s\` does not exist |
| 44000     | 20032       | Actual tables \`%s\` are in use |
| 42S01     | 20033       | View name has to bind to %s tables |
| HY000     | 20050       | Routed target \`%s\` does not exist, available targets are \`%s\` |
| HY000     | 20051       | \`%s %s\` can not route correctly for %s \`%s\` |
| 42S02     | 20052       | Can not find data source in sharding rule, invalid actual data node \`%s\` |
| HY004     | 24000       | Encrypt algorithm \`%s\` initialize failed, reason is: %s |
| HY004     | 24001       | The SQL clause \`%s\` is unsupported in encrypt rule |
| 44000     | 24002       | Altered column \`%s\` must use same encrypt algorithm with previous column \`%s\` in table \`%s\` |
| 42000     | 24003       | Insert value of index \`%s\` can not support for encrypt |
| 44000     | 24004       | Can not find logic encrypt column by \`%s\` |
| HY004     | 25000       | Shadow column \`%s\` of table \`%s\` does not support \`%s\` type |
| 42000     | 25003       | Insert value of index \`%s\` can not support for shadow |
| HY004     | 30000       | Unknown exception: %s |
