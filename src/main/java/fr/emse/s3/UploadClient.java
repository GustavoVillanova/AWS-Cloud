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
        String bucketName = "mybucket0000000000";

        String folderPath = "/home/gustavo/aws-cloud - Copy/base";

        S3Client s3Client = S3Client.builder()
                .region(Region.US_EAST_1) 
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();

        uploadFolderToS3(s3Client, bucketName, folderPath, "input/");

        s3Client.close();
    }

    public static void uploadFolderToS3(S3Client s3Client, String bucketName, String folderPath, String targetFolder) {
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("O caminho especificado não é uma pasta válida: " + folderPath);
            return;
        }

        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("Nenhum arquivo encontrado na pasta: " + folderPath);
            return;
        }

        for (File file : files) {
            if (file.isFile()) {
                String fileName = file.getName();
                String filePath = file.getAbsolutePath();
                String s3Key = targetFolder + fileName;
                try {
                    uploadFileToS3(s3Client, bucketName, s3Key, filePath);
                } catch (Exception e) {
                    System.err.println("Erro ao enviar o arquivo " + fileName + ": " + e.getMessage());
                }
            }
        }
    }

    public static void uploadFileToS3(S3Client s3Client, String bucketName, String s3Key, String filePath) {
        try {
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.putObject(putObjectRequest, Paths.get(filePath));

            System.out.println("Arquivo " + filePath + " enviado com sucesso para " + bucketName + "/" + s3Key);
        } catch (S3Exception e) {
            System.err.println("Erro ao enviar arquivo para o S3: " + e.awsErrorDetails().errorMessage());
        }
    }
}
