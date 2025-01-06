package fr.emse.ec2;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.InstanceType;

public class CreateEC2Instance {

    public static void main(String[] args) {

        String amiId = "ami-06b21ccaeff8cd686";
        Region region = Region.US_EAST_1; // Define a região

        // Cria o cliente EC2
        Ec2Client ec2 = Ec2Client.builder()
                .region(region)
                .build();

        // Configura a solicitação para criar a instância
        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .imageId(amiId) // ID da AMI
                .instanceType(InstanceType.T2_MICRO) // Tipo da instância
                .maxCount(1) // Número máximo de instâncias
                .minCount(1) // Número mínimo de instâncias
                .build();

        // Executa a solicitação e obtém a resposta
        RunInstancesResponse response = ec2.runInstances(runRequest);

        // Obtém o ID da instância criada
        String instanceId = response.instances().get(0).instanceId();

        System.out.println("Instância EC2 criada com ID: " + instanceId);
    }
}