// 게이트웨이 프록시 호출 URL — ProxyController가 /gateway/{code}/** 를 받아
// api_products.base_url + 나머지 경로로 그대로 포워드한다 (gateway.md 프록시 흐름 참고).
// code는 product_id 대신 쓰는 상품별 슬러그(api_products.code)다.
export function gatewayCallUrl(code) {
  return `${window.location.protocol}//${window.location.hostname}:8080/gateway/${code}`
}
