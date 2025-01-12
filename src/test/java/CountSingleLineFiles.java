import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class CountSingleLineFiles {

    private static final String INPUT_BUCKET = "mybucket0000000000"; // Nome do bucket de entrada
    private static final S3Client s3Client = S3Client.builder()
            .region(Region.US_EAST_1) // Ajuste para a região correta
            .credentialsProvider(ProfileCredentialsProvider.create())
            .build();

    public static void main(String[] args) {
        try {
            // Lista todos os arquivos no diretório summaries/
            List<String> objectKeys = listObjects(INPUT_BUCKET, "summaries/");

            int totalFiles = objectKeys.size();
            int singleLineFiles = 0;

            for (String key : objectKeys) {
                if (hasSingleLine(INPUT_BUCKET, key)) {
                    singleLineFiles++;
                }
            }

            System.out.println("Total de arquivos em summaries/: " + totalFiles);
            System.out.println("Arquivos com apenas uma linha de dados: " + singleLineFiles);
            System.out.printf("Percentual de arquivos com uma linha: %.2f%%\n", ((double) singleLineFiles / totalFiles) * 100);
        } catch (Exception e) {
            System.err.println("Erro ao verificar os arquivos: " + e.getMessage());
        }
    }

    private static List<String> listObjects(String bucketName, String prefix) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix) // Apenas arquivos em summaries/
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);
        return response.contents().stream()
                .map(S3Object::key)
                .filter(key -> key.endsWith(".csv")) // Apenas arquivos CSV
                .collect(Collectors.toList());
    }

    private static boolean hasSingleLine(String bucketName, String key) {
        GetObjectRequest request = GetObjectRequest.builder().bucket(bucketName).key(key).build();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                s3Client.getObject(request), StandardCharsets.UTF_8))) {
            // Ignora o cabeçalho
            reader.readLine();

            // Verifica se há apenas uma linha de dados
            return reader.readLine() != null && reader.readLine() == null;
        } catch (Exception e) {
            System.err.println("Erro ao processar o arquivo " + key + ": " + e.getMessage());
            return false; // Tratar como arquivo inválido
        }
    }
}