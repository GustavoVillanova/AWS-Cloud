# 🌐 Projeto de Processamento e Consolidação de Dados de IoT

## 📝 Descrição

Este projeto visa resumir e consolidar os dados de tráfego de IoT gerados por dispositivos distribuídos geograficamente em uma infraestrutura AWS escalável. Ele utiliza buckets S3, funções Lambda, instâncias EC2 e filas SQS para processar e analisar dados com eficiência e resiliência.

---

## 🏢 Arquitetura

- **👤 UploadClient**: Faz o upload de arquivos CSV para o bucket S3 `mybucket0000000000/input`.
- **🔄 SummarizeWorker**: Processa arquivos CSV e gera resumos diários, armazenando-os em `mybucket0000000000/summaries`.
- **💻 EC2 ConsolidatorWorker**: Consolida os resumos, calculando métricas estatísticas e salvando os resultados consolidados em `mybucket1111111111111/consolidated`.
- **📊 Export Client**: Combina os dados consolidados e gera um arquivo CSV final disponível para download.

### Fluxo de Dados

1. [Dispositivos IoT] → [Upload Client] → [S3 Bucket `mybucket0000000000/input`]
2. [S3 Bucket] → [SQS Queue] → [Summarize Worker]
3. [Summarize Worker] → [S3 Bucket `mybucket0000000000/summaries`]
4. [S3 Bucket] → [EC2 ConsolidatorWorker]
5. [EC2 ConsolidatorWorker] → [S3 Bucket `mybucket1111111111111/consolidated`]
6. [S3 Bucket] → [Export Client] → [CSV Final]

### Justificativa dos Componentes AWS

- **S3**: Armazenamento confiável e escalável para arquivos CSV.
- **SQS**: Garantia de processamento resiliente e desacoplamento entre componentes.
- **Lambda**: Processamento eficiente e escalabilidade automática.
- **EC2**: Controle granular para consolidação de dados com altos volumes e cálculos intensivos.

---

## 🚀 Configuração e Execução

### Configuração Inicial

1. **Buckets S3**:
   - Crie os seguintes buckets e pastas dentro deles:
     - `mybucket0000000000/input`: Para armazenar arquivos CSV brutos enviados pelas filiais.
     - `mybucket0000000000/summaries`: Para armazenar os resumos gerados pelo Lambda Worker.
     - `mybucket1111111111111/consolidated`: Para armazenar os dados consolidados pelo EC2 Worker.

2. **Lambda Worker**:
   - Crie uma função Lambda que seja acionada sempre que novos arquivos forem adicionados ao bucket `mybucket0000000000/input`.
   - Certifique-se de que a função possui permissões para ler do bucket de entrada e gravar no bucket de resumos.

3. **Instância EC2**:
   - Configure uma instância EC2 com permissões para acessar os buckets S3 e a fila SQS.
   - Faça o upload do Consolidator Worker (arquivo .jar gerado pelo Maven) para a instância.
   - Configure o Consolidator Worker para iniciar automaticamente ao ligar a instância. Para isso, utilize o `systemd` ou o script `/etc/rc.local`.

4. **SQS Queue**:
   - Crie uma fila no SQS para gerenciar mensagens de processamento entre o Lambda Worker e o EC2 Worker.
   - Atualize o código do EC2 Worker para usar o URL da fila SQS criada.

5. **Credenciais AWS**:
   - Atualize as credenciais AWS na IDE Java para desenvolvimento local.
   - Adicione as credenciais na instância EC2 (use o arquivo `~/.aws/credentials`).

### Execução Regular

1. **Upload de Arquivos**:
   - Use o UploadClient para enviar arquivos CSV para `mybucket0000000000/input`. A aplicação solicita o nome e o caminho do arquivo.

2. **Monitoramento**:
   - Acompanhe os logs da execução do Lambda Worker no CloudWatch para verificar o processamento de novos arquivos.

3. **Consolidação de Dados**:
   - A instância EC2 monitora a fila SQS. Quando novas mensagens são recebidas, o Consolidator Worker processa os arquivos resumidos e grava os resultados no bucket `mybucket1111111111111/consolidated`.

4. **Exportação de Dados**:
   - Execute o Export Client para combinar os dados consolidados em um único arquivo CSV. O arquivo final será salvo em `mybucket1111111111111`.

5. **Validação**:
   - Verifique se os arquivos gerados no bucket de consolidação e o CSV final estão corretos e completos.

---

## 🔄 Componentes e Funções

### 📥 UploadClient

- **Função**: Faz upload de arquivos CSV para `mybucket0000000000/input`.
- **Configuração**: Arquivo e caminho definidos pelo usuário ou valores padrão.

### 🔧 SummarizeWorker

- **Função**: Processa arquivos CSV e gera resumos diários.
- **Saída**: Resumos armazenados em `mybucket0000000000/summaries`.

### 💻 EC2 ConsolidatorWorker

- **Comando**: `java -jar Consolidator-1.0-SNAPSHOT.jar`
- **Função**: Consolida resumos e calcula métricas (média e desvio padrão).
- **Saída**: Resultados armazenados em `mybucket1111111111111/consolidated`.

### 📊 Export Client

- **Função**: Gera um arquivo CSV com todos os dados consolidados.
- **Saída**: Arquivo final armazenado em `mybucket1111111111111`.

---

## 🔧 Ferramentas Utilizadas

- **AWS S3**: Armazenamento escalável.
- **AWS Lambda**: Processamento serverless.
- **AWS EC2**: Consolidação personalizada.
- **AWS SQS**: Gerenciamento de mensagens.
- **Java**: Linguagem principal.
- **Maven**: Gerenciamento de dependências e build.

---

## 🛠️ Recomendações

- **Monitoramento**: Use o CloudWatch para logs e alertas.
- **Otimização**: Exclua arquivos intermediários para reduzir custos de armazenamento.
- **Automatização**: Considere configurar gatilhos para automatizar o Export Client.