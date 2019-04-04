
custom replication agent that publishes the events to a SNS or SQS or files to S3 .
The config also has a property to enable embedding the content as a json with the message. Since the message has a finite size, we use a s3 bucket for uploading the json content on replication and the published sns message will have a path to the file in s3 bucket.
This way, we will be able to persist the content.
This pattern allows both the architecture to evolve independent of each other.
Now that the replication event is exposed as a SNS topic, other applications can subscribe to it and act accordingly.

Steps
1.Configure system user for aws-user

2.Configure service user mapping


3.install the bundle

4.configure com.adobe.ams.replication.aws.sqs.SQSReplicationConfig and com.adobe.ams.replication.aws.AWSReplicationConfig by following https://sling.apache.org/documentation/bundles/context-aware-configuration/context-aware-configuration.html

5.These allow different set up for different content paths.

6.The property names are in the source code


7.Create a custom replication agent ui (named sqs) by copying one of the other agents at /etc/replication/agents.author .


