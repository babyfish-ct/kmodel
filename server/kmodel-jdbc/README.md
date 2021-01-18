# KModel对JDBC的扩展

kmodel-jdbc一个JDBC代理，此代理有两个功能
1. 改变SQL的行为。
2. 监听数据库的变更。

## 改变SQL行为
包含两方面，扩展SQL的能力，以及限制框架不期望的SQL用法。

### 功能扩展
1. 无论使用何种关系型数据库，均支持MySQL风格的
    ```
    insert into table(id, name) 
    values(?, ?), (?, ?)
    on duplicated key 
    update set name = upper(values(name))
    ```
    或Postgres风格的
    ```
    insert into table(id, name) 
    values(?, ?), (?, ?)
    on on conflict(id) 
    do update set name = upper(excluded.name)
    ```
    或
    ```
    insert into table(id, name) 
    values(?, ?), (?, ?)
    on on conflict(id) 
    do nothing
    ```
2. 无论使用何种关系型数据库，均可以在delete语句中使用join
3. （即将完成）delete语句支持动态cascade子句，比如
    ```
    delete from parent 
    where id = ?
    cascade by constraint 
        fk_child1_parent,
        fk_child2_parent
    ```
    
### 功能限制
1. insert必须插入主键，不支持自动编号。
请使用序列或更适合分布式系统开发的组建策略，例如，UUID，雪花id
2. insert和update不支持join多表操作
3. 不支持executeLargeBatch
4. 不只是autoCommit = false模式，Statement未经显式commit/rollback会导致close报错提示
5. 外键关系设置级联删除会导致报错提示，请使用扩展功能重的"delete ... cascade by constraint ..."语句替换

### 监听数据库
无论单独使用kmodel-jdbc，
还是和[seata](https://github.com/seata/seata)配合使用，
均能监听数据库的修改。
大致实现原理和seata类似，拦截DML，并在单个事务内植入额外的查询，
在数据库修改前后查询新旧数据，并通知应用程序。

此功能有两个用途
1. 用于和kmodel其它模块配合，实现数据库和redis的强一致性。
让业务系统可以充分利用redis缓存，包括对象关系缓存和业务缓存，系统会自动处理好一致性问题。
2. 向应用程序提供类似于数据库触发器的通知，让此能力的应用范围不再仅仅局限于
[seata](https://github.com/seata/seata)的分布式事务和本框架的redis一致性。

此功能有两种等效的实现机制
1. 如果kmodel-jdbc被单独使用，
或[seata](https://github.com/seata/seata)未处于分布式事务中，则由kmodel-jdbc独自完成；
2. 如果kmodel-jdbc和[seata](https://github.com/seata/seata)配合使用(需引入kmodel-seata依赖)，
且[seata](https://github.com/seata/seata)已处于分布式事务中，
则由[seata](https://github.com/seata/seata)辅助完成。