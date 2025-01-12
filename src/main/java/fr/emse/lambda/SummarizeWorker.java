package fr.emse.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class SummarizeWorker implements RequestHandler<SQSEvent, Void> {

    private static final String OUTPUT_BUCKET = "mybucket0000000000"; 
    private static final S3Client s3Client = S3Client.builder()
            .region(Region.US_EAST_1) 
            .build();

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        for (SQSEvent.SQSMessage msg : event.getRecords()) {
            try {
                
                String bucketName = msg.getBody().split("\"name\":\"")[1].split("\"")[0];
                String objectKey = msg.getBody().split("\"key\":\"")[1].split("\"")[0];

                String csvContent = readCsvFromS3(bucketName, objectKey);

                Map<String, Map<LocalDate, Summary>> summaries = processCsv(csvContent);

                saveSummariesToS3(summaries, objectKey);
            } catch (Exception e) {
                context.getLogger().log("Erro ao processar mensagem: " + e.getMessage());
            }
        }
        return null;
    }

    private String readCsvFromS3(String bucketName, String objectKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(s3Client.getObject(getObjectRequest), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao ler arquivo CSV do S3", e);
        }
    }

    private Map<String, Map<LocalDate, Summary>> processCsv(String csvContent) {
        Map<String, Map<LocalDate, Summary>> summaries = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new java.io.StringReader(csvContent))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .parse(reader);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");

            for (CSVRecord record : records) {
                String srcIp = record.get("Src IP");
                String dstIp = record.get("Dst IP");
                String timestamp = record.get("Timestamp");
                LocalDate date = LocalDate.parse(timestamp, formatter);
                long flowDuration = Long.parseLong(record.get("Flow Duration"));
                long totFwdPkts = Long.parseLong(record.get("Tot Fwd Pkts"));

                String devicePair = srcIp + "-" + dstIp;
                summaries.computeIfAbsent(devicePair, k -> new HashMap<>())
                        .computeIfAbsent(date, k -> new Summary())
                        .add(flowDuration, totFwdPkts);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar CSV", e);
        }

        return summaries;
    }

    private void saveSummariesToS3(Map<String, Map<LocalDate, Summary>> summaries, String originalObjectKey) {
        for (Map.Entry<String, Map<LocalDate, Summary>> entry : summaries.entrySet()) {
            String devicePair = entry.getKey();
            Map<LocalDate, Summary> dailySummaries = entry.getValue();

            for (Map.Entry<LocalDate, Summary> dailyEntry : dailySummaries.entrySet()) {
                LocalDate date = dailyEntry.getKey();
                Summary summary = dailyEntry.getValue();

                String outputKey = "summaries/" + devicePair + "/" + date + ".csv";
                String outputContent = String.format("Src IP,Dst IP,Date,Total Flow Duration,Total Tot Fwd Pkts\n%s,%s,%s,%d,%d",
                        devicePair.split("-")[0], devicePair.split("-")[1], date, summary.totalFlowDuration, summary.totalTotFwdPkts);

                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(OUTPUT_BUCKET)
                        .key(outputKey)
                        .build();

                s3Client.putObject(putObjectRequest, RequestBody.fromString(outputContent));
            }
        }
    }

    private static class Summary {
        private long totalFlowDuration = 0;
        private long totalTotFwdPkts = 0;

        public void add(long flowDuration, long totFwdPkts) {
            this.totalFlowDuration += flowDuration;
            this.totalTotFwdPkts += totFwdPkts;
        }
    }
}