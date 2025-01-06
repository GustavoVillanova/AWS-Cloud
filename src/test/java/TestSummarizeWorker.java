import com.amazonaws.services.lambda.runtime.events.SQSEvent;

import fr.emse.lambda.SummarizeWorker;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.util.List;

public class TestSummarizeWorker {
    public static void main(String[] args) {
        // Cria uma instância da função Lambda
        SummarizeWorker worker = new SummarizeWorker();

        // Cria um evento SQS simulado
        SQSEvent event = new SQSEvent();
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setBody("{\"Records\":[{\"s3\":{\"bucket\":{\"name\":\"mybucket00000000000000\"},\"object\":{\"key\":\"data-20221207.csv\"}}}]}");
        event.setRecords(List.of(message));

        // Cria um contexto simulado
        Context context = new TestContext();

        // Executa a função Lambda
        worker.handleRequest(event, context);
    }

    // Classe de contexto simulada
    static class TestContext implements Context {
        @Override
        public String getAwsRequestId() {
            return "test-request-id";
        }

        @Override
        public String getLogGroupName() {
            return "test-log-group";
        }

        @Override
        public String getLogStreamName() {
            return "test-log-stream";
        }

        @Override
        public String getFunctionName() {
            return "TestSummarizeWorker";
        }

        @Override
        public String getFunctionVersion() {
            return "1"; // Versão simulada da função
        }

        @Override
        public String getInvokedFunctionArn() {
            return "arn:aws:lambda:us-east-1:123456789012:function:TestSummarizeWorker"; // ARN simulado
        }

        @Override
        public int getMemoryLimitInMB() {
            return 512; // Limite de memória simulado (512 MB)
        }

        @Override
        public int getRemainingTimeInMillis() {
            return 30000; // Tempo restante simulado (30 segundos)
        }

        @Override
        public LambdaLogger getLogger() {
            return new LambdaLogger() {
                @Override
                public void log(String message) {
                    System.out.println(message);
                }

                @Override
                public void log(byte[] message) {
                    System.out.println(new String(message));
                }
            };
        }

        @Override
        public CognitoIdentity getIdentity() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getIdentity'");
        }

        @Override
        public ClientContext getClientContext() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getClientContext'");
        }
    }
}