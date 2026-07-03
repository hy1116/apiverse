# apiverse — 로컬 k8s 배포 가이드

레지스트리 없이 로컬 클러스터에 이미지를 직접 로드해서 쓰는 구성입니다. 실제 운영 클러스터에 붙일 때는
`imagePullPolicy: Never` → `IfNotPresent`/`Always`로 바꾸고 이미지를 실제 레지스트리에 push해야 합니다.

`kubectl config current-context`로 어떤 클러스터를 쓰고 있는지 먼저 확인하세요. 아래는 **Docker Desktop**
기준이고, kind/minikube를 쓴다면 2단계만 다르게 하면 됩니다(하단 참고).

## 스크립트로 한 번에 하기

`k8s/deploy.sh`가 아래 1~3단계(이미지 빌드 → 클러스터별 이미지 로드 → ingress-nginx 설치 → 매니페스트 적용 →
롤아웃 대기)를 자동으로 처리합니다. `kubectl config current-context`를 보고 docker-desktop/kind/minikube를
자동 판별합니다.

```bash
./k8s/deploy.sh up       # 배포
./k8s/deploy.sh status   # 파드/서비스/ingress 상태 확인
./k8s/deploy.sh down     # 전부 삭제
```

아래는 스크립트가 내부적으로 하는 동작을 단계별로 풀어 쓴 것 — 스크립트가 실패하거나 직접 손보고 싶을 때 참고하세요.

## 1. 이미지 빌드

저장소 루트에서 실행 (gateway/admin은 `core` 모듈에 의존하므로 빌드 컨텍스트가 루트여야 함):

```bash
docker build -f gateway/Dockerfile  -t apiverse/gateway:local .
docker build -f admin/Dockerfile    -t apiverse/admin:local   .
docker build -f web/Dockerfile      -t apiverse/web:local     web
docker build -f console/Dockerfile  -t apiverse/console:local console
```

Docker Desktop k8s는 도커 데몬을 그대로 공유하므로 이 단계만으로 클러스터에서 바로 이미지를 쓸 수 있습니다
(kind/minikube처럼 별도 로드 단계 불필요).

## 2. ingress-nginx 설치 (최초 1회만)

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml
kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=120s
```

Docker Desktop은 `LoadBalancer` 타입 Service를 자동으로 `localhost`에 매핑해주므로 위 cloud provider용
manifest로 충분합니다.

## 3. 매니페스트 적용

```bash
kubectl apply -k k8s/
```

```bash
kubectl get pods -n apiverse -w
```

전부 `Running`/`Ready`가 될 때까지 확인. gateway는 부팅 시 스키마를 생성하므로 postgres가 뜬 직후 몇 번
재시작할 수 있음(initContainer가 postgres 응답을 기다리긴 하지만 완전히 없어지진 않음).

## 4. 도메인 연결

`/etc/hosts`(Windows: `C:\Windows\System32\drivers\etc\hosts`, 관리자 권한 필요)에 추가:

```
127.0.0.1  apiverse.local admin.apiverse.local
```

## 5. 확인

```bash
curl http://apiverse.local/api/products
curl http://admin.apiverse.local/api/admin/products   # 401/403 나오면 정상 (인증 필요)
```

브라우저로 `http://apiverse.local`(사용자 화면), `http://admin.apiverse.local`(어드민 화면) 접속.

## kind / minikube를 쓰는 경우

1단계(이미지 빌드) 이후 이미지를 클러스터로 로드하는 단계가 추가로 필요합니다:

**kind**
```bash
kind load docker-image apiverse/gateway:local apiverse/admin:local apiverse/web:local apiverse/console:local
```

**minikube**
```bash
minikube image load apiverse/gateway:local
minikube image load apiverse/admin:local
minikube image load apiverse/web:local
minikube image load apiverse/console:local
```

ingress-nginx 설치도 provider별 manifest가 다릅니다:

**kind** (`ingress-ready=true` 라벨이 있는 kind 클러스터 설정 가정 — 없으면 kind 공식 문서의 "Ingress" 가이드 참고)
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=120s
```

**minikube**
```bash
minikube addons enable ingress
```

도메인 연결(4단계)도 환경별로 다릅니다:
- **kind**: `ingress-ready=true`로 만든 클러스터라면 `127.0.0.1`로 충분.
- **minikube**: 별도 터미널에서 `minikube tunnel` 실행 중이거나, `127.0.0.1` 대신 `minikube ip` 결과를 `/etc/hosts`에 적어야 함.

## 구조 메모

- `/api/**`, `/gateway/**` → Ingress가 경로 기준으로 `gateway` Service(8080)에 직접 라우팅. web 파드의 nginx.conf에 있는
  `/api` proxy_pass 블록(`host.docker.internal` 대상)은 로컬 `docker run` 단독 실행용이고, k8s에서는 Ingress가 먼저
  가로채서 실행되지 않는다.
- admin.apiverse.local의 `/api/**`도 마찬가지로 `admin` Service(8090)로 직접 라우팅.
- postgres는 PVC(`postgres-pvc`, 1Gi)로 데이터 보존, redis는 레이트리미터 캐시 용도라 휘발성(PVC 없음).
- `TRUST_FORWARDED_HEADERS=true`가 gateway에 설정되어 있음 — nginx-ingress가 유일한 신뢰 프록시 홉이라는 전제.
  만약 이중 프록시(예: 클라우드 LB + nginx-ingress)를 추가로 두게 되면 `ProxyService.resolveClientIp`의
  홉 처리 로직을 다시 봐야 함.
- `k8s/01-secret.yaml`은 로컬 테스트용 평문 dev 자격 증명이다. 실제 배포 전엔 반드시 교체.

## 정리 (클러스터에서 제거)

```bash
kubectl delete -k k8s/
```

`postgres-pvc`는 `kubectl delete -k`로 지워지지 않을 수 있으니 완전히 초기화하려면 별도 확인:
```bash
kubectl get pvc -n apiverse
```
