#!/usr/bin/env bash
# apiverse 로컬 k8s 배포/정리 스크립트. Git Bash / macOS / Linux 셸에서 실행.
# 사용법:
#   ./k8s/deploy.sh up      배포 (이미지 빌드 → ingress-nginx 설치 → 매니페스트 적용 → hosts 안내)
#   ./k8s/deploy.sh down    전부 삭제
#   ./k8s/deploy.sh status  파드/서비스/ingress 상태 확인
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NAMESPACE=apiverse
ACTION="${1:-up}"

log() { printf '\n\033[1;34m==> %s\033[0m\n' "$1"; }

check_context() {
    local ctx
    ctx="$(kubectl config current-context 2>/dev/null || true)"
    if [[ -z "$ctx" ]]; then
        echo "kubectl 컨텍스트를 확인할 수 없습니다. 클러스터가 떠 있는지 확인하세요." >&2
        exit 1
    fi
    log "현재 kubectl 컨텍스트: $ctx"
    case "$ctx" in
        docker-desktop) IMAGE_LOAD=none ;;
        kind-*)         IMAGE_LOAD=kind ;;
        minikube)       IMAGE_LOAD=minikube ;;
        *)
            echo "알 수 없는 컨텍스트($ctx)입니다. docker-desktop/kind/minikube 중 하나로 전환 후 다시 실행하세요." >&2
            exit 1
            ;;
    esac
}

build_images() {
    log "이미지 빌드"
    docker build -f "$REPO_ROOT/gateway/Dockerfile"  -t apiverse/gateway:local  "$REPO_ROOT"
    docker build -f "$REPO_ROOT/admin/Dockerfile"    -t apiverse/admin:local   "$REPO_ROOT"
    docker build -f "$REPO_ROOT/web/Dockerfile"      -t apiverse/web:local     "$REPO_ROOT/web"
    docker build -f "$REPO_ROOT/console/Dockerfile"  -t apiverse/console:local "$REPO_ROOT/console"
}

load_images() {
    case "$IMAGE_LOAD" in
        none)
            log "Docker Desktop은 도커 데몬을 공유하므로 이미지 로드 단계 생략"
            ;;
        kind)
            log "kind 클러스터에 이미지 로드"
            kind load docker-image apiverse/gateway:local apiverse/admin:local apiverse/web:local apiverse/console:local
            ;;
        minikube)
            log "minikube에 이미지 로드"
            for img in gateway admin web console; do
                minikube image load "apiverse/$img:local"
            done
            ;;
    esac
}

install_ingress_nginx() {
    if kubectl get ns ingress-nginx >/dev/null 2>&1; then
        log "ingress-nginx 이미 설치됨 — 건너뜀"
        return
    fi
    log "ingress-nginx 설치"
    local manifest_url
    case "$IMAGE_LOAD" in
        kind) manifest_url="https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml" ;;
        *)    manifest_url="https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml" ;;
    esac
    kubectl apply -f "$manifest_url"
    kubectl wait --namespace ingress-nginx \
        --for=condition=ready pod \
        --selector=app.kubernetes.io/component=controller \
        --timeout=180s
}

apply_manifests() {
    log "매니페스트 적용 (kustomize)"
    kubectl apply -k "$REPO_ROOT/k8s/"

    log "롤아웃 대기"
    for dep in postgres redis gateway admin web console; do
        kubectl -n "$NAMESPACE" rollout status "deployment/$dep" --timeout=180s
    done
}

print_hosts_hint() {
    log "hosts 파일 설정 필요 (관리자 권한)"
    echo "  Windows: C:\\Windows\\System32\\drivers\\etc\\hosts"
    echo "  macOS/Linux: /etc/hosts"
    echo "  127.0.0.1  apiverse.local admin.apiverse.local"

    if [[ "$IMAGE_LOAD" == "minikube" ]]; then
        echo
        echo "  minikube는 127.0.0.1 대신 'minikube ip' 결과를 쓰거나, 별도 터미널에서 'minikube tunnel'을 실행해야 합니다."
    fi
}

print_verify_hint() {
    log "확인"
    echo "  curl http://apiverse.local/api/products"
    echo "  curl http://admin.apiverse.local/api/admin/products   # 401/403이면 정상(인증 필요)"
}

do_up() {
    check_context
    build_images
    load_images
    install_ingress_nginx
    apply_manifests
    print_hosts_hint
    print_verify_hint
}

do_down() {
    log "리소스 삭제"
    kubectl delete -k "$REPO_ROOT/k8s/" --ignore-not-found
    echo
    echo "postgres-pvc는 kustomize delete로 안 지워질 수 있습니다. 완전 초기화하려면:"
    echo "  kubectl delete pvc postgres-pvc -n $NAMESPACE"
}

do_status() {
    kubectl -n "$NAMESPACE" get deploy,pod,svc
    echo
    kubectl -n "$NAMESPACE" get ingress
}

case "$ACTION" in
    up)     do_up ;;
    down)   do_down ;;
    status) do_status ;;
    *)
        echo "사용법: $0 {up|down|status}" >&2
        exit 1
        ;;
esac
