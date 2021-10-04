# Kafka Connect



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

## Kafka Connect step-by step


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

