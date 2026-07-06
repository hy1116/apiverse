// 게이트웨이 프록시 호출 URL — ProxyController가 /gateway/{code}/** 를 받아
// api_products.base_url + 나머지 경로로 그대로 포워드한다 (gateway.md 프록시 흐름 참고).
// code는 product_id 대신 쓰는 상품별 슬러그(api_products.code)다.
// 포트는 하드코딩하지 않고 현재 페이지가 로드된 origin을 그대로 쓴다 — ingress가 '/'와 '/gateway'를
// 같은 host:port로 라우팅하므로(k8s/ingress.yaml 참고) 배포 환경(kind:8080, OrbStack/minikube:80)에
// 관계없이 항상 맞는 포트를 가리킨다.
export function gatewayCallUrl(code) {
  return `${window.location.origin}/gateway/${code}`
}
