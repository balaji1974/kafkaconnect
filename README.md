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

docker pull mysql  
ocker run --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=<pwd> -d mysql:latest 

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

### Kakfa Source Connector (MySQL) in standalone mode
```xml

Download the Kafka source connector

First download the Kafka source connector from the link:
https://www.confluent.io/product/connectors/

In my case my source was a JDBC driver which I download from 
https://www.confluent.io/hub/confluentinc/kafka-connect-jdbc

Next I unzipped it and moved it to a folder called connect-plugin inside my kafka installation folder. So the unzipped folder resided inside the below directory in my case. 
/Users/balaji/kafka_2.13-3.0.0/connect-plugin/confluentinc-kafka-connect-jdbc-10.2.3

Also make sure to copy the MySQL connection driver to the lib directory of the above folder.
In my case the driver was mysql-connector-java-8.0.26.jar

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

## Kafka Source Connector CDC (MySQL) in standalone mode


```xml
MySQL query to run for CDC to work 

SET GLOBAL binlog_format = 'ROW';
SET GLOBAL binlog_row_image='FULL';
set @@GLOBAL.gtid_mode=OFF_PERMISSIVE;
set @@GLOBAL.gtid_mode=ON_PERMISSIVE;
set @@GLOBAL.gtid_mode=ON;
set @@GLOBAL.enforce_gtid_consistency=ON;
set @@GLOBAL.binlog_rows_query_log_events=ON; 

show variables like 'server_id';
show global variables like '%GTID%';

But this method of adding will disappear if the mysql server restarts. 

To make it permenant add this to the my.cnf file located in /etc/ directory. My setting for my.cnf file is:
[mysqld]
binlog_format=ROW
binlog_row_image=FULL
binlog_rows_query_log_events=ON 
gtid_mode=OFF_PERMISSIVE
gtid_mode=ON_PERMISSIVE
gtid_mode=ON
enforce_gtid_consistency=ON

Save the file and restart mysql to make the changes effective. 

If you are using docker export this file first make the changes, save and import back to docker
Eg. 
docker cp mysql:/etc/my.cnf . - Import the file from docker to the local folder where container name is 'mysql'
docker cp my.cnf mysql:/etc/my.cnf - Export it back to the container named 'mysql'

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

The below command will run cdc source connectors and you can watch the data flow from MySQL 
connect-standalone.sh connect-config/worker.properties connect-config/mysqlcdc.properties

For us to see if we are receiving the data from the source create a consumer to the topic and check: 
kafka-console-consumer.sh --topic employeecdc --from-beginning --bootstrap-server localhost:9092 

kafka-console-consumer.sh --topic localhost.test.employee --from-beginning --bootstrap-server localhost:9092

```

## Kafka Sink Connector (Elasticsearch) in standalone mode

```xml
For regular elasticsearch install, setup and run, please refer to my elasticsearch documentation under my nosql repository 

Install Elasicsearch using docker
docker pull elasticsearch:7.14.1 -> Note version is mandatory as elastic does not support the 'latest' tag. 

docker network create elasticnetwork -> Create our own network to run elasticsearch inside docker

docker run -d --name elasticsearch --net elasticnetwork -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" elasticsearch:7.14.1

Query our server status by opening our rest client and running the follow URL:
GET localhost:9200/


For the sake of simplicy I will be using the same connector I used in my first example for streaming my data into the Kafka topic. 
From their I will sink that data into my elasticsearch. 

Next download the elastic sink connector from the below URL:
https://www.confluent.io/hub/confluentinc/kafka-connect-elasticsearch

Move the unzipped folder to our /Users/balaji/kafka_2.13-3.0.0/connect-plugin/ folder as below. 
Note: Pls move the entire folder and not the lib files alone. 

Create the Kafka elastic sink config:

Under thee folder called connect-config inside my kafka installation create create two property files. 
worker-elastic.properties
elasticsink.properties 
mysql.propeties (this will be the same file as my first example)

Lets look at them one by one. 

worker-elastic.properties -> This is the main properties file for the worker and it contains the following configuration: 

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
plugin.path=/Users/balaji/kafka_2.13-3.0.0/connect-plugin/confluentinc-kafka-connect-jdbc-10.2.3,/Users/balaji/kafka_2.13-3.0.0/connect-plugin/confluentinc-kafka-connect-elasticsearch-11.1.2


elasticsink.properties -> This is the elastic sink connector specific configuration and it contains the follow configuration: 

name=confluent-elastic-sink
connector.class=io.confluent.connect.elasticsearch.ElasticsearchSinkConnector
tasks.max=1
connection.url=http://localhost:9200
#connection.user=root
#connection.password=<pwd>
topics=mysql-employee
key.ignore=true
type.name=kafka-connect

mysql.properties -> Content is the same as the first example

ame=confluent-mysql-source
connector.class=io.confluent.connect.jdbc.JdbcSourceConnector
tasks.max=1
connection.url=jdbc:mysql://localhost:3306/test
connection.user=root
connection.password=STKVALUE
topic.prefix=mysql-
mode=incrementing
incrementing.column.name=id
validate.non.null=false
table.types=TABLE, VIEW
poll.interval.ms=1000
table.whitelist=test.employee

For us to see if we are receiving the data from the source create a consumer to the topic and check: 
kafka-console-consumer.sh --topic mysql-employee --from-beginning --bootstrap-server localhost:9092

The below command will run both the source and sink connectors and you can watch the data flow from MySQL to Elasticsearch
connect-standalone.sh connect-config/worker-elastic.properties connect-config/mysql.properties connect-config/elasticsink.properties

Note: if you need to run both your source and sink connectors in different instances then configure the property called rest.port in the worker.properties to differnt ports and run them seperately.  

```

## Kafka Source and Sink Connector (Elasticsearch - MySQL) CDC in standalone mode

```xml
For regular elasticsearch install, refer previous section


For the sake of simplicy I will be using the same mysql cdc connector I used in my second example for streaming my data into the Kafka topic. 
From their I will sink that data into my elasticsearch. 

Download and installation of mysql cdc source connector and elastic sink connector already discussed in the previous examples. 
Please follow that

Create the Kafka elastic sink config:

Under thee folder called connect-config inside my kafka installation create create three property files. 
worker-elasticcdc.properties 
mysqlcdc.propeties 
elasticsinkcdc.properties

Lets look at them one by one. 

worker-elasticcdc.properties -> This is the main properties file for the worker and it contains the following configuration: 

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
plugin.path=/Users/balaji/kafka_2.13-2.8.0/connect-plugin/debezium-debezium-connector-mysql-1.7.0,/Users/balaji/kafka_2.13-2.8.0/connect-plugin/confluentinc-kafka-connect-elasticsearch-11.1.2

mysqlcdc.propeties -> This will be the same as the mysql source cdc example before

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
include.schema.changes=false
table.whitelist=test.employee

eelasticsinkcdc.properties -> This is the elastic sink connector specific configuration and it contains the follow configuration: 

name=confluent-elastic-sink
connector.class=io.confluent.connect.elasticsearch.ElasticsearchSinkConnector
tasks.max=1
connection.url=http://localhost:9200
#connection.user=root
#connection.password=<pwd>
topics=localhost.test.employee
transforms=key
#transforms.unwrap.type=io.debezium.transforms.UnwrapFromEnvelope
transforms.key.type=org.apache.kafka.connect.transforms.ExtractField$Key
transforms.key.field=id
key.ignore=false
type.name=employee

The below command will run both the source and sink connectors and you can watch the data flow from MySQL to Elasticsearch
connect-standalone.sh connect-config/worker-elasticcdc.properties connect-config/mysqlcdc.properties connect-config/elasticsinkcdc.properties

For us to see if we are receiving the data from the source create a consumer to the topic and check: 
kafka-console-consumer --topic localhost.test.employee --from-beginning --bootstrap-server localhost:9092

Note: if you need to run both your source and sink connectors in different instances then configure the property called rest.port in the worker.properties to differnt ports and run them seperately.  

Connector Supported Transformations:
InsertField – Add a field using either static data or record metadata
ReplaceField – Filter or rename fields
MaskField – Replace a field with the valid null value for the type (zero or an empty string, for example)
HoistField – Wrap the entire event as a single field inside a struct or a map
ExtractField – Extract a specific field from struct and map and include only this field in the results
SetSchemaMetadata – Modify the schema name or version
TimestampRouter – Modify the topic of a record based on original topic and timestamp
RegexRouter – Modify the topic of a record based on original topic, a replacement string, and a regular expression

A transformation is configured using the following parameters:
transforms – A comma-separated list of aliases for the transformations
transforms.$alias.type – Class name for the transformation
transforms.$alias.$transformationSpecificConfig – Configuration for the respective transformation

```


## Kafka Source and Sink Connector (Elasticsearch - MySQL) CDC in distributed mode

```xml
For regular mysql & elasticsearch  install, refer previous section
For mysql configurations for CDC please refer to my previous section

Download and installation of mysql cdc source connector and elastic sink connector already discussed in the previous examples. 

Create the Kafka worker config:

Under thee folder called connect-config inside my kafka installation create a property file called. 
worker-elasticcdc.properties

Lets look the contents of this file. 

worker-elasticcdc.properties -> This is the main properties file for the worker and it contains the following configuration: 
bootstrap.servers=localhost:9092
group.id=cluster-1-distributed-cluster
key.converter=org.apache.kafka.connect.json.JsonConverter
value.converter=org.apache.kafka.connect.json.JsonConverter
key.converter.schemas.enable=false
value.converter.schemas.enable=false
offset.storage.topic=cluster-1-distributed-offsets
offset.storage.replication.factor=1
offset.storage.partitions=50
config.storage.topic=cluster-1-distributed-config
config.storage.replication.factor=1
config.storage.partitions=1
status.storage.topic=cluster-1-distributed-status
status.storage.replication.factor=1
status.storage.partitions=10
offset.flush.interval.ms=10000
rest.host.name=localhost
rest.port=8083
rest.advertised.host.name=127.0.0.1
rest.advertised.port=8083
plugin.path=/Users/balaji/kafka_2.13-2.8.0/connect-plugin/debezium-debezium-connector-mysql-1.7.0,/Users/balaji/kafka_2.13-2.8.0/connect-plugin/confluentinc-kafka-connect-elasticsearch-11.1.2
internal.value.converter=org.apache.kafka.connect.json.JsonConverter
internal.key.converter=org.apache.kafka.connect.json.JsonConverter
task.shutdown.graceful.timeout.ms=10000
offset.flush.timeout.ms=5000

With this in place we can start our connector in distributed mode using the command 
connect-distributed connect-config/worker-elasticcdc.properties

We can check if for registed source and sink in the connector using our REST client using the url: 
GET http://localhost:8083/connectors

Now lets register the source and the sink connectors (refer mysqlcdc.json & elasticsink.json files)
POST http://localhost:8083/connectors
{
    "name": "employee-mysql-source-connector",
    "config" : {
        "connector.class": "io.debezium.connector.mysql.MySqlConnector",
        "tasks.max": "1",
        "database.hostname": "localhost",
        "database.port": "3306",
        "database.user": "root",
        "database.password": <pwd>,
        "database.server.id": "1",
        "database.server.name": "localhost",
        "database.include.list": "test",
        "database.history.kafka.bootstrap.servers": "localhost:9092",
        "database.history.kafka.topic": "employeecdc",
        "include.schema.changes": "false",
        "table.inclue.list": "employee"
    }
}

POST http://localhost:8083/connectors
{
    "name": "employee-elastic-sink-connector",
    "config" : {
        "connector.class" : "io.confluent.connect.elasticsearch.ElasticsearchSinkConnector",
        "tasks.max": "1",
        "connection.url" : "http://localhost:9200",
        "topics": "localhost.test.employee",
        "transforms": "unwrap,key",
        "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
        "transforms.key.type": "org.apache.kafka.connect.transforms.ExtractField$Key",
        "transforms.key.field": "id",
        "key.ignore" :"false",
        "schema.ignore" : "true",
        "type.name" : "employee"
    }
}

After the connectors are registered if we access the same URL again: 
GET http://localhost:8083/connectors
we will get the following response: 
[
    "employee-mysql-source-connector",
    "employee-elastic-sink-connector"
]

This proves that our source and sink connectors are registered successfully. 

Next create a console consumer to check if changes in the database table are pused to Kakfa with the following command. 
kafka-console-consumer --topic localhost.test.employee --from-beginning --bootstrap-server localhost:9092

Note: if you need to run both your source and sink connectors in different instances then configure the property called rest.port in the worker.properties to differnt ports and run them seperately. 

Next check if the sink connector is receving the data correctly or not from the following URL: 
GET http://localhost:9200/localhost.test.employee/_search&size=50

```

## Delete a connector installed in distributed mode 
```xml
DELETE ttp://localhost:8083/connectors/<connector-name>

```

## worker.properties file explained 
```xml
# References https://docs.confluent.io/platform/current/connect/references/allconfigs.htm
# List of Common configurations
# -----------------------------

# List of host/port pairs to use for establishing the initial connection to the Kafka cluster. 
# Since these servers are just used for the initial connection to discover the full cluster membership, 
# this list need not contain the full set of servers (you may want more than one, though, in case a server is down) 
bootstrap.servers=localhost:9092

# This controls the format of the key data that will be written to Kafka for source connectors 
# or read from Kafka for sink connectors.
key.converter=org.apache.kafka.connect.json.JsonConverter

# This controls the format of the value data that will be written to Kafka for source connectors 
# or read from Kafka for sink connectors
value.converter=org.apache.kafka.connect.json.JsonConverter

# Used for converting key data for offsets and configs.
internal.key.converter=org.apache.kafka.connect.json.JsonConverter

# Used for converting value data for offsets and configs.
internal.value.converter=org.apache.kafka.connect.json.JsonConverter

# Interval at which to try committing offsets for tasks
offset.flush.interval.ms=10000

# Maximum number of milliseconds to wait for records to flush and partition offset data to be committed to offset storage 
# or cancel and resume in the next attempt
offset.flush.timeout.ms=5000

# The comma-separated list of paths to directories that contain Kafka Connect plugins
plugin.path=/Users/balaji/kafka_2.13-2.8.0/connect-plugin/debezium-debezium-connector-mysql-1.7.0,/Users/balaji/kafka_2.13-2.8.0/connect-plugin/confluentinc-kafka-connect-elasticsearch-11.1.2

# Hostname for the REST API, give the actual host name in production
rest.host.name=localhost

# Port for the REST API to listen on
rest.port=8083

# This is the hostname that will be given out to other Workers to connect to, do not set localhost in production
rest.advertised.host.name=localhost

# This is the port that will be given out to other Workers to connect to.
rest.advertised.port=8083

# Configures the listener used for communication between Workers. Valid values are either http or https
rest.advertised.listener=http

# Amount of time to wait for tasks to shutdown gracefully
task.shutdown.graceful.timeout.ms=10000


# Standalone worker configuration
# -------------------------------

# The file to store connector offsets in. By storing offsets on disk, a standalone process can be stopped 
# and started on a single node and resume where it previously left off.
offset.storage.file.filename=offsets/standalone.offsets


# Distributed worker configuration
# --------------------------------

# A unique string that identifies the Connect cluster group this Worker belongs to
# This is not applicable for sink connectors. For sink connectors, the group.id is created programmatically 
# using the prefix connect- and the connector name
group.id=cluster-1-distributed-cluster

# The name of the topic where connector and task configuration data are stored. 
# This must be the same for all Workers with the same group.id
config.storage.topic=cluster-1-distributed-config

# The replication factor used when Kafka Connects creates the topic used to store connector and task configuration data.
# Recommended 3 in production system
config.storage.replication.factor=1

# The name of the topic where connector and task configuration offsets are stored. 
# This must be the same for all Workers with the same group.id.
offset.storage.topic=cluster-1-distributed-offsets

# The replication factor used when Connect creates the topic used to store connector offsets. 
# This should always be at least 3 for a production system
offset.storage.replication.factor=1

# The number of partitions used when Connect creates the topic used to store connector offsets.
# Large value is recommended. Default is 25.
offset.storage.partitions=50

# The name of the topic where connector and task configuration status updates are stored. 
# This must be the same for all Workers with the same group.id. 
status.storage.topic=cluster-1-distributed-status

# The replication factor used when Connect creates the topic used to store connector and task status updates. 
# This should always be at least 3 for a production system
status.storage.replication.factor=1

# The number of partitions used when Connect creates the topic used to store connector and task status updates.
# Default value is 5
status.storage.partitions=10


These are the minimal needed configurations that need to be set for a Kafka connect worker

```


## Single Message Transformers - To complete Later (eg.)

```xml
Must be used only for simple transformation and must not be used for heavy processing.
If heavy transformation is needed then we need to use Kafka Streams as an intermediate layer

    
    "transforms": "convert_op_ts,convert_current_ts", 
    "transforms.convert_op_ts.type": "org.apache.kafka.connect.transforms.TimestampConverter$Value", 
    "transforms.convert_op_ts.target.type": "Timestamp",
    "transforms.convert_op_ts.field": "current_ts",
    "transforms.convert_op_ts.format": "yyyy-MM-dd HH:mm:ss.SSSSSS",
    "transforms.convert_current_ts.type": "org.apache.kafka.connect.transforms.TimestampConverter$Value",
    "transforms.convert_current_ts.target.type": "Timestamp",
    "transforms.convert_current_ts.field": "op_ts",
    "transforms.convert_current_ts.format": "yyyy-MM-dd HH:mm:ss.SSSSSS"
```

## Creating Custom Connectors 
Refer to the project in the java folder of my repo.  

## Interesting Kakfa connect UI available from the following URL
https://hub.docker.com/r/landoop/kafka-connect-ui

## Kafka Source and Sink Connector (MSSQL Server 1 to MSSQL Server 2) - Moving new records from one server to another - Distributed version 
### This project file is under the sqlserver-source-sink-connector folder

```xml
Download and installation of JDBC connector (both source and sink in one jar) from the following url
https://www.confluent.io/hub/confluentinc/kafka-connect-jdbc 

Extract the jar and move it to your connector plugin folder. In my case it is under: 
C:\kafka_2.13-2.8.1\connectors\plugin

Create the Kafka worker config file under the connector config folder. In my case it is under: 
C:\kafka_2.13-2.8.1\connectors\config\worker.properties

Lets look at the contents of this file. 

worker.properties -> This is the main properties file for the worker and it contains the following configuration: 
bootstrap.servers=localhost:9092
group.id=time-trn-cluster
key.converter=org.apache.kafka.connect.json.JsonConverter
value.converter=org.apache.kafka.connect.json.JsonConverter
offset.storage.topic=time-trn-offsets
offset.storage.replication.factor=1
offset.storage.partitions=50
config.storage.topic=time-trn-config
config.storage.replication.factor=1
config.storage.partitions=1
status.storage.topic=time-trn-status
status.storage.replication.factor=1
status.storage.partitions=10
offset.flush.interval.ms=10000
rest.host.name=localhost
rest.port=8083
rest.advertised.host.name=localhost
rest.advertised.port=8083
plugin.path=C:/kafka_2.13-2.8.1/connectors/plugin/confluentinc-kafka-connect-jdbc-10.2.5
internal.value.converter=org.apache.kafka.connect.json.JsonConverter
internal.key.converter=org.apache.kafka.connect.json.JsonConverter
internal.key.converter.schemas.enable=false
internal.value.converter.schemas.enable=false
task.shutdown.graceful.timeout.ms=10000
offset.flush.timeout.ms=5000

Now lets start our Zookeeper and Kafka server with the following commands: (issue it from the Kafka root directory)
bin\windows\zookeeper-server-start config\zookeeper.properties
bin\windows\kafka-server-start config\server.properties

With this in place we can start our connector in distributed mode using the command (issue it from the Kafka root directory)
bin\windows\connect-distributed connectors\config\worker.properties

We can check if for registed source and sink in the connector using our REST client using the url: 
GET http://localhost:8083/connectors

Now lets register the source and the sink connectors (source.json & sink.json files)

Source Connector
POST http://localhost:8083/connectors
{
    "name": "time-trn-source-connector", 
    "config": {
        "connector.class": "io.confluent.connect.jdbc.JdbcSourceConnector", 
        "dialect.name": "SqlServerDatabaseDialect", 
        "connection.url": "jdbc:sqlserver://<servername or ip>:1433;database=<database name>", 
        "connection.database" : "<database name>", 
        "connection.user": "<user id>",  
        "connection.password": "<password>",  
        "mode":"incrementing",
        "incrementing.column.name":"TRN_NO",
        "validate.non.null":"false",
        "table.types": "TABLE, VIEW",
        "poll.interval.ms":"1000",
        "table.whitelist":"TIME_TRN"
    }
}

Here I have the mode as 'incrementing' and the incrementing column as TRN_NO. Every time a new record is added the TRN_NO is autoincremented and 
this will automatically trigger an event to the kafka topic. The topic name is the name of the table in the table whitelist. 

Sink Connector 
POST http://localhost:8083/connectors
{"name": "core1-sink-connector",
    "config": {  
        "connector.class": "io.confluent.connect.jdbc.JdbcSinkConnector",  
        "tasks.max": "1",  
        "key.converter":"org.apache.kafka.connect.json.JsonConverter",
        "value.converter": "org.apache.kafka.connect.json.JsonConverter",
        "topics": "TIME_TRN", 
        "dialect.name": "SqlServerDatabaseDialect", 
        "connection.url": "jdbc:sqlserver://<server name>:1433;database=<database name>", 
        "connection.database" : "<database name>", 
        "connection.user": "<user id>",  
        "connection.password": "<password>",  
        "auto.create": "true",
        "transforms": "ReplaceField",
        "transforms.ReplaceField.type": "org.apache.kafka.connect.transforms.ReplaceField$Value",
        "transforms.ReplaceField.blacklist": "TRN_NO"
    }
}

Here I have made a transformation of the key field that comes in by removing it before inserting into the target table.
This is my specific requirements where the target tables key is autogenerated but it depends if you need it or not. 

After the connectors are registered if we access the same URL again: 
GET http://localhost:8083/connectors
we will get the following response: 
[
    "time-trn-source-connector",
    "core1-sink-connector"
]

This proves that our source and sink connectors are registered successfully. 

Next create a console consumer to check if records added in the database table are parsed to Kakfa with the following command. 
kafka-console-consumer --topic TIME_TRN --from-beginning --bootstrap-server localhost:9092

Note: if you need to run both your source and sink connectors in different instances then configure the property called rest.port in the worker.properties to differnt ports and run them seperately. 

Next check if the target database tables receives the data or not by doing an SQL query on the target table. 

```


## MS	SQL server CDC 
```xml
For MS SQL Server to enable CDC on a Database run the following:
USE <Datavase Name>
GO
EXEC sys.sp_cdc_enable_db
GO

For MS SQL Server to enable CDC on a Table run the following: 
USE <Database Name>
GO

EXEC sys.sp_cdc_enable_table
@source_schema = N'dbo',
@source_name   = N'MyTable', 
@role_name     = N'MyRole',  
@filegroup_name = N'MyDB_CT',
@supports_net_changes = 0
GO

MS SQL Connector configuration sample:
{
    "name": "inventory-connector", 
    "config": {
        "connector.class": "io.debezium.connector.sqlserver.SqlServerConnector", 
        "database.hostname": "192.168.99.100", 
        "database.port": "1433", 
        "database.user": "sa", 
        "database.password": "Password!", 
        "database.dbname": "testDB", 
        "database.server.name": "fullfillment", 
        "table.include.list": "dbo.customers", 
        "database.history.kafka.bootstrap.servers": "kafka:9092", 
        "database.history.kafka.topic": "dbhistory.fullfillment" 
    }
}
You can find a good example in this link
https://medium.com/@ankulwarganesh10/streaming-sql-server-cdc-with-apache-kafka-using-debezium-82d89aafb885

```


## Kafka CDC Source and JDBC Sink Connector (MSSQL Server 1 to MSSQL Server 2) - Moving any change records from one server to another - Distributed version 
### This project file is under the sqlserver-cdc-source-jdbc-sink-connector folder

```xml
Download and install the Debezium CDC source connector for SQL Server and JDBC Sink connector from the following urls: 
https://www.confluent.io/hub/debezium/debezium-connector-sqlserver
https://www.confluent.io/hub/confluentinc/kafka-connect-jdbc 

Extract the jar and move it to your connector plugin folder. In my case it is under: 
C:\kafka_2.13-2.8.1\connectors\plugin

Create the Kafka worker config file under the connector config folder. In my case it is under: 
C:\kafka_2.13-2.8.1\connectors\config\worker.properties

Lets look at the contents of this file. 

worker.properties -> This is the main properties file for the worker and it contains the following configuration: 
bootstrap.servers=localhost:9092
group.id=time-trn-cluster
key.converter=org.apache.kafka.connect.json.JsonConverter
value.converter=org.apache.kafka.connect.json.JsonConverter
key.converter.schemas.enable=true
value.converter.schemas.enable=true
internal.value.converter=org.apache.kafka.connect.json.JsonConverter
internal.key.converter=org.apache.kafka.connect.json.JsonConverter
internal.key.converter.schemas.enable=false
internal.value.converter.schemas.enable=false
offset.storage.topic=time-trn-offsets
offset.storage.replication.factor=1
offset.storage.partitions=50
config.storage.topic=time-trn-config
config.storage.replication.factor=1
config.storage.partitions=1
status.storage.topic=time-trn-status
status.storage.replication.factor=1
status.storage.partitions=10
offset.flush.interval.ms=10000
rest.host.name=localhost
rest.port=8083
rest.advertised.host.name=localhost
rest.advertised.port=8083
plugin.path=C:/kafka_2.13-2.8.1/connectors/plugin/debezium-debezium-connector-sqlserver-1.7.0,C:/kafka_2.13-2.8.1/connectors/plugin/confluentinc-kafka-connect-jdbc-10.2.5
task.shutdown.graceful.timeout.ms=10000
offset.flush.timeout.ms=5000


Now lets start our Zookeeper and Kafka server with the following commands: (issue it from the Kafka root directory)
bin\windows\zookeeper-server-start config\zookeeper.properties
bin\windows\kafka-server-start config\server.properties

With this in place we can start our connector in distributed mode using the command (issue it from the Kafka root directory)
bin\windows\connect-distributed connectors\config\worker.properties

We can check for registed source and sink connectors using our REST client from the url: 
GET http://localhost:8083/connectors

Now lets register the source and the sink connectors (source.json & sink.json files)

Source Connector
POST http://localhost:8083/connectors
{
    "name": "time-trn-source-connector",
    "config": {
        "connector.class": "io.debezium.connector.sqlserver.SqlServerConnector",
        "database.hostname": "<host name or ip>",
        "database.port": "1433",
        "database.user": "<user name>",
        "database.password": "<password>",
        "database.dbname": "<database name>",
        "database.server.name": "<server name>",
        "table.include.list": "dbo.time_trn",
        "database.history.kafka.bootstrap.servers": "localhost:9092",
        "database.history.kafka.topic": "dbhistory.time_trn",
        "transforms": "route, unwrap",
        "transforms.unwrap.type" : "io.debezium.transforms.ExtractNewRecordState",
        "transforms.unwrap.drop.tombstones" : "false",
        "transforms.route.type": "org.apache.kafka.connect.transforms.RegexRouter",
        "transforms.route.regex": "([^.]+)\\.([^.]+)\\.([^.]+)",
        "transforms.route.replacement": "$3"
    }
}

Here we will have to note the 2 transformers that I user. 
First one is the route transformer for overriding the default topic created by CDC connector which would be database.schema.tablenae with just the name of the table.
Next is the unwrap transformer which is used for easy transformation of data from CDC format to the JDBC consumer format. Also setting the tombstone to false 
means the delete columns would be sent as null records for the sink connectors to perfrom the delete query. 

Sink Connector 
POST http://localhost:8083/connectors
{"name": "time-trn-sink-connector",
    "config": {  
        "connector.class": "io.confluent.connect.jdbc.JdbcSinkConnector",  
        "tasks.max": "1",
        "topics": "TIME_TRN",
        "dialect.name": "SqlServerDatabaseDialect",
        "connection.url": "jdbc:sqlserver://hr-tst:1433;database=AACDB_PROD_2021_08_11",
        "connection.database" : "AACDB_PROD_2021_08_11",
        "connection.user": "sa",  
        "connection.password": "satest12$45",  
        "auto.create": "true",
        "insert.mode": "upsert",
        "drop.tombstones": "true",
        "delete.enabled": "true",
        "delete.retention.ms" : "100",
        "pk.fields": "TRN_NO",
        "pk.mode": "record_key",
        "schemas.enable":"true"
    }
}

Here insert mode upsert means that records will be either inserted or updated. 
Also delete enabled will perfrom the delete query for deleted records. 

After the connectors are registered if we access the same URL again: 
GET http://localhost:8083/connectors
we will get the following response: 
[
    "time-trn-source-connector",
    "time-trn-sink-connector"
]

This proves that our source and sink connectors are registered successfully. 

Next create a console consumer to check if records added in the database table are parsed to Kakfa with the following command. 
kafka-console-consumer --topic TIME_TRN --from-beginning --bootstrap-server localhost:9092

Note: if you need to run both your source and sink connectors in different instances then configure the property called rest.port in the worker.properties
 to differnt ports and run them seperately. 

Next check if the target database tables syncs the data as per the actions that take place on the source table (insert/update/delete). 

```


```xml
References: 
https://www.confluent.io/hub/debezium/debezium-connector-mysql
https://debezium.io/documentation/reference/1.7/connectors/mysql.html 
https://debezium.io/documentation/reference/1.7/connectors/sqlserver.html
https://towardsdatascience.com/stream-your-data-changes-in-mysql-into-elasticsearch-using-debizium-kafka-and-confluent-jdbc-b93821d4997b
https://medium.com/dana-engineering/streaming-data-changes-in-mysql-into-elasticsearch-using-debezium-kafka-and-confluent-jdbc-sink-8890ad221ccf
https://www.confluent.io/blog/kafka-connect-single-message-transformation-tutorial-with-examples/
https://www.cnblogs.com/lenmom/p/10763589.html
https://www.baeldung.com/kafka-connectors-guide
https://medium.com/@ankulwarganesh10/streaming-sql-server-cdc-with-apache-kafka-using-debezium-82d89aafb885
https://medium.com/@adrianedbertluman/syncing-sql-server-database-using-kafka-part-2-running-kafka-connect-3ebc8234bfe
```
