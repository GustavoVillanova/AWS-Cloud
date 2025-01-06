package fr.emse.ec2;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

public class ManageEC2Instance {

    public static void main(String[] args) {

        String instanceId = "i-06cd2fd4ecd3341e9"; // Obtém o ID da instância do argumento
        Region region = Region.US_EAST_1; // Define a região

        // Cria o cliente EC2
        Ec2Client ec2 = Ec2Client.builder()
                .region(region)
                .build();

        // Configura a solicitação para verificar o status da instância
        DescribeInstanceStatusRequest statusRequest = DescribeInstanceStatusRequest.builder()
                .instanceIds(instanceId) // ID da instância
                .build();

        // Executa a solicitação e obtém a resposta
        DescribeInstanceStatusResponse statusResponse = ec2.describeInstanceStatus(statusRequest);

        // Verifica se a instância existe
        if (statusResponse.instanceStatuses().isEmpty()) {
            System.out.println("Instância não encontrada.");
            return;
        }

        // Obtém o estado atual da instância
        InstanceStatus status = statusResponse.instanceStatuses().get(0);
        String state = status.instanceState().name().toString();

        // Verifica o estado e toma a ação apropriada
        if (state.equals("stopped")) {
            // Inicia a instância se estiver parada
            StartInstancesRequest startRequest = StartInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();
            ec2.startInstances(startRequest);
            System.out.println("Instância iniciada: " + instanceId);
        } else if (state.equals("running")) {
            // Interrompe a instância se estiver em execução
            StopInstancesRequest stopRequest = StopInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();
            ec2.stopInstances(stopRequest);
            System.out.println("Instância interrompida: " + instanceId);
        } else {
            System.out.println("A instância está em um estado que não pode ser alterado: " + state);
        }
    }
}