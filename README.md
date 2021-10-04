# Kafka Connect


## General Concepts

```xml
Source -> Kafka Connect Clusters -> Kakfa Topics -> Kafka Connect Clusters -> Sink 

Kakfa Connect Clusters have multiple loaded reusable java jar files which are basically connectors

Connectors + User Configuration => Tasks

A job can span multiple tasks 

We have a lot of readymade source and sink connectors available online.
The best place to find them is in the following url: 
https://www.confluent.io/product/connectors/

If not found here then search in Google 

```

## Kafka Source Connect with MySQL step-by step


### Download and install Kafka
```xml
Download Apache Kafka from the following URL:

https://kafka.apache.org/downloads

Extract the compressed file to a folder as per your choice.

Add the bin path of this folder to the PATH variable on your PC
For eg. on MAC 

Run the following command:
sudo nano /etc/paths

Now edit the file and add the PATH folder to this file: 
/Users/balaji/kafka_2.13-3.0.0/bin (in my case) but will vary as per user choice of the installation folder

Now save the file -> CTL+X in my case and press “Y” on the confirmation prompt. 

```

### Start the Zookeeper and Kafka servers
```xml
Go to the root of the folder where you have Kafka extracted. 
For eg. in my case: /Users/balaji/kafka_2.13-3.0.0/

Start Zookeeper: 
zookeeper-server-start.sh config/zookeeper.properties

Start Kafka server: 
kafka-server-start.sh config/server.properties

This will start both Zookeeper and Kafka with system-defined defaults. 

Let’s now create a topic on which data will be synced from the source using Kafka Connect cluster (worker nodes)

Create Topic 
kafka-topics.sh --topic mysql-source-topic --create --partitions 3 --replication-factor 1 --bootstrap-server localhost:9092

List to see if the topic has been created or not: 
kafka-topics.sh --list --bootstrap-server localhost:9092

```

### MySQL settings
```xml

MySQL settings:
Connection URL: localhost:3306
Database Name: test
Table Name: employee

Creation script:
CREATE TABLE `employee` (
  `id` bigint NOT NULL,
  `employeename` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

Insert sample Data:
INSERT INTO test.employee (id,employeename) VALUES
	 (1,'Balaji'),
	 (2,'Krithika'),
	 (3,'Havisha');


```

### Running the Kakfa Connect in standalone mode
```xml

Download the Kafka source connector

First download the Kafka source connector from the link:
https://www.confluent.io/product/connectors/

In my case my source was a JDBC driver which I download from 
https://www.confluent.io/hub/confluentinc/kafka-connect-jdbc

Next I unzipped it and moved it to a folder called connect-plugin inside my kafka installation folder. So the unzipped folder resided inside the below directory in my case. 
/Users/balaji/kafka_2.13-3.0.0/connect-plugin/confluentinc-kafka-connect-jdbc-10.2.3

Create the Kafka connect config:

Create a folder called connect-config inside my kafka installation folder. So in my case the folder looked like the below: 
/Users/balaji/kafka_2.13-3.0.0/connect-config

Here I created 2 files: 
worker.properties
mysql.properties 

Lets look at them one by one. 

worker.properties -> This is the main properties file for the worker and it contains the following configuration: 

bootstrap.servers=127.0.0.1:9092
key.converter=org.apache.kafka.connect.json.JsonConverter
key.converter.schemas.enable=true
value.converter=org.apache.kafka.connect.json.JsonConverter
value.converter.schemas.enable=true
internal.key.converter=org.apache.kafka.connect.json.JsonConverter
internal.key.converter.schemas.enable=true
internal.value.converter=org.apache.kafka.connect.json.JsonConverter
internal.value.converter.schemas.enable=true
offset.storage.file.filename=offsets/standalone.offsets
offset.flush.interval.ms=10000
plugin.path=/Users/balaji/kafka_2.13-3.0.0/connect-plugin/confluentinc-kafka-connect-jdbc-10.2.3

Note: Please create the offsets folder inside the /Users/balaji/kafka_2.13-3.0.0 folder for the connector to store the offsets file. This path must match the config settings in worker.properties file [offset.storage.file.filename]

mysql.properties -> This is the mysql source connector specific configuration and it contains the follow configuration: 

name=confluent-mysql-source
connector.class=io.confluent.connect.jdbc.JdbcSourceConnector
tasks.max=1
connection.url=jdbc:mysql://localhost:3306/test
connection.user=root
connection.password=<pwd>
# if topic prefix is needed we use the below
#topic.prefix=mysql-
mode=incrementing
incrementing.column.name=id
validate.non.null=false
table.types=TABLE, VIEW
poll.interval.ms=1000
table.whitelist=test.employee

Note: By default the topic name will be the name of the table unless a topic.prefix is given

Running the souce connector:
connect-standalone.sh connect-config/worker.properties connect-config/mysql.properties

For us to see if we are receiving the data from the source create a consumer to the topic and check: 
kafka-console-consumer.sh --topic employee --from-beginning --bootstrap-server localhost:9092

```

## Kafka Source Connect with MySQL CDC step-by step


```xml
MySQL query to run for CDC to work 

SET GLOBAL binlog_format = 'ROW';
SET GLOBAL binlog_row_image='FULL';
show variables like 'server_id';
set @@GLOBAL.gtid_mode=OFF_PERMISSIVE;
set @@GLOBAL.gtid_mode=ON_PERMISSIVE;
set @@GLOBAL.ENFORCE_GTID_CONSISTENCY=ON;
set @@GLOBAL.gtid_mode=ON;
set @@GLOBAL.binlog_rows_query_log_events=ON; 

show global variables like '%GTID%';


Download the Kafka source connector

First download the Kafka source connector from the link:
https://www.confluent.io/product/connectors/

In my case my source was a JDBC driver which I download from 
https://www.confluent.io/hub/debezium/debezium-connector-mysql/

Next I unzipped it and moved it to a folder called connect-plugin inside my kafka installation folder. So the unzipped folder resided inside the below directory in my case. 
/Users/balaji/kafka_2.13-3.0.0/connect-plugin/debezium-debezium-connector-mysql-1.6.0

Create the Kafka connect config:

Create a folder called connect-config inside my kafka installation folder. So in my case the folder looked like the below: 
/Users/balaji/kafka_2.13-3.0.0/connect-config

Here I created 2 files: 
worker.properties
mysqlcdc.properties 

Lets look at them one by one. 

worker.properties -> This is the main properties file for the worker and it contains the following configuration: 

# from more information, visit: http://docs.confluent.io/3.2.0/connect/userguide.html#common-worker-configs
bootstrap.servers=127.0.0.1:9092
key.converter=org.apache.kafka.connect.json.JsonConverter
key.converter.schemas.enable=true
value.converter=org.apache.kafka.connect.json.JsonConverter
value.converter.schemas.enable=true
# we always leave the internal key to JsonConverter
internal.key.converter=org.apache.kafka.connect.json.JsonConverter
internal.key.converter.schemas.enable=true
internal.value.converter=org.apache.kafka.connect.json.JsonConverter
internal.value.converter.schemas.enable=true
# this config is only for standalone workers
offset.storage.file.filename=offsets/standalone.offsets
offset.flush.interval.ms=10000
topic.creation.enable=true 
plugin.path=/Users/balaji/kafka_2.13-3.0.0/connect-plugin/debezium-debezium-connector-mysql-1.6.0

Note: Please create the offsets folder inside the /Users/balaji/kafka_2.13-3.0.0 folder for the connector to store the offsets file. This path must match the config settings in worker.properties file [offset.storage.file.filename]

mysqlcdc.properties -> This is the mysql source connector specific configuration and it contains the follow configuration: 

name=employee-connector
tasks.max=1
connector.class=io.debezium.connector.mysql.MySqlConnector
database.hostname=localhost
database.port=3306
database.user=root
database.password=<pwd>
database.server.id=1
database.server.name=localhost
database.include.list=test
database.history.kafka.bootstrap.servers=localhost:9092
database.history.kafka.topic=employeecdc
include.schema.changes=true
table.whitelist=test.employee


Note: By default 2 topics will be created. One will have the name set in the database.history.kafka.topic parameter. This will hold the complete DDL details and will monitor for changes in DDL if the parameter is set. Another will have the name set by the combinataion of "database.server.name"."schema-name"."table-name" 

For us to see if we are receiving the data from the source create a consumer to the topic and check: 
kafka-console-consumer.sh --topic employeecdc --from-beginning --bootstrap-server localhost:9092

kafka-console-consumer.sh --topic localhost.test.employee --from-beginning --bootstrap-server localhost:9092

```

```xml
References: 
https://www.confluent.io/hub/debezium/debezium-connector-mysql
https://debezium.io/documentation/reference/1.7/connectors/mysql.html 

```
