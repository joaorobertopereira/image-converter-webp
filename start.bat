@echo off
REM ==========================================
REM Script de inicializacao com variaveis de ambiente
REM Edite os valores abaixo com suas credenciais reais
REM ==========================================

set WASABI_ACCESS_KEY=coloque_sua_access_key_aqui
set WASABI_SECRET_KEY=coloque_sua_secret_key_aqui
set WASABI_REGION=us-east-1
set WASABI_ENDPOINT=https://s3.wasabisys.com
set WASABI_BUCKET_NAME=coloque_seu_bucket_name_aqui

echo Configuracoes definidas:
echo Endpoint: %WASABI_ENDPOINT%
echo Bucket: %WASABI_BUCKET_NAME%
echo.
echo Iniciando a aplicacao Spring Boot...
call mvnw spring-boot:run