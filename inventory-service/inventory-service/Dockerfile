# Etapa 1: Construir o JAR da aplicação
FROM maven:3.9.5-eclipse-temurin-21 AS builder

# Definir o diretório de trabalho
WORKDIR /app

# Copiar o arquivo pom.xml e as dependências do projeto
COPY pom.xml ./
COPY src ./src

# Instalar as dependências e construir o artefato
RUN mvn clean package -DskipTests

# Etapa 2: Criar a imagem final para execução
FROM eclipse-temurin:21-jre

# Configurar o diretório de trabalho
WORKDIR /app

# Copiar o JAR gerado na etapa de construção
COPY --from=builder /app/target/*.jar app.jar

# Expor a porta da aplicação
EXPOSE 8082

# Configurar o comando de inicialização
CMD ["java", "-jar", "app.jar"]