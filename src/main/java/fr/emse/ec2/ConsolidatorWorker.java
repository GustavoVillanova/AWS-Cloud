package fr.emse.ec2;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class ConsolidatorWorker {

    private static final String INPUT_BUCKET = "mybucket0000000000"; 
    private static final String OUTPUT_BUCKET = "mybucket1111111111111"; 
    private static final S3Client s3Client = S3Client.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(ProfileCredentialsProvider.create())
            .build();

    public static void main(String[] args) {
        while (true) { 
            try {
                
                long startTime = System.currentTimeMillis();


                List<String> objectKeys = listObjects(INPUT_BUCKET);

                
                for (String key : objectKeys) {
                    String fileContent = readObject(INPUT_BUCKET, key);
                    Map<String, Metrics> metrics = processFile(fileContent);
                    saveConsolidatedData(OUTPUT_BUCKET, key, metrics);
                }

                long endTime = System.currentTimeMillis();
                System.out.println("Tempo total de execução: " + (endTime - startTime) + " ms");    

            } catch (Exception e) {
                System.err.println("Erro: " + e.getMessage());
            }
        }
    }

    private static List<String> listObjects(String bucketName) {
        
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix("summaries/") 
                .build();
    
        ListObjectsV2Response response = s3Client.listObjectsV2(request);
    
        return response.contents().stream()
                .map(S3Object::key)
                .filter(key -> key.endsWith(".csv"))
                .collect(Collectors.toList());
    }

    private static String readObject(String bucketName, String key) {
        GetObjectRequest request = GetObjectRequest.builder().bucket(bucketName).key(key).build();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                s3Client.getObject(request), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao ler o arquivo: " + key, e);
        }
    }

    private static Map<String, Metrics> processFile(String content) {
        Map<String, Metrics> metricsMap = new HashMap<>();
    
        try (BufferedReader reader = new BufferedReader(new java.io.StringReader(content))) {
            String header = reader.readLine(); 
    
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; 
                }
    
                
                String[] parts = line.split(",");
    
                
                if (parts.length != 5) {
                    throw new RuntimeException("Formato inválido: esperados 5 campos, mas encontrados " + parts.length + ". Linha: " + line);
                }
    
                String srcIp = parts[0].trim();
                String dstIp = parts[1].trim();
                String date = parts[2].trim();
                long flowDuration = Long.parseLong(parts[3].trim());
                long totFwdPkts = Long.parseLong(parts[4].trim());
    
                String devicePair = srcIp + "-" + dstIp;
                metricsMap.computeIfAbsent(devicePair, k -> new Metrics()).addData(flowDuration, totFwdPkts);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar o arquivo. Detalhes: " + e.getMessage(), e);
        }
    
        return metricsMap;
    }

    private static void saveConsolidatedData(String bucketName, String originalKey, Map<String, Metrics> metricsMap) {
        StringBuilder output = new StringBuilder("Device Pair,Avg Flow Duration,Std Dev Flow Duration,Avg Tot Fwd Pkts,Std Dev Tot Fwd Pkts\n");
        for (Map.Entry<String, Metrics> entry : metricsMap.entrySet()) {
            String devicePair = entry.getKey();
            Metrics metrics = entry.getValue();
            output.append(String.format("%s,%.2f,%.2f,%.2f,%.2f\n",
                    devicePair,
                    metrics.getAverageFlowDuration(),
                    metrics.getStdDevFlowDuration(),
                    metrics.getAverageTotFwdPkts(),
                    metrics.getStdDevTotFwdPkts()));
        }

        String cleanKey = originalKey.replaceFirst("^summaries/", "");
        String outputKey = "consolidated/" + cleanKey.replace(".csv", "-consolidated.csv");    

        PutObjectRequest request = PutObjectRequest.builder().bucket(bucketName).key(outputKey).build();
        s3Client.putObject(request, software.amazon.awssdk.core.sync.RequestBody.fromString(output.toString()));
    }

    static class Metrics {
        private final List<Long> flowDurations = new ArrayList<>();
        private final List<Long> totFwdPkts = new ArrayList<>();

        public void addData(long flowDuration, long totFwdPkt) {
            flowDurations.add(flowDuration);
            totFwdPkts.add(totFwdPkt);
        }

        public double getAverageFlowDuration() {
            return flowDurations.stream().mapToDouble(Long::doubleValue).average().orElse(0);
        }

        public double getStdDevFlowDuration() {
            double mean = getAverageFlowDuration();
            return Math.sqrt(flowDurations.stream().mapToDouble(d -> Math.pow(d - mean, 2)).average().orElse(0));
        }

        public double getAverageTotFwdPkts() {
            return totFwdPkts.stream().mapToDouble(Long::doubleValue).average().orElse(0);
        }

        public double getStdDevTotFwdPkts() {
            double mean = getAverageTotFwdPkts();
            return Math.sqrt(totFwdPkts.stream().mapToDouble(d -> Math.pow(d - mean, 2)).average().orElse(0));
        }
    }
}
