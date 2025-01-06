package fr.emse.s3;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.File;
import java.nio.file.Paths;

public class UploadClient {

    public static void main(String[] args) {
        // Nome do bucket S3
        String bucketName = "mybucket00000000000000";

        // Caminho do arquivo CSV local
        String filePath = "/home/gustavo/aws-cloud - Copy/data-20221207.csv";

        // Extrai o nome do arquivo do caminho local
        String fileName = new File(filePath).getName();

        // Cria o cliente S3
        S3Client s3Client = S3Client.builder()
                .region(Region.US_EAST_1) // Altere para a região do seu bucket
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();

        // Faz o upload do arquivo
        uploadFileToS3(s3Client, bucketName, fileName, filePath);

        // Fecha o cliente S3
        s3Client.close();
    }

    public static void uploadFileToS3(S3Client s3Client, String bucketName, String s3Key, String filePath) {
        try {
            // Cria a requisição de upload
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key) // Usa o nome do arquivo como chave no S3
                    .build();

            // Faz o upload do arquivo
            s3Client.putObject(putObjectRequest, Paths.get(filePath));

            System.out.println("Arquivo " + filePath + " enviado com sucesso para " + bucketName + "/" + s3Key);
        } catch (S3Exception e) {
            System.err.println("Erro ao enviar arquivo para o S3: " + e.awsErrorDetails().errorMessage());
        }
    }
}