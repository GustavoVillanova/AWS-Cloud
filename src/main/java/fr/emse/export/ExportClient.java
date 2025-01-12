package fr.emse.export;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class ExportClient {

    private static final String CONSOLIDATED_BUCKET = "mybucket1111111111111"; 
    private static final String FINAL_FILE_NAME = "final-consolidated.csv";
    private static final S3Client s3Client = S3Client.builder()
            .region(Region.US_EAST_1) 
            .credentialsProvider(ProfileCredentialsProvider.create())
            .build();

    public static void main(String[] args) {
        try {
            
            List<String> objectKeys = listConsolidatedFiles(CONSOLIDATED_BUCKET);

            
            File finalCsv = new File(FINAL_FILE_NAME);
            try (CSVPrinter printer = new CSVPrinter(new FileWriter(finalCsv), CSVFormat.DEFAULT)) {
                
                printer.printRecord("Device Pair", "Avg Flow Duration", "Std Dev Flow Duration", "Avg Tot Fwd Pkts", "Std Dev Tot Fwd Pkts");

                
                for (String key : objectKeys) {
                    String fileContent = readConsolidatedFile(CONSOLIDATED_BUCKET, key);
                    
                    addDataToCsv(printer, fileContent);
                }
            }

            
            uploadFinalCsvToS3(CONSOLIDATED_BUCKET, FINAL_FILE_NAME, finalCsv);

            System.out.println("Arquivo consolidado final gerado e enviado para o S3: " + FINAL_FILE_NAME);
        } catch (Exception e) {
            System.err.println("Erro ao exportar os dados consolidados: " + e.getMessage());
        }
    }

    private static List<String> listConsolidatedFiles(String bucketName) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix("consolidated/") 
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);
        return response.contents().stream()
                .map(S3Object::key)
                .filter(key -> key.endsWith(".csv")) 
                .collect(Collectors.toList());
    }

    private static String readConsolidatedFile(String bucketName, String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                s3Client.getObject(request), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao ler o arquivo consolidado: " + key, e);
        }
    }

    private static void addDataToCsv(CSVPrinter printer, String fileContent) {
        try (BufferedReader reader = new BufferedReader(new StringReader(fileContent))) {
            
            reader.readLine();

            
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                printer.printRecord((Object[]) values);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao adicionar dados ao CSV final", e);
        }
    }

    private static void uploadFinalCsvToS3(String bucketName, String fileName, File file) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        s3Client.putObject(request, software.amazon.awssdk.core.sync.RequestBody.fromFile(file));
    }
}
