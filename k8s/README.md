# apiverse — 로컬 k8s 배포 가이드

레지스트리 없이 로컬 클러스터에 이미지를 직접 로드해서 쓰는 구성입니다. 실제 운영 클러스터에 붙일 때는
`imagePullPolicy: Never` → `IfNotPresent`/`Always`로 바꾸고 이미지를 실제 레지스트리에 push해야 합니다.

## ⚠️ Docker Desktop 내장 Kubernetes는 쓰지 말 것

실제로 붙여본 결과, Docker Desktop의 내장 Kubernetes는 `docker build`가 쓰는 도커 엔진과 **완전히 분리된
자체 containerd**를 씁니다. `docker images`엔 보여도 k8s 노드는 그 이미지를 전혀 모르기 때문에
`imagePullPolicy: Never`든 뭐든 무조건 `ErrImageNeverPull`이 납니다. 로컬 registry로 우회하는 것도
안 됩니다 — Docker Desktop k8s는 모든 pull을 내부 `registry-mirror` 프록시로 강제 경유시키는데, 이 프록시가
HTTP(비-TLS) 레지스트리를 거부해서 그마저도 막힙니다. 이 프록시는 사용자가 설정할 방법이 없습니다.

**→ 로컬 테스트는 kind를 쓰세요.** kind는 노드가 그냥 평범한 docker 컨테이너라 `kind load docker-image`로
이미지가 확실하게 들어갑니다. 아래 가이드는 전부 kind 기준입니다.

## 0. kind 설치 & 클러스터 생성 (최초 1회)

```bash
winget install Kubernetes.kind   # 또는: choco install kind / go install sigs.k8s.io/kind@latest

kind create cluster --name apiverse --config k8s/kind-config.yaml
```

`k8s/kind-config.yaml`은 ingress-nginx가 붙을 수 있게 `ingress-ready=true` 라벨과 포트 매핑을 해둔 설정입니다.
**호스트 포트는 80/443이 아니라 8080/8443**입니다 — Docker Desktop 자체(`com.docker.backend.exe`)가 이미
호스트 80을 점유하고 있어서(이 머신에서 실제로 충돌 확인함) 8080/8443으로 뺐습니다. 80/443이 비어있는
머신이라면 `kind-config.yaml`의 `hostPort`를 80/443으로 바꿔써도 됩니다.

클러스터를 만들면 kubectl 컨텍스트가 자동으로 `kind-apiverse`로 전환됩니다.

## 스크립트로 한 번에 하기

`k8s/deploy.sh`가 이미지 빌드 → kind에 이미지 로드 → ingress-nginx 설치 → 매니페스트 적용 → 롤아웃 대기까지
자동으로 처리합니다(0단계의 클러스터 생성은 최초 1회 수동으로 해야 함).

```bash
./k8s/deploy.sh up               # 배포
./k8s/deploy.sh status            # 파드/서비스/ingress 상태 확인
./k8s/deploy.sh down              # deployment/service/ingress만 삭제 (namespace/postgres-pvc 보존 → 데이터 유지)
./k8s/deploy.sh down --wipe-data   # postgres-pvc/namespace까지 완전 삭제 (데이터 삭제됨)
```

아래는 스크립트가 내부적으로 하는 동작을 단계별로 풀어 쓴 것 — 스크립트가 실패하거나 직접 손보고 싶을 때 참고하세요.

## 1. 이미지 빌드

저장소 루트에서 실행 (gateway/admin은 `core` 모듈에 의존하므로 빌드 컨텍스트가 루트여야 함):

```bash
docker build -f gateway/Dockerfile         -t apiverse/gateway:local         .
docker build -f admin/Dockerfile           -t apiverse/admin:local           .
docker build -f event-consumer/Dockerfile  -t apiverse/event-consumer:local  .
docker build -f web/Dockerfile             -t apiverse/web:local             web
docker build -f console/Dockerfile         -t apiverse/console:local         console
```

## 2. kind 클러스터에 이미지 로드

```bash
kind load docker-image apiverse/gateway:local apiverse/admin:local apiverse/event-consumer:local apiverse/web:local apiverse/console:local --name apiverse
```

## 3. ingress-nginx 설치 (최초 1회만)

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=180s
```

## 4. 매니페스트 적용

```bash
kubectl apply -k k8s/
kubectl get pods -n apiverse -w
```

전부 `Running`/`1/1`이 될 때까지 확인. gateway/admin은 postgres가 완전히 뜰 때까지 initContainer에서
대기하므로 최초 기동 시 조금 걸릴 수 있습니다.

## 5. 도메인 연결

`/etc/hosts`(Windows: `C:\Windows\System32\drivers\etc\hosts`, 관리자 권한 필요)에 추가:

```
127.0.0.1  apiverse.local admin.apiverse.local
```

## 6. 확인

포트가 8080으로 매핑되어 있으니 URL에 포트를 붙여야 합니다:

```bash
curl http://apiverse.local:8080/api/products
curl http://admin.apiverse.local:8080/api/admin/products   # 401 나오면 정상 (인증 필요)
```

브라우저로 `http://apiverse.local:8080`(사용자 화면), `http://admin.apiverse.local:8080`(어드민 화면) 접속.
실제로 위 4개 경로(web root, console root, `/api/products`, `/gateway/{code}/**`) 전부 라이브 클러스터에 붙여서
정상 응답 확인함.

## minikube를 쓰는 경우

kind 대신 minikube를 쓴다면:

```bash
minikube image load apiverse/gateway:local
minikube image load apiverse/admin:local
minikube image load apiverse/web:local
minikube image load apiverse/console:local
minikube addons enable ingress
```

도메인 연결은 별도 터미널에서 `minikube tunnel`을 띄워두거나, `127.0.0.1` 대신 `minikube ip` 결과를
`/etc/hosts`에 적어야 합니다. minikube는 검증해보지 않았으니 (Docker Desktop 내장 k8s처럼) 이미지 공유가
안 될 가능성을 염두에 두세요.

## macOS + OrbStack을 쓰는 경우

OrbStack의 Kubernetes는 OrbStack 도커 엔진과 이미지 저장소를 공유하므로(공식 문서 기준) `docker build`만
하면 별도 로드 단계 없이 바로 보입니다. `deploy.sh`가 `orbstack` 컨텍스트를 자동 인식해서 이미지 로드를
생략하고, ingress-nginx도 cloud provider용 manifest로 설치합니다(OrbStack이 LoadBalancer Service를
자동으로 localhost에 매핑해줌). 포트도 8080이 아니라 80을 그대로 씁니다:

```bash
curl http://apiverse.local/api/products
```

다만 이 조합은 실측 검증은 안 했으니, 이미지가 안 보이거나(`ErrImageNeverPull`) ingress 파드가 안 뜨면
`kubectl get pods -n ingress-nginx`로 상태부터 확인하세요.

## 구조 메모

- `/api/**`, `/gateway/**` → Ingress가 경로 기준으로 `gateway` Service(8080)에 직접 라우팅. web 파드의 nginx.conf에 있는
  `/api` proxy_pass 블록(`host.docker.internal` 대상)은 로컬 `docker run` 단독 실행용이고, k8s에서는 Ingress가 먼저
  가로채서 실행되지 않는다.
- admin.apiverse.local의 `/api/**`도 마찬가지로 `admin` Service(8090)로 직접 라우팅.
- postgres는 PVC(`postgres-pvc`, 1Gi)로 데이터 보존, redis는 레이트리미터 캐시 용도라 휘발성(PVC 없음).
- billing_logs는 gateway → Kafka(`kafka` Service, 9092) → `event-consumer`가 배치로 모아 DB에 적재하는 구조로 바뀌었다.
  kafka는 단일 노드 KRaft 구성이고 PVC(`kafka-pvc`, 2Gi)로 토픽 데이터를 보존한다. gateway는 Kafka 발행 실패 시
  (`KafkaProducerConfig`의 `REQUEST_TIMEOUT_MS`/`MAX_BLOCK_MS` 3초 이내) `ProxyService`가 R2DBC 직접 저장으로
  자동 폴백하므로 kafka/event-consumer가 잠깐 죽어도 로그 유실은 없다. event-consumer는
  `spring.main.web-application-type=none`이라 HTTP 포트가 없어 Service/Ingress가 없고, gateway/admin과 마찬가지로
  롤아웃 시 재시작 대상에 포함된다(`deploy.sh`의 rollout restart 목록 참고).
- `TRUST_FORWARDED_HEADERS=true`가 gateway에 설정되어 있음 — nginx-ingress가 유일한 신뢰 프록시 홉이라는 전제.
  만약 이중 프록시(예: 클라우드 LB + nginx-ingress)를 추가로 두게 되면 `ProxyService.resolveClientIp`의
  홉 처리 로직을 다시 봐야 함.
- **모든 Deployment에 `enableServiceLinks: false`가 설정되어 있음.** k8s는 같은 네임스페이스의 모든 Service에
  대해 `<SERVICE명>_PORT` 같은 레거시 env를 파드에 자동 주입하는데, `admin` Service가 있으면 모든 파드에
  `ADMIN_PORT=tcp://10.x.x.x:8090` 같은 값이 주입됩니다. admin 앱 자신의 `application.properties`가
  `server.port=${ADMIN_PORT:8090}`을 쓰기 때문에 이 자동 주입 값이 그대로 덮어써져 부팅이 깨졌던 실제 사례가
  있었습니다(`NumberFormatException`). `enableServiceLinks: false`로 이 자동 주입 자체를 꺼서 근본 차단함.
- `k8s/01-secret.yaml`은 로컬 테스트용 평문 dev 자격 증명이다. 실제 배포 전엔 반드시 교체.

## 정리 (클러스터에서 제거)

`./k8s/deploy.sh down`을 쓰세요. `kubectl delete -k k8s/`를 직접 쓰면 `postgres-pvc`와 namespace가
kustomization에 포함돼 있어서 같이 지워지고, storageclass reclaimPolicy가 `Delete`라 postgres 데이터까지
삭제됩니다. `deploy.sh down`은 deployment/service/ingress만 지우고 namespace/PVC는 남겨서 데이터를 보존하며,
완전 초기화가 필요하면 `./k8s/deploy.sh down --wipe-data`로 PVC까지 지울 수 있습니다.

클러스터 자체를 지우려면:
```bash
kind delete cluster --name apiverse
```
