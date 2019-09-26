# Kafka test case

We have a service which retrieves batch of messages from arbitrary IDs range.
It accepts offset and batchSize.

We need to write service which:
- will act as multi instance application with selected leader
- if leader goes down other application in cluster should take over
- we should preserve sequence of writing into a kafka, all IDs should be sequential
- no duplicates

## Prerequisites

- zookeeper running locally
- kafka running locally
- mysql running locally

## Implementation details
For leader election we use Zookeeper and apache curator library which allows us to handle leader's election.
See ```LeaderElection.startLeaderElection``` method.

In case leader reelection we need to preserve id where we should start from in MySql. 

Before Kafka transaction we lock the counter for the topic so that any other producer would block here.
Inside Kafka transaction we save that last ID. If in case saving is failed then Kafka transaction fails as well and messages will not be seen by consumer.
 