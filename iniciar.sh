#!/bin/bash
echo "Iniciando FinancasPro..."
DIR="$(cd "$(dirname "$0")" && pwd)"
java -jar "$DIR/target/financas.jar"
