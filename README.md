

1.Configure system user for aws-user

2.Configure service user mapping


3.install the bundle

4.configure com.adobe.ams.replication.aws.sqs.SQSReplicationConfig and com.adobe.ams.replication.aws.AWSReplicationConfig by following https://sling.apache.org/documentation/bundles/context-aware-configuration/context-aware-configuration.html

5.These allow different set up for different content paths.

6.The property names are in the source code


7.Create a custom replication agent ui (named sqs) by copying one of the other agents at /etc/replication/agents.author .


