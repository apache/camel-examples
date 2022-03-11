package org.apache.camel.example.debezium.eventhubs.blob.producer;

import org.apache.camel.builder.RouteBuilder;

public class AzureEventHubsProducerToAzureBlobRouteBuilder extends RouteBuilder {

    @Override
    public void configure() {
        from("azure-eventhubs:?connectionString=RAW({{eventhubs.connectionString}})"
                + "&blobContainerName={{blob.containerName}}"
                + "&blobAccountName={{blob.accountName}}"
                + "&blobAccessKey=RAW({{blob.accessKey}})")
                // write our data to Azure Blob Storage but committing to an existing append blob
                .to("azure-storage-blob://{{blob.accountName}}/{{blob.containerName}}?operation=commitAppendBlob"
                        + "&accessKey=RAW({{blob.accessKey}})"
                        + "&blobName={{blob.blobName}}")
                .end();
    }
}
