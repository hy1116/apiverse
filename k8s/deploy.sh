#!/usr/bin/env bash
# apiverse 로컬 k8s 배포/정리 스크립트. Git Bash / macOS / Linux 셸에서 실행.
# 사용법:
#   ./k8s/deploy.sh up               배포 (이미지 빌드 → ingress-nginx 설치 → 매니페스트 적용 → hosts 안내)
#   ./k8s/deploy.sh down              deployment/service/ingress만 삭제 (namespace/postgres-pvc 보존 → 데이터 유지)
#   ./k8s/deploy.sh down --wipe-data  postgres-pvc/namespace까지 완전 삭제 (데이터 삭제)
#   ./k8s/deploy.sh status           파드/서비스/ingress 상태 확인
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NAMESPACE=apiverse
ACTION="${1:-up}"
PF_PID_FILE="$REPO_ROOT/k8s/.postgres-port-forward.pid"
PF_LOG_FILE="$REPO_ROOT/k8s/.postgres-port-forward.log"

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
        docker-desktop)
            # 실측 확인됨: Docker Desktop 내장 k8s는 docker build 이미지 저장소와 분리된 containerd를 쓰고,
            # 모든 pull이 내부 registry-mirror를 강제 경유해서 로컬 레지스트리 우회도 막힌다.
            # → 로컬 이미지가 절대 뜨지 않으니(ErrImageNeverPull) 여기서 진행하지 않는다.
            echo "Docker Desktop 내장 Kubernetes는 로컬 빌드 이미지를 못 읽습니다(k8s/README.md 'Docker Desktop' 절 참고)." >&2
            echo "kind를 쓰세요: kind create cluster --name apiverse --config k8s/kind-config.yaml" >&2
            exit 1
            ;;
        kind-*)         IMAGE_LOAD=kind; INGRESS_PORT=8080 ;;
        minikube)       IMAGE_LOAD=minikube; INGRESS_PORT=80 ;;
        orbstack)
            # OrbStack의 k8s는 OrbStack 도커 엔진과 같은 이미지 저장소를 공유한다(공식 문서 기준).
            # docker-desktop과 달리 별도 containerd로 분리돼 있지 않아 build만 하면 바로 보이므로 로드 단계 불필요.
            IMAGE_LOAD=none; INGRESS_PORT=80
            ;;
        *)
            echo "알 수 없는 컨텍스트($ctx)입니다. kind/minikube/orbstack 중 하나로 전환 후 다시 실행하세요." >&2
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
        kind)
            log "kind 클러스터에 이미지 로드"
            kind load docker-image apiverse/gateway:local apiverse/admin:local apiverse/web:local apiverse/console:local --name apiverse
            ;;
        minikube)
            log "minikube에 이미지 로드"
            for img in gateway admin web console; do
                minikube image load "apiverse/$img:local"
            done
            ;;
        none)
            log "이미지 저장소를 공유하는 환경(OrbStack 등) — 이미지 로드 단계 생략"
            ;;
    esac
}

install_ingress_nginx() {
    if kubectl get ns ingress-nginx >/dev/null 2>&1; then
        log "ingress-nginx 이미 설치됨 — 건너뜀"
        return
    fi
    log "ingress-nginx 설치"
    case "$IMAGE_LOAD" in
        minikube)
            minikube addons enable ingress
            return
            ;;
        none)
            # OrbStack은 LoadBalancer Service를 자동으로 localhost에 매핑해주므로 cloud provider용 manifest 사용.
            kubectl apply -f "https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml"
            ;;
        *)
            kubectl apply -f "https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml"
            ;;
    esac
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
    local suffix=""
    [[ "$INGRESS_PORT" != "80" ]] && suffix=":$INGRESS_PORT"
    echo "  curl http://apiverse.local${suffix}/api/products"
    echo "  curl http://admin.apiverse.local${suffix}/api/admin/products   # 401/403이면 정상(인증 필요)"
}

start_postgres_port_forward() {
    if [[ -f "$PF_PID_FILE" ]] && kill -0 "$(cat "$PF_PID_FILE")" 2>/dev/null; then
        log "postgres port-forward 이미 실행 중 (PID $(cat "$PF_PID_FILE"))"
        return
    fi
    log "postgres port-forward 시작 (localhost:5432)"
    nohup kubectl port-forward -n "$NAMESPACE" svc/postgres 5432:5432 \
        > "$PF_LOG_FILE" 2>&1 &
    echo $! > "$PF_PID_FILE"
    sleep 1
    if kill -0 "$(cat "$PF_PID_FILE")" 2>/dev/null; then
        echo "  postgres → localhost:5432 (PID $(cat "$PF_PID_FILE"), 로그: k8s/.postgres-port-forward.log)"
    else
        echo "postgres port-forward 시작 실패 — 로그 확인: k8s/.postgres-port-forward.log" >&2
        rm -f "$PF_PID_FILE"
    fi
}

stop_postgres_port_forward() {
    if [[ -f "$PF_PID_FILE" ]]; then
        local pid
        pid="$(cat "$PF_PID_FILE")"
        if kill -0 "$pid" 2>/dev/null; then
            log "postgres port-forward 종료 (PID $pid)"
            kill "$pid" 2>/dev/null || true
        fi
        rm -f "$PF_PID_FILE"
    fi
}

do_up() {
    check_context
    build_images
    load_images
    install_ingress_nginx
    apply_manifests
    start_postgres_port_forward
    print_hosts_hint
    print_verify_hint
}

do_down() {
    stop_postgres_port_forward

    if [[ "${1:-}" == "--wipe-data" ]]; then
        log "리소스 전체 삭제 (--wipe-data: postgres-pvc/namespace 포함 → 데이터 삭제됨)"
        kubectl delete -k "$REPO_ROOT/k8s/" --ignore-not-found
        kubectl delete ns "$NAMESPACE" --ignore-not-found
        return
    fi

    # namespace/postgres-pvc는 kustomization.yaml에 포함돼 있어 `kubectl delete -k`를 쓰면 같이 지워지고,
    # storageclass reclaimPolicy가 Delete라서 PVC 삭제 = 실데이터 삭제로 이어진다.
    # 그래서 기본 down은 워크로드(deployment/service/ingress)만 지우고 namespace/PVC는 남겨서 데이터를 보존한다.
    log "리소스 삭제 (namespace/postgres-pvc는 보존 — 데이터 유지)"
    kubectl delete -n "$NAMESPACE" deployment --all --ignore-not-found
    kubectl delete -n "$NAMESPACE" service --all --ignore-not-found
    kubectl delete -n "$NAMESPACE" ingress --all --ignore-not-found
    echo
    echo "postgres-pvc와 namespace는 보존했습니다 (데이터 유지). 완전 초기화하려면:"
    echo "  ./k8s/deploy.sh down --wipe-data"
}

do_status() {
    kubectl -n "$NAMESPACE" get deploy,pod,svc
    echo
    kubectl -n "$NAMESPACE" get ingress
    echo
    if [[ -f "$PF_PID_FILE" ]] && kill -0 "$(cat "$PF_PID_FILE")" 2>/dev/null; then
        echo "postgres port-forward: 실행 중 (PID $(cat "$PF_PID_FILE"), localhost:5432)"
    else
        echo "postgres port-forward: 꺼짐"
    fi
}

case "$ACTION" in
    up)     do_up ;;
    down)   do_down "${2:-}" ;;
    status) do_status ;;
    *)
        echo "사용법: $0 {up|down [--wipe-data]|status}" >&2
        exit 1
        ;;
esac
