#!/bin/bash
# ==========================================
# Script de inicializacao com variaveis de ambiente
# Edite os valores abaixo com suas credenciais reais
# ==========================================

export WASABI_ACCESS_KEY="coloque_sua_access_key_aqui"
export WASABI_SECRET_KEY="coloque_sua_secret_key_aqui"
export WASABI_REGION="us-east-1"
export WASABI_ENDPOINT="https://s3.wasabisys.com"
export WASABI_BUCKET_NAME="coloque_seu_bucket_name_aqui"

echo "Configuracoes definidas:"
echo "Endpoint: $WASABI_ENDPOINT"
echo "Bucket: $WASABI_BUCKET_NAME"
echo ""
echo "Iniciando a aplicacao Spring Boot..."
./mvnw spring-boot:run