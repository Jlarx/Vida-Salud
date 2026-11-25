#!/bin/bash
# ==============================================================================
# ⚡ VidaSalud - Inicio Inmediato (Sin Recompilar - ~30 segundos)
# ==============================================================================
cd "$(dirname "$0")"
chmod +x deploy-ec2.sh *.sh 2>/dev/null || true
./deploy-ec2.sh fast
